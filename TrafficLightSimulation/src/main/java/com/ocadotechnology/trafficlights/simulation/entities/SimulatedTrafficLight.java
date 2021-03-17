/*
 * Copyright © 2017-2021 Ocado (Ocava)
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
package com.ocadotechnology.trafficlights.simulation.entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.eventbus.Subscribe;
import com.ocadotechnology.event.scheduling.EventSchedulerType;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.notification.NotificationRouter;
import com.ocadotechnology.notification.Subscriber;
import com.ocadotechnology.trafficlights.SchedulerLayerType;
import com.ocadotechnology.trafficlights.controller.LightColour;
import com.ocadotechnology.trafficlights.controller.TrafficLightState;
import com.ocadotechnology.trafficlights.simulation.comms.PedestrianCrossingRequestedNotification;
import com.ocadotechnology.trafficlights.simulation.comms.PedestrianLightChangedNotification;
import com.ocadotechnology.trafficlights.simulation.comms.TrafficLightChangedNotification;
import com.ocadotechnology.trafficlights.simulation.notification.CarsCanMoveNotification;
import com.ocadotechnology.trafficlights.simulation.notification.PedestriansCanCrossNotification;

public class SimulatedTrafficLight implements Subscriber {

    private static final Logger logger = LoggerFactory.getLogger(SimulatedTrafficLight.class);

    private TrafficLightState simulatedState;

    private SimulatedTrafficLight(LightColour startingTrafficLightColour, LightColour startingPedestrianLightColour) {
        this.simulatedState = new TrafficLightState(startingTrafficLightColour, startingPedestrianLightColour, false);
    }

    public static SimulatedTrafficLight createAndSubscribe(LightColour startingTrafficLightColour, LightColour startingPedestrianLightColour) {
        SimulatedTrafficLight simulatedTrafficLight = new SimulatedTrafficLight(startingTrafficLightColour, startingPedestrianLightColour);
        simulatedTrafficLight.subscribeForNotifications();
        return simulatedTrafficLight;
    }

    @Subscribe
    public void trafficLightChanged(TrafficLightChangedNotification n) {
        simulatedState = simulatedState.builder().setLightColour(n.lightColour).build();

        if (simulatedState.canCarMove()) {
            NotificationRouter.get().broadcast(new CarsCanMoveNotification());
        }
    }

    @Subscribe
    public void pedestrianLightChanged(PedestrianLightChangedNotification n) {
        simulatedState = simulatedState.builder().setPedestrianColour(n.lightColour).build();

        if (simulatedState.canPedestrianCross()) {
            simulatedState = simulatedState.builder().setCrossingRequested(false).build();
            NotificationRouter.get().broadcast(new PedestriansCanCrossNotification());
        }
    }

    public boolean canCarMove() {
        return simulatedState.canCarMove();
    }

    public boolean canPedestrianCross() {
        return simulatedState.canPedestrianCross();
    }

    @Override
    public EventSchedulerType getSchedulerType() {
        return SchedulerLayerType.SIMULATION;
    }

    public void requestLightChange(Id<SimulatedPedestrian> pedestrianId) {
        if (!simulatedState.isPedestrianCrossingRequested()) {
            logger.info("Pedestrian {} requesting light change.", pedestrianId);
            simulatedState = simulatedState.builder().setCrossingRequested(true).build();
            NotificationRouter.get().broadcast(new PedestrianCrossingRequestedNotification());
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("simulatedState", simulatedState)
                .toString();
    }
}
