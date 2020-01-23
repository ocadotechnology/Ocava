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
package com.ocadotechnology.scenario.scenarios;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.FixRequired;
import com.ocadotechnology.scenario.Story;

/**
 * If the assertion failure is ignored, the test will pass which should then fail the FixRequired annotation
 */
@Story
@FixRequired("Test")
class StopAfterAssertionFailureTest extends AbstractFrameworkTestStory {

    @Test
    void scenario() {
        when.simStarts();
        when.testEvent.scheduled(1, "first");
        then.testEvent.occurs("first");
        //Test should not be executed
        Assertions.fail();
    }

}
