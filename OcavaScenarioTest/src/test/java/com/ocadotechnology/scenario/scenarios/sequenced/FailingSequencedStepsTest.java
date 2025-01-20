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
package com.ocadotechnology.scenario.scenarios.sequenced;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.FixRequired;
import com.ocadotechnology.scenario.Story;

/**
 * A collection of tests to prove that failures will be raised from sequenced steps as expected
 */
@Story
@FixRequired("Checking that tests fail as expected")
public class FailingSequencedStepsTest extends AbstractFrameworkTestStory {
    private static final String SEQUENCE_A_EVENT_1 = "SequenceA firstEvent";
    private static final String SEQUENCE_A_EVENT_2 = "SequenceA secondEvent";
    private static final String SEQUENCE_B_EVENT_1 = "SequenceB firstEvent";

    private static final String SEQUENCE_A = "SequenceA";
    private static final String SEQUENCE_B = "SequenceB";

    /**
     * Test that we correctly fail a sequence of check steps when they occur out of sequence
     */
    @Test
    void checkStepsInSequence() {
        when.simStarts();
        when.testEvent.scheduled(1, SEQUENCE_A_EVENT_2);
        when.testEvent.scheduled(4, SEQUENCE_A_EVENT_1);

        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_1);
        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_2);
        then.unordered.failingStep().waitForSteps(SEQUENCE_A);
    }

    /**
     * Test that we correctly fail a sequence of check steps with time constraint modifiers when the time constraints are violated
     */
    @Test
    void withinCheckStepInSequence() {
        when.simStarts();
        when.testEvent.scheduled(1, SEQUENCE_A_EVENT_1);
        when.testEvent.scheduled(4, SEQUENCE_A_EVENT_2);

        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_1);
        then.testEvent.sequenced(SEQUENCE_A).within(2, TimeUnit.MILLISECONDS).failingStep().occurs(SEQUENCE_A_EVENT_2);
    }

    @Test
    void afterExactlyTooEarlyCheckStepInSequence() {
        when.simStarts();
        when.testEvent.scheduled(1, SEQUENCE_A_EVENT_1);
        when.testEvent.scheduled(4, SEQUENCE_A_EVENT_2);

        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_1);
        then.testEvent.sequenced(SEQUENCE_A).afterExactly(4, TimeUnit.MILLISECONDS).failingStep().occurs(SEQUENCE_A_EVENT_2);
    }

    @Test
    void afterExactlyTooLateCheckStepInSequence() {
        when.simStarts();
        when.testEvent.scheduled(1, SEQUENCE_A_EVENT_1);
        when.testEvent.scheduled(4, SEQUENCE_A_EVENT_2);

        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_1);
        then.testEvent.sequenced(SEQUENCE_A).afterExactly(2, TimeUnit.MILLISECONDS).failingStep().occurs(SEQUENCE_A_EVENT_2);
    }

    @Test
    void afterAtLeastCheckStepInSequence() {
        when.simStarts();
        when.testEvent.scheduled(1, SEQUENCE_A_EVENT_1);
        when.testEvent.scheduled(4, SEQUENCE_A_EVENT_2);

        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_1);
        then.testEvent.sequenced(SEQUENCE_A).afterAtLeast(3.5, TimeUnit.MILLISECONDS).failingStep().occurs(SEQUENCE_A_EVENT_2);
    }

    /**
     * Test that we correctly fail a sequence including ExecuteSteps from the AbstractThenSteps
     */
    @Test
    void thenExecuteStepsInSequence() {
        when.simStarts();
        when.testEvent.scheduled(1, SEQUENCE_A_EVENT_1);

        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_1);
        then.testEvent.sequenced(SEQUENCE_A).failingStep().executeStep(true);
        then.unordered.waitForSteps(SEQUENCE_A);
    }

    /**
     * Test that we correctly fail a sequence including ExecuteSteps from the AbstractWhenSteps
     */
    @Test
    void whenExecuteStepsInSequence() {
        when.simStarts();
        when.testEvent.scheduled(4, SEQUENCE_A_EVENT_1);

        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_1);
        //This fails due to scheduling the event in the past, but when steps don't have a failingStep modifier since they so rarely fail.
        when.testEvent.sequenced(SEQUENCE_A).scheduled(1, SEQUENCE_A_EVENT_2);
        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_2);

        then.unordered.waitForSteps(SEQUENCE_A);
    }

    /**
     * Test that we correctly fail when unordered waitForSteps fails within a sequence
     */
    @Test
    void waitForStepsInSequence () {
        when.simStarts();
        when.testEvent.scheduled(1, SEQUENCE_A_EVENT_1);
        when.testEvent.scheduled(4, SEQUENCE_A_EVENT_2);

        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_1);
        then.unordered.sequenced(SEQUENCE_A).failingStep().waitForSteps(SEQUENCE_B);
        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_2);

        then.testEvent.sequenced(SEQUENCE_B).occurs(SEQUENCE_B_EVENT_1);
    }

    /**
     * Test that we fail correctly when removesUnorderedSteps is called late in a sequence
     */
    @Test
    void removeUnorderedForStepsInSequence () {
        when.simStarts();
        when.testEvent.scheduled(1, SEQUENCE_A_EVENT_2);
        when.testEvent.scheduled(4, SEQUENCE_A_EVENT_1);

        String neverStep = "Never Step";
        then.testEvent.never(neverStep).failingStep().occurs(SEQUENCE_A_EVENT_2);

        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_1);
        then.unordered.sequenced(SEQUENCE_A).removesUnorderedSteps(neverStep);
        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_2);
        then.unordered.waitForSteps(SEQUENCE_A);
    }

    /**
     * Test that we fail correctly when a sequence including WaitSteps causes scheduled events to be missed
     */
    @Test
    void timeWaitStepSequences() {
        when.simStarts();
        when.testEvent.scheduled(1, SEQUENCE_A_EVENT_1);
        when.testEvent.scheduled(2, SEQUENCE_A_EVENT_2);

        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_1);
        then.time.sequenced(SEQUENCE_A).waitForDuration(7, TimeUnit.MILLISECONDS);
        then.testEvent.sequenced(SEQUENCE_A).failingStep().occurs(SEQUENCE_A_EVENT_2);
    }

    /**
     * Test that we fail correctly when a sequence including ExecuteSteps from the TimeThenSteps
     */
    @Test
    void timeExecuteStepSequences() {
        when.simStarts();
        when.testEvent.scheduled(10, SEQUENCE_A_EVENT_1);

        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_1);
        then.time.sequenced(SEQUENCE_A).failingStep().timeIsLessThan(5, TimeUnit.MILLISECONDS);
    }
}
