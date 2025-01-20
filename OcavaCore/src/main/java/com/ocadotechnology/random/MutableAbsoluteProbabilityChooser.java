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
package com.ocadotechnology.random;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnegative;

import com.google.common.base.Preconditions;

/**
 * Encapsulation of logic for choosing probabilistically between multiple outcomes.
 *
 * Each outcome has an associated probability, where the total of all probabilities is no greater than 1. There is also
 * a default outcome for when none of the other outcomes are chosen. The probabilities are not known at construction
 * time and can subsequently be defined and altered.
 *
 * <pre>
 * 0                                                     1
 * +-------+-------+-------+-------+-------+-------------+
 * | p(o1) | p(o2) | p(o3) |  ...  | p(oN) | p(oDefault) |
 * +-------+-------+-------+-------+-------+-------------+
 * </pre>
 *
 * @param <T> Common type for each of the possible outcomes.
 */
public class MutableAbsoluteProbabilityChooser<T> {
    public static final double ROUNDING_TOLERANCE = 1e-12;

    private final Map<T, Double> outcomesToProbability = new LinkedHashMap<>();
    private final T defaultOutcome;

    /**
     * @param defaultOutcome the value which will be returned if none of the other options are selected.
     */
    public MutableAbsoluteProbabilityChooser(T defaultOutcome) {
        this.defaultOutcome = defaultOutcome;
    }

    /**
     * Sets the probability of choosing a specific outcome.  Overrides any probability previously set for this outcome.
     *
     * @param outcome the outcome to be returned.
     * @param probability the probability of returning this outcome.  Must be in the range 0 to 1.
     * @throws IllegalArgumentException if the outcome is equal to the defaultOutcome provided in the constructor.
     * @throws IllegalArgumentException if the probability is less than zero.
     * @throws IllegalStateException if the total probability for all defined outcomes sums to greater than 1 (allowing
     *          for a degree of rounding error)
     */
    public void setProbability(T outcome, @Nonnegative double probability) {
        Preconditions.checkArgument(!outcome.equals(defaultOutcome), "Attempted to set the probability of the default result %s", defaultOutcome);
        Preconditions.checkArgument(probability >= 0, "Attempted to set probability for outcome %s to invalid value %s (must be >= 0)", outcome, probability);
        if (probability == 0) {
            outcomesToProbability.remove(outcome);
        } else {
            double newTotalProbability = outcomesToProbability.values().stream().reduce(0.0, Double::sum)
                    + probability
                    - outcomesToProbability.getOrDefault(outcome, 0.0);
            Preconditions.checkState(newTotalProbability <= 1 + ROUNDING_TOLERANCE, "Sum of probabilities for all outcomes has exceeded 1");
            outcomesToProbability.put(outcome, probability);
        }
    }

    /**
     * Removes any chance of returning the selected outcome.
     * @param outcome - the outcome to disable.
     * @throws IllegalArgumentException if the outcome is equal to the defaultOutcome provided in the constructor.
     */
    public void removeOutcome(T outcome) {
        setProbability(outcome, 0);
    }

    /**
     * Clears the probability defined for any outcomes, resetting the Chooser to a state where it is guaranteed to return
     * the defaultOutcome.
     */
    public void clear() {
        outcomesToProbability.clear();
    }

    /**
     * Selects an option from the defined outcomes.
     *
     * Current implementation is O(N) across the number of possible outcomes. Should this be a concern, it would be
     * possible to construct an {@link ImmutableAbsoluteProbabilityChooser} in
     * {@link MutableAbsoluteProbabilityChooser#setProbability(Object, double)}. This would then benefit from the
     * improved performance of the ImmutableRangeMap when calling choose at the cost of additional construction during
     * the setProbability method.
     *
     * @return An outcome selected from those with defined probabilities using a random number provided from the defined
     *          random number supplier.
     */
    public T choose() {
        double rand = RepeatableRandom.nextDouble();
        double cumulativeSum = 0;
        for (Entry<T, Double> entry : outcomesToProbability.entrySet()) {
            cumulativeSum += entry.getValue();
            if (cumulativeSum > rand) { // Not ">=" to avoid choosing zero weight outcomes when rand is 0!
                return entry.getKey();
            }
        }
        return defaultOutcome;
    }
}
