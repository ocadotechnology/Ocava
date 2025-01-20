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
package com.ocadotechnology.scenario.scenarios;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ocadotechnology.event.EventUtil;
import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.Story;

@Story
class WithinStepFailureTest extends AbstractFrameworkTestStory {

    private static final int MILLISECOND_LIMIT = 2;
    public static final String FIRST_EVENT = "first";
    public static final String SECOND_EVENT = "second";

    @Override
    public void executeTestSteps() {
        AssertionError e = Assertions.assertThrows(
                AssertionError.class,
                super::executeTestSteps,
                "No error thrown");
        Assertions.assertEquals("Within step failed - didn't finish within " + EventUtil.durationToString(MILLISECOND_LIMIT) + " ==> expected: <true> but was: <false>", e.getMessage());
    }

    @Test
    void scenario() {
        when.simStarts();
        when.testEvent.scheduled(2, FIRST_EVENT);
        when.testEvent.scheduled(5, SECOND_EVENT);

        then.testEvent.occurs(FIRST_EVENT);
        then.testEvent.within(MILLISECOND_LIMIT, TimeUnit.MILLISECONDS).occurs(SECOND_EVENT);
    }
}
