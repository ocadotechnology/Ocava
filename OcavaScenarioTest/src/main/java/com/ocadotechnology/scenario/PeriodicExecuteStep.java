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

import com.ocadotechnology.event.scheduling.Cancelable;
import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.event.scheduling.RepeatingRunnable;

public class PeriodicExecuteStep extends ExecuteStep implements Cancelable {
    private final ExecuteStep executeStep;
    private final EventScheduler scheduler;
    private final double period;
    private boolean required = true; // it has to be executed only once
    private Cancelable cancelable;

    public PeriodicExecuteStep(ExecuteStep executeStep, EventScheduler scheduler, double period) {
        this.executeStep = executeStep;
        this.scheduler = scheduler;
        this.period = period;
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    @Override
    protected void executeStep() {
        required = false;
        cancelable = RepeatingRunnable.startInDaemonWithFixedDelay(0, period, "periodic execute step", executeStep::executeStep, scheduler);
    }

    @Override
    public void cancel() {
        cancelable.cancel();
    }
}
