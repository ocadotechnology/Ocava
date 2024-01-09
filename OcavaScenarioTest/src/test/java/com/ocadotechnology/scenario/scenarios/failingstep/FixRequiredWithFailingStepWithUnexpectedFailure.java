/*
 * Copyright © 2017-2024 Ocado (Ocava)
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
import org.junit.jupiter.api.Nested;
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
public class FixRequiredWithFailingStepWithUnexpectedFailure extends AbstractFrameworkTestStory {
    private String failurePrefix = "Missing step: ";

    @Override
    public void executeTestSteps() {
        Throwable e = Assertions.assertThrows(
                Throwable.class,
                super::executeTestSteps,
                "No error thrown");
        Assertions.assertTrue(e.getMessage().startsWith(failurePrefix));
    }

    /**
     * Check that test fails when marked with failingStep and FixRequired
     */
    @Test
    void simpleThenStepPasses() {
        when.simStarts();
        when.testEvent.scheduled(1, "first");
        then.testEvent.failingStep().occurs("first");
        then.testEvent.occurs("second");
    }

    /**
     * Check when an execute step would fail
     */
    @Test
    void passingExecuteStep() {
        failurePrefix = "Test setup requires a failure";
        when.simStarts();
        then.testEvent.failingStep().executeStep(false);
        then.testEvent.executeStep(true);
    }

    /**
     * Validate that failing step works with unordered, when the unordered passes
     */
    @Test
    void unorderedStepPasses() {
        when.simStarts();
        when.testEvent.scheduled(1, "first");
        then.testEvent.failingStep().unordered().occurs("first");
        then.testEvent.unordered().occurs("second");
    }

    @Test
    void unorderedStepPassesOrdering() {
        when.simStarts();
        when.testEvent.scheduled(1, "first");
        then.testEvent.unordered().failingStep().occurs("first");
        then.testEvent.unordered().occurs("second");
    }

    /**
     * Validate that failing step works with never, when the never violated
     */
    @FixRequired("failing step with fix required will pass")
    @Story
    @Nested
    class NeverSteps extends AbstractFrameworkTestStory {

        @Override
        public void executeTestSteps() {
            AssertionFailedError e = Assertions.assertThrows(
                    AssertionFailedError.class,
                    super::executeTestSteps,
                    "No error thrown");
            Assertions.assertTrue(e.getMessage().contains("Never condition violated. Step: "));
        }

        @Test
        void neverThenStepPasses() {
            when.simStarts();
            when.testEvent.scheduled(1, "first");
            then.testEvent.failingStep().never().occurs("not-first");
            then.testEvent.never().occurs("first");
        }

        @Test
        void neverThenStepPassesOrdering() {
            when.simStarts();
            when.testEvent.scheduled(1, "first");
            then.testEvent.never().failingStep().occurs("not-first");
            then.testEvent.never().occurs("first");
        }
    }

    /**
     * Validate that failing step works with within, if within completes
     */
    @FixRequired("failing step with fix required will pass")
    @Story
    @Nested
    class WithinSteps extends AbstractFrameworkTestStory {

        @Override
        public void executeTestSteps() {
            AssertionFailedError e = Assertions.assertThrows(
                    AssertionFailedError.class,
                    super::executeTestSteps,
                    "No error thrown");
            Assertions.assertTrue(e.getMessage().contains("Within step failed - didn't finish within"));
        }

        @Test
        void withinStepPasses() {
            when.simStarts();
            when.testEvent.scheduled(2, "first");
            then.testEvent.failingStep().within(5, TimeUnit.MILLISECONDS).occurs("first");
            then.testEvent.within(2, TimeUnit.MILLISECONDS).occurs("third");
        }

        @Test
        void withinStepPassesOrdering() {
            when.simStarts();
            when.testEvent.scheduled(2, "first");
            then.testEvent.within(5, TimeUnit.MILLISECONDS).failingStep().occurs("first");
            then.testEvent.within(2, TimeUnit.MILLISECONDS).occurs("third");
        }
    }
}
