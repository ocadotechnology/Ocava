/*
 * Copyright © 2017-2022 Ocado (Ocava)
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.base.Preconditions;
import com.ocadotechnology.time.AdjustableTimeProvider;

public class RepeatingRunnableTest {

    private final EventSchedulerType DUMMY_SCHEDULER_TYPE = new EventSchedulerType() {};
    private SimpleDiscreteEventScheduler simpleDiscreteEventScheduler;
    private ExceptionSwallowingEventExecutor exceptionSwallowingEventExecutor;

    @BeforeEach
    public void setUp() {
        exceptionSwallowingEventExecutor = new ExceptionSwallowingEventExecutor();
        simpleDiscreteEventScheduler = new SimpleDiscreteEventScheduler(
                exceptionSwallowingEventExecutor,
                () -> {},
                DUMMY_SCHEDULER_TYPE,
                new AdjustableTimeProvider(0),
                true);

    }

    @Test
    public void whenCancelled_thenRunnableDoesNotRunAgain() {
        ArrayList<Double> executionTimes = new ArrayList<>();
        simpleDiscreteEventScheduler.pause();
        Cancelable cancelable = RepeatingRunnable.startIn(0, 1, "Test Event", executionTimes::add, simpleDiscreteEventScheduler);
        simpleDiscreteEventScheduler.doAt(10, cancelable::cancel);
        simpleDiscreteEventScheduler.doAt(100, simpleDiscreteEventScheduler::stop); // Don't run indefinitely if someone breaks the functionality
        simpleDiscreteEventScheduler.unPause();

        Assertions.assertEquals(10, executionTimes.size());
    }

    @Test
    public void whenRunnableIsADamonEvent_theSchedulerKnowsItHasOnlyDamonEvents() {
        simpleDiscreteEventScheduler.pause();
        RepeatingRunnable.startInDaemon(0, 1, "Test Event", time -> {}, simpleDiscreteEventScheduler);
        simpleDiscreteEventScheduler.doAt(20, () -> Preconditions.checkState(simpleDiscreteEventScheduler.hasOnlyDaemonEvents()));
        simpleDiscreteEventScheduler.doAtDaemon(100, simpleDiscreteEventScheduler::stop, "Shutdown Event");
        simpleDiscreteEventScheduler.unPause();

        Assertions.assertNull(exceptionSwallowingEventExecutor.getFirstExceptionEncountered());
    }

    @Test
    public void whenRunnableIsNotADamonEvent_theSchedulerKnowsItDoesNotHaveOnlyDamonEvents() {
        simpleDiscreteEventScheduler.pause();
        RepeatingRunnable.startIn(0, 1, "Test Event", time -> {}, simpleDiscreteEventScheduler);
        simpleDiscreteEventScheduler.doAt(20, () -> Preconditions.checkState(!simpleDiscreteEventScheduler.hasOnlyDaemonEvents()));
        simpleDiscreteEventScheduler.doAtDaemon(100, simpleDiscreteEventScheduler::stop, "Shutdown Event");
        simpleDiscreteEventScheduler.unPause();

        Assertions.assertNull(exceptionSwallowingEventExecutor.getFirstExceptionEncountered());
    }

}
