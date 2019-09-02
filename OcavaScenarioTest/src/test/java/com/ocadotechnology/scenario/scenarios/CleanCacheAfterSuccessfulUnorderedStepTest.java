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
class CleanCacheAfterSuccessfulUnorderedStepTest extends AbstractFrameworkTestStory {

    @Test
    void scenario() {
        when.simStarts();
        when.testEvent.scheduled(1, "first");
        when.testEvent.scheduled(2, "second");
        then.testEvent.occurs("first");

        //not successful test should not affect caches
        then.testEvent.unordered("Unsuccessful").occurs("missing");

        //first successful unordered step should clean up caches
        then.testEvent.unordered("SUCCESSFUL").occurs("second");

        //we should not be able to process the same notification multiple times
        then.testEvent.unordered("Unsuccessful").doesNotOccurInCaches("second");

        then.unordered.waitForSteps("SUCCESSFUL");
        then.unordered.removesUnorderedSteps("Unsuccessful");
    }
}