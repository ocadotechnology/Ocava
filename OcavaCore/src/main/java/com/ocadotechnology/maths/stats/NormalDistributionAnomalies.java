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

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * This class represents which observations of a given map are probably anomalous, and their overall mean and standard deviation.
 * <p>
 * It assumes the observations are normally distributed.
 *
 * @param <T> The type of target of the observation, which is the key in the map of the original observations.
 */
@ParametersAreNonnullByDefault
public class NormalDistributionAnomalies<T> {
    private final ImmutableList<ObservationAndProbability<T>> anomalouslyHighObservations;
    private final ImmutableList<ObservationAndProbability<T>> anomalouslyLowObservations;
    private final MeanAndStandardDeviation observedMeanAndStandardDeviation;

    NormalDistributionAnomalies(ImmutableList<ObservationAndProbability<T>> anomalouslyHighObservations, ImmutableList<ObservationAndProbability<T>> anomalouslyLowObservations, MeanAndStandardDeviation observedMeanAndStandardDeviation) {
        this.anomalouslyHighObservations = anomalouslyHighObservations;
        this.anomalouslyLowObservations = anomalouslyLowObservations;
        this.observedMeanAndStandardDeviation = observedMeanAndStandardDeviation;
    }

    /**
     * This method allows you to detect which observations of a given map are probably anomalous.
     *
     * @param observations     The observations, keyed on target and with the value being the value of the observation. For instance if the observations are car speeds,
     *                         the key here is the Car ID and the value the speed.
     * @param anomalyThreshold This is a measure of how strict we are when detecting anomalies. The higher this value is, the more values will be detected as anomalies.
     *                         If you set this value too high, we will get false positives; set too low, and we will get false negatives. This value will be compared to
     *                         the value returned by {@link ObservationAndProbability#getProbability()}, which is taken from the CDF of the normal distribution.
     * @param <T>              The type of target of the observation
     * @return An object representing the values that have been determined to be anomalously high or low, and the observed mean and standard deviation of the observations.
     */
    public static <T> NormalDistributionAnomalies<T> create(Map<T, Double> observations, Probability anomalyThreshold) {
        return create(observations, anomalyThreshold, observations.values());
    }

    static <T> NormalDistributionAnomalies<T> create(Map<T, Double> observations, Probability anomalyThreshold, Collection<Double> oldObservations) {
        MeanAndStandardDeviation observedMeanAndStandardDeviation = MeanAndStandardDeviation.calculateObservedMeanAndStandardDeviation(oldObservations);
        if (observations.isEmpty()) {
            return new NormalDistributionAnomalies<>(ImmutableList.of(), ImmutableList.of(), observedMeanAndStandardDeviation);
        }
        double observedMean = observedMeanAndStandardDeviation.getMean();
        GaussianDistributionCalculator gaussianDistributionCalculator = new GaussianDistributionCalculator(observedMeanAndStandardDeviation);

        Map<Boolean, ImmutableList<ObservationAndProbability<T>>> anomalousValuesByHighOrLow = observations.entrySet().stream()
                .map(e -> ObservationAndProbability.create(e.getKey(), e.getValue(), gaussianDistributionCalculator))
                .filter(e -> e.getProbability().compareTo(anomalyThreshold) < 0)
                .collect(Collectors.partitioningBy(e -> e.getObservation() > observedMean, ImmutableList.toImmutableList()));
        return new NormalDistributionAnomalies<>(anomalousValuesByHighOrLow.get(true), anomalousValuesByHighOrLow.get(false), observedMeanAndStandardDeviation);
    }

    /**
     * The observations that have been determined to be anomalously higher than the observed mean, and their probabilities.
     */
    public ImmutableList<ObservationAndProbability<T>> getAnomalouslyHighObservations() {
        return anomalouslyHighObservations;
    }

    /**
     * The observations that have been determined to be anomalously lower than the observed mean, and their probabilities.
     */
    public ImmutableList<ObservationAndProbability<T>> getAnomalouslyLowObservations() {
        return anomalouslyLowObservations;
    }

    /**
     * The observed mean of all the observations.
     */
    public double getObservedMean() {
        return observedMeanAndStandardDeviation.getMean();
    }

    /**
     * The observed mean and standard deviation of all the observations.
     */
    public MeanAndStandardDeviation getObservedMeanAndStandardDeviation() {
        return observedMeanAndStandardDeviation;
    }

    /**
     * The observations that have been determined to be anomalously higher than the observed mean, keyed on target and with the observation value as value.
     */
    public ImmutableMap<T, Double> getAnomalouslyHighObservationsAsMap() {
        return anomalouslyHighObservations.stream().collect(ImmutableMap.toImmutableMap(ObservationAndProbability::getTarget, ObservationAndProbability::getObservation));
    }

    /**
     * The observations that have been determined to be anomalously lower than the observed mean, keyed on target and with the observation value as value.
     */
    public ImmutableMap<T, Double> getAnomalouslyLowObservationsAsMap() {
        return anomalouslyLowObservations.stream().collect(ImmutableMap.toImmutableMap(ObservationAndProbability::getTarget, ObservationAndProbability::getObservation));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NormalDistributionAnomalies<?> that = (NormalDistributionAnomalies<?>) o;
        return Objects.equal(observedMeanAndStandardDeviation, that.observedMeanAndStandardDeviation) && Objects.equal(anomalouslyHighObservations, that.anomalouslyHighObservations) && Objects.equal(anomalouslyLowObservations, that.anomalouslyLowObservations);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(anomalouslyHighObservations, anomalouslyLowObservations, observedMeanAndStandardDeviation);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("anomalouslyHighObservations", anomalouslyHighObservations).add("anomalouslyLowObservations", anomalouslyLowObservations).add("observedMeanAndStandardDeviation", observedMeanAndStandardDeviation).toString();
    }
}
