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
package com.ocadotechnology.trafficlights.controller;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.config.Config;
import com.ocadotechnology.event.scheduling.Cancelable;
import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.trafficlights.TrafficConfig;
import com.ocadotechnology.trafficlights.TrafficConfig.TrafficLight;

public class TrafficLightController implements RestHandler{

    private static final Logger logger = LoggerFactory.getLogger(TrafficLightController.class);
    private static final int CYCLE_CHANGE_DELAY = 100;
    private final boolean automaticTrafficLightChangeEnabled;
    private final double pedestrianButtonPressLightChangeDelay;

    private final RestSender restSender;

    private final EventScheduler scheduler;
    private final ImmutableMap<LightColour, Long> lightDurationsForTraffic;
    private TrafficLightState currentState;
    private double timeOfPreviousCycleChange;

    private Cancelable nextCycleEvent;

    public TrafficLightController(
            RestSender restSender,
            EventScheduler scheduler,
            Config<TrafficConfig> trafficConfig,
            LightColour startingTrafficLightColour,
            LightColour startingPedestrianLightColour) {
        this.restSender = restSender;
        this.scheduler = scheduler;
        this.lightDurationsForTraffic = ImmutableMap.<LightColour, Long>builder()
                .put(LightColour.RED, trafficConfig.getTime(TrafficLight.RED_LIGHT_INTERVAL))
                .put(LightColour.GREEN, trafficConfig.getTime(TrafficLight.GREEN_LIGHT_INTERVAL))
                .build();

        this.pedestrianButtonPressLightChangeDelay = trafficConfig.getTime(TrafficLight.PEDESTRIAN_CROSSING_CHANGE_DELAY);
        this.automaticTrafficLightChangeEnabled = trafficConfig.getBoolean(TrafficLight.ENABLE_AUTOMATIC_CHANGE);

        scheduler.doNow(() -> initialiseTrafficLight(startingTrafficLightColour, startingPedestrianLightColour));
    }

    private void initialiseTrafficLight(LightColour lightColour, LightColour pedestrianColour) {
        this.currentState = new TrafficLightState(lightColour, pedestrianColour, false);
        updateTrafficLights(Function.identity());

        if (automaticTrafficLightChangeEnabled || currentState.canPedestrianCross()) {
            scheduleNextCycleIn(getDurationOfCycle(currentState));
        }
    }

    @Override
    public void pedestrianLightsButtonPressed() {
        if (currentState.isPedestrianCrossingRequested() || currentState.canPedestrianCross()) {
            return;
        }

        updateTrafficLights(builder -> builder.setCrossingRequested(true));

        double currentCycleElapsedTime = scheduler.getTimeProvider().getTime() - timeOfPreviousCycleChange;
        double minimumCycleDuration = getDurationOfCycle(currentState);
        double pedestrianLightChangeDelay = Math.max(minimumCycleDuration - currentCycleElapsedTime, pedestrianButtonPressLightChangeDelay);
        scheduleNextCycleIn(pedestrianLightChangeDelay);
    }

    private void scheduleNextCycleIn(double cycleTime) {
        if (nextCycleEvent != null) {
            nextCycleEvent.cancel();
        }
        nextCycleEvent = scheduler.doIn(cycleTime, this::cycleLights, "Traffic light change state event");
    }

    private void cycleLights() {
        TrafficLightState.LightType nextToTurnRed = currentState.nextTypeToTurnRed();
        TrafficLightState.LightType nextToTurnGreen = TrafficLightState.LightType.getInverse(nextToTurnRed);

        updateTrafficLights(builder -> builder.setColourForType(nextToTurnRed, LightColour.RED));

        scheduler.doIn(CYCLE_CHANGE_DELAY, (now) -> {
            updateTrafficLights(builder -> builder.setColourForType(nextToTurnGreen, LightColour.GREEN).setCrossingRequested(false));
            timeOfPreviousCycleChange = now;

            if (automaticTrafficLightChangeEnabled || currentState.canPedestrianCross()) {
                scheduleNextCycleIn(getDurationOfCycle(currentState));
            }
        }, "Finish light change event.");
    }

    private void updateTrafficLights(Function<TrafficLightState.Builder, TrafficLightState.Builder> mutator) {
        TrafficLightState oldState = currentState;
        currentState = mutator.apply(currentState.builder()).build();
        lightsStateChanged(oldState, currentState);
    }

    private void lightsStateChanged(TrafficLightState oldState, TrafficLightState newState) {
        logger.info("Traffic light is now {}", newState);

        if (!oldState.getPedestrianColour().equals(newState.getPedestrianColour())) {
            restSender.setPedestrianColour(newState.getPedestrianColour());
        }

        if (!oldState.getTrafficColour().equals(newState.getTrafficColour())) {
            restSender.setTrafficColour(newState.getTrafficColour());
        }
    }

    private double getDurationOfCycle(TrafficLightState currentState) {
        return lightDurationsForTraffic.get(currentState.getTrafficColour());
    }

    @VisibleForTesting
    public void setTrafficLight(LightColour lightColour) {
        LightColour pedestrianColour = LightColour.getInverse(lightColour);
        updateTrafficLights(builder -> builder.setPedestrianColour(pedestrianColour).setLightColour(lightColour));

        scheduleNextCycleIn(getDurationOfCycle(currentState));
    }

}
