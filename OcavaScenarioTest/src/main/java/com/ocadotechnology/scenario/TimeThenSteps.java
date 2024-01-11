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

import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;

import com.ocadotechnology.simulation.Simulation;

/**
 * Collection of steps used for pausing step execution for a fixed time, or making assertions about the simulation time.
 * These steps are final so that the decorator methods will work with them.
 */
@ParametersAreNonnullByDefault
public final class TimeThenSteps<S extends Simulation> {
    private final ScenarioSimulationApi<S> scenarioSimulationApi;
    private final StepManager<S> stepManager;
    private final ScenarioNotificationListener scenarioNotificationListener;
    private final NamedStepExecutionType namedStepExecutionType;

    public TimeThenSteps(ScenarioSimulationApi<S> scenarioSimulationApi, StepManager<S> stepManager, ScenarioNotificationListener scenarioNotificationListener) {
        this(scenarioSimulationApi, stepManager, scenarioNotificationListener, NamedStepExecutionType.ordered());
    }

    private TimeThenSteps(
            ScenarioSimulationApi<S> scenarioSimulationApi,
            StepManager<S> stepManager,
            ScenarioNotificationListener scenarioNotificationListener,
            NamedStepExecutionType namedStepExecutionType) {
        this.scenarioSimulationApi = scenarioSimulationApi;
        this.stepManager = stepManager;
        this.scenarioNotificationListener = scenarioNotificationListener;
        this.namedStepExecutionType = namedStepExecutionType;
    }

    /**
     * @return an instance of the TimeThenSteps where the steps it creates will use the supplied NamedStepExecutionType
     *          object. Used in composite steps which contain a {@link TimeThenSteps} instance.
     */
    public TimeThenSteps<S> modify(NamedStepExecutionType executionType) {
        return new TimeThenSteps<>(scenarioSimulationApi, stepManager, scenarioNotificationListener, executionType);
    }

    /**
     * @return an instance of the TimeThenSteps where the steps it creates will use a NamedStepExecutionType calculated
     *          from the supplied CheckStepExecutionType object. Used in composite steps which contain a
     *          {@link TimeThenSteps} instance.
     */
    public TimeThenSteps<S> modify(CheckStepExecutionType executionType) {
        return modify(executionType.getNamedStepExecutionType());
    }

    /**
     * @return an instance of the TimeThenSteps where the steps it creates has the
     * {@code NamedStepExecutionType.isFailingStep} flag set to true. The failingStep flag is checked after the scenario
     * test has completed successfully or exceptionally and should be used in conjunction with {@link FixRequired}
     *
     * @throws IllegalStateException if called after a previous invocation of this method
     */
    public TimeThenSteps<S> failingStep() {
        return new TimeThenSteps<>(scenarioSimulationApi, stepManager, scenarioNotificationListener, NamedStepExecutionType.failingStep().merge(namedStepExecutionType));
    }

    /**
     * @return an instance of the TimeThenSteps where the steps it creates are linked to
     * create an ordered sub-sequence with other steps of the same name.
     *
     * @throws IllegalStateException if called after a previous invocation of this method
     * @throws NullPointerException if the name is null
     */
    public TimeThenSteps<S> sequenced(String name) {
        return new TimeThenSteps<>(scenarioSimulationApi, stepManager, scenarioNotificationListener, NamedStepExecutionType.sequenced(name).merge(namedStepExecutionType));
    }

    private void addExecuteStep(Runnable runnable) {
        stepManager.add(new SimpleExecuteStep(runnable), namedStepExecutionType);
    }

    /**
     * Extracts the simulation time at the point that this step executes, as returned by the simulation scheduler's timeProvider
     *
     * @return {@link StepFuture} which will contain the current simulation time at the point that this step executes
     */
    public StepFuture<Double> getCurrentTime() {
        MutableStepFuture<Double> currentTime = new MutableStepFuture<>();
        addExecuteStep(() -> {
            double now = scenarioSimulationApi.getEventScheduler().getTimeProvider().getTime();
            currentTime.populate(now);
        });
        return currentTime;
    }

    /**
     * Waits until a specified duration since the start of the test
     */
    public void waitUntil(double time, TimeUnit unit) {
        stepManager.add(new WaitStep(scenarioSimulationApi, scenarioNotificationListener) {
            @Override
            protected double time() {
                return scenarioSimulationApi.getSchedulerStartTime() + convertToUnit(time, unit, stepManager.getTimeUnit());
            }
        }, namedStepExecutionType);
    }

    /**
     * Waits until a specified point in time
     *
     * @param instant - the absolute time as returned by the simulation scheduler's timeProvider that the test should wait for
     */
    public void waitUntil(StepFuture<Double> instant) {
        stepManager.add(new WaitStep(scenarioSimulationApi, scenarioNotificationListener) {
            @Override
            protected double time() {
                return instant.get();
            }
        }, namedStepExecutionType);
    }

    /**
     * Waits for 1 clock tick after this step is started
     */
    public void waitForNextClockTick() {
        stepManager.add(new WaitStep(scenarioSimulationApi, scenarioNotificationListener) {
            @Override
            protected double time() {
                return scenarioSimulationApi.getEventScheduler().getTimeProvider().getTime() + scenarioSimulationApi.getEventScheduler().getMinimumTimeDelta();
            }
        }, namedStepExecutionType);
    }

    /**
     * Waits for a specified duration after this step is started
     */
    public void waitForDuration(double duration, TimeUnit unit) {
        stepManager.add(new WaitStep(scenarioSimulationApi, scenarioNotificationListener) {
            @Override
            protected double time() {
                return scenarioSimulationApi.getEventScheduler().getTimeProvider().getTime() + convertToUnit(duration, unit, stepManager.getTimeUnit());
            }
        }, namedStepExecutionType);
    }

    /**
     * Waits for a specified duration after this step is started
     */
    public void waitForDuration(StepFuture<Double> duration) {
        stepManager.add(new WaitStep(scenarioSimulationApi, scenarioNotificationListener) {
            @Override
            protected double time() {
                return scenarioSimulationApi.getEventScheduler().getTimeProvider().getTime() + duration.get();
            }
        }, namedStepExecutionType);
    }

    /**
     * Asserts that the specified duration has not yet passed at the time this step executes.
     * The provided time is compared with the elapsed time since the simulation started. (see
     * {@link ScenarioSimulationApi#getSchedulerStartTime()})
     */
    public void timeIsLessThan(double time, TimeUnit unit) {
        addExecuteStep(() -> {
            double beforeTime = scenarioSimulationApi.getSchedulerStartTime() + convertToUnit(time, unit, stepManager.getTimeUnit());
            double now = scenarioSimulationApi.getEventScheduler().getTimeProvider().getTime();
            Assertions.assertTrue(now < beforeTime, "Time now (" + now + ") should not exceed " + beforeTime);
        });
    }

    /**
     * Asserts that the specified instant has not yet passed at the time this step executes.
     * The provided time is compared with the time returned by the simulation scheduler's timeProvider.
     */
    public void timeIsLessThan(StepFuture<Double> instant) {
        addExecuteStep(() -> {
            double beforeTime = instant.get();
            double now = scenarioSimulationApi.getEventScheduler().getTimeProvider().getTime();
            Assertions.assertTrue(now < beforeTime, "Time now (" + now + ") should not exceed " + beforeTime);
        });
    }

    public static double convertToUnit(double magnitude, TimeUnit sourceUnit, TimeUnit targetUnit) {
        return sourceUnit.toNanos(1) * magnitude / targetUnit.toNanos(1);
    }
}
