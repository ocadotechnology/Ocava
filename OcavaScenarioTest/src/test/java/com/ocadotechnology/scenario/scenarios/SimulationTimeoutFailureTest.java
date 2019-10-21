/*
 * Copyright © 2017-2019 Ocado (Ocava)
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
class SimulationTimeoutFailureTest extends AbstractFrameworkTestStory {

    private static final int MILLISECOND_LIMIT = 100;

    @Override
    public void executeTestSteps() {
        AssertionError e = Assertions.assertThrows(
                AssertionError.class,
                super::executeTestSteps,
                "No error thrown");
        Assertions.assertEquals("Discrete event timeout reached", e.getMessage());
    }

    @Test
    void scenario() {
        given.timeout.addSimulationTimeout(MILLISECOND_LIMIT, TimeUnit.MILLISECONDS);
        when.simStarts();
        then.time.waitForDuration(MILLISECOND_LIMIT * 2, TimeUnit.MILLISECONDS);
    }
}
