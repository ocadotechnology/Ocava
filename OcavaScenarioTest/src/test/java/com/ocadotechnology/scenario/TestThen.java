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

public class TestThen {
    public final TimeThenSteps<?> time;
    public final UnorderedSteps<?> unordered;
    public final ExceptionThenSteps<?> exception;
    public final FuturesThenSteps futures;
    public final AssertionThenSteps<?> assertThat;

    public final TestEventThenSteps testEvent;

    public TestThen(StepManager<FrameworkTestSimulation> stepManager, NotificationCache notificationCache, FrameworkTestSimulationApi simulationApi, ScenarioNotificationListener listener) {
        this.time = new TimeThenSteps<>(simulationApi, stepManager, listener);
        this.unordered = new UnorderedSteps<>(stepManager);
        this.exception = new ExceptionThenSteps<>(stepManager);
        this.futures = new FuturesThenSteps(stepManager);
        this.testEvent = new TestEventThenSteps(stepManager, notificationCache);
        this.assertThat = new AssertionThenSteps<>(stepManager, notificationCache);
    }
}
