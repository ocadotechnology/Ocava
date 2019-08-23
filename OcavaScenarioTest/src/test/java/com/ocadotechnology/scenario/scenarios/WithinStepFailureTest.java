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

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ocadotechnology.event.EventUtil;
import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.Story;

@Story
public class WithinStepFailureTest extends AbstractFrameworkTestStory {

    public static final int MILLISECOND_LIMIT = 2;

    @Override
    public void executeTestSteps() {
        Assertions.assertThrows(
                AssertionError.class,
                super::executeTestSteps,
                "Within step failed - didn't finish within " + EventUtil.durationToString(MILLISECOND_LIMIT) + " ==> expected: <true> but was: <false>");
    }

    @Test
    public void scenario() {
        when.testEvent().scheduled(2, "first");
        when.testEvent().scheduled(5, "second");
        when.simStarts();

        then.testEvent().occurs("first");
        then.testEvent().within(MILLISECOND_LIMIT, TimeUnit.MILLISECONDS).occurs("second");
    }
}
