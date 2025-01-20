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
package com.ocadotechnology.trafficlights.simulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.Subscribe;
import com.ocadotechnology.config.Config;
import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.notification.NotificationRouter;
import com.ocadotechnology.notification.SimpleSubscriber;
import com.ocadotechnology.trafficlights.TrafficConfig;
import com.ocadotechnology.trafficlights.simulation.entities.SimulatedPedestrian;
import com.ocadotechnology.trafficlights.simulation.entities.SimulatedTrafficLight;
import com.ocadotechnology.trafficlights.simulation.notification.PedestriansCanCrossNotification;

/**
 * Simulates pedestrians queuing at a junction and only crossing when the pedestrian light is green.
 * Pedestrian arrival is handled by {@link com.ocadotechnology.trafficlights.simulation.PedestrianSpawner}
 */
public class PedestrianSimulation implements SimpleSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(PedestrianSimulation.class);

    private final EventScheduler scheduler;
    private final SimulatedPedestrianCache simulatedPedestrianCache;
    private final SimulatedTrafficLight simulatedTrafficLight;

    private final long timeToLeaveJunction;

    private PedestrianSimulation(
            EventScheduler scheduler,
            SimulatedPedestrianCache simulatedPedestrianCache,
            Config<TrafficConfig> trafficConfig,
            SimulatedTrafficLight simulatedTrafficLight) {

        this.scheduler = scheduler;
        this.simulatedPedestrianCache = simulatedPedestrianCache;
        this.simulatedTrafficLight = simulatedTrafficLight;

        this.timeToLeaveJunction = trafficConfig.getValue(TrafficConfig.Pedestrians.TIME_TO_LEAVE_JUNCTION).asTime();
    }

    public static PedestrianSimulation createAndSubscribe(
            EventScheduler scheduler,
            SimulatedPedestrianCache simulatedPedestrianCache,
            Config<TrafficConfig> trafficConfig,
            SimulatedTrafficLight simulatedTrafficLight) {
        PedestrianSimulation pedestrianSimulation = new PedestrianSimulation(scheduler, simulatedPedestrianCache, trafficConfig, simulatedTrafficLight);
        pedestrianSimulation.subscribeForNotifications();
        return pedestrianSimulation;
    }

    @Subscribe
    public void pedestriansCanCross(PedestriansCanCrossNotification n) {
        scheduler.doNow(() -> simulatedPedestrianCache.getAllStationary().forEach(this::crossRoad));
    }

    public void pedestrianArrivesAtJunction(Id<SimulatedPedestrian> pedestrianId) {
        crossRoadIfAble(pedestrianId);
    }

    private void crossRoadIfAble(Id<SimulatedPedestrian> pedestrianId) {
        if (simulatedTrafficLight.canPedestrianCross()) {
            crossRoad(pedestrianId);
        } else {
            simulatedTrafficLight.requestLightChange(pedestrianId);
        }
    }

    private void crossRoad(Id<SimulatedPedestrian> pedestrianId) {
        SimulatedPedestrian simulatedPedestrian = simulatedPedestrianCache.get(pedestrianId);
        simulatedPedestrianCache.update(simulatedPedestrian, simulatedPedestrian.startsCrossing());

        logger.info("Pedestrian {} starts crossing.", pedestrianId);
        NotificationRouter.get().broadcast(new PedestrianStartsCrossingNotification());
        scheduler.doIn(timeToLeaveJunction, () -> finishCrossingRoad(pedestrianId), "Pedestrian finishes crossing.");
    }

    private void finishCrossingRoad(Id<SimulatedPedestrian> pedestrianId) {
        SimulatedPedestrian simulatedPedestrian = simulatedPedestrianCache.delete(pedestrianId);
        Preconditions.checkState(simulatedPedestrian.isCrossing(), "Pedestrian %s attempted to finish crossing but isn't moving.", simulatedPedestrian);

        logger.info("Pedestrian {} has crossed the junction.", pedestrianId);
        NotificationRouter.get().broadcast(new PedestrianFinishesCrossingNotification());
    }
}
