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
package com.ocadotechnology.scenario.scenarios;

import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.StepFuture;
import com.ocadotechnology.scenario.Story;

@Story
class AfterAtLeastWithStepFutureTest extends AbstractFrameworkTestStory {
    public static final String FIRST_EVENT = "first";
    public static final String SECOND_EVENT = "second";
    public static final String STEP_NAME = "step_name";

    @Test
    void timeUnitNoName() {
        when.simStarts();
        when.testEvent.scheduled(2, FIRST_EVENT);
        then.testEvent.occurs(FIRST_EVENT);

        StepFuture<Double> delay = when.testEvent.scheduled(6, SECOND_EVENT);
        delay = delay.map(t -> t - 1);
        then.testEvent.afterAtLeast(delay).occurs(SECOND_EVENT);
    }

    @Test
    void timeUnitWithName() {
        when.simStarts();
        when.testEvent.scheduled(2, FIRST_EVENT);
        then.testEvent.occurs(FIRST_EVENT);

        StepFuture<Double> delay = when.testEvent.scheduled(6, SECOND_EVENT);
        then.testEvent.afterAtLeast(STEP_NAME, delay).occurs(SECOND_EVENT);
        then.unordered.waitForSteps(STEP_NAME);
    }
}