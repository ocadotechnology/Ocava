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
package com.ocadotechnology.scenario;

public class TestWhen {
    private final CoreSimulationWhenSteps<?> simulationWhenSteps;
    public final TestEventWhenSteps testEvent;
    public final TestThreadWhenSteps testThread;

    public TestWhen(StepManager<FrameworkTestSimulation> stepManager, FrameworkTestSimulationApi simulationApi, ScenarioNotificationListener listener) {
        this.simulationWhenSteps = new CoreSimulationWhenSteps<>(stepManager, simulationApi, listener);
        this.testEvent = new TestEventWhenSteps(stepManager);
        this.testThread = new TestThreadWhenSteps(stepManager);
    }

    public void simStarts() {
        simulationWhenSteps.starts(TestSimulationStarts.class);
    }
}
