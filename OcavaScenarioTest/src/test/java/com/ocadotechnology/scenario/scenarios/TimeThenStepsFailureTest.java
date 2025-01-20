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
package com.ocadotechnology.scenario.scenarios;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.FixRequired;
import com.ocadotechnology.scenario.StepFuture;
import com.ocadotechnology.scenario.Story;

/**
 * A set of tests that validate the TimeThenSteps methods
 */
@Story
@FixRequired("Intentionally failing test")
class TimeThenStepsFailureTest extends AbstractFrameworkTestStory {
    private static final String EVENT_1 = "EVENT_1";

    @Test
    void timeIsLessThan_withAbsoluteValue_fails() {
        when.simStarts();
        when.testEvent.scheduled(10, EVENT_1);
        then.testEvent.occurs(EVENT_1);
        then.time.timeIsLessThan(9, TimeUnit.MILLISECONDS);
    }

    @Test
    void timeIsLessThan_withStepFuture_fails() {
        when.simStarts();
        StepFuture<Double> delay = when.testEvent.scheduled(10, EVENT_1);
        then.testEvent.occurs(EVENT_1);
        then.time.timeIsLessThan(delay.map(t -> t - 1));
    }
}
