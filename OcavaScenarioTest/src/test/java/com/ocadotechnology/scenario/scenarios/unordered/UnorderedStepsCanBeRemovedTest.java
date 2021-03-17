/*
 * Copyright © 2017-2021 Ocado (Ocava)
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
package com.ocadotechnology.scenario.scenarios.unordered;

import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.Story;

@Story
class UnorderedStepsCanBeRemovedTest extends AbstractFrameworkTestStory {

    private static final String SOMETHING_THAT_WILL_HAPPEN = "STEP_THAT_WILL_HAPPEN";
    private static final String UNSENT = "Unsent Event";
    private static final String UNSENT_2 = "Unsent Event 2";

    @Test
    void singleStepIsRemovedForScenarioToFinishSuccessfully() {
        when.simStarts();
        when.testEvent.scheduled(1, SOMETHING_THAT_WILL_HAPPEN);

        //define an unordered step that will never be sent, so will fail the test if not removed
        then.testEvent.unordered(UNSENT).occurs("event that is never sent");

        //remove the step that will never happen, so we can continue and pass the test
        then.unordered.removesUnorderedSteps(UNSENT);

        then.testEvent.occurs(SOMETHING_THAT_WILL_HAPPEN);
    }

    @Test
    void multipleStepsAreRemovedForScenarioToFinishSuccessfully() {
        when.simStarts();
        when.testEvent.scheduled(1, SOMETHING_THAT_WILL_HAPPEN);

        //define an unordered step that will never be sent, so will fail the test if not removed
        then.testEvent.unordered(UNSENT).occurs("event that is never sent");
        then.testEvent.unordered(UNSENT_2).occurs("another event that is never sent");

        //remove all steps that will never happen, so we can continue and pass the test
        then.unordered.removesUnorderedSteps(UNSENT, UNSENT_2);

        then.testEvent.occurs(SOMETHING_THAT_WILL_HAPPEN);
    }
}
