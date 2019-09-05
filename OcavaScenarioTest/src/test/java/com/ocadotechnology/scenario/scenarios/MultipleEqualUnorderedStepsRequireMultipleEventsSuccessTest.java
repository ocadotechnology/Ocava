/*
 * Copyright © 2017 Ocado (Ocava)
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

import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.Story;

@Story
class MultipleEqualUnorderedStepsRequireMultipleEventsSuccessTest extends AbstractFrameworkTestStory {

    private static final String FIRST_TRACKED_EVENT = "First Tracked Event";
    private static final String SECOND_TRACKED_EVENT = "Second Tracked Event";
    private static final String UNSENT = "Unsent Event";
    private static final String TRACKED = "tracked";

    @Test
    void scenario() {
        when.simStarts();
        when.testEvent.scheduled(1, "ignored");
        when.testEvent.scheduled(2, TRACKED);
        when.testEvent.scheduled(3, TRACKED);

        //not successful test should not affect caches
        then.testEvent.unordered(UNSENT).occurs("never sent");

        then.testEvent.unordered(FIRST_TRACKED_EVENT).occurs(TRACKED);
        then.testEvent.unordered(SECOND_TRACKED_EVENT).occurs(TRACKED);

        //we are sending two notifications with the same name, so this should now complete
        then.unordered.waitForSteps(FIRST_TRACKED_EVENT, SECOND_TRACKED_EVENT);

        //allows the test to complete.
        then.unordered.removesUnorderedSteps(UNSENT);
    }
}