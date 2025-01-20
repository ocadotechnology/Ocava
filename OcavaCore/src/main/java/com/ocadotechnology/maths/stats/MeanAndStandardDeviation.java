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
package com.ocadotechnology.maths.stats;

import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * This class represents the mean and standard deviation of either a distribution, or some observations.
 */
@ParametersAreNonnullByDefault
public class MeanAndStandardDeviation {
    private final double mean;
    private final double stdDev;

    /**
     * @param mean The mean of the distribution or observations
     * @param stdDev The standard deviation of the distribution or observations
     */
    public MeanAndStandardDeviation(double mean, double stdDev) {
        this.mean = mean;
        this.stdDev = stdDev;
    }

    /**
     * Calculates the observed mean and standard deviation of some observations.
     * @param observations The observations
     * @return The mean and standard deviation observed
     */
    public static MeanAndStandardDeviation calculateObservedMeanAndStandardDeviation(Collection<Double> observations) {
        double observedMean = observations.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double observedVariance = observations.stream().mapToDouble(d -> Math.pow(d - observedMean, 2)).average().orElse(0);
        return MeanAndStandardDeviation.fromMeanAndVariance(observedMean, observedVariance);
    }

    /**
     * @param mean The mean of the distribution or observations
     * @param variance The variance of the distribution or observations
     */
    public static MeanAndStandardDeviation fromMeanAndVariance(double mean, double variance) {
        return new MeanAndStandardDeviation(mean, Math.sqrt(variance));
    }

    /**
     * @return The mean of the distribution or observations
     */
    public double getMean() {
        return mean;
    }

    /**
     * @return The standard deviation of the distribution or observations
     */
    public double getStdDev() {
        return stdDev;
    }

    /**
     * @return The variance of the distribution or observations
     */
    public double getVariance() {
        return Math.pow(stdDev, 2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MeanAndStandardDeviation that = (MeanAndStandardDeviation) o;
        return Double.compare(mean, that.mean) == 0 && Double.compare(stdDev, that.stdDev) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mean, stdDev);
    }

    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("mean", mean)
                .add("standardDeviation", stdDev)
                .toString();
    }
}
