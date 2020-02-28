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
package com.ocadotechnology.scenario.scenarios.unordered;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.Story;

@Story
class MultipleEqualUnorderedStepsRequireMultipleEventsFailureTest extends AbstractFrameworkTestStory {

    private static final String FIRST_TRACKED_EVENT = "First Tracked Event";
    private static final String SECOND_TRACKED_EVENT = "Second Tracked Event";
    private static final String UNSENT = "Unsent Event";
    private static final String TRACKED = "tracked";

    @Override
    public void executeTestSteps() {
        IllegalStateException e = Assertions.assertThrows(
                IllegalStateException.class,
                super::executeTestSteps,
                "No error thrown");
        Assertions.assertEquals("Missing step: 7 (MultipleEqualUnorderedStepsRequireMultipleEventsFailureTest.java:54).waitForSteps   [" + SECOND_TRACKED_EVENT + "]", e.getMessage());
    }

    @Test
    void scenario() {
        when.simStarts();
        when.testEvent.scheduled(1, "ignored");
        when.testEvent.scheduled(2, TRACKED);

        //not successful test should not affect caches
        then.testEvent.unordered(UNSENT).occurs("never sent");

        then.testEvent.unordered(FIRST_TRACKED_EVENT).occurs(TRACKED);
        then.testEvent.unordered(SECOND_TRACKED_EVENT).occurs(TRACKED);

        //we should not be able to process the same notification multiple times, so the second step should never complete, causing the failure asserted above
        then.unordered.waitForSteps(FIRST_TRACKED_EVENT, SECOND_TRACKED_EVENT);
    }
}