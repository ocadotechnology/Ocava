/*
 * Copyright © 2017 Ocado (Ocava)
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
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.util.concurrent.Runnables;
import com.ocadotechnology.time.AdjustableTimeProvider;

public class SourceTrackingEventSchedulerTest {
    private enum TestSchedulerType implements EventSchedulerType {
        T1, T2;
    }

    private SourceSchedulerTracker tracker = new SourceSchedulerTracker();
    private final SimpleDiscreteEventScheduler eventScheduler = new SimpleDiscreteEventScheduler(new EventExecutor(), Runnables.doNothing(), TestSchedulerType.T1, new AdjustableTimeProvider(0), true);
    private final SourceTrackingEventScheduler simScheduler = new SourceTrackingEventScheduler(tracker, TestSchedulerType.T1, eventScheduler);
    private final SourceTrackingEventScheduler planningScheduler = new SourceTrackingEventScheduler(tracker, TestSchedulerType.T2, eventScheduler);

    @BeforeEach
    public void setup() {
        eventScheduler.pause();
    }

    @Test
    public void whenEventExecuted_thenCorrectSchedulerTypeSet() {
        List<EventSchedulerType> observedTypes = new ArrayList<>();
        simScheduler.doAt(50, () -> observedTypes.add(tracker.getActiveSchedulerType()), "");
        planningScheduler.doAt(100, () -> observedTypes.add(tracker.getActiveSchedulerType()), "");
        simScheduler.doAt(150, () -> observedTypes.add(tracker.getActiveSchedulerType()), "");
        eventScheduler.unPause();

        List<TestSchedulerType> expectedTypes = new ArrayList<>();
        expectedTypes.add(TestSchedulerType.T1);
        expectedTypes.add(TestSchedulerType.T2);
        expectedTypes.add(TestSchedulerType.T1);
        Assertions.assertEquals(expectedTypes, observedTypes);
    }

    @Test
    public void whenEventCancelled_thenEventNotExecuted() {
        List<Boolean> executed = new ArrayList<>();
        executed.add(false);
        Cancelable cancelable = simScheduler.doAt(50, () -> executed.set(0, true), "");
        simScheduler.doAt(25d, cancelable::cancel, "");
        eventScheduler.unPause();
        Assertions.assertFalse(executed.get(0), "event executed after cancellation");
    }
}
