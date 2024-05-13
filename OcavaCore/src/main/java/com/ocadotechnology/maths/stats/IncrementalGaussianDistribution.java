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

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.EvictingQueue;

/**
 * This class allows us to track historical observations over time. That way, when new observations are observed, we can
 * classify the new observations in comparison with all the old ones without having to reclassify the old ones as well.
 */
@ParametersAreNonnullByDefault
public class IncrementalGaussianDistribution {
    private final EvictingQueue<Double> data;

    /**
     * @param maxSize The number of observations before we start dropping them.
     */
    public IncrementalGaussianDistribution(int maxSize) {
        data = EvictingQueue.create(maxSize);
    }

    MeanAndStandardDeviation calculateObservedMeanAndStandardDeviation() {
        return MeanAndStandardDeviation.calculateObservedMeanAndStandardDeviation(data);
    }

    /**
     * Tracks the newly observed observations. If more observations have been observed than the maxSize, the old
     * observations will be dropped in favor of the new ones.
     */
    public void addNewObservations(Collection<Double> newObservations) {
        data.addAll(newObservations);
    }

    /**
     * Identifies which of the newly observed observations are likely to be anomalous, in comparison to those already
     * tracked.
     * @param <T> The type of object being observed.
     */
    public <T> NormalDistributionAnomalies<T> identifyAnomalousObservations(Map<T, Double> newObservations, Probability anomalyThreshold) {
        return NormalDistributionAnomalies.create(newObservations, anomalyThreshold, data);
    }
    /**
     * Tracks the new observations and identifies which of them are likely to be anomalous, in comparison to those already
     * tracked.
     * @param <T> The type of object being observed.
     */
    public <T> NormalDistributionAnomalies<T> addNewObservationsAndIdentifyAnomalies(Map<T, Double> newObservations, Probability anomalyThreshold) {
        addNewObservations(newObservations.values());
        return identifyAnomalousObservations(newObservations, anomalyThreshold);
    }
}
