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
package com.ocadotechnology.trafficlights.glue;

import com.ocadotechnology.scenario.ScenarioNotificationListener;
import com.ocadotechnology.scenario.StepManager;
import com.ocadotechnology.trafficlights.TrafficSimulation;
import com.ocadotechnology.trafficlights.TrafficSimulationApi;
import com.ocadotechnology.trafficlights.steps.CarWhenSteps;
import com.ocadotechnology.trafficlights.steps.PedestrianWhenSteps;
import com.ocadotechnology.trafficlights.steps.SimulationWhenSteps;
import com.ocadotechnology.trafficlights.steps.TrafficLightWhenSteps;

public class When {
    //generic steps
    public final SimulationWhenSteps simulation;

    //simulation specific steps
    public final CarWhenSteps car;
    public final PedestrianWhenSteps pedestrian;
    public final TrafficLightWhenSteps trafficLight;

    public When(StepManager<TrafficSimulation> stepManager, TrafficSimulationApi simulationApi, ScenarioNotificationListener listener) {
        simulation = new SimulationWhenSteps(stepManager, simulationApi, listener);
        car = new CarWhenSteps(stepManager);
        pedestrian = new PedestrianWhenSteps(stepManager);
        trafficLight = new TrafficLightWhenSteps(stepManager);
    }
}
