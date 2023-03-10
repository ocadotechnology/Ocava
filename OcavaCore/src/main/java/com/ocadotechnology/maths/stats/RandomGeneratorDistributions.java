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
package com.ocadotechnology.maths.stats;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.distribution.GeometricDistribution;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import com.google.common.base.Preconditions;
import com.ocadotechnology.random.InstancedRepeatableRandom;
import com.ocadotechnology.random.RepeatableRandom;

public class RandomGeneratorDistributions implements Distributions {
    @Nullable
    private RandomGenerator randomGenerator;

    public RandomGeneratorDistributions(@Nonnull RandomGenerator randomGenerator) {
        this.randomGenerator = randomGenerator;
    }

    public static Distributions createWithRandomSeed() {
        return new RandomGeneratorDistributions();
    }

    private RandomGeneratorDistributions() {
        this.randomGenerator = null;
    }

    @Override
    public int getPoisson(double lambda) {
        return new PoissonDistribution(
                getRandomGenerator(),
                lambda,
                PoissonDistribution.DEFAULT_EPSILON,
                PoissonDistribution.DEFAULT_MAX_ITERATIONS).sample();
    }

    @Override
    public double getNormal(MeanAndStandardDeviation meanAndStandardDeviation) {
        return meanAndStandardDeviation.getMean() + meanAndStandardDeviation.getStdDev() * getRandomGenerator().nextGaussian();
    }

    @Override
    public double getChiSquare(int k) {
        return new ChiSquaredDistribution(getRandomGenerator(), k).sample();
    }

    /**
     * Calculate the demand using aQ with Q chi square, with k degrees of freedom (and so mean k and variance 2k).
     */
    @Override
    public double getScaledChiSquare(MeanAndStandardDeviation meanAndStandardDeviation) {
        double mean = meanAndStandardDeviation.getMean();
        int k = (int) Math.ceil(2 * Math.pow(mean, 2) / meanAndStandardDeviation.getVariance());
        double a = mean / k;
        return a * getChiSquare(k);
    }

    @Override
    public double getGamma(MeanAndStandardDeviation meanAndStandardDeviation) {
        var variance = meanAndStandardDeviation.getVariance();
        var mean = meanAndStandardDeviation.getMean();
        double shape = Math.pow(mean, 2) / variance;
        double scale = variance / mean;

        if (shape == 0) { // shape == 0 when mean is so small to be rounded to 0 when squared
            return mean;
        }

        double sampled = new GammaDistribution(getRandomGenerator(), shape, scale).sample();

        /*
         * With some very low mean to variance ratio, GammaDistribution may return 0 when it should not.
         */
        if (sampled <= 0 && mean > 0) {
            sampled = mean;
        }

        return sampled;
    }

    @Override
    public int getBinomial(int n, double p) {
        return new BinomialDistribution(getRandomGenerator(), n, p).sample();
    }

    @Override
    public int getGeometric(double probSuccess) {
        return new GeometricDistribution(getRandomGenerator(), probSuccess).sample();
    }

    @Override
    public boolean getBernoulli(double p) {
        Preconditions.checkArgument(
                p >= 0 && p <= 1,
                "Bernoulli distribution parameter should be between 0 and 1");
        return getRandomGenerator().nextDouble() <= p;
    }

    @Override
    public double getLogNormal(MeanAndStandardDeviation meanAndStandardDeviation) {
        /*
         * Scale and shape are obtained reversing:
         * mean = exp(scale + shape^2 / 2)
         * variance = [exp(shape^2) - 1] * exp(2*scale + shape^2)
         */
        var variance = meanAndStandardDeviation.getVariance();
        var mean = meanAndStandardDeviation.getMean();
        double phi = 1 + variance / Math.pow(mean, 2);
        double scale = Math.log(mean / Math.sqrt(phi));
        double shape = Math.sqrt(Math.log(phi));

        return new LogNormalDistribution(getRandomGenerator(), scale, shape).sample();
    }

    private RandomGenerator getRandomGenerator() {
        if (randomGenerator == null) {
            randomGenerator = InstancedRepeatableRandom.fromSeed(RepeatableRandom.nextLong());
        }

        return randomGenerator;
    }
}

