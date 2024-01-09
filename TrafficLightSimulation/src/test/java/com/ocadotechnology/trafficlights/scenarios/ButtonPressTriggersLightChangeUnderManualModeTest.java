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
package com.ocadotechnology.trafficlights.scenarios;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.Story;
import com.ocadotechnology.trafficlights.TrafficConfig.TrafficLight;
import com.ocadotechnology.trafficlights.TrafficSimulationStory;
import com.ocadotechnology.trafficlights.controller.LightColour;

@Story
public class ButtonPressTriggersLightChangeUnderManualModeTest extends TrafficSimulationStory {

    private static final String LIGHTS_NEVER_CHANGE = "LIGHTS_NEVER_CHANGE";

    @Test
    void scenario() {
        given.config.set(TrafficLight.INITIAL_TRAFFIC_STATE, LightColour.GREEN);
        given.config.set(TrafficLight.INITIAL_PEDESTRIAN_STATE, LightColour.RED);

        when.simulation.starts();

        when.trafficLight.placeUnderManualControl();
        then.trafficLight.never(LIGHTS_NEVER_CHANGE).changesTrafficLightTo(LightColour.RED);
        then.trafficLight.never(LIGHTS_NEVER_CHANGE).changesPedestrianLightTo(LightColour.GREEN);

        //Wait to verify that lights do not change automatically.
        then.time.waitForDuration(10, TimeUnit.MINUTES);

        //Assert that the lights will still change when a pedestrian requests to cross.
        then.unordered.removesUnorderedSteps(LIGHTS_NEVER_CHANGE);

        when.pedestrian.arrives();
        then.pedestrian.pressesButton();

        then.trafficLight.changesTrafficLightTo(LightColour.RED);
        then.trafficLight.changesPedestrianLightTo(LightColour.GREEN);

        //Make sure that the lights will not revert back automatically after the pedestrian has crossed.
        then.pedestrian.finishesCrossing();

        then.trafficLight.never(LIGHTS_NEVER_CHANGE).changesTrafficLightTo(LightColour.GREEN);
        then.trafficLight.never(LIGHTS_NEVER_CHANGE).changesPedestrianLightTo(LightColour.RED);
        then.simulation.hasFinished("Wait until simulation end to verify the never conditions.");
    }
}
