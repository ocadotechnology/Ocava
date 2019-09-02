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

import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.Story;

@Story
class WallClockTimeoutFailureTest extends AbstractFrameworkTestStory {

    private static final int SECOND_LIMIT = 1;

    @Override
    public void executeTestSteps() {
        AssertionError e = Assertions.assertThrows(
                AssertionError.class,
                super::executeTestSteps,
                "No error thrown");
        Assertions.assertEquals("Wall clock timeout exceeded ==> expected: <false> but was: <true>", e.getMessage());
    }

    @Test
    void scenario() {
        given.timeout.addWallClockTimeout(SECOND_LIMIT, TimeUnit.SECONDS);
        when.simStarts();

        when.testEvent.scheduled(2, "first");
        when.testThread.pauseThreadExecution(SECOND_LIMIT * 2, TimeUnit.SECONDS);
        then.testEvent.occurs("first");
    }
}
