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
package com.ocadotechnology.scenario.scenarios.failingstep;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.FixRequired;
import com.ocadotechnology.scenario.Story;

/**
 * Validate that a passing test that is marked as fix required by the {@link com.ocadotechnology.scenario.AbstractStory#isFixRequired}
 * function, as opposed to the {@link FixRequired} annotation, is designated as failing.
 */
@Story
class FixRequiredPassingTestWithoutUseOfAnnotationTest extends AbstractFrameworkTestStory {

    // This assertion cannot be in the scenario as the test is marked as fix required, meaning if this step failed the test would still pass.
    static {
        Assertions.assertFalse(FixRequiredPassingTestWithoutUseOfAnnotationTest.class.isAnnotationPresent(FixRequired.class));
    }

    @Override
    public boolean isFixRequired() {
        return true;
    }

    @Override
    public void executeTestSteps() {
        AssertionFailedError e = Assertions.assertThrows(
                AssertionFailedError.class,
                super::executeTestSteps,
                "No error thrown");
        Assertions.assertTrue(e.getMessage().contains("Test is successful but it is marked as fix required"));
    }

    @Test
    void scenario() {
        when.simStarts();
    }
}
