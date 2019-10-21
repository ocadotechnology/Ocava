/*
 * Copyright © 2017-2019 Ocado (Ocava)
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
package com.ocadotechnology.scenario;

import com.ocadotechnology.notification.NotificationRouter;

public class TestEventWhenSteps {

    private final StepManager stepManager;
    private final ScenarioSimulationApi simulationHolder;

    public TestEventWhenSteps(StepManager stepManager, ScenarioSimulationApi simulationHolder) {
        this.stepManager = stepManager;
        this.simulationHolder = simulationHolder;
    }

    public void scheduled(int time, String name) {
        stepManager.addExecuteStep(() -> {
                simulationHolder.getEventScheduler().doAt(
                        time,
                        () -> NotificationRouter.get().broadcast(new TestEventNotification(name)),
                        "scheduled(" + time + ", \"" + name + "\")");
            });
    }
}
