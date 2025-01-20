/*
 * Copyright © 2017-2025 Ocado (Ocava)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ocadotechnology.event.scheduling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Runnables;
import com.ocadotechnology.time.AdjustableTimeProvider;
import com.ocadotechnology.wrappers.Pair;

public class SourceTrackingEventSchedulerTest {
    private enum TestSchedulerType implements EventSchedulerType {
        T1, T2;
    }

    private SourceSchedulerTracker tracker = new SourceSchedulerTracker();
    private final SimpleDiscreteEventScheduler backingScheduler = new SimpleDiscreteEventScheduler(new EventExecutor(), Runnables.doNothing(), TestSchedulerType.T1, new AdjustableTimeProvider(0), true);
    private final SourceTrackingEventScheduler threadOneScheduler = new SourceTrackingEventScheduler(tracker, TestSchedulerType.T1, backingScheduler);
    private final SourceTrackingEventScheduler threadTwoScheduler = new SourceTrackingEventScheduler(tracker, TestSchedulerType.T2, backingScheduler);

    @BeforeEach
    public void setup() {
        backingScheduler.pause();
    }

    @Test
    void whenEventExecuted_thenCorrectSchedulerTypeSet() {
        List<EventSchedulerType> observedTypes = new ArrayList<>();
        threadOneScheduler.doAt(50, () -> observedTypes.add(tracker.getActiveSchedulerType()), "");
        threadTwoScheduler.doAt(100, () -> observedTypes.add(tracker.getActiveSchedulerType()), "");
        threadOneScheduler.doAt(150, () -> observedTypes.add(tracker.getActiveSchedulerType()), "");
        backingScheduler.unPause();

        List<TestSchedulerType> expectedTypes = new ArrayList<>();
        expectedTypes.add(TestSchedulerType.T1);
        expectedTypes.add(TestSchedulerType.T2);
        expectedTypes.add(TestSchedulerType.T1);
        Assertions.assertEquals(expectedTypes, observedTypes);
    }

    @Test
    void whenEventCancelled_thenEventNotExecuted() {
        List<Boolean> executed = new ArrayList<>();
        executed.add(false);
        Cancelable cancelable = threadOneScheduler.doAt(50, () -> executed.set(0, true), "");
        threadOneScheduler.doAt(25d, cancelable::cancel, "");
        backingScheduler.unPause();
        Assertions.assertFalse(executed.get(0), "event executed after cancellation");
    }

    @Test
    void whenSchedulerIsStopping_thenSchedulerShutsDownGracefullyByRunningAllDoNowsOnly() {
        List<Integer> completedEventNotifications = new ArrayList<>();

        threadOneScheduler.doAt(50,
                () -> Assertions.fail("prepareToStop should be called first, and we don't run doAt while stopping"),
                "doAt before prepareToStop");
        threadOneScheduler.doNow(
                () -> completedEventNotifications.add(1),
                "doNow before prepareToStop");
        threadOneScheduler.doNow(
                threadOneScheduler::prepareToStop,
                "Calling prepareToStop");
        threadOneScheduler.doNow(() -> {
                    completedEventNotifications.add(2);  // make sure we've been called
                    Cancelable ev = threadOneScheduler.doAt(100, () -> Assertions.fail("Should never be scheduled"), "fail(1)");
                    Assertions.assertNull(ev, "If stopping, then calling doAt should be rejected");
                },
                "doNow to add doAt after call to prepareToStop");
        threadOneScheduler.doNow(() -> {
                    completedEventNotifications.add(3);  // make sure we've been called
                    Cancelable ev = threadOneScheduler.doNow(() -> completedEventNotifications.add(30), "success(30)");
                    Assertions.assertNotNull(ev, "If stopping, then calling doNow should be fine");
                },
                "doNow to add doNow after call to prepareToStop");
        threadOneScheduler.doNow(() -> {
                    completedEventNotifications.add(4);  // make sure we've been called
                    threadOneScheduler.stop();
                },
                "Calling stop");
        threadOneScheduler.doNow(() -> {
                    // At this point, we've called prepareToStop, then Stop, so we're flushing out existing
                    // doNow events.  This event was already scheduled, so we're ok to run it, and because
                    // we're still flushing, we're ok to add more doNow events:
                    completedEventNotifications.add(5);  // make sure we've been called
                    Cancelable ev = threadOneScheduler.doNow(() -> completedEventNotifications.add(50), "success(50)");
                    Assertions.assertNotNull(ev, "If stopping, then calling doNow should be fine");
                },
                "doNow to add doNow after call to stop");
        backingScheduler.unPause();

        // Now that we've stopped, all events are rejected
        Cancelable nowEvent = threadOneScheduler.doNow(() -> Assertions.fail("Should not be called after a stop"), "Post-stop event");
        Assertions.assertNull(nowEvent, "Can't add doNow events once stopped");
        Cancelable atEvent = threadOneScheduler.doAt(50, () -> Assertions.fail("Should not be called after a stop"), "Post-stop event");
        Assertions.assertNull(atEvent, "Can't add doAt events once stopped");

        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 30, 50), completedEventNotifications);
    }

    @Test
    void whenSchedulerStopped_thenSchedulerJustShutsDown() {
        List<Integer> completedEventsNotifications = new ArrayList<>();

        threadOneScheduler.doAt(50, () -> Assertions.fail("Should never be run"), "Stop before we run any doAt");
        threadOneScheduler.doNow(() -> {
                    completedEventsNotifications.add(1);
                },
                "doNow before call to stop");
        threadOneScheduler.doNow(() -> {
                    // This is executed before we call stop, but scheduled to run after we call stop
                    completedEventsNotifications.add(2);  // make sure we've been called
                    Cancelable ev = threadOneScheduler.doNow(() -> Assertions.fail("Nothing runs after stop called"), "After stopped event");
                    Assertions.assertNotNull(ev, "Adding doNow before calling stop is fine");
                },
                "doNow for a later doNow");
        threadOneScheduler.doNow(() -> {
                    completedEventsNotifications.add(3);  // make sure we've been called
                    threadOneScheduler.stop();
                },
                "Stop");
        threadOneScheduler.doNow(() -> {
                    Assertions.fail("Executed after stop called, so should not be called");
                },
                "doNow after call to stop");
        backingScheduler.unPause();

        // Now that we've stopped, all events are rejected
        Cancelable nowEvent = threadOneScheduler.doNow(() -> Assertions.fail("Should not be called after a stop"), "Post-stop event");
        Assertions.assertNull(nowEvent, "Can't add doNow events once stopped");
        Cancelable atEvent = threadOneScheduler.doAt(50, () -> Assertions.fail("Should not be called after a stop"), "Post-stop event");
        Assertions.assertNull(atEvent, "Can't add doAt events once stopped");

        Assertions.assertEquals(Arrays.asList(1, 2, 3), completedEventsNotifications);
    }

    /**
     * Basic tests to demonstrate that thread pauses affect both doAt and doNow events on the specific thread being
     * paused
     */
    @Nested
    @DisplayName("Thread pause timing tests")
    class ThreadPauseTimingTests {
        private final List<Pair<String, Double>> eventTimeList = new ArrayList<>();

        @Test
        @DisplayName("On thread event is delayed")
        void whenThreadPaused_thenEventOnThreadIsDelayed() {
            double pauseTime = threadOneScheduler.getTimeProvider().getTime() + 10_000;
            double eventTime = pauseTime + 4321;
            double pauseEndTime = eventTime + 12_345;

            threadOneScheduler.doAt(pauseTime, () -> threadOneScheduler.delayExecutionUntil(pauseEndTime));
            threadOneScheduler.doAt(eventTime, () -> eventTimeList.add(Pair.of("EVENT", threadOneScheduler.getTimeProvider().getTime())));
            backingScheduler.unPause();

            Assertions.assertEquals(ImmutableList.of(Pair.of("EVENT", pauseEndTime)), ImmutableList.copyOf(eventTimeList), "Incorrect events recorded");
        }

        @Test
        @DisplayName("On thread doNow event is delayed")
        void whenThreadPaused_thenDoNowEventOnThreadIsDelayed() {
            double pauseTime = threadOneScheduler.getTimeProvider().getTime() + 10_000;
            double eventTime = pauseTime + 4321;
            double pauseEndTime = eventTime + 12_345;

            threadOneScheduler.doAt(pauseTime, () -> threadOneScheduler.delayExecutionUntil(pauseEndTime));
            threadTwoScheduler.doAt(eventTime, () ->
                    threadOneScheduler.doNow(() -> eventTimeList.add(Pair.of("EVENT", threadOneScheduler.getTimeProvider().getTime())))
            );
            backingScheduler.unPause();

            Assertions.assertEquals(ImmutableList.of(Pair.of("EVENT", pauseEndTime)), ImmutableList.copyOf(eventTimeList), "Incorrect events recorded");
        }

        @Test
        @DisplayName("Off thread event is not delayed")
        void whenThreadPaused_thenEventOnOtherThreadIsExecuted() {
            double pauseTime = threadTwoScheduler.getTimeProvider().getTime() + 10_000;
            double eventTime = pauseTime + 4321;
            double pauseEndTime = eventTime + 12_345;

            threadOneScheduler.doAt(pauseTime, () -> threadOneScheduler.delayExecutionUntil(pauseEndTime));
            threadTwoScheduler.doAt(eventTime, () -> eventTimeList.add(Pair.of("EVENT", threadTwoScheduler.getTimeProvider().getTime())));
            backingScheduler.unPause();

            Assertions.assertEquals(ImmutableList.of(Pair.of("EVENT", eventTime)), ImmutableList.copyOf(eventTimeList), "Incorrect events recorded");
        }
    }

    /**
     * More advanced tests to assert that events will be execute in the expected sequence when a thread pause happens.
     *
     * A scheduler has two event queues - the doNow and doAt queues.  When executing events the doNow queue will be used
     * until it is empty, then the doAt queue will be polled from the earliest event ot the latest.
     *
     * If a thread is experiencing a thread pause (due to GC or similar), the events will still be paused and executed
     * in this sequence, even if some doNow events were from another thread after the doAt event should have executed.
     *
     * Note: all cross-thread communication should ideally be performed using the doNow mechanism.  Using doAt can
     * result in slightly different behaviour between the DES and realtime cases when the thread is paused.
     */
    @Nested
    @DisplayName("Thread pause sequence tests")
    class ThreadPauseSequenceTests {
        private final List<String> eventList = new ArrayList<>();

        @Test
        @DisplayName("Delayed doNow event executes before scheduled doAt event")
        void whenThreadPaused_thenDelayedDoNowEventIsExecutedBeforeDoAt() {
            double pauseTime = threadOneScheduler.getTimeProvider().getTime() + 10_000;
            double eventTime = pauseTime + 4321;
            double pauseEndTime = eventTime + 12_345;

            threadOneScheduler.doAt(pauseTime, () -> threadOneScheduler.delayExecutionUntil(pauseEndTime));
            threadTwoScheduler.doAt(eventTime, () -> scheduleDoNow(threadOneScheduler, "DO_NOW"));
            threadOneScheduler.doAt(pauseEndTime, () -> eventList.add("DO_AT"));
            backingScheduler.unPause();

            ImmutableList<String> expectedEvents = ImmutableList.of("DO_NOW", "DO_AT");
            Assertions.assertEquals(expectedEvents, ImmutableList.copyOf(eventList), "Incorrect events recorded");
        }

        @Test
        @DisplayName("Delayed doNow event executes before delayed doAt event")
        void whenThreadPaused_thenDelayedDoNowEventIsExecutedBeforeDelayedDoAt() {
            double pauseTime = threadOneScheduler.getTimeProvider().getTime() + 10_000;
            double doAtTime = pauseTime + 4321;
            double doNowTime = doAtTime + 4321;
            double pauseEndTime = doNowTime + 12_345;

            threadOneScheduler.doAt(pauseTime, () -> threadOneScheduler.delayExecutionUntil(pauseEndTime));
            threadOneScheduler.doAt(doAtTime, () -> eventList.add("DO_AT"));
            threadTwoScheduler.doAt(doNowTime, () -> scheduleDoNow(threadOneScheduler, "DO_NOW"));
            backingScheduler.unPause();

            ImmutableList<String> expectedEvents = ImmutableList.of("DO_NOW", "DO_AT");
            Assertions.assertEquals(expectedEvents, ImmutableList.copyOf(eventList), "Incorrect events recorded");
        }

        @Test
        @DisplayName("Delayed doAt event executes before scheduled doAt event")
        void whenThreadPaused_thenDelayedDoAtEventIsExecutedBeforeScheduledDoAt() {
            double pauseTime = threadOneScheduler.getTimeProvider().getTime() + 10_000;
            double doAtTime = pauseTime + 4321;
            double pauseEndTime = doAtTime + 12_345;

            threadOneScheduler.doAt(pauseTime, () -> threadOneScheduler.delayExecutionUntil(pauseEndTime));
            threadOneScheduler.doAt(doAtTime, () -> eventList.add("DELAYED_DO_AT"));
            threadOneScheduler.doAt(pauseEndTime, () -> eventList.add("SCHEDULED_DO_AT"));
            backingScheduler.unPause();

            ImmutableList<String> expectedEvents = ImmutableList.of("DELAYED_DO_AT", "SCHEDULED_DO_AT");
            Assertions.assertEquals(expectedEvents, ImmutableList.copyOf(eventList), "Incorrect events recorded");
        }

        @Test
        @DisplayName("Delayed doNow events execute before additional doNow events")
        void whenThreadPaused_thenAdditionalDoNowEventIsExecutedAfterDelayedDoNow() {
            double pauseTime = threadOneScheduler.getTimeProvider().getTime() + 10_000;
            double doNowTime = pauseTime + 4321;
            double pauseEndTime = doNowTime + 12_345;

            threadOneScheduler.doAt(pauseTime, () -> threadOneScheduler.delayExecutionUntil(pauseEndTime));
            threadTwoScheduler.doAt(doNowTime, () -> scheduleDoNow(threadOneScheduler, "DELAYED_DO_NOW_1"));
            threadTwoScheduler.doAt(doNowTime, () -> scheduleRecursiveDoNow(threadOneScheduler, "DELAYED_DO_NOW_2", "TRIGGERED_DO_NOW"));
            backingScheduler.unPause();

            ImmutableList<String> expectedEvents = ImmutableList.of(
                    "DELAYED_DO_NOW_1",
                    "DELAYED_DO_NOW_2",
                    "TRIGGERED_DO_NOW");
            Assertions.assertEquals(expectedEvents, ImmutableList.copyOf(eventList), "Incorrect events recorded");
        }

        @Test
        @DisplayName("Delayed doAt events execute after additional doNow events")
        void whenThreadPaused_thenAdditionalDoNowEventIsExecutedBeforeDelayedDoAt() {
            double pauseTime = threadOneScheduler.getTimeProvider().getTime() + 10_000;
            double doAtTime = pauseTime + 4321;
            double doNowTime = doAtTime + 4321;
            double pauseEndTime = doNowTime + 12_345;

            threadOneScheduler.doAt(pauseTime, () -> threadOneScheduler.delayExecutionUntil(pauseEndTime));
            threadOneScheduler.doAt(doAtTime, () -> eventList.add("DO_AT"));
            threadTwoScheduler.doAt(doNowTime, () -> scheduleRecursiveDoNow(threadOneScheduler, "DELAYED_DO_NOW", "TRIGGERED_DO_NOW"));
            backingScheduler.unPause();

            ImmutableList<String> expectedEvents = ImmutableList.of(
                    "DELAYED_DO_NOW",
                    "TRIGGERED_DO_NOW",
                    "DO_AT"
            );
            Assertions.assertEquals(expectedEvents, ImmutableList.copyOf(eventList), "Incorrect events recorded");
        }

        private void scheduleDoNow(SourceTrackingEventScheduler scheduler, String eventName) {
            scheduler.doNow(() -> eventList.add(eventName));
        }

        private void scheduleRecursiveDoNow(SourceTrackingEventScheduler scheduler, String eventName, String triggeredEventName) {
            scheduler.doNow(() -> {
                eventList.add(eventName);
                scheduler.doNow(() -> eventList.add(triggeredEventName));
            });
        }
    }

    @Nested
    @DisplayName("Thread blocking behaviour tests")
    class ThreadPauseBlockingBehaviourTests {
        private final List<String> eventList = new ArrayList<>();

        @Test
        @DisplayName("When delayExecutionUntil doesn't pause current event, we can still schedule but not execute")
        void whenThreadPausedButNotBlocked_thenEventContinuesAndSchedules() {
            double pauseTime = threadOneScheduler.getTimeProvider().getTime() + 10_000;
            double pauseEndTime = pauseTime + 1_000;

            threadTwoScheduler.doAt(pauseTime - 10, () -> eventList.add("2_PRE_PAUSE"));
            threadOneScheduler.doAt(pauseTime, () -> {
                threadOneScheduler.delayExecutionUntil(pauseEndTime, false);

                eventList.add("1_CONTINUES");
                threadTwoScheduler.doNow(() -> eventList.add("2_IN_PAUSE"));  // added immediately -- run now
                threadOneScheduler.doNow(() -> eventList.add("1_IN_PAUSE"));  // added immediately -- run later
            });
            // This should not be run until thread1 is un-paused
            threadOneScheduler.doAt(pauseTime + 10, () -> eventList.add("1_DELAYED"));

            // This should run at +20 and be undelayed
            threadTwoScheduler.doAt(pauseTime + 20, () -> eventList.add("2_POST_PAUSE"));
            backingScheduler.unPause();

            ImmutableList<String> expectedEvents = ImmutableList.of("2_PRE_PAUSE", "1_CONTINUES", "2_IN_PAUSE", "2_POST_PAUSE", "1_IN_PAUSE", "1_DELAYED");
            Assertions.assertEquals(expectedEvents, ImmutableList.copyOf(eventList), "Incorrect events recorded");
        }
        @Test
        @DisplayName("When delayedExecutionUntil also pauses the current event, then nothing happens until unpaused")
        void whenThreadPausedAndBlocked_thenEventStopsUntilThreadUnpaused() {
            double pauseTime = threadOneScheduler.getTimeProvider().getTime() + 10_000;
            double pauseEndTime = pauseTime + 1_000;

            threadTwoScheduler.doAt(pauseTime - 10, () -> eventList.add("2_PRE_PAUSE"));
            threadOneScheduler.doAt(pauseTime, () -> {
                threadOneScheduler.delayExecutionUntil(pauseEndTime, true);  // blocks

                // code should only be run after thead1 unpauses
                eventList.add("1_CONTINUES");
                threadTwoScheduler.doNow(() -> eventList.add("2_IN_PAUSE"));
                threadOneScheduler.doNow(() -> eventList.add("1_IN_PAUSE"));
            });
            threadOneScheduler.doAt(pauseTime + 10, () -> eventList.add("1_DELAYED"));
            threadTwoScheduler.doAt(pauseTime + 20, () -> eventList.add("2_POST_PAUSE"));
            backingScheduler.unPause();

            ImmutableList<String> expectedEvents = ImmutableList.of("2_PRE_PAUSE", "2_POST_PAUSE", "1_CONTINUES", "2_IN_PAUSE", "1_IN_PAUSE", "1_DELAYED");
            Assertions.assertEquals(expectedEvents, ImmutableList.copyOf(eventList), "Incorrect events recorded");
        }
    }
}
