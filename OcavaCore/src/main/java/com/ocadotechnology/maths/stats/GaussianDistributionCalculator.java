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

import org.apache.commons.math3.distribution.NormalDistribution;

/**
 * This class calculates the likelihood that an observation is at least a certain distance from the mean of a Gaussian distribution.
 */
@ParametersAreNonnullByDefault
public class GaussianDistributionCalculator {
    private final NormalDistribution normalDistribution;
    private final double observedMean;

    public GaussianDistributionCalculator(MeanAndStandardDeviation observedMeanAndStandardDeviation) {
        this.normalDistribution = new NormalDistribution(observedMeanAndStandardDeviation.getMean(), observedMeanAndStandardDeviation.getStdDev());

        this.observedMean = observedMeanAndStandardDeviation.getMean();
    }

    /**
     * This uses the Cumulative Distribution Function of the normal distribution to calculate the likelihood that the observation
     * is at least this far from the mean.
     * <p>
     * For instance, if the observation is 4 and the mean is 2, then the observation is 2 away from the mean, so we'll calculate the likelihood
     * that the observation is either greater than 4 or less than 0.
     */
    public Probability calculateProbability(double observation) {
        double distanceFromMean = Math.abs(observation - observedMean);
        return new Probability(2 * normalDistribution.cumulativeProbability(observedMean - distanceFromMean));
    }
}
