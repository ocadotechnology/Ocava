/*
 * Copyright © 2017-2023 Ocado (Ocava)
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

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.Story;

@Story
public class UnorderedTimeConstrainedStepsSuccessTest extends AbstractFrameworkTestStory {
    public static final String FIRST_EVENT = "first";
    public static final String SECOND_EVENT = "second";
    public static final String THIRD_EVENT = "third";

    @Test
    void unorderedWithin() {
        when.simStarts();
        when.testEvent.scheduled(2, FIRST_EVENT);
        when.testEvent.scheduled(6, SECOND_EVENT);

        then.testEvent.unordered().within(7, TimeUnit.MILLISECONDS).occurs(SECOND_EVENT);
        then.testEvent.occurs(FIRST_EVENT);
    }

    @Test
    void afterWithinUnordered() {
        when.simStarts();
        when.testEvent.scheduled(2, FIRST_EVENT);
        when.testEvent.scheduled(6, SECOND_EVENT);

        then.testEvent.within(7, TimeUnit.MILLISECONDS).unordered().occurs(SECOND_EVENT);
        then.testEvent.occurs(FIRST_EVENT);
    }

    @Test
    void withinChainedUnordered() {
        when.simStarts();
        when.testEvent.scheduled(2, FIRST_EVENT);
        when.testEvent.scheduled(6, SECOND_EVENT);
        when.testEvent.scheduled(7, THIRD_EVENT);

        then.testEvent.within(8, TimeUnit.MILLISECONDS).unordered().occurs(THIRD_EVENT);
        then.testEvent.within(7, TimeUnit.MILLISECONDS).unordered().occurs(SECOND_EVENT);
        then.testEvent.occurs(FIRST_EVENT);
    }

    @Test
    void unorderedAfterExactly() {
        when.simStarts();
        when.testEvent.scheduled(2, FIRST_EVENT);
        when.testEvent.scheduled(6, SECOND_EVENT);

        then.testEvent.unordered().afterExactly(6, TimeUnit.MILLISECONDS).occurs(SECOND_EVENT);
        then.testEvent.occurs(FIRST_EVENT);
    }

    @Test
    void afterExactlyUnordered() {
        when.simStarts();
        when.testEvent.scheduled(2, FIRST_EVENT);
        when.testEvent.scheduled(6, SECOND_EVENT);

        then.testEvent.afterExactly(6, TimeUnit.MILLISECONDS).unordered().occurs(SECOND_EVENT);
        then.testEvent.occurs(FIRST_EVENT);
    }

    @Test
    void afterExactlyChainedUnordered() {
        when.simStarts();
        when.testEvent.scheduled(2, FIRST_EVENT);
        when.testEvent.scheduled(6, SECOND_EVENT);
        when.testEvent.scheduled(7, THIRD_EVENT);

        then.testEvent.afterExactly(7, TimeUnit.MILLISECONDS).unordered().occurs(THIRD_EVENT);
        then.testEvent.afterExactly(6, TimeUnit.MILLISECONDS).unordered().occurs(SECOND_EVENT);
        then.testEvent.occurs(FIRST_EVENT);
    }

    @Test
    void unorderedAfterAtLeast() {
        when.simStarts();
        when.testEvent.scheduled(2, FIRST_EVENT);
        when.testEvent.scheduled(6, SECOND_EVENT);

        then.testEvent.unordered().afterAtLeast(5, TimeUnit.MILLISECONDS).occurs(SECOND_EVENT);
        then.testEvent.occurs(FIRST_EVENT);
    }

    @Test
    void afterAtLeastUnordered() {
        when.simStarts();
        when.testEvent.scheduled(2, FIRST_EVENT);
        when.testEvent.scheduled(6, SECOND_EVENT);

        then.testEvent.afterAtLeast(5, TimeUnit.MILLISECONDS).unordered().occurs(SECOND_EVENT);
        then.testEvent.occurs(FIRST_EVENT);
    }

    @Test
    void afterAtLeastChainedUnordered() {
        when.simStarts();
        when.testEvent.scheduled(2, FIRST_EVENT);
        when.testEvent.scheduled(6, SECOND_EVENT);
        when.testEvent.scheduled(7, THIRD_EVENT);

        then.testEvent.afterAtLeast(6, TimeUnit.MILLISECONDS).unordered().occurs(THIRD_EVENT);
        then.testEvent.afterAtLeast(5, TimeUnit.MILLISECONDS).unordered().occurs(SECOND_EVENT);
        then.testEvent.occurs(FIRST_EVENT);
    }
}
