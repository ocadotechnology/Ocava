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

import static com.ocadotechnology.trafficlights.TrafficConfig.TrafficLight.INITIAL_PEDESTRIAN_STATE;
import static com.ocadotechnology.trafficlights.TrafficConfig.TrafficLight.INITIAL_TRAFFIC_STATE;
import static com.ocadotechnology.trafficlights.controller.LightColour.GREEN;
import static com.ocadotechnology.trafficlights.controller.LightColour.RED;

import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.Story;
import com.ocadotechnology.trafficlights.TrafficConfig;
import com.ocadotechnology.trafficlights.TrafficSimulationStory;
import com.ocadotechnology.trafficlights.controller.TrafficLightController.Mode;

@Story
public class BothLightsStartupRedAndCycleTest extends TrafficSimulationStory {

    @Test
    void scenario() {
        given.config.set(INITIAL_TRAFFIC_STATE, RED);
        given.config.set(INITIAL_PEDESTRIAN_STATE, RED);
        given.config.set(TrafficConfig.TrafficLight.MODE, Mode.AUTOMATIC_CHANGE);

        when.simulation.starts();

        then.trafficLight.changesPedestrianLightTo(GREEN);
        then.trafficLight.changesPedestrianLightTo(RED);
        then.trafficLight.changesTrafficLightTo(GREEN);
        then.trafficLight.changesTrafficLightTo(RED);
    }

}
