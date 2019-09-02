/*
 * Copyright © 2017 Ocado (Ocava)
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

import java.util.concurrent.TimeUnit;

public class TimeThenSteps {
    private final ScenarioSimulationApi scenarioSimulationApi;
    private final StepManager stepManager;
    private final ScenarioNotificationListener scenarioNotificationListener;

    public TimeThenSteps(ScenarioSimulationApi scenarioSimulationApi, StepManager stepManager, ScenarioNotificationListener scenarioNotificationListener) {
        this.scenarioSimulationApi = scenarioSimulationApi;
        this.stepManager = stepManager;
        this.scenarioNotificationListener = scenarioNotificationListener;
    }

    public void waitUntil(long time, TimeUnit unit) {
        stepManager.add(new WaitStep(scenarioSimulationApi, scenarioNotificationListener) {
            @Override
            protected double time() {
                return scenarioSimulationApi.getSchedulerStartTime() + stepManager.getTimeUnit().convert(time, unit);
            }
        });
    }

    public void waitForNextClockTick() {
        stepManager.add(new WaitStep(scenarioSimulationApi, scenarioNotificationListener) {
            @Override
            protected double time() {
                return scenarioSimulationApi.getEventScheduler().getTimeProvider().getTime() + scenarioSimulationApi.getEventScheduler().getMinimumTimeDelta();
            }
        });
    }

    public void waitForDuration(long duration, TimeUnit unit) {
        stepManager.add(new WaitStep(scenarioSimulationApi, scenarioNotificationListener) {
            @Override
            protected double time() {
                return scenarioSimulationApi.getEventScheduler().getTimeProvider().getTime() + stepManager.getTimeUnit().convert(duration, unit);
            }
        });
    }
}
