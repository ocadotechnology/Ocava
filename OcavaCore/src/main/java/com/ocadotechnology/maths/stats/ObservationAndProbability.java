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
package com.ocadotechnology.maths.stats;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * When detecting anomalies, this class tracks how plausible the observation is in comparison to all the other observations.
 *
 * @param <T> The type of target of the observation, which is the key in the map of the original observations.
 */
@ParametersAreNonnullByDefault
public class ObservationAndProbability<T> {
    private final T target;
    private final double observation;
    private final Probability probability;

    ObservationAndProbability(T target, double observation, Probability probability) {
        this.target = target;
        this.observation = observation;
        this.probability = probability;
    }

    static <T> ObservationAndProbability<T> create(T target, double observation, GaussianDistributionCalculator gaussianDistributionCalculator) {
        return new ObservationAndProbability<>(target, observation, gaussianDistributionCalculator.calculateProbability(observation));
    }

    /**
     * This represents the object being observed. For instance, if you are observing car speeds, this would be
     * the ID of the Car.
     */
    public T getTarget() {
        return target;
    }

    /**
     * This represents the value that was observed. For instance, when observing car speeds, this would be
     * the speed of the car.
     */
    public double getObservation() {
        return observation;
    }

    /**
     * This represents the plausibility of this observation in comparison to all the other observations. In general,
     * the higher this value is the more plausible the observation is and the closer it is to the mean of the observations.
     * <p>
     * This value represents the probability that this observation is as far away from the mean as it is or further.
     */
    public Probability getProbability() {
        return probability;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObservationAndProbability<?> that = (ObservationAndProbability<?>) o;
        return Double.compare(observation, that.observation) == 0 && Objects.equal(probability, that.probability) && Objects.equal(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(target, observation, probability);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("target", target).add("observation", observation).add("probability", probability).toString();
    }
}
