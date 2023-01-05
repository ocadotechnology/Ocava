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
package com.ocadotechnology.random;

import java.util.Map.Entry;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.validation.Failer;

/**
 * Utility to choose randomly from a supplied set of items, choosing each in proportion to the provided weight of that
 * item. Never returns null, but preconditions on being supplied invalid arguments.
 */
@ParametersAreNonnullByDefault
public class RandomWeightedChooser<E> {
    private final Optional<InstancedRepeatableRandom> instancedRepeatableRandom;
    private final ImmutableMap<E, Double> itemsByWeight;
    private final double sumOfWeights;

    public RandomWeightedChooser(ImmutableMap<E, Double> itemsByWeight) {
        this(itemsByWeight, Optional.empty());
    }

    public RandomWeightedChooser(
            ImmutableMap<E, Double> itemsByWeight,
            InstancedRepeatableRandom instancedRepeatableRandom) {
        this(itemsByWeight, Optional.of(instancedRepeatableRandom));
    }

    public RandomWeightedChooser(
            ImmutableMap<E, Double> itemsByWeight,
            Optional<InstancedRepeatableRandom> instancedRepeatableRandom) {
        Preconditions.checkArgument(!itemsByWeight.isEmpty(),
                "Must supply non-empty set of items from which to choose");
        itemsByWeight.forEach((item, weight) -> Preconditions.checkState(weight >= 0,
                "Negative weight supplied for item %s - cannot correspond to a probability", item));
        this.itemsByWeight = itemsByWeight;
        this.instancedRepeatableRandom = instancedRepeatableRandom;
        sumOfWeights = itemsByWeight.values().stream().mapToDouble(w -> w).sum();
        Preconditions.checkArgument(sumOfWeights > 0,
                "Non-empty item set should have at least one positive weight");
    }

    public E choose() {
        double nextDouble = instancedRepeatableRandom.map(InstancedRepeatableRandom::nextDouble)
                .orElseGet(RepeatableRandom::nextDouble);
        double rand = nextDouble * sumOfWeights;
        double cumulativeSum = 0;
        for (Entry<E, Double> entry : itemsByWeight.entrySet()) {
            cumulativeSum += entry.getValue();
            if (cumulativeSum > rand) { // Not ">=" to avoid choosing zero weight items when rand is 0!
                return entry.getKey();
            }
        }
        throw Failer.fail("Failed to make a random choice between specified elements");
    }
}