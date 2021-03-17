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
package com.ocadotechnology.scenario.scenarios.failingstep;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.FixRequired;
import com.ocadotechnology.scenario.Story;

@FixRequired("Testing failing step chaining")
@Story
class FailingStepChainedTest extends AbstractFrameworkTestStory {

    @Override
    public void executeTestSteps() {
        AssertionFailedError e = Assertions.assertThrows(
                AssertionFailedError.class,
                super::executeTestSteps,
                "No error thrown");
        Assertions.assertTrue(e.getMessage().contains("Test is successful but it is annotated with FixRequired:"));
    }

    /**
     * Test that chaining failing steps doesn't negate the failingStep flag, or lead to the wrong CheckStep being
     * stored as the failing step.
     */
    @Test
    void scenario() {
        when.simStarts();
        when.testEvent.scheduled(1, "first");
        then.testEvent.failingStep().failingStep().occurs("first");
    }
}
