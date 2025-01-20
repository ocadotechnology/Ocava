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

import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.ocadotechnology.random.RepeatableRandom;

class RandomGeneratorDistributionsTest {
    private static final int SAMPLE_SIZE = 1000000;
    private static final double EPSILON = 0.01;

    private final Distributions distributions = RandomGeneratorDistributions.createWithRandomSeed();

    @BeforeEach
    void beforeEach() {
        RepeatableRandom.initialiseWithSeed(0);
    }

    @AfterEach
    void afterEach() {
        RepeatableRandom.clear();
    }

    @Test
    void testGammaWithTooLowMeanVarianceRatio() {
        double mean = 2 * Math.sqrt(Double.MIN_VALUE);
        double variance = 2048;
        Assertions.assertEquals(
                mean,
                distributions.getGamma(MeanAndStandardDeviation.fromMeanAndVariance(mean, variance)));
    }

    @ParameterizedTest
    @MethodSource("simpleMeansAndVariances")
    void testGammaMeanAndVariance(MeanAndStandardDeviation meanAndStandardDeviation) {
        assertMeanAndVarianceConvergence(
                () -> distributions.getGamma(meanAndStandardDeviation),
                meanAndStandardDeviation
        );
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.8, 0.1, 0.2, 0.3})
    void testGeometric(double p) {
        assertMeanAndVarianceConvergence(
                () -> (double) distributions.getGeometric(p),
                MeanAndStandardDeviation.fromMeanAndVariance(
                        (1 - p) / p,
                        (1 - p) / Math.pow(p, 2)
                )
        );
    }

    @Test
    void testBinomial() {
        var n = 100;
        var p = 0.3;
        assertMeanAndVarianceConvergence(
                () -> (double)distributions.getBinomial(n, p),
                MeanAndStandardDeviation.fromMeanAndVariance(n * p, n * p * (1 - p))
        );
    }

    @ParameterizedTest
    @MethodSource("simpleMeansAndVariances")
    void testLogNormalMeanAndVariance(MeanAndStandardDeviation meanAndStandardDeviation) {
        RepeatableRandom.initialiseWithSeed(1); // Test extremely sensitive to seed variations
        assertMeanAndVarianceConvergence(
                () -> distributions.getLogNormal(meanAndStandardDeviation),
                meanAndStandardDeviation
        );
    }

    @Test
    void testScaledChiSquare() {
        // this test can run very slowly, when run with all the potential configurations.
        var meanAndStandardDeviation = MeanAndStandardDeviation.fromMeanAndVariance(1000, 200);
        assertMeanAndVarianceConvergence(
                () -> distributions.getScaledChiSquare(meanAndStandardDeviation),
                meanAndStandardDeviation
        );
    }

    @ParameterizedTest
    @MethodSource("simpleMeansAndVariances")
    void testGetNormal(MeanAndStandardDeviation meanAndStandardDeviation) {
        assertMeanAndVarianceConvergence(
                () -> distributions.getNormal(meanAndStandardDeviation),
                meanAndStandardDeviation
        );
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.8, 0.1, 0.2, 0.3})
    void testGetPoisson(double lambda) {
        assertMeanAndVarianceConvergence(
                () -> (double)distributions.getPoisson(lambda),
                MeanAndStandardDeviation.fromMeanAndVariance(lambda, lambda)
        );
    }

    private static Stream<MeanAndStandardDeviation> simpleMeansAndVariances() {
        return Stream.of(
                MeanAndStandardDeviation.fromMeanAndVariance(1, 0.1),
                MeanAndStandardDeviation.fromMeanAndVariance(1, 1.5),
                MeanAndStandardDeviation.fromMeanAndVariance(1000, 200),
                MeanAndStandardDeviation.fromMeanAndVariance(100000, 20000)
        );
    }

    @Test
    void testBernoulliMeanAndVariance() {
        double p = 0.7;
        Supplier<Double> bernoulliConverter = () -> distributions.getBernoulli(p) ? 1.0 : 0.0;
        assertMeanAndVarianceConvergence(
                bernoulliConverter,
                new MeanAndStandardDeviation(
                        p,
                        Math.sqrt(p * (1 - p))
                )
        );
    }

    @Test
    void testBernoulliOnlyAcceptsProportions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> distributions.getBernoulli(10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> distributions.getBernoulli(-1));
    }

    private void assertMeanAndVarianceConvergence(
            Supplier<Double> distribution,
            MeanAndStandardDeviation meanAndStandardDeviation
    ) {
        var samples = Stream.generate(distribution)
                .limit(SAMPLE_SIZE)
                .collect(Collectors.toList());

        var sampledMeanAndStdDeviation = MeanAndStandardDeviation.calculateObservedMeanAndStandardDeviation(samples);

        Assertions.assertEquals(
                meanAndStandardDeviation.getMean(),
                sampledMeanAndStdDeviation.getMean(),
                EPSILON * sampledMeanAndStdDeviation.getMean());

        Assertions.assertEquals(
                meanAndStandardDeviation.getVariance(),
                sampledMeanAndStdDeviation.getVariance(),
                EPSILON * meanAndStandardDeviation.getVariance());
    }
}
