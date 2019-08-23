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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Causes a scenario test to wait until the specified time.
 */
public abstract class WaitStep extends NamedStep implements Executable {
    private final ScenarioSimulationApi scenarioSimulationApi;
    private final ScenarioNotificationListener listener;

    private final AtomicBoolean finished = new AtomicBoolean(false);

    private boolean executed;

    public WaitStep(ScenarioSimulationApi scenarioSimulationApi, ScenarioNotificationListener listener) {
        this.scenarioSimulationApi = scenarioSimulationApi;
        this.listener = listener;
    }

    @Override
    public boolean isRequired() {
        return true;
    }

    @Override
    public boolean isFinished() {
        return finished.get();
    }

    @Override
    public void execute() {
        // Only one call to execute should action something
        if (executed) {
            return;
        }
        executed = true;

        // We don't care about notifications we receive whilst waiting
        listener.suspend();

        scenarioSimulationApi.getEventScheduler().doAt(time(), this::executeScheduledStep, "Execute ScheduledStep Test Step: " + this);
    }

    @Override
    public boolean isMergeable() {
        return false;
    }

    @Override
    public void merge(Executable step) {}

    private void executeScheduledStep() {
        listener.resume();
        finished.set(true);
        listener.tryToExecuteNextStep(false);
    }

    /**
     * The time that you want the scenario test to wait until.
     */
    protected abstract double time();
}
