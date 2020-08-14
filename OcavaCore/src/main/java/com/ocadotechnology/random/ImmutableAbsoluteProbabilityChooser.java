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

import javax.annotation.Nonnegative;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;

/**
 * Encapsulation of logic for choosing probabilistically between multiple outcomes.
 *
 * Each outcome has an associated probability, where the total of all probabilities is no greater than 1. There is also
 * a default outcome for when none of the other outcomes are chosen. The probabilities must be known at construction
 * time and cannot subsequently be altered.
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
@FunctionalInterface
public interface ImmutableAbsoluteProbabilityChooser<T> {
    /**
     * @return An outcome selected from those with defined probabilities using a random number provided from the defined
     *          random number supplier.
     */
    T choose();

    static <T> Builder<T> create(T defaultOutcome) {
        return new Builder<>(defaultOutcome);
    }

    final class Builder<T> {
        private @Nonnegative double sumOfProbabilities;
        private T defaultOutcome;
        private final ImmutableRangeMap.Builder<Double, T> probabilisticOutcomes;

        private Builder(T defaultOutcome) {
            this.defaultOutcome = defaultOutcome;
            this.sumOfProbabilities = 0;
            this.probabilisticOutcomes = ImmutableRangeMap.builder();
        }

        /**
         * Sets the probability of choosing a specific outcome.  Sums with any probability previously set for this outcome.
         *
         * @param outcome the outcome to be returned.
         * @param probability the probability of returning this outcome.  Must be in the range 0 to 1.
         * @throws IllegalArgumentException if the outcome is equal to the defaultOutcome provided in the constructor.
         * @throws IllegalArgumentException if the probability is less than zero.
         * @throws IllegalStateException if the total probability for all defined outcomes sums to greater than 1
         *          (allowing for a degree of rounding error)
         */
        public Builder<T> withOutcome(T outcome, @Nonnegative double probability) {
            Preconditions.checkArgument(!outcome.equals(defaultOutcome), "Attempted to set the probability of the default result %s", defaultOutcome);
            Preconditions.checkArgument(probability >= 0, "Attempted to set probability for outcome %s to invalid value %s (must be >= 0)", outcome, probability);
            if (probability > 0) {
                Range<Double> range = getNextRange(probability);
                probabilisticOutcomes.put(range, outcome);
            }
            return this;
        }

        private Range<Double> getNextRange(@Nonnegative double probability) {
            double lowerBound = sumOfProbabilities;
            double upperBound = sumOfProbabilities + probability;
            Preconditions.checkState(upperBound <= 1 + 1e-6, "Sum of probabilities is greater than 1");
            sumOfProbabilities = upperBound;
            return Range.closedOpen(lowerBound, upperBound);
        }

        /**
         * Finalises the construction of the {@link ImmutableAbsoluteProbabilityChooser}
         *
         * @return the final chooser
         */
        public ImmutableAbsoluteProbabilityChooser<T> build() {
            if (sumOfProbabilities < 1) {
                Preconditions.checkState(defaultOutcome != null, "Default outcome not set");
                Range<Double> range = Range.closedOpen(sumOfProbabilities, 1.0);
                probabilisticOutcomes.put(range, defaultOutcome);
            }

            ImmutableRangeMap<Double, T> outcomes = probabilisticOutcomes.build();

            ImmutableMap<Range<Double>, T> rangeMap = outcomes.asMapOfRanges();
            if (rangeMap.size() == 1) {
                // Optimisation to avoid RepeatableRandom calls
                T onlyPossibleResult = rangeMap.values().iterator().next();
                return () -> onlyPossibleResult;
            }

            return new ImmutableAbsoluteProbabilityChooserWithManyOutcomes<>(outcomes);

        }
    }
}
