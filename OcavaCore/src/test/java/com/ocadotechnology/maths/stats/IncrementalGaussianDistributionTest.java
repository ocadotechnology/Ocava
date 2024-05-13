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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.IdGenerator;
import com.ocadotechnology.utils.ImmutableMapFactory;

class IncrementalGaussianDistributionTest {
    private static class Car {
        public static final ImmutableList<Car> ALL_CARS = ImmutableList.of(
                new Car(1.0),
                new Car(2.0),
                new Car(3.0),
                new Car(4.0),
                new Car(5.0)
        );

        public final Id<Car> carId = IdGenerator.getId(Car.class);
        public final double speed;

        private Car(double speed) {
            this.speed = speed;
        }
    }

    public static final Probability MIDDLING_THRESHOLD = new Probability(0.2);

    @Test
    void shouldTrackDataOverTime() {
        IncrementalGaussianDistribution incrementalGaussianDistribution = new IncrementalGaussianDistribution(100);
        assertEquals(incrementalGaussianDistribution.calculateObservedMeanAndStandardDeviation(), new MeanAndStandardDeviation(0, 0));
        incrementalGaussianDistribution.addNewObservations(List.of(1.0));
        assertEquals(incrementalGaussianDistribution.calculateObservedMeanAndStandardDeviation(), new MeanAndStandardDeviation(1, 0));
        incrementalGaussianDistribution.addNewObservations(List.of(2.0, 3.0));
        assertEquals(incrementalGaussianDistribution.calculateObservedMeanAndStandardDeviation(), new MeanAndStandardDeviation(2.0, Math.sqrt(2.0 / 3)));
    }

    @Test
    public void identifyAnomalousObservations_whenPassedEmptyDataAndTrackingDataOverTime_shouldReturnEmptyData() {
        IncrementalGaussianDistribution incrementalGaussianDistribution = new IncrementalGaussianDistribution(100);
        incrementalGaussianDistribution.addNewObservations(ImmutableList.of());
        Assertions.assertEquals(
                incrementalGaussianDistribution.identifyAnomalousObservations(ImmutableMap.of(), MIDDLING_THRESHOLD),
                new NormalDistributionAnomalies<Id<Car>>(ImmutableList.of(), ImmutableList.of(), new MeanAndStandardDeviation(0, 0))
        );
    }

    @Test
    public void identifyAnomalousObservations_shouldTrackDataOverTime() {
        IncrementalGaussianDistribution incrementalGaussianDistribution = new IncrementalGaussianDistribution(100);
        Set<Id<Car>> expectedAnomalousCars = Set.of(Id.createCached(0), Id.createCached(4));
        Map<Id<Car>, Double> carSpeeds = Car.ALL_CARS.stream().collect(Collectors.toMap(car -> car.carId, car -> car.speed));

        ImmutableMap<Id<Car>, Double> roundOneObservations = ImmutableMapFactory.filterByKeys(carSpeeds, Predicate.not(expectedAnomalousCars::contains));
        ImmutableMap<Id<Car>, Double> roundTwoObservations = ImmutableMapFactory.filterByKeys(carSpeeds, expectedAnomalousCars::contains);
        incrementalGaussianDistribution.addNewObservations(roundOneObservations.values());
        incrementalGaussianDistribution.addNewObservations(roundTwoObservations.values());
        NormalDistributionAnomalies<Id<Car>> highAndLowCarSpeeds = incrementalGaussianDistribution.identifyAnomalousObservations(roundTwoObservations, MIDDLING_THRESHOLD);
        Assertions.assertEquals(highAndLowCarSpeeds.getObservedMean(), 3.0);
        Assertions.assertEquals(highAndLowCarSpeeds.getAnomalouslyHighObservationsAsMap(), ImmutableMap.of(Id.create(4), 5.0));
        Assertions.assertEquals(highAndLowCarSpeeds.getAnomalouslyLowObservationsAsMap(), ImmutableMap.of(Id.create(0), 1.0));
    }
}
