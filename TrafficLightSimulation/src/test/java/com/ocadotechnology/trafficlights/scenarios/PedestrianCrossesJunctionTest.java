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
package com.ocadotechnology.trafficlights.scenarios;

import static com.ocadotechnology.trafficlights.TrafficConfig.TrafficLight.INITIAL_PEDESTRIAN_STATE;
import static com.ocadotechnology.trafficlights.TrafficConfig.TrafficLight.INITIAL_TRAFFIC_STATE;
import static com.ocadotechnology.trafficlights.controller.LightColour.GREEN;
import static com.ocadotechnology.trafficlights.controller.LightColour.RED;

import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.Story;
import com.ocadotechnology.trafficlights.TrafficSimulationStory;

@Story
class PedestrianCrossesJunctionTest extends TrafficSimulationStory {

    @Test
    void scenario() {

        //Simulation starts with a green traffic light.
        given.config.set(INITIAL_PEDESTRIAN_STATE, RED);
        given.config.set(INITIAL_TRAFFIC_STATE, GREEN);
        when.simulation.starts();

        //When person arrives at crossing
        when.pedestrian.arrives();
        //Then button pressed
        then.pedestrian.pressesButton();

        //Controller sends traffic and crossing light change
        then.trafficLight.changesTrafficLightTo(RED);
        then.trafficLight.changesPedestrianLightTo(GREEN);
        //Person crosses
        then.pedestrian.startsCrossing();
        then.pedestrian.finishesCrossing();

    }
}
