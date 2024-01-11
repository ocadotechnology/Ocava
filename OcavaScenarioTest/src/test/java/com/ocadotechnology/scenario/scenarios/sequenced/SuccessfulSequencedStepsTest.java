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

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.StepFuture;
import com.ocadotechnology.scenario.Story;

/**
 * A collection of tests to prove sequenced steps will pass as expected
 */
@Story
public class SuccessfulSequencedStepsTest extends AbstractFrameworkTestStory {
    private static final String SEQUENCE_A_EVENT_1 = "SequenceA firstEvent";
    private static final String SEQUENCE_A_EVENT_2 = "SequenceA secondEvent";
    private static final String SEQUENCE_B_EVENT_1 = "SequenceB firstEvent";
    private static final String SEQUENCE_B_EVENT_2 = "SequenceB secondEvent";

    private static final String SEQUENCE_A = "SequenceA";
    private static final String SEQUENCE_B = "SequenceB";

    /**
     * Test that we can successfully run a sequence of check steps
     */
    @Test
    void checkStepsInSequence() {
        when.simStarts();
        when.testEvent.scheduled(1, SEQUENCE_A_EVENT_1);
        when.testEvent.scheduled(2, SEQUENCE_B_EVENT_1);
        when.testEvent.scheduled(3, SEQUENCE_B_EVENT_2);
        when.testEvent.scheduled(4, SEQUENCE_A_EVENT_2);

        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_1);
        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_2);
        then.testEvent.sequenced(SEQUENCE_B).occurs(SEQUENCE_B_EVENT_1);
        then.testEvent.sequenced(SEQUENCE_B).occurs(SEQUENCE_B_EVENT_2);
        then.unordered.waitForSteps(SEQUENCE_A, SEQUENCE_B);
    }

    /**
     * Test that we can successfully run a sequence of check steps with time constraint modifiers
     */
    @Test
    void timeConstrainedCheckStepsInSequence() {
        when.simStarts();
        when.testEvent.scheduled(1, SEQUENCE_A_EVENT_1);
        when.testEvent.scheduled(2, SEQUENCE_B_EVENT_1);
        when.testEvent.scheduled(3, SEQUENCE_B_EVENT_2);
        when.testEvent.scheduled(4, SEQUENCE_A_EVENT_2);

        then.testEvent.sequenced(SEQUENCE_A).within(5, TimeUnit.MILLISECONDS).occurs(SEQUENCE_A_EVENT_1);
        then.testEvent.sequenced(SEQUENCE_A).afterAtLeast(2, TimeUnit.MILLISECONDS).occurs(SEQUENCE_A_EVENT_2);
        then.testEvent.sequenced(SEQUENCE_B).occurs(SEQUENCE_B_EVENT_1);
        then.testEvent.sequenced(SEQUENCE_B).afterExactly(1, TimeUnit.MILLISECONDS).occurs(SEQUENCE_B_EVENT_2);
        then.unordered.waitForSteps(SEQUENCE_A, SEQUENCE_B);
    }

    /**
     * Test that we can successfully run a sequence including ExecuteSteps from the AbstractThenSteps
     */
    @Test
    void thenExecuteStepsInSequence() {
        when.simStarts();
        when.testEvent.scheduled(1, SEQUENCE_A_EVENT_1);
        when.testEvent.scheduled(2, SEQUENCE_B_EVENT_1);

        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_1);
        then.testEvent.sequenced(SEQUENCE_A).timeIsExactly(1);
        then.testEvent.sequenced(SEQUENCE_B).timeIsExactly(0); //Test that when a sequence starts with an execute step, that step is executed at the correct time.
        then.testEvent.sequenced(SEQUENCE_B).occurs(SEQUENCE_B_EVENT_1);
        then.unordered.waitForSteps(SEQUENCE_A, SEQUENCE_B);
    }

    /**
     * Test that we can successfully run a sequence including ExecuteSteps from the AbstractWhenSteps
     */
    @Test
    void whenExecuteStepsInSequence() {
        when.simStarts();
        when.testEvent.scheduled(4, SEQUENCE_A_EVENT_1);
        when.testEvent.scheduled(5, SEQUENCE_B_EVENT_1);

        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_1);
        when.testEvent.sequenced(SEQUENCE_A).scheduledIn(1, SEQUENCE_A_EVENT_2);
        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_2);

        then.testEvent.sequenced(SEQUENCE_B).occurs(SEQUENCE_B_EVENT_1);
        when.testEvent.sequenced(SEQUENCE_B).scheduledIn(2, SEQUENCE_B_EVENT_2);
        then.testEvent.sequenced(SEQUENCE_B).occurs(SEQUENCE_B_EVENT_2);

        then.unordered.waitForSteps(SEQUENCE_A, SEQUENCE_B);
    }

    /**
     * Test that we can successfully call unordered waitForSteps within a sequence
     */
    @Test
    void waitForStepsInSequence () {
        when.simStarts();
        when.testEvent.scheduled(1, SEQUENCE_A_EVENT_1);
        when.testEvent.scheduled(2, SEQUENCE_B_EVENT_1);
        when.testEvent.scheduled(3, SEQUENCE_B_EVENT_2);
        when.testEvent.scheduled(4, SEQUENCE_A_EVENT_2);

        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_1);
        then.unordered.sequenced(SEQUENCE_A).waitForSteps(SEQUENCE_B);
        then.testEvent.sequenced(SEQUENCE_A).timeIsExactly(3); //Validate that a subsequent execute step will be triggered immediately
        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_2);

        then.testEvent.sequenced(SEQUENCE_B).occurs(SEQUENCE_B_EVENT_1);
        then.testEvent.sequenced(SEQUENCE_B).occurs(SEQUENCE_B_EVENT_2);
        then.unordered.waitForSteps(SEQUENCE_A);
    }

    /**
     * Test that we can successfully call removesUnorderedSteps in a sequence
     */
    @Test
    void removeUnorderedForStepsInSequence () {
        when.simStarts();
        when.testEvent.scheduled(1, SEQUENCE_A_EVENT_1);
        when.testEvent.scheduled(2, SEQUENCE_B_EVENT_1);
        when.testEvent.scheduled(3, SEQUENCE_B_EVENT_2);
        when.testEvent.scheduled(4, SEQUENCE_A_EVENT_2);

        String neverStep = "Never Step";
        then.testEvent.never(neverStep).occurs(SEQUENCE_A_EVENT_2);

        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_1);
        then.unordered.sequenced(SEQUENCE_A).removesUnorderedSteps(neverStep);
        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_2);

        then.testEvent.sequenced(SEQUENCE_B).occurs(SEQUENCE_B_EVENT_1);
        then.testEvent.sequenced(SEQUENCE_B).occurs(SEQUENCE_B_EVENT_2);
        then.unordered.waitForSteps(SEQUENCE_A);
    }

    /**
     * Test that we can successfully run a sequence including WaitSteps
     */
    @Test
    void timeWaitStepSequences() {
        when.simStarts();
        when.testEvent.scheduled(1, SEQUENCE_A_EVENT_1);
        when.testEvent.scheduled(2, SEQUENCE_B_EVENT_1);

        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_1);
        then.time.sequenced(SEQUENCE_A).waitForDuration(7, TimeUnit.MILLISECONDS);
        StepFuture<Double> sequenceATime = then.time.sequenced(SEQUENCE_A).getCurrentTime();

        then.testEvent.sequenced(SEQUENCE_B).occurs(SEQUENCE_B_EVENT_1);
        then.time.sequenced(SEQUENCE_B).waitForDuration(9, TimeUnit.MILLISECONDS);
        StepFuture<Double> sequenceBTime = then.time.sequenced(SEQUENCE_B).getCurrentTime();

        then.unordered.waitForSteps(SEQUENCE_A, SEQUENCE_B);

        then.futures.assertEquals(8.0, sequenceATime);
        then.futures.assertEquals(11.0, sequenceBTime);
    }

    /**
     * Test that we can successfully run a sequence including ExecuteSteps from the TimeThenSteps
     */
    @Test
    void timeExecuteStepSequences() {
        when.simStarts();
        when.testEvent.scheduled(1, SEQUENCE_A_EVENT_1);
        when.testEvent.scheduled(10, SEQUENCE_B_EVENT_1);

        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_1);
        StepFuture<Double> sequenceATime = then.time.sequenced(SEQUENCE_A).getCurrentTime();
        then.testEvent.sequenced(SEQUENCE_B).occurs(SEQUENCE_B_EVENT_1);
        StepFuture<Double> sequenceBTime = then.time.sequenced(SEQUENCE_B).getCurrentTime();
        then.unordered.waitForSteps(SEQUENCE_A, SEQUENCE_B);

        then.futures.assertEquals(1.0, sequenceATime);
        then.futures.assertEquals(10.0, sequenceBTime);
    }
}
