/*
 * Copyright © 2017-2020 Ocado (Ocava)
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

public class ImmutableAbsoluteProbabilityChooserTest {
    private static final String DEFAULT = "DEFAULT";
    private static final String VALUE_1 = "VALUE_1";
    private static final String VALUE_2 = "VALUE_2";
    private static final String VALUE_3 = "VALUE_3";

    @Test
    void whenNoProbabilitiesDefined_thenReturnsDefaultValue() {
        ImmutableAbsoluteProbabilityChooser<String> chooser = ImmutableAbsoluteProbabilityChooser.create(DEFAULT)
                .build();
        testInRange(chooser, 0, 1, DEFAULT);
    }

    @Test
    void whenItemProbabilityDefined_thenReturnsExpectedValue() {
        ImmutableAbsoluteProbabilityChooser<String> chooser = ImmutableAbsoluteProbabilityChooser.create(DEFAULT)
                .withOutcome(VALUE_1, 0.5)
                .build();
        testInRange(chooser, 0, 0.5, VALUE_1);
        testInRange(chooser, 0.5, 1, DEFAULT);
    }

    @Test
    void whenMultipleItemProbabilitiesDefined_thenReturnsExpectedValue() {
        ImmutableAbsoluteProbabilityChooser<String> chooser = ImmutableAbsoluteProbabilityChooser.create(DEFAULT)
                .withOutcome(VALUE_1, 0.5)
                .withOutcome(VALUE_2, 0.3)
                .withOutcome(VALUE_3, 0.2)
                .build();
        testInRange(chooser, 0, 0.5, VALUE_1);
        testInRange(chooser, 0.5, 0.8, VALUE_2);
        testInRange(chooser, 0.8, 1, VALUE_3);
    }

    @Test
    void whenItemProbabilityRepeated_thenUsesTotalProbability() {
        ImmutableAbsoluteProbabilityChooser<String> chooser = ImmutableAbsoluteProbabilityChooser.create(DEFAULT)
                .withOutcome(VALUE_1, 0.5)
                .withOutcome(VALUE_1, 0.2)
                .build();
        testInRange(chooser, 0, 0.7, VALUE_1);
        testInRange(chooser, 0.7, 1, DEFAULT);
    }

    @Test
    void whenDefaultResultProbabilitySet_thenThrowsException() {
        ImmutableAbsoluteProbabilityChooser.Builder<String> builder = ImmutableAbsoluteProbabilityChooser.create(DEFAULT);
        Assertions.assertThrows(IllegalArgumentException.class, () -> builder.withOutcome(DEFAULT, 0.5));
    }

    @Test
    void whenNegativeProbabilitySet_thenThrowsException() {
        ImmutableAbsoluteProbabilityChooser.Builder<String> builder = ImmutableAbsoluteProbabilityChooser.create(DEFAULT);
        Assertions.assertThrows(IllegalArgumentException.class, () -> builder.withOutcome(VALUE_1, -0.1));
    }

    @Test
    void whenSingleTooLargeProbabilitySet_thenThrowsException() {
        ImmutableAbsoluteProbabilityChooser.Builder<String> builder = ImmutableAbsoluteProbabilityChooser.create(DEFAULT);
        Assertions.assertThrows(IllegalStateException.class, () -> builder.withOutcome(VALUE_1, 1.1));
    }

    @Test
    void whenCumulativeProbabilitySetTooLarge_thenThrowsException() {
        ImmutableAbsoluteProbabilityChooser.Builder<String> builder = ImmutableAbsoluteProbabilityChooser.create(DEFAULT);
        builder.withOutcome(VALUE_1, 0.9);
        Assertions.assertThrows(IllegalStateException.class, () -> builder.withOutcome(VALUE_2, 0.2));
    }

    private void testInRange(ImmutableAbsoluteProbabilityChooser<String> chooser, double lowerInclusive, double upperExclusive, String expectedValue) {
        for (double selectionValue = lowerInclusive; selectionValue < upperExclusive; selectionValue += (upperExclusive - lowerInclusive) / 20) {
            RepeatableRandom.initialiseWithFixedValue(selectionValue);
            Assertions.assertEquals(expectedValue, chooser.choose());
        }
    }
}
