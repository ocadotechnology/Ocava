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
package com.ocadotechnology.trafficlights.scenarios;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.ocadotechnology.scenario.Story;
import com.ocadotechnology.trafficlights.TrafficConfig;
import com.ocadotechnology.trafficlights.TrafficConfig.TrafficLight;
import com.ocadotechnology.trafficlights.TrafficSimulationStory;
import com.ocadotechnology.trafficlights.controller.LightColour;
import com.ocadotechnology.trafficlights.controller.TrafficLightController.Mode;

@Story
class TrafficLightsChangeTest extends TrafficSimulationStory {

    private static final String CHANGED = "CHANGED";

    @ParameterizedTest
    @EnumSource(LightColour.class) //will be run with RED and GREEN
    void scenarioWithWaitForSteps(LightColour initialColour) {
        //setting the initial state - RED/GREEN
        given.config.set(TrafficLight.INITIAL_TRAFFIC_STATE, initialColour);

        //setting traffic light to switch between RED and GREEN automatically
        given.config.set(TrafficConfig.TrafficLight.MODE, Mode.AUTOMATIC_CHANGE);
        when.simulation.starts();

        //define two steps to be executed in any order
        then.trafficLight.unordered(CHANGED).changesTrafficLightTo(LightColour.RED);
        then.trafficLight.unordered(CHANGED).changesTrafficLightTo(LightColour.GREEN);

        //wait for all steps labelled "CHANGED" to finish
        then.unordered.waitForSteps(CHANGED);

        //add other steps here that will be executed specifically after the `then.unordered.waitForSteps` step above
    }

    @ParameterizedTest
    @EnumSource(LightColour.class) //will be run with RED and GREEN
    void scenarioWithoutWaitForSteps(LightColour initialColour) {
        //setting the initial state - RED/GREEN
        given.config.set(TrafficLight.INITIAL_TRAFFIC_STATE, initialColour);

        //setting traffic light to switch between RED and GREEN automatically
        given.config.set(TrafficConfig.TrafficLight.MODE, Mode.AUTOMATIC_CHANGE);
        when.simulation.starts();

        //define two steps to be executed in any order
        then.trafficLight.unordered(CHANGED).changesTrafficLightTo(LightColour.RED);
        then.trafficLight.unordered(CHANGED).changesTrafficLightTo(LightColour.GREEN);

        //if the unordered steps are the last two steps in the test, we do not need waitForSteps,
        //scenario test framework will wait for outstanding unordered steps by default
    }
}