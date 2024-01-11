/*
 * Copyright © 2017-2024 Ocado (Ocava)
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

import javax.annotation.ParametersAreNonnullByDefault;

import com.ocadotechnology.simulation.Simulation;

/**
 * An abstract class which should be extended by each distinct set of when events that need to be implemented as
 * part of the testing package.  Each implementation should be generic on itself so that it can be correctly modified by
 * the decorator method {@link AbstractWhenSteps#sequenced(String)}
 */
@ParametersAreNonnullByDefault
public abstract class AbstractWhenSteps<S extends Simulation, W extends AbstractWhenSteps<S, ?>> {
    private final StepManager<S> stepManager;
    private final NamedStepExecutionType namedStepExecutionType;

    public AbstractWhenSteps(StepManager<S> stepManager, NamedStepExecutionType namedStepExecutionType) {
        this.stepManager = stepManager;
        this.namedStepExecutionType = namedStepExecutionType;
    }

    /**
     * Abstract method used to create instances of the sub-class with modified {@link NamedStepExecutionType} values
     */
    protected abstract W create(StepManager<S> stepManager, NamedStepExecutionType executionType);

    /**
     * @return an instance of the concrete sub-class of AbstractWhenSteps where the steps it creates will use the
     *          supplied NamedStepExecutionType object. Used in composite steps which contain a
     *          {@link AbstractWhenSteps} instance.
     */
    public W modify(NamedStepExecutionType executionType) {
        return create(stepManager, executionType);
    }

    /**
     * @return an instance of the concrete sub-class of AbstractWhenSteps where the steps it creates will use a
     *          NamedStepExecutionType object created from the supplied CheckStepExecutionType object. Used in composite
     *          steps which contain an {@link AbstractWhenSteps} instance.
     */
    public W modify(CheckStepExecutionType executionType) {
        return modify(executionType.getNamedStepExecutionType());
    }

    /**
     * @return an instance of the concrete sub-class of AbstractWhenSteps where the steps it creates are linked to
     * create an ordered sub-sequence with other steps of the same name.
     *
     * @throws IllegalStateException if called after a previous invocation of this method
     * @throws NullPointerException if the name is null
     */
    public W sequenced(String name) {
        return create(stepManager, NamedStepExecutionType.sequenced(name).merge(namedStepExecutionType));
    }

    protected void addExecuteStep(Runnable r) {
        stepManager.add(new SimpleExecuteStep(r), namedStepExecutionType);
    }

    protected S getSimulation() {
        return stepManager.getSimulation();
    }
}
