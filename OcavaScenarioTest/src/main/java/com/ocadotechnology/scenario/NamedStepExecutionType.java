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

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Preconditions;
import com.ocadotechnology.scenario.CheckStepExecutionType.UnorderedModifier;

/**
 * Defines the execution mode for one or more associated execute steps. This is used with the modifier methods sequenced
 * and failingStep which are available on various framework step definition classes.
 */
@ParametersAreNonnullByDefault
public final class NamedStepExecutionType {
    @CheckForNull
    private final String name;
    private final boolean isSequenced;
    private final boolean isFailingStep;

    NamedStepExecutionType(@CheckForNull String name, boolean isSequenced, boolean isFailingStep) {
        this.name = name;
        this.isSequenced = isSequenced;
        this.isFailingStep = isFailingStep;
    }

    public static NamedStepExecutionType ordered() {
        return new NamedStepExecutionType(null, false, false);
    }

    public static NamedStepExecutionType sequenced(String name) {
        return new NamedStepExecutionType(Preconditions.checkNotNull(name, "Sequenced steps must have a name"), true, false);
    }

    public static NamedStepExecutionType failingStep() {
        return new NamedStepExecutionType(null, false, true);
    }

    public boolean isSequenced() {
        return isSequenced;
    }

    public String getName() {
        return Preconditions.checkNotNull(name, "No defined name provided for named step");
    }

    public boolean isFailingStep() {
        return isFailingStep;
    }

    public boolean isBasicOrdered() {
        return !isFailingStep && !isSequenced;
    }

    CheckStepExecutionType getCheckStepExecutionType() {
        return new CheckStepExecutionType(
                name,
                null,
                isSequenced ? UnorderedModifier.SEQUENCED : null,
                isFailingStep,
                null,
                null);
    }

    public NamedStepExecutionType merge(NamedStepExecutionType other) {
        Preconditions.checkState(!this.isFailingStep || !other.isFailingStep,
                "Cannot merge two failing NamedStepExecutionType instances");
        Preconditions.checkState(!this.isSequenced || !other.isSequenced,
                "Cannot merge two sequenced NamedStepExecutionType instances");
        Preconditions.checkState(this.name == null || other.name == null,
                "Cannot merge two NamedStepExecutionType instances with defined names");

        return new NamedStepExecutionType(
                this.name != null ? this.name : other.name,
                this.isSequenced || other.isSequenced,
                this.isFailingStep || other.isFailingStep
        );
    }
}
