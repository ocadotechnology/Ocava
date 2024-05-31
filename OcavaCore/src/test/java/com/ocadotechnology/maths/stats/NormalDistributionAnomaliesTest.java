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

import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.IdGenerator;

class NormalDistributionAnomaliesTest {
    @ParametersAreNonnullByDefault
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
    public static final Probability STRICT_THRESHOLD = new Probability(0.5);
    public static final Probability LAX_THRESHOLD = Probability.ZERO;

    @Test
    void create_identifiesNoAnomaliesWhenAllTheValuesAreTheSame() {
        Map<Id<Car>, Double> carSpeeds = Car.ALL_CARS.stream().collect(Collectors.toMap(car -> car.carId, anything -> 3d));
        NormalDistributionAnomalies<Id<Car>> highAndLowCarSpeeds = NormalDistributionAnomalies.create(carSpeeds, MIDDLING_THRESHOLD);
        Assertions.assertEquals(highAndLowCarSpeeds.getObservedMean(), 3.0);
        Assertions.assertEquals(highAndLowCarSpeeds.getAnomalouslyHighObservationsAsMap(), ImmutableMap.of());
        Assertions.assertEquals(highAndLowCarSpeeds.getAnomalouslyLowObservationsAsMap(), ImmutableMap.of());
    }

    @Test
    void create_identifiesObservedMeanAndHighAndLowValues() {
        Map<Id<Car>, Double> carSpeeds = Car.ALL_CARS.stream().collect(Collectors.toMap(car -> car.carId, car -> car.speed));
        NormalDistributionAnomalies<Id<Car>> highAndLowCarSpeeds = NormalDistributionAnomalies.create(carSpeeds, MIDDLING_THRESHOLD);
        Assertions.assertEquals(highAndLowCarSpeeds.getObservedMean(), 3.0);
        Assertions.assertEquals(highAndLowCarSpeeds.getAnomalouslyHighObservationsAsMap(), ImmutableMap.of(Id.create(4), 5.0));
        Assertions.assertEquals(highAndLowCarSpeeds.getAnomalouslyLowObservationsAsMap(), ImmutableMap.of(Id.create(0), 1.0));
    }

    @Test
    void create_whenGivenStricterThreshold_identifiesMoreValuesAsAnomalous() {
        Map<Id<Car>, Double> carSpeeds = Car.ALL_CARS.stream().collect(Collectors.toMap(car -> car.carId, car -> car.speed));
        NormalDistributionAnomalies<Id<Car>> highAndLowCarSpeeds = NormalDistributionAnomalies.create(carSpeeds, STRICT_THRESHOLD);
        Assertions.assertEquals(highAndLowCarSpeeds.getObservedMean(), 3.0);
        Assertions.assertEquals(highAndLowCarSpeeds.getAnomalouslyHighObservationsAsMap(), ImmutableMap.of(Id.create(4), 5.0, Id.create(3), 4.0));
        Assertions.assertEquals(highAndLowCarSpeeds.getAnomalouslyLowObservationsAsMap(), ImmutableMap.of(Id.create(0), 1.0, Id.create(1), 2.0));
    }

    @Test
    void create_whenGivenLaxerThreshold_identifiesNoValuesAsAnomalous() {
        Map<Id<Car>, Double> carSpeeds = Car.ALL_CARS.stream().collect(Collectors.toMap(car -> car.carId, car -> car.speed));
        NormalDistributionAnomalies<Id<Car>> highAndLowCarSpeeds = NormalDistributionAnomalies.create(carSpeeds, LAX_THRESHOLD);
        Assertions.assertEquals(highAndLowCarSpeeds, new NormalDistributionAnomalies<>(ImmutableList.of(), ImmutableList.of(), new MeanAndStandardDeviation(3, Math.sqrt(2))));
    }

    @Test
    public void create_whenPassedEmptyData_shouldReturnEmptyData() {
        Assertions.assertEquals(
                NormalDistributionAnomalies.<Id<Car>>create(ImmutableMap.of(), MIDDLING_THRESHOLD),
                new NormalDistributionAnomalies<Id<Car>>(ImmutableList.of(), ImmutableList.of(), new MeanAndStandardDeviation(0, 0))
        );
    }
}
