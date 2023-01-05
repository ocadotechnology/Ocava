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
package com.ocadotechnology.trafficlights.simulation;

import java.util.EnumSet;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.ocadotechnology.config.Config;
import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.random.RepeatableRandom;
import com.ocadotechnology.trafficlights.TrafficConfig;
import com.ocadotechnology.trafficlights.simulation.entities.SimulatedCar;

/**
 * Simulates cars arriving at a junction at configurable intervals.
 */
public class CarSpawner {

    private static final Logger logger = LoggerFactory.getLogger(CarSpawner.class);

    private final EventScheduler scheduler;
    private final SimulatedCarCache simulatedCarCache;
    private final CarSimulation carSimulation;

    private final int minTimeBetweenArrivals;
    private final int maxTimeBetweenArrivals;

    public CarSpawner(
            EventScheduler scheduler,
            SimulatedCarCache simulatedCarCache,
            CarSimulation carSimulation, Config<TrafficConfig> trafficConfig) {
        this.scheduler = scheduler;
        this.simulatedCarCache = simulatedCarCache;
        this.carSimulation = carSimulation;

        int initialVehicles = trafficConfig.getValue(TrafficConfig.Vehicles.INITIAL_VEHICLES).asInt();
        this.minTimeBetweenArrivals = (int) trafficConfig.getValue(TrafficConfig.Vehicles.MIN_TIME_BETWEEN_ARRIVALS).asTime();
        this.maxTimeBetweenArrivals = (int) trafficConfig.getValue(TrafficConfig.Vehicles.MAX_TIME_BETWEEN_ARRIVALS).asTime();

        IntStream.range(0, initialVehicles)
                .forEach(i -> {
                    SimulatedCar simulatedCar = new SimulatedCar(scheduler.getTimeProvider().getTime(), getRandomColour());
                    simulatedCarCache.add(simulatedCar); //Insertion order is still preserved (id tie-braker)
                });

        if (trafficConfig.getValue(TrafficConfig.Vehicles.ENABLE_RANDOM_ARRIVAL).asBoolean()) {
            scheduleNextRandomArrival();
        }
    }

    private void scheduleNextRandomArrival() {
        int arrivalDelay = minTimeBetweenArrivals + RepeatableRandom.nextInt(maxTimeBetweenArrivals - minTimeBetweenArrivals);

        scheduler.doIn(
                arrivalDelay,
                (now) -> {
                    carArrivesAtJunction(now);
                    scheduleNextRandomArrival();
                },
                "Car arrival");
    }

    @VisibleForTesting
    public void carArrivesAtJunction() {
        carArrivesAtJunction(scheduler.getTimeProvider().getTime());
    }

    private void carArrivesAtJunction(double arrivalTime) {
        SimulatedCar simulatedCar = new SimulatedCar(arrivalTime, getRandomColour());
        simulatedCarCache.add(simulatedCar);
        logger.info("Car {} arrived.", simulatedCar.getId());
        carSimulation.carArrivesAtJunction();
    }

    private SimulatedCar.Colour getRandomColour() {
        return RepeatableRandom.randomElementOf(EnumSet.allOf(SimulatedCar.Colour.class));
    }

}
