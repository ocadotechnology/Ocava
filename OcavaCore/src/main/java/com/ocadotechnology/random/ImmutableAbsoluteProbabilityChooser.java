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

import java.util.stream.Stream;

import javax.annotation.Nonnegative;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
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

    /**
     * Select an outcome based on the provided probabilities a number of times and return a multiset of the outcomes, from which you can get the count of each outcome.
     * @param numberOfTimes the number of times to select an outcome
     * @return an {@link ImmutableMultiset} containing the actions that happened. The size of this ImmutableMultiset is numberOfTimes
     */
    default ImmutableMultiset<T> choose(int numberOfTimes) {
        return Stream.generate(this::choose)
                .limit(numberOfTimes)
                .collect(ImmutableMultiset.toImmutableMultiset());
    }

    static <T> Builder<T> create(T defaultOutcome) {
        return new Builder<>(defaultOutcome);
    }

    static <T> ImmutableAbsoluteProbabilityChooser<T> fromMap(ImmutableMap<T, Double> probabilitiesByItem) {
        Builder<T> builder = create(null);

        probabilitiesByItem.forEach(builder::withOutcome);

        return builder.build();
    }

    final class Builder<T> {
        public static final double ROUNDING_TOLERANCE = 1e-12;

        private @Nonnegative double sumOfProbabilities;
        private final T defaultOutcome;
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
            // In some cases, the probabilities supplied might not sum exactly to one, because of a rounding error. In the case where they sum
            // to slightly less than one, we extend the last outcome to one.
            double lowerBound = sumOfProbabilities;
            double upperBound = sumOfProbabilities + probability;
            Preconditions.checkState(upperBound <= 1 + ROUNDING_TOLERANCE, "Sum of probabilities is greater than 1");
            sumOfProbabilities = upperBound;
            return Range.closedOpen(lowerBound, upperBound);
        }

        private T getDefaultOutcome() {
            if (defaultOutcome != null) {
                return defaultOutcome;
            }
            Preconditions.checkState(sumOfProbabilities >= 1 - ROUNDING_TOLERANCE, "Default outcome not set when probabilities are less than 1");
            return probabilisticOutcomes.build().get(sumOfProbabilities);
        }

        /**
         * Finalises the construction of the {@link ImmutableAbsoluteProbabilityChooser}
         *
         * @return the final chooser
         */
        public ImmutableAbsoluteProbabilityChooser<T> build() {
            if (sumOfProbabilities < 1) {
                Range<Double> range = Range.closedOpen(sumOfProbabilities, 1.0);
                probabilisticOutcomes.put(range, getDefaultOutcome());
            }

            ImmutableRangeMap<Double, T> outcomes = probabilisticOutcomes.build();

            ImmutableMap<Range<Double>, T> rangeMap = outcomes.asMapOfRanges();
            if (rangeMap.size() == 1) {
                // Optimisation to avoid RepeatableRandom calls
                T onlyPossibleResult = rangeMap.values().iterator().next();
                return () -> onlyPossibleResult;
            }

            return () -> outcomes.get(RepeatableRandom.nextDouble());

        }
    }
}
