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
package com.ocadotechnology.trafficlights.steps;

import com.ocadotechnology.scenario.AbstractWhenSteps;
import com.ocadotechnology.scenario.NamedStepExecutionType;
import com.ocadotechnology.scenario.StepManager;
import com.ocadotechnology.trafficlights.TrafficSimulation;
import com.ocadotechnology.trafficlights.controller.LightColour;

public class TrafficLightWhenSteps extends AbstractWhenSteps<TrafficSimulation, TrafficLightWhenSteps> {
    public TrafficLightWhenSteps(StepManager<TrafficSimulation> stepManager) {
        this(stepManager, NamedStepExecutionType.ordered());
    }

    private TrafficLightWhenSteps(StepManager<TrafficSimulation> stepManager, NamedStepExecutionType executionType) {
        super(stepManager, executionType);
    }

    @Override
    protected TrafficLightWhenSteps create(StepManager<TrafficSimulation> stepManager, NamedStepExecutionType executionType) {
        return new TrafficLightWhenSteps(stepManager, executionType);
    }

    public void isChangedTo(LightColour colour) {
        addExecuteStep(() -> getSimulation().getTrafficLightController().setTrafficLight(colour));
    }

    public void placeUnderManualControl() {
        addExecuteStep(() -> getSimulation().getTrafficLightController().placeUnderManualControl());
    }
}
