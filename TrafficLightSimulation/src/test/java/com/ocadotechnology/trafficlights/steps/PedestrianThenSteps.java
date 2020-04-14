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

import com.ocadotechnology.scenario.AbstractThenSteps;
import com.ocadotechnology.scenario.NotificationCache;
import com.ocadotechnology.scenario.StepManager;
import com.ocadotechnology.scenario.StepManager.CheckStepExecutionType;
import com.ocadotechnology.trafficlights.simulation.PedestrianFinishesCrossingNotification;
import com.ocadotechnology.trafficlights.simulation.PedestrianStartsCrossingNotification;
import com.ocadotechnology.trafficlights.simulation.comms.PedestrianCrossingRequestedNotification;

public class PedestrianThenSteps extends AbstractThenSteps<PedestrianThenSteps> {

    public PedestrianThenSteps(StepManager stepManager, NotificationCache notificationCache) {
        this(stepManager, notificationCache, CheckStepExecutionType.ordered(), false);
    }

    private PedestrianThenSteps(StepManager stepManager, NotificationCache notificationCache, CheckStepExecutionType checkStepExecutionType, boolean isFailingStep) {
        super(stepManager, notificationCache, checkStepExecutionType, isFailingStep);
    }

    @Override
    protected PedestrianThenSteps create(StepManager stepManager, NotificationCache notificationCache, CheckStepExecutionType checkStepExecutionType, boolean isFailingStep) {
        return new PedestrianThenSteps(stepManager, notificationCache, checkStepExecutionType, isFailingStep);
    }

    public void pressesButton() {
        addCheckStep(PedestrianCrossingRequestedNotification.class, notification -> true);
    }

    public void startsCrossing() {
        addCheckStep(PedestrianStartsCrossingNotification.class, notification -> true);
    }

    public void finishesCrossing() {
        addCheckStep(PedestrianFinishesCrossingNotification.class, notification -> true);
    }
}
