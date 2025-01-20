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

import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.Story;
import com.ocadotechnology.trafficlights.TrafficConfig.TrafficLight;
import com.ocadotechnology.trafficlights.TrafficSimulationStory;
import com.ocadotechnology.trafficlights.controller.LightColour;

@Story
class CarPassesOnGreenTest extends TrafficSimulationStory {

    @Test
    void scenario() {
        //we set traffic light to be GREEN initially
        given.config.set(TrafficLight.INITIAL_TRAFFIC_STATE, LightColour.GREEN);

        when.simulation.starts();

        //when we add a car, it should leave
        when.car.arrives();
        then.car.leaves();
    }
}
