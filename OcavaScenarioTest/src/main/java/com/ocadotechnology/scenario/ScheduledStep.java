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

import org.junit.jupiter.api.Assertions;

import com.ocadotechnology.event.scheduling.EventScheduler;

public abstract class ScheduledStep extends NamedStep implements Executable {
    protected final AtomicBoolean finished = new AtomicBoolean(false);
    protected final ScenarioSimulationApi scenarioSimulationApi;

    public ScheduledStep(ScenarioSimulationApi scenarioSimulationApi) {
        this.scenarioSimulationApi = scenarioSimulationApi;
    }

    public boolean isRequired() {
        return true;
    }

    public boolean isFinished() {
        return this.finished.get();
    }

    public void execute() {
        this.finished.set(true);
        if (this.isSchedulable()) {
            EventScheduler eventScheduler = this.scenarioSimulationApi.getEventScheduler();
            Assertions.assertNotNull(eventScheduler, "SimulationEventScheduler was not initialized");
            double when = this.time(eventScheduler);
            Runnable what = this::executeScheduledStep;
            String why = "Execute ScheduledStep Test Step: " + this;
            if (this.isDaemon()) {
                eventScheduler.doAtDaemon(when, what, why);
            } else {
                eventScheduler.doAt(when, what, why);
            }

        }
    }

    public boolean isMergeable() {
        return false;
    }

    public void merge(Executable step) {}

    protected boolean isSchedulable() {
        return true;
    }

    protected boolean isDaemon() {
        return false;
    }

    protected abstract void executeScheduledStep();

    protected abstract double time(EventScheduler scheduler);
}
