/*
 * Copyright © 2017-2020 Ocado (Ocava)
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

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.StepFuture;
import com.ocadotechnology.scenario.Story;

/**
 * Test that handling "any of unordered steps" will not finish if only a single step in a group finishes,
 * and that it will finish for a step which is interleaved between events which would cause the group to finish
 */
@Story
public class AnyOfUnorderedStepsGroupDoesNotFinishEarlyTest extends AbstractFrameworkTestStory {
    private static final String GROUP_OF_STEPS = "Group Of Steps";
    private static final String INDIVIDUAL_STEP = "Individual Step";

    private static final String GROUP_EVENT_1 = "Group Event 1";
    private static final String GROUP_EVENT_2 = "Group Event 2";
    private static final String INDIVIDUAL_EVENT = "Individual Event";

    @Test
    public void scenario() {
        when.simStarts();

        when.testEvent.scheduled(1, GROUP_EVENT_1);
        when.testEvent.scheduled(2, INDIVIDUAL_EVENT);
        when.testEvent.scheduled(3, GROUP_EVENT_2);

        then.testEvent.unordered(GROUP_OF_STEPS).occurs(GROUP_EVENT_1);
        then.testEvent.unordered(GROUP_OF_STEPS).occurs(GROUP_EVENT_2);
        then.testEvent.unordered(INDIVIDUAL_STEP).occurs(INDIVIDUAL_EVENT);

        StepFuture<List<String>> finishedSteps = then.unordered.waitForAnyOfSteps(GROUP_OF_STEPS, INDIVIDUAL_STEP);

        // check that the wait has not finished after seeing the first group event
        then.futures.assertEquals(ImmutableList.of(INDIVIDUAL_STEP), finishedSteps);
    }
}
