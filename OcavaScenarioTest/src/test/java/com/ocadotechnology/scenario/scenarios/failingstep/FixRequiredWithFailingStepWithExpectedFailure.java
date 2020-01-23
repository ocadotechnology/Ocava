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
package com.ocadotechnology.scenario.scenarios.failingstep;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.FixRequired;
import com.ocadotechnology.scenario.Story;

/**
 * Test all iterations of a test marked with @{@link FixRequired} where the failing step actually fails
 */
@FixRequired("failing step with fix required will pass")
@Story
class FixRequiredWithFailingStepWithExpectedFailure extends AbstractFrameworkTestStory {

    /**
     * Validate the test passes with a failingStep
     */
    @Test
    void simpleFailingStep() {
        when.simStarts();
        when.testEvent.scheduled(1, "first");
        then.testEvent.failingStep().occurs("not-first");
    }

    /**
     * Check when an execute step would fail
     */
    @Test
    void failingExecuteStep() {
        when.simStarts();
        then.testEvent.failingStep().executeStep(true);
    }

    /**
     * Validate that having multiple failingSteps works
     */
    @Test
    void multipleFailingSteps() {
        when.simStarts();
        when.testEvent.scheduled(1, "first");
        then.testEvent.failingStep().occurs("really-not-first");
        then.testEvent.failingStep().occurs("not-first");
    }

    /**
     * Validate that failing step works with never
     */

    @Test
    void neverStepNeverCalled() {
        when.simStarts();
        when.testEvent.scheduled(1, "first");
        then.testEvent.failingStep().never().occurs("first");
    }

    @Test
    void neverStepNeverCalledOrdering() {
        when.simStarts();
        when.testEvent.scheduled(1, "first");
        then.testEvent.never().failingStep().occurs("first");
    }

    /**
     * Validate that failing step works with unordered, when the unordered fails
     */

    @Test
    void unorderedStepDoesntFinish() {
        when.simStarts();
        when.testEvent.scheduled(1, "first");
        then.testEvent.failingStep().unordered().occurs("not-first");
    }

    @Test
    void unorderedStepDoesntFinishOrdering() {
        when.simStarts();
        when.testEvent.scheduled(1, "first");
        then.testEvent.unordered().failingStep().occurs("not-first");
    }

    private static final int MILLISECOND_LIMIT = 2;

    /**
     * Validate that failing step works with within
     */
    @Test
    void withinDoesntFinish() {
        when.simStarts();
        when.testEvent.scheduled(MILLISECOND_LIMIT * 10, "first");
        then.testEvent.failingStep().within(MILLISECOND_LIMIT, TimeUnit.MILLISECONDS).occurs("first");
    }

    @Test
    void withinDoesntFinishOrdering() {
        when.simStarts();
        when.testEvent.scheduled(MILLISECOND_LIMIT * 10, "first");
        then.testEvent.within(MILLISECOND_LIMIT, TimeUnit.MILLISECONDS).failingStep().occurs("first");
    }

}
