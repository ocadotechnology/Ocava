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
package com.ocadotechnology.scenario.scenarios.unordered;

import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.Story;

@Story
class AllStepsAreAlreadyFinishedTest extends AbstractFrameworkTestStory {

    private static final String STEP_1 = "A Step";
    private static final String STEP_2 = "Another Step";

    private static final String EVENT_1 = "Event 1";
    private static final String EVENT_2 = "Event 2";

    @Test
    void scenario() {
        when.simStarts();

        //schedule by the order defined
        when.testEvent.scheduled(1, EVENT_1);
        when.testEvent.scheduled(2, EVENT_2);

        then.testEvent.unordered(STEP_1).occurs(EVENT_1);
        then.testEvent.unordered(STEP_2).occurs(EVENT_2);

        then.unordered.waitForSteps(STEP_1, STEP_2);
        then.unordered.allStepsAreAlreadyFinished(STEP_1, STEP_2);
    }
}
