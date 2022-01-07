/*
 * Copyright © 2017-2022 Ocado (Ocava)
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
package com.ocadotechnology.random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MutableAbsoluteProbabilityChooserTest {
    private static final String DEFAULT = "DEFAULT";
    private static final String VALUE_1 = "VALUE_1";
    private static final String VALUE_2 = "VALUE_2";
    private static final String VALUE_3 = "VALUE_3";

    private MutableAbsoluteProbabilityChooser<String> chooser = new MutableAbsoluteProbabilityChooser<>(DEFAULT);

    @Test
    void whenNoProbabilitiesDefined_thenReturnsDefaultValue() {
        testInRange(0, 1, DEFAULT);
    }

    @Test
    void whenOutcomeProbabilityDefined_thenReturnsExpectedValue() {
        chooser.setProbability(VALUE_1, 0.5);
        testInRange(0, 0.5, VALUE_1);
        testInRange(0.5, 1, DEFAULT);
    }

    @Test
    void whenMultipleOutcomeProbabilitiesDefined_thenReturnsExpectedValue() {
        chooser.setProbability(VALUE_1, 0.5);
        chooser.setProbability(VALUE_2, 0.3);
        chooser.setProbability(VALUE_3, 0.2);
        testInRange(0, 0.5, VALUE_1);
        testInRange(0.5, 0.8, VALUE_2);
        testInRange(0.8, 1, VALUE_3);
    }

    @Test
    void whenOutcomeProbabilityOverridden_thenUsesNewProbability() {
        chooser.setProbability(VALUE_1, 0.5);
        testInRange(0, 0.5, VALUE_1);
        testInRange(0.5, 1, DEFAULT);

        chooser.setProbability(VALUE_1, 0.7);
        testInRange(0, 0.7, VALUE_1);
        testInRange(0.7, 1, DEFAULT);
    }

    @Test
    void whenOutcomeProbabilitiesCleared_thenRevertsToDefaultBehaviour() {
        chooser.setProbability(VALUE_1, 0.5);
        testInRange(0, 0.5, VALUE_1);
        testInRange(0.5, 1, DEFAULT);

        chooser.clear();
        testInRange(0, 1, DEFAULT);
    }

    @Test
    void whenOutcomeRemoved_thenRevertsToDefaultBehaviour() {
        chooser.setProbability(VALUE_1, 0.5);
        testInRange(0, 0.5, VALUE_1);
        testInRange(0.5, 1, DEFAULT);

        chooser.removeOutcome(VALUE_1);
        testInRange(0, 1, DEFAULT);
    }

    @Test
    void whenDefaultResultProbabilitySet_thenThrowsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> chooser.setProbability(DEFAULT, 0.5));
    }

    @Test
    void whenNegativeProbabilitySet_thenThrowsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> chooser.setProbability(VALUE_1, -0.1));
    }

    @Test
    void whenSingleTooLargeProbabilitySet_thenThrowsException() {
        Assertions.assertThrows(IllegalStateException.class, () -> chooser.setProbability(VALUE_1, 1.1));
    }

    @Test
    void whenCumulativeProbabilitySetTooLarge_thenThrowsException() {
        chooser.setProbability(VALUE_1, 0.9);
        Assertions.assertThrows(IllegalStateException.class, () -> chooser.setProbability(VALUE_2, 0.2));
    }

    private void testInRange(double lowerInclusive, double upperExclusive, String expectedValue) {
        for (double selectionValue = lowerInclusive; selectionValue < upperExclusive; selectionValue += (upperExclusive - lowerInclusive) / 20) {
            RepeatableRandom.initialiseWithFixedValue(selectionValue);
            Assertions.assertEquals(expectedValue, chooser.choose());
        }
    }
}
