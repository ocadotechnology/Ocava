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
package com.ocadotechnology.random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

public class RandomWeightedChooserTest {
    @Test
    public void whenWeightIsZero_thenDoesNotChooseItem() {
        RandomWeightedChooser<String> chooser = new RandomWeightedChooser<>(ImmutableMap.of("A", 0d, "B", 1d, "C", 1d));
        RepeatableRandom.initialiseWithFixedValue(0);
        Assertions.assertNotEquals(chooser.choose(), "A");
        RepeatableRandom.initialiseWithFixedValue(0.01);
        Assertions.assertNotEquals(chooser.choose(), "A");
    }

    private static final ImmutableMap<String, Double> WEIGHTED_MAP =
            ImmutableMap.of("A", 1d, "B", 2d, "C", 3d);

    private static final Double SUM =
            WEIGHTED_MAP.values().stream().mapToDouble(w -> w).sum();

    private RandomWeightedChooser<String> createWeightedChooser(double seed) {
        return new RandomWeightedChooser<>(WEIGHTED_MAP, InstancedRepeatableRandom.fromFixedValue(seed));
    }

    @Test
    public void whenWeightIsZero_thenDoesNotChooseItemUsingInstancedRandom() {
        RandomWeightedChooser<String> chooser = new RandomWeightedChooser<>(
                ImmutableMap.of("A", 0d, "B", 1d, "C", 1d),
                InstancedRepeatableRandom.fromFixedValue(0)
        );
        Assertions.assertNotEquals(chooser.choose(), "A");
        chooser = new RandomWeightedChooser<>(
                ImmutableMap.of("A", 0d, "B", 1d, "C", 1d),
                InstancedRepeatableRandom.fromFixedValue(0.01)
        );
        Assertions.assertNotEquals(chooser.choose(), "A");
    }

    @Test
    public void whenRepeatableRandomReturnsSpecificValue_thenReturnsAppropriateItem() {
        RandomWeightedChooser<String> chooser = new RandomWeightedChooser<>(WEIGHTED_MAP);

        RepeatableRandom.initialiseWithFixedValue(0 / SUM);
        Assertions.assertEquals(chooser.choose(), "A");
        RepeatableRandom.initialiseWithFixedValue(0.9 / SUM);
        Assertions.assertEquals(chooser.choose(), "A");
        RepeatableRandom.initialiseWithFixedValue(1.1 / SUM);
        Assertions.assertEquals(chooser.choose(), "B");
        RepeatableRandom.initialiseWithFixedValue(2.9 / SUM);
        Assertions.assertEquals(chooser.choose(), "B");
        RepeatableRandom.initialiseWithFixedValue(3.1 / SUM);
        Assertions.assertEquals(chooser.choose(), "C");
        RepeatableRandom.initialiseWithFixedValue(5.9 / SUM);
        Assertions.assertEquals(chooser.choose(), "C");
    }

    @Test
    public void whenInstancedRepeatableRandomReturnsSpecificValue_thenReturnsAppropriateItem() {
        Assertions.assertEquals(createWeightedChooser(0 / SUM).choose(), "A");
        Assertions.assertEquals(createWeightedChooser(0.9 / SUM).choose(), "A");
        Assertions.assertEquals(createWeightedChooser(1.1 / SUM).choose(), "B");
        Assertions.assertEquals(createWeightedChooser(2.9 / SUM).choose(), "B");
        Assertions.assertEquals(createWeightedChooser(3.1 / SUM).choose(), "C");
        Assertions.assertEquals(createWeightedChooser(5.9 / SUM).choose(), "C");
    }
}