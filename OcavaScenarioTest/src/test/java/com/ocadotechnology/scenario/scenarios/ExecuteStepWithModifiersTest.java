/*
 * Copyright © 2017-2022 Ocado (Ocava)
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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.Story;

/**
 * A set of tests that validate that if FixRequired is missing, but there is a failing step that the test would fail
 */
@Story
class ExecuteStepWithModifiersTest extends AbstractFrameworkTestStory {

    @Test
    void anonymousUnorderedStep() {
        when.simStarts();
        verifyStepCreationFails(() -> then.testEvent.unordered().executeStep(false));
    }

    @Test
    void namedUnorderedStep() {
        when.simStarts();
        verifyStepCreationFails(() -> then.testEvent.unordered("name").executeStep(false));
    }

    @Test
    void anonymousNeverStep() {
        when.simStarts();
        verifyStepCreationFails(() -> then.testEvent.never().executeStep(false));
    }

    @Test
    void namedNeverStep() {
        when.simStarts();
        verifyStepCreationFails(() -> then.testEvent.never("name").executeStep(false));
    }

    @Test
    void withinStep() {
        when.simStarts();
        verifyStepCreationFails(() -> then.testEvent.within(1, TimeUnit.SECONDS).executeStep(false));
    }

    @Test
    void withinDurationStep() {
        when.simStarts();
        verifyStepCreationFails(() -> then.testEvent.within(Duration.ofSeconds(1)).executeStep(false));
    }

    private void verifyStepCreationFails(Executable r) {
        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class, r, "Expected exception not thrown");
        Assertions.assertEquals("Execute steps must be ORDERED.  Remove any within, unordered or never modification method calls from this line.", e.getMessage());
    }
}
