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

import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.FixRequired;
import com.ocadotechnology.scenario.Story;

/**
 * Validate that a failing test that is marked as fix required by the {@link com.ocadotechnology.scenario.AbstractStory#isFixRequired}
 * function, as opposed to the {@link FixRequired} annotation, is designated as passing.
 */
@Story
class FixRequiredFailingTestWithoutAnnotationTest extends AbstractFrameworkTestStory {

    // This assertion cannot be in the scenario as the test is marked as fix required, meaning if this step failed the test would still pass.
    static {
        Assertions.assertFalse(FixRequiredFailingTestWithoutAnnotationTest.class.isAnnotationPresent(FixRequired.class));
    }

    @Override
    public boolean isFixRequired() {
        return true;
    }

    @Test
    void scenario() {
        when.simStarts();
        when.testEvent.scheduled(1, "first");
        then.testEvent.occurs("really-not-first");
        then.testEvent.occurs("not-first");
    }
}
