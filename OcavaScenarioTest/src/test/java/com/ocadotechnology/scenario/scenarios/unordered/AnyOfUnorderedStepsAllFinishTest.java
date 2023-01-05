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
package com.ocadotechnology.scenario.scenarios.unordered;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.StepFuture;
import com.ocadotechnology.scenario.Story;

/**
 *  Test that the possibility of all specified steps finishing is a valid outcome for handling "any of unordered steps"
 */
@Story
public class AnyOfUnorderedStepsAllFinishTest extends AbstractFrameworkTestStory {

    private static final String FIRST_STEP = "First Step";
    private static final String SECOND_STEP = "Second Step";

    private static final String EVENT_1 = "Event 1";
    private static final String EVENT_2 = "Event 2";

    @Test
    public void scenario() {
        when.simStarts();
        when.testEvent.scheduled(1, EVENT_1);
        when.testEvent.scheduled(2, EVENT_2);

        then.testEvent.unordered(FIRST_STEP).occurs(EVENT_1);
        then.testEvent.unordered(SECOND_STEP).occurs(EVENT_2);

        then.time.waitForDuration(3, TimeUnit.MILLISECONDS); // ensure that both events occur before the wait steps executes

        StepFuture<List<String>> finishedSteps = then.unordered.waitForAnyOfSteps(FIRST_STEP, SECOND_STEP);

        then.futures.assertEquals(ImmutableList.of(FIRST_STEP, SECOND_STEP), finishedSteps);
    }
}
