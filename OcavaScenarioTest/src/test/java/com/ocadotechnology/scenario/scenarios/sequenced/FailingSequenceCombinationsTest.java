/*
 * Copyright © 2017-2024 Ocado (Ocava)
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
package com.ocadotechnology.scenario.scenarios.sequenced;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.Story;

/**
 * A collection of tests to prove that using a sequence name for other unordered step modifiers is not permitted.
 */
@Story
public class FailingSequenceCombinationsTest extends AbstractFrameworkTestStory {
    private static final String EVENT_NAME = "event";
    private static final String SEQUENCE = "sequence";

    @Override
    public void executeTestSteps() {
        IllegalStateException e = Assertions.assertThrows(
                IllegalStateException.class,
                super::executeTestSteps,
                "No error thrown");
        Assertions.assertTrue(
                e.getMessage().startsWith("Cannot add sequenced step with name [" + SEQUENCE + "] as it has already been used for an unordered step.")
                || e.getMessage().startsWith("Cannot add unordered step with name [" + SEQUENCE + "] as it has already been used for a sequenced step."),
                "Incorrect exception message: " + e.getMessage()
        );
    }

    /**
     * Test that we correctly fail if a sequence contains unordered steps
     */
    @Test
    void checkSequencedAndUnordered() {
        when.simStarts();

        //Delay building the unordered steps
        when.testEvent.scheduled(10, EVENT_NAME);

        then.testEvent.sequenced(SEQUENCE).executeStep(false);
        then.testEvent.unordered(SEQUENCE).occurs(EVENT_NAME);
    }

    @Test
    void checkUnorderedAndSequenced() {
        when.simStarts();

        //Delay building the unordered steps
        when.testEvent.scheduled(10, EVENT_NAME);
        then.testEvent.occurs(EVENT_NAME);

        then.testEvent.unordered(SEQUENCE).occurs(EVENT_NAME);
        then.testEvent.sequenced(SEQUENCE).executeStep(false);
    }

    /**
     * Test that we correctly fail if a sequence contains never steps
     */
    @Test
    void checkSequencedAndNever() {
        when.simStarts();

        then.testEvent.sequenced(SEQUENCE).executeStep(false);
        then.testEvent.never(SEQUENCE).occurs(EVENT_NAME);
    }

    @Test
    void checkNeverAndSequenced() {
        when.simStarts();

        then.testEvent.never(SEQUENCE).occurs(EVENT_NAME);
        then.testEvent.sequenced(SEQUENCE).executeStep(false);
    }
}
