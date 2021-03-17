/*
 * Copyright © 2017-2021 Ocado (Ocava)
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

import org.junit.jupiter.api.Assertions;

import com.ocadotechnology.simulation.Simulation;

public class TimeThenSteps<S extends Simulation> {
    private final ScenarioSimulationApi<S> scenarioSimulationApi;
    private final StepManager<S> stepManager;
    private final ScenarioNotificationListener scenarioNotificationListener;

    public TimeThenSteps(ScenarioSimulationApi<S> scenarioSimulationApi, StepManager<S> stepManager, ScenarioNotificationListener scenarioNotificationListener) {
        this.scenarioSimulationApi = scenarioSimulationApi;
        this.stepManager = stepManager;
        this.scenarioNotificationListener = scenarioNotificationListener;
    }

    public void waitUntil(double time, TimeUnit unit) {
        stepManager.add(new WaitStep(scenarioSimulationApi, scenarioNotificationListener) {
            @Override
            protected double time() {
                return scenarioSimulationApi.getSchedulerStartTime() + convertToUnit(time, unit, stepManager.getTimeUnit());
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

    public void waitForDuration(double duration, TimeUnit unit) {
        stepManager.add(new WaitStep(scenarioSimulationApi, scenarioNotificationListener) {
            @Override
            protected double time() {
                return scenarioSimulationApi.getEventScheduler().getTimeProvider().getTime() + convertToUnit(duration, unit, stepManager.getTimeUnit());
            }
        });
    }

    public void waitForDuration(StepFuture<Double> duration) {
        stepManager.add(new WaitStep(scenarioSimulationApi, scenarioNotificationListener) {
            @Override
            protected double time() {
                return scenarioSimulationApi.getEventScheduler().getTimeProvider().getTime() + duration.get();
            }
        });
    }

    /**
     * Asserts that the specified duration has not yet passed at the time this step executes.
     * The provided time is compared with the elapsed time since the simulation started. (see
     * {@link ScenarioSimulationApi#getSchedulerStartTime()})
     */
    public void timeIsLessThan(double time, TimeUnit unit) {
        stepManager.addExecuteStep(() -> {
            double beforeTime = scenarioSimulationApi.getSchedulerStartTime() + convertToUnit(time, unit, stepManager.getTimeUnit());
            double now = scenarioSimulationApi.getEventScheduler().getTimeProvider().getTime();
            Assertions.assertTrue(now < beforeTime, "Time now (" + now + ") should not exceed " + beforeTime);
        });
    }

    public static double convertToUnit(double magnitude, TimeUnit sourceUnit, TimeUnit targetUnit) {
        return sourceUnit.toNanos(1) * magnitude / targetUnit.toNanos(1);
    }
}
