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
import com.ocadotechnology.scenario.Story;

/**
 * A set of tests that validate that if FixRequired is missing, but there is a failing step that the test would fail
 */
@Story
class FailingStepWithoutFixRequired extends AbstractFrameworkTestStory {

    @Override
    public void executeTestSteps() {
        AssertionFailedError e = Assertions.assertThrows(
                AssertionFailedError.class,
                super::executeTestSteps,
                "No error thrown");
        Assertions.assertTrue(e.getMessage().contains("Step marked as failingStep but there is not @FixRequired annotation"));
    }

    /**
     * Check when the step would fail
     */
    @Test
    void occursWouldFailPassingTest() {
        when.simStarts();
        when.testEvent.scheduled(1, "first");
        then.testEvent.failingStep().occurs("not-first");
    }

    /**
     * Check when the step would pass
     */
    @Test
    void occursWouldPassFailingTest() {
        when.simStarts();
        when.testEvent.scheduled(1, "first");
        then.testEvent.failingStep().occurs("first");
    }

    /**
     * Check when a different step would fail
     */
    @Test
    void secondOccursWouldFailFailingTest() {
        when.simStarts();
        when.testEvent.scheduled(1, "first");
        then.testEvent.failingStep().occurs("first");
        then.testEvent.occurs("second");
    }

    /**
     * Check when an execute step would fail
     */
    @Test
    void executeStepWouldFailFailingTest() {
        when.simStarts();
        then.testEvent.failingStep().executeStep(true);
    }

    /**
     * Check when a marked execute step would pass
     */
    @Test
    void executeStepWouldPassFailingTest() {
        when.simStarts();
        then.testEvent.failingStep().executeStep(false);
    }
}
