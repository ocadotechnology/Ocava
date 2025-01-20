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

import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;

public class CapturingStepManager extends StepManager<FrameworkTestSimulation> {
    private CheckStepExecutionType recordedCheckStepType;
    private NamedStepExecutionType recordedNamedStepType;

    public CapturingStepManager() {
        super(new StepCache(), new FrameworkTestSimulationApi(), null, null);
    }

    @Override
    public TimeUnit getTimeUnit() {
        return TimeUnit.SECONDS;
    }

    @Override
    public void add(CheckStep<?> checkStep, CheckStepExecutionType checkStepExecutionType) {
        Preconditions.checkState(recordedCheckStepType == null, "Received two calls to add a check step");
        recordedCheckStepType = checkStepExecutionType;
    }

    @Override
    public void add(NamedStep namedStep, NamedStepExecutionType namedStepExecutionType) {
        Preconditions.checkState(recordedNamedStepType == null, "Received two calls to add a named step");
        recordedNamedStepType = namedStepExecutionType;
    }

    public CheckStepExecutionType getAndResetRecordedCheckStepType() {
        CheckStepExecutionType type = recordedCheckStepType;
        recordedCheckStepType = null;
        return type;
    }

    public NamedStepExecutionType getAndResetRecordedNamedStepType() {
        NamedStepExecutionType type = recordedNamedStepType;
        recordedNamedStepType = null;
        return type;
    }
}
