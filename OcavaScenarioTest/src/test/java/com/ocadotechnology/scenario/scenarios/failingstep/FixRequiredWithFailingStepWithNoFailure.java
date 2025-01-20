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
package com.ocadotechnology.scenario.scenarios.failingstep;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.FixRequired;
import com.ocadotechnology.scenario.Story;

/**
 * Test all iterations of a test marked with @{@link FixRequired} where there is a failing step but no failure at all
 */
@FixRequired("failing step with fix required will pass")
@Story
public class FixRequiredWithFailingStepWithNoFailure extends AbstractFrameworkTestStory {

    @Override
    public void executeTestSteps() {
        AssertionFailedError e = Assertions.assertThrows(
                AssertionFailedError.class,
                super::executeTestSteps,
                "No error thrown");
        Assertions.assertTrue(e.getMessage().contains("Test is successful but it is marked as fix required"));
    }

    /**
     * Check that test fails when a failingStep passes
     */
    @Test
    void simpleThenStepPasses() {
        when.simStarts();
        when.testEvent.scheduled(1, "first");
        then.testEvent.failingStep().occurs("first");
    }

    /**
     * Check when an execute step would fail
     */
    @Test
    void executeStepPasses() {
        when.simStarts();
        then.testEvent.failingStep().executeStep(false);
    }

    /**
     * Validate that failing step works with never, when the never violated
     */

    @Test
    void neverThenStepPasses() {
        when.simStarts();
        when.testEvent.scheduled(1, "first");
        then.testEvent.failingStep().never().occurs("not-first");
    }

    @Test
    void neverThenStepPassesOrdering() {
        when.simStarts();
        when.testEvent.scheduled(1, "first");
        then.testEvent.never().failingStep().occurs("not-first");
    }

    /**
     * Validate that failing step works with unordered, when the unordered passes
     */
    @Test
    void unorderedStepPasses() {
        when.simStarts();
        when.testEvent.scheduled(1, "first");
        then.testEvent.failingStep().unordered().occurs("first");
    }

    @Test
    void unorderedStepPassesOrdering() {
        when.simStarts();
        when.testEvent.scheduled(1, "first");
        then.testEvent.unordered().failingStep().occurs("first");
    }

    /**
     * Validate that failing step works with within, if within completes
     */
    @Test
    void withinStepPasses() {
        when.simStarts();
        when.testEvent.scheduled(2, "first");
        then.testEvent.failingStep().within(5, TimeUnit.MILLISECONDS).occurs("first");
    }

    @Test
    void withinStepPassesOrdering() {
        when.simStarts();
        when.testEvent.scheduled(2, "first");
        then.testEvent.within(5, TimeUnit.MILLISECONDS).failingStep().occurs("first");
    }

    /**
     * Validate that failing step works with sequenced, if the sequenced step passes.
     */
    @Test
    void sequencedStepPasses() {
        when.simStarts();
        when.testEvent.scheduled(1, "A-1");
        when.testEvent.scheduled(4, "A-2");

        then.testEvent.sequenced("A").occurs("A-1");
        then.testEvent.failingStep().sequenced("A").occurs("A-2");
    }

    @Test
    void sequencedStepPassesOrdering() {
        when.simStarts();
        when.testEvent.scheduled(1, "A-1");
        when.testEvent.scheduled(4, "A-2");

        then.testEvent.sequenced("A").occurs("A-1");
        then.testEvent.sequenced("A").failingStep().occurs("A-2");
    }
}
