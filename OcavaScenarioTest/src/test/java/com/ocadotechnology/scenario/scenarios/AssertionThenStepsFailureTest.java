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
package com.ocadotechnology.scenario.scenarios;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.MutableStepFuture;
import com.ocadotechnology.scenario.StepFuture;
import com.ocadotechnology.scenario.Story;

/**
 * Tests for the AssertionThenSteps class methods that test failure cases
 */
@Story
public class AssertionThenStepsFailureTest extends AbstractFrameworkTestStory {

    @Override
    public void executeTestSteps() {
        Assertions.assertThrows(
                IllegalStateException.class,
                super::executeTestSteps);
    }

    @Test
    public void testValueIsInCollectionFailsWhenValueIsNotInCollection() {
        StepFuture<String> expectedFuture = StepFuture.of("hi");

        StepFuture<String> future1 = StepFuture.of("hello");
        StepFuture<String> future2 = StepFuture.of("greetings");
        StepFuture<String> future3 = StepFuture.of("salutations");
        ImmutableList<StepFuture<String>> futureCollection = ImmutableList.of(future1, future2, future3);

        when.simStarts();
        then.assertThat.valueIsInCollection(expectedFuture, futureCollection);
    }

    @Test
    public void testValueIsInCollectionFailsWhenValueIsNotPopulated() {
        StepFuture<String> expectedFuture = new MutableStepFuture<>();

        StepFuture<String> future1 = new MutableStepFuture<>();;
        StepFuture<String> future2 = StepFuture.of("hi");
        StepFuture<String> future3 = StepFuture.of("hello");
        ImmutableList<StepFuture<String>> futureCollection = ImmutableList.of(future1, future2, future3);

        when.simStarts();
        then.assertThat.valueIsInCollection(expectedFuture, futureCollection);
    }

    @Test
    public void testValuesAreEqualFailsWhenValuesAreUnequal() {
        StepFuture<String> future1 = StepFuture.of("hi");
        StepFuture<String> future2 = StepFuture.of("hello");

        when.simStarts();
        then.assertThat.valuesAreEqual(future1, future2);
    }

    @Test
    public void testValuesAreEqualFailsWhenOneValueIsUnpopulated() {
        StepFuture<String> future1 = StepFuture.of("hi");
        StepFuture<String> future2 = new MutableStepFuture<>();

        when.simStarts();
        then.assertThat.valuesAreEqual(future1, future2);
    }

    @Test
    public void testValuesAreEqualFailsWhenBothValuesAreUnpopulated() {
        StepFuture<String> future1 = new MutableStepFuture<>();
        StepFuture<String> future2 = new MutableStepFuture<>();

        when.simStarts();
        then.assertThat.valuesAreEqual(future1, future2);
    }

    @Test
    public void testValuesAreDistinctFailsWhenValuesAreIndistinct() {
        StepFuture<String> future1 = StepFuture.of("hi");
        StepFuture<String> future2 = StepFuture.of("hello");
        StepFuture<String> future3 = StepFuture.of("hi");

        when.simStarts();
        then.assertThat.valuesAreDistinct(future1, future2, future3);
    }

    @Test
    public void testValuesAreDistinctFailsWhenNotAllValuesArePopulated() {
        StepFuture<String> future1 = StepFuture.of("hi");
        StepFuture<String> future2 = StepFuture.of("hello");
        StepFuture<String> future3 = new MutableStepFuture<>();

        when.simStarts();
        then.assertThat.valuesAreDistinct(future1, future2, future3);
    }
}
