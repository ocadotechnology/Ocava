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
package com.ocadotechnology.trafficlights.steps;

import org.slf4j.LoggerFactory;

import com.ocadotechnology.scenario.AbstractThenSteps;
import com.ocadotechnology.scenario.NotificationCache;
import com.ocadotechnology.scenario.StepManager;
import com.ocadotechnology.scenario.StepManager.CheckStepExecutionType;
import com.ocadotechnology.trafficlights.TrafficSimulation;
import com.ocadotechnology.trafficlights.simulation.CarLeavesNotification;

public class CarThenSteps extends AbstractThenSteps<TrafficSimulation, CarThenSteps> {

    public CarThenSteps(StepManager<TrafficSimulation> stepManager, NotificationCache notificationCache) {
        this(stepManager, notificationCache, CheckStepExecutionType.ordered());
    }

    private CarThenSteps(StepManager<TrafficSimulation> stepManager, NotificationCache notificationCache, CheckStepExecutionType checkStepExecutionType) {
        super(stepManager, notificationCache, checkStepExecutionType);
    }

    @Override
    protected CarThenSteps create(StepManager<TrafficSimulation> stepManager, NotificationCache notificationCache, CheckStepExecutionType checkStepExecutionType) {
        return new CarThenSteps(stepManager, notificationCache, checkStepExecutionType);
    }

    public void leaves() {
        addCheckStep(CarLeavesNotification.class, notification -> {
            LoggerFactory.getLogger(CarThenSteps.class).info("Car left, {}", notification.simulatedCar);
            return true;
        });
    }
}
