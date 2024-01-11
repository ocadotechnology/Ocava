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
import com.ocadotechnology.scenario.Story;

/**
 * Wait steps pause the execution of other steps in the test. These tests ensure that a wait step in a sequence does not
 * prevent other sequences or the main path from progressing, and vice versa.
 */
@Story
public class SequencedWaitStepsTest extends AbstractFrameworkTestStory {
    private static final String SEQUENCE_A_EVENT_1 = "SequenceA firstEvent";
    private static final String SEQUENCE_A_EVENT_2 = "SequenceA secondEvent";
    private static final String SEQUENCE_B_EVENT_1 = "SequenceB firstEvent";
    private static final String SEQUENCE_B_EVENT_2 = "SequenceB secondEvent";
    private static final String MAIN_PATH_EVENT_1 = "Main Path firstEvent";
    private static final String MAIN_PATH_EVENT_2 = "Main Path secondEvent";

    private static final String SEQUENCE_A = "SequenceA";
    private static final String SEQUENCE_B = "SequenceB";

    /**
     * Test that a wait step in a sequence does not prevent other paths from proceeding
     */
    @Test
    void waitStepInSequenceDoesntBlockOtherPaths() {
        when.simStarts();
        when.testEvent.scheduled(1, SEQUENCE_A_EVENT_1);
        //Start waiting on SEQUENCE A
        when.testEvent.scheduled(2, SEQUENCE_B_EVENT_1);
        when.testEvent.scheduled(3, MAIN_PATH_EVENT_1);
        when.testEvent.scheduled(4, SEQUENCE_B_EVENT_2);
        when.testEvent.scheduled(5, MAIN_PATH_EVENT_2);
        //End waiting
        when.testEvent.scheduled(10, SEQUENCE_A_EVENT_2);

        //Sequenced wait step covers all of the other events
        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_1);
        then.time.sequenced(SEQUENCE_A).waitForDuration(8, TimeUnit.MILLISECONDS);
        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_2);

        //Other sequence
        then.testEvent.sequenced(SEQUENCE_B).occurs(SEQUENCE_B_EVENT_1);
        then.testEvent.sequenced(SEQUENCE_B).occurs(SEQUENCE_B_EVENT_2);

        //main path
        then.testEvent.occurs(MAIN_PATH_EVENT_1);
        then.testEvent.occurs(MAIN_PATH_EVENT_2);

        then.unordered.waitForSteps(SEQUENCE_A, SEQUENCE_B);
    }

    /**
     * Test that a wait step in the main path does not prevent sequences from proceeding
     */
    @Test
    void waitStepInMainPathDoesntBlockOtherPaths() {
        when.simStarts();
        when.testEvent.scheduled(1, MAIN_PATH_EVENT_1);
        //Start waiting on main path
        when.testEvent.scheduled(2, SEQUENCE_A_EVENT_1);
        when.testEvent.scheduled(5, SEQUENCE_A_EVENT_2);
        //End waiting
        when.testEvent.scheduled(10, MAIN_PATH_EVENT_2);

        //Sequenced events occur during main path wait
        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_1);
        then.testEvent.sequenced(SEQUENCE_A).occurs(SEQUENCE_A_EVENT_2);

        //main path wait covers all sequence steps
        then.testEvent.occurs(MAIN_PATH_EVENT_1);
        then.time.waitForDuration(8, TimeUnit.MILLISECONDS);
        then.testEvent.occurs(MAIN_PATH_EVENT_2);

        then.unordered.waitForSteps(SEQUENCE_A);
    }
}
