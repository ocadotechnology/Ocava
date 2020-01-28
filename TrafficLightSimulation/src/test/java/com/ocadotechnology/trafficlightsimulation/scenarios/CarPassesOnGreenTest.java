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
package com.ocadotechnology.trafficlightsimulation.scenarios;

import static com.ocadotechnology.trafficlightsimulation.controller.TrafficLightController.State.GREEN;

import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.Story;
import com.ocadotechnology.trafficlightsimulation.TrafficConfig.TrafficLight;
import com.ocadotechnology.trafficlightsimulation.TrafficSimulationStory;

@Story
class CarPassesOnGreenTest extends TrafficSimulationStory {

    @Test
    void scenario() {
        //we set traffic light to be GREEN initially
        given.config.set(TrafficLight.INITIAL_STATE, GREEN);

        when.simulation.starts();

        //when we add a car, it should leave
        when.car.arrives();
        then.car.leaves();
    }
}
