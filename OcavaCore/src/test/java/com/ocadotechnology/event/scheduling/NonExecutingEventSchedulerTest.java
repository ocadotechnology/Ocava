/*
 * Copyright © 2017-2023 Ocado (Ocava)
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ocadotechnology.notification.TestSchedulerType;
import com.ocadotechnology.time.AdjustableTimeProvider;

public class NonExecutingEventSchedulerTest {

    private AdjustableTimeProvider timeProvider;
    private NonExecutingEventScheduler eventScheduler;
    private ExecutionTrackingRunnable runnable;

    @BeforeEach
    public void setUp() {
        timeProvider = new AdjustableTimeProvider(0);
        eventScheduler = new NonExecutingEventScheduler(TestSchedulerType.TEST_SCHEDULER_TYPE, timeProvider);
        runnable = new ExecutionTrackingRunnable();
    }

    @Test
    public void getTimeProviderReturnsTimeProvider() {
        Assertions.assertEquals(timeProvider, eventScheduler.getTimeProvider());
    }

    @Test
    public void scheduledRunnablesNotExecutedAutomaticallyWhenTimeAdvances() {
        double delay = 20;
        eventScheduler.doAt(delay, runnable);

        timeProvider.advanceTime(delay + 1);
        Assertions.assertEquals(0, runnable.runCount());
    }

    @Test
    public void executeOverdueEventsExecutesScheduledRunnableAtCorrectTimeAndOnlyOnce() {
        double delay = 20;
        eventScheduler.doAt(delay, runnable);

        timeProvider.advanceTime(delay - 1);
        eventScheduler.executeOverdueEvents();
        Assertions.assertEquals(0, runnable.runCount());

        timeProvider.advanceTime(1);
        eventScheduler.executeOverdueEvents();
        Assertions.assertEquals(1, runnable.runCount());

        timeProvider.advanceTime(50);
        eventScheduler.executeOverdueEvents();
        Assertions.assertEquals(1, runnable.runCount());
    }

    @Test
    public void executeAllEventsExecutesScheduledRunnableRegardlessOfScheduledTimeAndOnlyOnce() {
        double delay = 20;
        eventScheduler.doAt(delay, runnable);

        timeProvider.advanceTime(delay - 1);
        eventScheduler.executeAllEvents();
        Assertions.assertEquals(1, runnable.runCount());

        timeProvider.advanceTime(1);
        eventScheduler.executeAllEvents();
        Assertions.assertEquals(1, runnable.runCount());
    }

    @Test
    public void executeOverdueEventsDoesNotExecuteCancelledEvents() {
        cancelledEventsNotExecuted(NonExecutingEventScheduler::executeOverdueEvents);
    }

    @Test
    public void executeAllEventsDoesNotExecuteCancelledEvents() {
        cancelledEventsNotExecuted(NonExecutingEventScheduler::executeAllEvents);
    }

    private void cancelledEventsNotExecuted(Consumer<NonExecutingEventScheduler> executionMethod) {
        double delay = 20;
        Cancelable event1 = eventScheduler.doAt(delay, runnable);
        Event event2 = (Event)eventScheduler.doAt(delay, runnable);

        timeProvider.advanceTime(delay - 1);
        event1.cancel();
        eventScheduler.cancel(event2);

        timeProvider.advanceTime(10);
        executionMethod.accept(eventScheduler);
        Assertions.assertEquals(0, runnable.runCount());
    }

    @Test
    public void executeOverdueEventsDoesNotExecuteScheduledEventsAfterReset() {
        scheduledEventsNotExecutedAfterReset(NonExecutingEventScheduler::executeOverdueEvents);
    }

    @Test
    public void executeAllEventsDoesNotExecuteScheduledEventsAfterReset() {
        scheduledEventsNotExecutedAfterReset(NonExecutingEventScheduler::executeAllEvents);
    }

    private void scheduledEventsNotExecutedAfterReset(Consumer<NonExecutingEventScheduler> executionMethod) {
        double delay = 20;
        eventScheduler.doAt(delay, runnable);

        timeProvider.advanceTime(delay - 1);
        eventScheduler.reset();

        timeProvider.advanceTime(10);
        executionMethod.accept(eventScheduler);
        Assertions.assertEquals(0, runnable.runCount());
    }

    @Test
    public void executeOverdueEventsExecutesEventsInCorrectOrder() {
        scheduledEventsExecutedInCorrectOrder(NonExecutingEventScheduler::executeOverdueEvents);
    }

    @Test
    public void executeAllEventsExecutesEventsInCorrectOrder() {
        scheduledEventsExecutedInCorrectOrder(NonExecutingEventScheduler::executeAllEvents);
    }

    private void scheduledEventsExecutedInCorrectOrder(Consumer<NonExecutingEventScheduler> executionMethod) {

        List<Runnable> executionOrder = new ArrayList<>();

        ExecutionOrderTrackingRunnable runnable1 = new ExecutionOrderTrackingRunnable(executionOrder);
        ExecutionOrderTrackingRunnable runnable2 = new ExecutionOrderTrackingRunnable(executionOrder);
        ExecutionOrderTrackingRunnable runnable3 = new ExecutionOrderTrackingRunnable(executionOrder);

        eventScheduler.doAt(50, runnable1, "THIRD");
        eventScheduler.doNow(runnable2, "FIRST");
        eventScheduler.doAt(1, runnable3, "SECOND");

        timeProvider.advanceTime(50);
        executionMethod.accept(eventScheduler);

        List<Runnable> expectedOrder = Arrays.asList(runnable2, runnable3, runnable1);
        Assertions.assertEquals(expectedOrder, executionOrder);
    }

    @Test
    public void hasOnlyDaemonEventsReturnsTrueWhenNoEventsScheduled() {
        Assertions.assertTrue(eventScheduler.hasOnlyDaemonEvents());
    }

    @Test
    public void hasOnlyDaemonEventsReturnsTrueWhenOnlyDaemonEventsScheduled() {
        eventScheduler.doAt(20, () -> {}, "Daemon event", true);
        Assertions.assertTrue(eventScheduler.hasOnlyDaemonEvents());

        eventScheduler.doAt(40, () -> {}, "Non-daemon event", false);
        eventScheduler.doAt(60, () -> {}, "Daemon event", true);
        Assertions.assertFalse(eventScheduler.hasOnlyDaemonEvents());

        timeProvider.advanceTime(50);
        eventScheduler.executeOverdueEvents();
        Assertions.assertTrue(eventScheduler.hasOnlyDaemonEvents());
    }

    private static class ExecutionTrackingRunnable implements Runnable {

        private final AtomicInteger runCount = new AtomicInteger(0);

        @Override
        public void run() {
            runCount.incrementAndGet();
        }

        public int runCount() {
            return runCount.intValue();
        }
    }

    private static class ExecutionOrderTrackingRunnable implements Runnable {

        private final List<Runnable> executionOrder;

        public ExecutionOrderTrackingRunnable(List<Runnable> executionOrder) {
            this.executionOrder = executionOrder;
        }

        @Override
        public synchronized void run() {
            executionOrder.add(this);
        }
    }
}
