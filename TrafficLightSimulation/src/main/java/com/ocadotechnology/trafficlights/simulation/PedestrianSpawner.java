/*
 * Copyright © 2017-2020 Ocado (Ocava)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.ocadotechnology.config.Config;
import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.random.RepeatableRandom;
import com.ocadotechnology.trafficlights.TrafficConfig;
import com.ocadotechnology.trafficlights.simulation.entities.SimulatedPedestrian;

/**
 * Simulates pedestrians arriving at a junction at configurable intervals.
 */
public class PedestrianSpawner {

    private static final Logger logger = LoggerFactory.getLogger(PedestrianSpawner.class);

    private final EventScheduler scheduler;
    private final SimulatedPedestrianCache simulatedPedestrianCache;
    private final PedestrianSimulation pedestrianSimulation;

    private final int minTimeBetweenArrivals;
    private final int maxTimeBetweenArrivals;

    public PedestrianSpawner(
            EventScheduler scheduler,
            SimulatedPedestrianCache simulatedPedestrianCache,
            PedestrianSimulation pedestrianSimulation,
            Config<TrafficConfig> trafficConfig) {
        this.scheduler = scheduler;
        this.simulatedPedestrianCache = simulatedPedestrianCache;
        this.pedestrianSimulation = pedestrianSimulation;

        this.minTimeBetweenArrivals = (int) trafficConfig.getTime(TrafficConfig.Pedestrians.MIN_TIME_BETWEEN_ARRIVALS);
        this.maxTimeBetweenArrivals = (int) trafficConfig.getTime(TrafficConfig.Pedestrians.MAX_TIME_BETWEEN_ARRIVALS);

        if (trafficConfig.getBoolean(TrafficConfig.Pedestrians.ENABLE_RANDOM_ARRIVAL)) {
            scheduleNextRandomArrival();
        }
    }

    private void scheduleNextRandomArrival() {
        int arrivalDelay = minTimeBetweenArrivals + RepeatableRandom.nextInt(maxTimeBetweenArrivals - minTimeBetweenArrivals);

        scheduler.doIn(
                arrivalDelay,
                (now) -> {
                    pedestrianArrivesAtJunction(now);
                    scheduleNextRandomArrival();
                },
                "Pedestrian arrival");
    }

    @VisibleForTesting
    public void pedestrianArrivesAtJunction() {
        pedestrianArrivesAtJunction(scheduler.getTimeProvider().getTime());
    }

    private void pedestrianArrivesAtJunction(double arrivalTime) {
        SimulatedPedestrian simulatedPedestrian = new SimulatedPedestrian(arrivalTime);
        simulatedPedestrianCache.add(simulatedPedestrian);

        logger.info("Pedestrian {} arrived.", simulatedPedestrian.getId());
        pedestrianSimulation.pedestrianArrivesAtJunction(simulatedPedestrian.getId());
    }

}
