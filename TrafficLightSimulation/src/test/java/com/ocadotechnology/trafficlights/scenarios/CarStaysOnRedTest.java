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

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.Story;
import com.ocadotechnology.trafficlights.TrafficConfig;
import com.ocadotechnology.trafficlights.TrafficConfig.TrafficLight;
import com.ocadotechnology.trafficlights.TrafficSimulationStory;
import com.ocadotechnology.trafficlights.controller.LightColour;
import com.ocadotechnology.trafficlights.controller.TrafficLightController.Mode;

@Story
class CarStaysOnRedTest extends TrafficSimulationStory {

    private static final String YOU_SHALL_NOT_PASS = "YOU_SHALL_NOT_PASS";

    @Test
    void scenario() {
        //we set traffic light to be RED initially
        given.config.set(TrafficLight.INITIAL_TRAFFIC_STATE, LightColour.RED);
        //making sure traffic light stays RED unless we tell it to change
        given.config.set(TrafficConfig.TrafficLight.MODE, Mode.PEDESTRIAN_REQUEST_ONLY);

        when.simulation.starts();
        when.car.arrives();

        //car should not pass on RED
        then.car.never(YOU_SHALL_NOT_PASS).leaves();

        //waiting for a while, so we are sure car is not passing
        then.time.waitForDuration(2, TimeUnit.MINUTES);

        //change traffic light to green, car can now leave
        when.trafficLight.isChangedTo(LightColour.GREEN);

        //remove the YOU_SHALL_NOT_PASS restriction, as we now want the car to move
        then.unordered.removesUnorderedSteps(YOU_SHALL_NOT_PASS);

        //car leaves
        then.car.leaves();
    }
}