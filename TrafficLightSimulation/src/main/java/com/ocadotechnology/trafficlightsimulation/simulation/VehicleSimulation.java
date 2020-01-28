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
package com.ocadotechnology.trafficlightsimulation.simulation;

import java.util.EnumSet;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.ocadotechnology.config.Config;
import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.event.scheduling.EventSchedulerType;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.notification.NotificationRouter;
import com.ocadotechnology.notification.Subscriber;
import com.ocadotechnology.random.RepeatableRandom;
import com.ocadotechnology.trafficlightsimulation.SchedulerLayerType;
import com.ocadotechnology.trafficlightsimulation.TrafficConfig;
import com.ocadotechnology.trafficlightsimulation.TrafficConfig.TrafficLight;
import com.ocadotechnology.trafficlightsimulation.controller.TrafficLightChangedNotification;
import com.ocadotechnology.trafficlightsimulation.controller.TrafficLightController.State;
import com.ocadotechnology.trafficlightsimulation.simulation.Vehicle.Colour;

public class VehicleSimulation implements Subscriber {
    private static final Logger logger = LoggerFactory.getLogger(VehicleSimulation.class);

    private final EventScheduler scheduler;
    private final VehicleCache vehicleCache;

    private boolean allowedToMove;

    private final int minTimeBetweenArrivals;
    private final int maxTimeBetweenArrivals;
    private final long timeToLeaveJunction;

    private VehicleSimulation(
            EventScheduler scheduler,
            VehicleCache vehicleCache,
            Config<TrafficConfig> trafficConfig) {

        this.scheduler = scheduler;
        this.vehicleCache = vehicleCache;

        int initialVehicles = trafficConfig.getInt(TrafficConfig.Vehicles.INITIAL_VEHICLES);
        this.minTimeBetweenArrivals = (int) trafficConfig.getTime(TrafficConfig.Vehicles.MIN_TIME_BETWEEN_ARRIVALS);
        this.maxTimeBetweenArrivals = (int) trafficConfig.getTime(TrafficConfig.Vehicles.MAX_TIME_BETWEEN_ARRIVALS);
        this.timeToLeaveJunction = trafficConfig.getTime(TrafficConfig.Vehicles.TIME_TO_LEAVE_JUNCTION);
        this.allowedToMove = trafficConfig.getEnum(TrafficLight.INITIAL_STATE, State.class).equals(State.GREEN);

        IntStream.range(0, initialVehicles)
                .forEach(i -> {
                    Vehicle vehicle = new Vehicle(scheduler.getTimeProvider().getTime(), getRandomColour());
                    vehicleCache.add(vehicle); //Insertion order is still preserved (id tie-braker)
                });

        if (trafficConfig.getBoolean(TrafficConfig.Vehicles.ENABLE_RANDOM_ARRIVAL)) {
            scheduleNextRandomArrival();
        }
    }

    public static VehicleSimulation createAndSubscribe(
            EventScheduler scheduler,
            VehicleCache vehicleCache,
            Config<TrafficConfig> trafficConfig) {

        VehicleSimulation vehicleSimulation = new VehicleSimulation(scheduler, vehicleCache, trafficConfig);

        vehicleSimulation.subscribeForNotifications();
        return vehicleSimulation;
    }

    @Subscribe
    public void trafficLightChanged(TrafficLightChangedNotification n) {
        allowedToMove = n.newState.equals(State.GREEN);
        moveCarIfAble();
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

    public void carArrivesAtJunction() {
        carArrivesAtJunction(scheduler.getTimeProvider().getTime());
    }

    private void carArrivesAtJunction(double arrivalTime) {
        Vehicle vehicle = new Vehicle(arrivalTime, getRandomColour());
        vehicleCache.add(vehicle);
        logger.info("Car {} arrived.", vehicle.getId());
        if (vehicleCache.size() == 1) {
            moveCarIfAble();
        }
    }

    private Colour getRandomColour() {
        return RepeatableRandom.randomElementOf(EnumSet.allOf(Colour.class));
    }

    private void vehicleLeavesJunction(Id<Vehicle> vehicleId) {
        Vehicle vehicle = vehicleCache.delete(vehicleId);
        logger.info("Car {} left the junction.", vehicle.getId());

        NotificationRouter.get().broadcast(new CarLeavesNotification(vehicle));

        moveCarIfAble();
    }

    private void moveCarIfAble() {
        if (!allowedToMove) {
            return;
        }

        vehicleCache.getStationaryEarliestArrival()
                .ifPresent(nextVehicle -> {
                    vehicleCache.update(nextVehicle, nextVehicle.startsMoving());
                    logger.info("Car {} starts moving.", nextVehicle.getId());
                    scheduler.doIn(timeToLeaveJunction, () -> vehicleLeavesJunction(nextVehicle.getId()), "Vehicle leaves junction");
                });
    }

    @Override
    public EventSchedulerType getSchedulerType() {
        return SchedulerLayerType.SIMULATION;
    }
}
