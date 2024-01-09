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
package com.ocadotechnology.trafficlights.simulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.ocadotechnology.config.Config;
import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.notification.NotificationRouter;
import com.ocadotechnology.notification.SimpleSubscriber;
import com.ocadotechnology.trafficlights.TrafficConfig;
import com.ocadotechnology.trafficlights.simulation.entities.SimulatedCar;
import com.ocadotechnology.trafficlights.simulation.entities.SimulatedTrafficLight;
import com.ocadotechnology.trafficlights.simulation.notification.CarsCanMoveNotification;

/**
 * Simulates cars queuing at a junction and only passing through (one by one) when the traffic light is green.
 * Car arrival is handled by {@link com.ocadotechnology.trafficlights.simulation.CarSpawner}
 */
public class CarSimulation implements SimpleSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(CarSimulation.class);

    private final EventScheduler scheduler;
    private final SimulatedCarCache simulatedCarCache;
    private final SimulatedTrafficLight simulatedTrafficLight;

    private final long timeToLeaveJunction;

    private CarSimulation(
            EventScheduler scheduler,
            SimulatedCarCache simulatedCarCache,
            Config<TrafficConfig> trafficConfig,
            SimulatedTrafficLight simulatedTrafficLight) {

        this.scheduler = scheduler;
        this.simulatedCarCache = simulatedCarCache;
        this.simulatedTrafficLight = simulatedTrafficLight;

        this.timeToLeaveJunction = trafficConfig.getValue(TrafficConfig.Vehicles.TIME_TO_LEAVE_JUNCTION).asTime();
    }

    public static CarSimulation createAndSubscribe(
            EventScheduler scheduler,
            SimulatedCarCache simulatedCarCache,
            Config<TrafficConfig> trafficConfig,
            SimulatedTrafficLight simulatedTrafficLight) {
        CarSimulation vehicleSimulation = new CarSimulation(scheduler, simulatedCarCache, trafficConfig, simulatedTrafficLight);
        vehicleSimulation.subscribeForNotifications();
        return vehicleSimulation;
    }

    @Subscribe
    public void carsCanMove(CarsCanMoveNotification n) {
        scheduler.doNow(this::moveCarIfAble);
    }

    public void carArrivesAtJunction() {
        if (simulatedCarCache.size() == 1) {
            moveCarIfAble();
        }
    }

    private void carLeavesJunction(Id<SimulatedCar> vehicleId) {
        SimulatedCar simulatedCar = simulatedCarCache.delete(vehicleId);
        logger.info("Car {} left the junction.", simulatedCar.getId());

        NotificationRouter.get().broadcast(new CarLeavesNotification(simulatedCar));

        moveCarIfAble();
    }

    private void moveCarIfAble() {
        if (!simulatedTrafficLight.canCarMove()) {
            return;
        }

        simulatedCarCache.getStationaryEarliestArrival()
                .ifPresent(nextSimulatedCar -> {
                    simulatedCarCache.update(nextSimulatedCar, nextSimulatedCar.startsMoving());
                    logger.info("Car {} starts moving.", nextSimulatedCar.getId());
                    scheduler.doIn(timeToLeaveJunction, () -> carLeavesJunction(nextSimulatedCar.getId()), "Vehicle leaves junction");
                });
    }
}
