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

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.StepFuture;
import com.ocadotechnology.scenario.Story;

/**
 * Regression test for #434.
 * <br>
 * If a scenario test is listening for a given notification class, and that notification is broadcast inside an
 * executeStep, the framework was immediately triggering the next steps in the scenario instead of waiting for the
 * current step to complete.
 */
@Story
public class WhenStepCompletesBeforeNextStepTest extends AbstractFrameworkTestStory {
    private static final String TEST_VALUE_1 = "TEST VALUE 1";
    private static final String TEST_VALUE_2 = "TEST VALUE 2";

    @Test
    void singleNotificationDoesNotInterruptFlow() {
        when.simStarts();

        then.testEvent.unordered().occurs(TEST_VALUE_1); // Ensure notification triggers an execution cycle
        StepFuture<List<String>> future = when.testEvent.broadcastThenPopulateFuture(TEST_VALUE_1);
        then.futures.assertEquals(List.of(TEST_VALUE_1), future); // Ensure the when step is not interrupted
    }

    @Test
    void multipleNotificationsAreAllProcessed() {
        when.simStarts();

        //Ensure both notifications trigger an execution cycle
        then.testEvent.unordered().occurs(TEST_VALUE_1);
        then.testEvent.unordered().occurs(TEST_VALUE_2);

        StepFuture<List<String>> future = when.testEvent.broadcastThenPopulateFuture(TEST_VALUE_1, TEST_VALUE_2);
        then.futures.assertEquals(List.of(TEST_VALUE_1, TEST_VALUE_2), future); // Ensure the when step is not interrupted
    }
}
