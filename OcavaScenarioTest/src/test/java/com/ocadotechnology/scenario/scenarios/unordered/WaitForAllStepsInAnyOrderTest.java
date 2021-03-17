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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.Story;

@Story
class WaitForAllStepsInAnyOrderTest extends AbstractFrameworkTestStory {

    private static final String STEP_1 = "A Step";
    private static final String STEP_2 = "Another Step";
    private static final String STEP_3 = "Yet Another Step";

    private static final String EVENT_1 = "Event 1";
    private static final String EVENT_2 = "Event 2";
    private static final String EVENT_3 = "Event 3";

    //3 events - means 6 possible combinations
    @ParameterizedTest
    @CsvSource({
            "1, 2, 3",
            "1, 3, 2",
            "2, 1, 3",
            "2, 3, 1",
            "3, 1, 2",
            "3, 2, 1"
    })
    void scenarioWithAllPossibleCombinations(int tEvent1, int tEvent2, int tEvent3) {
        when.simStarts();

        //schedule by the order defined
        when.testEvent.scheduled(tEvent1, EVENT_1);
        when.testEvent.scheduled(tEvent2, EVENT_2);
        when.testEvent.scheduled(tEvent3, EVENT_3);

        //define unordered steps
        then.testEvent.unordered(STEP_1).occurs(EVENT_1);
        then.testEvent.unordered(STEP_2).occurs(EVENT_2);
        then.testEvent.unordered(STEP_3).occurs(EVENT_3);

        //wait for the events to happen, in any order, all combinations must pass this test
        then.unordered.waitForSteps(STEP_1, STEP_2, STEP_3);
    }

    @Test
    void waitForASetOfStepNames() {
        when.simStarts();

        //schedule events, note EVENT_1 is actually after EVENT_2
        when.testEvent.scheduled(10, EVENT_1);
        when.testEvent.scheduled(1, EVENT_2);

        //define unordered steps
        then.testEvent.unordered(STEP_1).occurs(EVENT_1);
        then.testEvent.unordered(STEP_2).occurs(EVENT_2);

        then.unordered.waitForSteps(ImmutableSet.of(STEP_1, STEP_2));
    }
}
