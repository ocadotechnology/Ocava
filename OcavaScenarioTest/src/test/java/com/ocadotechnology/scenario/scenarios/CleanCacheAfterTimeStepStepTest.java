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

import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.Story;

@Story
public class CleanCacheAfterTimeStepStepTest extends AbstractFrameworkTestStory {

    @Test
    public void scenario() {
        when.testEvent().scheduled(1, "first");
        when.simStarts();

        //time steps will trigger next steps. It should also reset notification caches. We should not be able to process 'first'
        then.timeSteps().waitForDuration(5, TimeUnit.MILLISECONDS);

        then.testEvent().doesNotOccurInCaches("first");
    }
}