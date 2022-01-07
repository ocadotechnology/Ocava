/*
 * Copyright © 2017-2022 Ocado (Ocava)
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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.config.Config;
import com.ocadotechnology.event.scheduling.Cancelable;
import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.trafficlights.TrafficConfig;
import com.ocadotechnology.trafficlights.TrafficConfig.TrafficLight;
import com.ocadotechnology.validation.Failer;

public class TrafficLightController implements RestHandler {

    public enum Mode {
        /*
        Only schedule light change if the pedestrian light is GREEN. Otherwise, vehicles should continue to move until
        a pedestrian crossing is requested via a button press.
        */
        PEDESTRIAN_REQUEST_ONLY,
        /*
        Always cycle lights. A pedestrian crossing request via a button press may separately adjust the cycle durations.
        */
        AUTOMATIC_CHANGE,
        /*
        Do nothing in manual mode. A pedestrian button press will still schedule a light change.
        In this case the lights will never switch back and vehicles will be stuck.
        */
        MANUAL
    }

    private static final Logger logger = LoggerFactory.getLogger(TrafficLightController.class);
    private static final int CYCLE_CHANGE_DELAY = 100;
    private final double pedestrianButtonPressLightChangeDelay;

    private final RestSender restSender;

    private final EventScheduler scheduler;
    private final ImmutableMap<LightColour, Long> lightDurationsForTraffic;
    private TrafficLightState currentState;
    private Mode trafficLightMode;
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
                .put(LightColour.RED, trafficConfig.getValue(TrafficLight.RED_LIGHT_INTERVAL).asTime())
                .put(LightColour.GREEN, trafficConfig.getValue(TrafficLight.GREEN_LIGHT_INTERVAL).asTime())
                .build();

        this.pedestrianButtonPressLightChangeDelay = trafficConfig.getValue(TrafficLight.PEDESTRIAN_CROSSING_CHANGE_DELAY).asTime();
        this.trafficLightMode = trafficConfig.getValue(TrafficLight.MODE).asEnum(Mode.class);
        Preconditions.checkState(!trafficLightMode.equals(Mode.MANUAL), "%s mode is only allowed in testing.", Mode.MANUAL.toString());

        scheduler.doNow(() -> initialiseTrafficLight(startingTrafficLightColour, startingPedestrianLightColour));
    }

    private void initialiseTrafficLight(LightColour lightColour, LightColour pedestrianColour) {
        this.currentState = new TrafficLightState(lightColour, pedestrianColour, false);
        updateTrafficLightState(Function.identity());

        scheduleNextEventInMode();
    }

    private void scheduleNextEventInMode() {
        switch (trafficLightMode) {
            case PEDESTRIAN_REQUEST_ONLY:
                if (currentState.canPedestrianCross()) {
                    scheduleNextCycleIn(getDurationOfCycle(currentState));
                }
                break;

            case AUTOMATIC_CHANGE:
                scheduleNextCycleIn(getDurationOfCycle(currentState));
                break;

            case MANUAL:
                break;

            default:
                throw Failer.fail("Unrecognised mode {}", trafficLightMode);
        }
    }

    @Override
    public void pedestrianLightsButtonPressed() {
        if (currentState.isPedestrianCrossingRequested() || currentState.canPedestrianCross()) {
            return;
        }

        updateTrafficLightState(builder -> builder.setCrossingRequested(true));

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
        TrafficLightState.LightType nextToTurnGreen = currentState.nextTypeToTurnGreen();
        TrafficLightState.LightType nextToTurnRed = TrafficLightState.LightType.getInverse(nextToTurnGreen);

        updateTrafficLightState(builder -> builder.setColourForType(nextToTurnRed, LightColour.RED));

        scheduler.doIn(CYCLE_CHANGE_DELAY, (now) -> {
            updateTrafficLightState(builder -> builder.setColourForType(nextToTurnGreen, LightColour.GREEN).setCrossingRequested(false));
            timeOfPreviousCycleChange = now;

            scheduleNextEventInMode();
        }, "Finish light change event.");
    }

    private void updateTrafficLightState(Function<TrafficLightState.Builder, TrafficLightState.Builder> mutator) {
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
        updateTrafficLightState(builder -> builder.setPedestrianColour(pedestrianColour).setLightColour(lightColour));

        scheduleNextCycleIn(getDurationOfCycle(currentState));
    }

    @VisibleForTesting
    public void placeUnderManualControl() {
        if (nextCycleEvent != null) {
            nextCycleEvent.cancel();
        }
        trafficLightMode = Mode.MANUAL;
    }

}
