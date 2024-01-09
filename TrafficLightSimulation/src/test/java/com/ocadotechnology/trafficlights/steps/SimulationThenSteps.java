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
package com.ocadotechnology.trafficlights.steps;

import org.slf4j.LoggerFactory;

import com.ocadotechnology.scenario.AbstractThenSteps;
import com.ocadotechnology.scenario.CheckStepExecutionType;
import com.ocadotechnology.scenario.NotificationCache;
import com.ocadotechnology.scenario.StepManager;
import com.ocadotechnology.trafficlights.SimulationEndedNotification;
import com.ocadotechnology.trafficlights.TrafficSimulation;

public class SimulationThenSteps extends AbstractThenSteps<TrafficSimulation, SimulationThenSteps> {

    private SimulationThenSteps(StepManager<TrafficSimulation> stepManager, NotificationCache notificationCache, CheckStepExecutionType checkStepExecutionType) {
        super(stepManager, notificationCache, checkStepExecutionType);
    }

    public SimulationThenSteps(StepManager<TrafficSimulation> stepManager, NotificationCache notificationCache) {
        this(stepManager, notificationCache, CheckStepExecutionType.ordered());
    }

    @Override
    protected SimulationThenSteps create(StepManager<TrafficSimulation> stepManager, NotificationCache notificationCache, CheckStepExecutionType checkStepExecutionType) {
        return new SimulationThenSteps(stepManager, notificationCache, checkStepExecutionType);
    }

    /**
     * Generally, we should avoid using this step. We should mostly be stopping on meeting
     * some test condition (i.e. physical outcome, message sent). This way, the test is more robust, and runs faster.
     * Some tests do require the simulation to run to the end, such as tests that graph over a specific time period etc.
     * These sorts of tests can use this step, specifying the reason that the simulation must run to completion.
     */
    public void hasFinished(String reason) {
        addCheckStep(SimulationEndedNotification.class, notification -> {
            LoggerFactory.getLogger(SimulationThenSteps.class).info("Simulation ended, {}", reason);
            return true;
        });
    }
}
