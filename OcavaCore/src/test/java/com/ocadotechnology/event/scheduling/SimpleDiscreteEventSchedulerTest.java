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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.google.common.util.concurrent.Runnables;
import com.ocadotechnology.notification.TestSchedulerType;
import com.ocadotechnology.time.AdjustableTimeProvider;

public class SimpleDiscreteEventSchedulerTest {
    @Test
    void testThatMultiplePauseUnpausesRunToCompletion() {
        SimpleDiscreteEventScheduler scheduler = new SimpleDiscreteEventScheduler(new EventExecutor(), Runnables.doNothing(), TestSchedulerType.TEST_SCHEDULER_TYPE, new AdjustableTimeProvider(0), true);
        double time = 10;

        AtomicBoolean checkThatUnPauseExecutesA = new AtomicBoolean(false);
        AtomicBoolean checkThatUnPauseExecutesB = new AtomicBoolean(false);

        // Scheduler will add event, then start event loop and execute event, pushing the clock forward by 'time'
        scheduler.doIn(time, scheduler::pause);

        // Scheduler is paused, so calling doIn here only adds the event
        scheduler.doIn(time + 1, () -> checkThatUnPauseExecutesA.set(true));  // = A
        assertFalse(checkThatUnPauseExecutesA.get(), "Scheduler should have paused before executing event A");

        // Scheduler is paused (but still executing).  Need 'unPause' which will also run the event loop
        scheduler.unPause();
        assertTrue(checkThatUnPauseExecutesA.get(), "Scheduler should started on 'unPause' and executed event A");

        // Scheduler runs out of work (is now labelled idle), so calling doIn restarts the event loop and calls pause
        scheduler.doIn(time, scheduler::pause);

        // Scheduler is paused, so calling doIn here only adds the event
        scheduler.doIn(time, () -> checkThatUnPauseExecutesB.set(true));  // = B
        assertFalse(checkThatUnPauseExecutesB.get(), "Scheduler is paused and should not have run event B");

        scheduler.unPause();
        assertTrue(checkThatUnPauseExecutesB.get(), "Scheduler should have started on 'unPause' and executed event B");
    }

    @Test
    void testThatStartingPausedAllowsIdenticalCyclesOfScheduleExecution() {
        SimpleDiscreteEventScheduler scheduler = new SimpleDiscreteEventScheduler(new EventExecutor(), Runnables.doNothing(), TestSchedulerType.TEST_SCHEDULER_TYPE, new AdjustableTimeProvider(0), true);
        double time = 10;

        AtomicBoolean flagA = new AtomicBoolean(false);
        AtomicBoolean flagB = new AtomicBoolean(false);

        scheduler.pause();

        scheduler.doIn(time, () -> { flagA.set(true);  scheduler.pause(); });  // = A
        assertFalse(flagA.get(), "Scheduler is paused, should not have executed A");
        scheduler.unPause();
        assertTrue(flagA.get(), "Scheduler was unpaused, should have executed A");

        // Scheduler should be paused because of event A

        scheduler.doIn(time, () -> { flagB.set(true);  scheduler.pause(); });  // = B
        assertFalse(flagB.get(), "Scheduler is paused, should not have executed B");
        scheduler.unPause();
        assertTrue(flagB.get(), "Scheduler was unpaused, should have executed B");

        // Scheduler should be paused because of event B
    }
}
