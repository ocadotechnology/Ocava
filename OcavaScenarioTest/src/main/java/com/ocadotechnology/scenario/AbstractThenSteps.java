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

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.notification.Notification;
import com.ocadotechnology.simulation.Simulation;

/**
 * An abstract class which should be extended by each distinct set of then conditions that need to be implemented as
 * part of the testing package.  Each implementation should be generic on itself so that it can be correctly modified by
 * the decorator methods {@link AbstractThenSteps#unordered}, {@link AbstractThenSteps#never} etc
 */
public abstract class AbstractThenSteps<S extends Simulation, T extends AbstractThenSteps<S, ?>> {
    private final StepManager<S> stepManager;
    private final CheckStepExecutionType checkStepExecutionType;
    private final NotificationCache notificationCache;

    protected AbstractThenSteps(StepManager<S> stepManager, NotificationCache notificationCache, CheckStepExecutionType checkStepExecutionType) {
        this.stepManager = stepManager;
        this.notificationCache = notificationCache;
        this.checkStepExecutionType = checkStepExecutionType;
    }

    /**
     * @return an instance of the concrete sub-class of AbstractThenSteps where the steps it creates can occur in any
     * order.  They will not block execution of other steps, but must complete for the test to pass.
     *
     * See OcavaScenarioTest/README.md file for explanation of what notifications the created step will receive.
     *
     * @throws IllegalStateException if called after an incompatible modifier
     */
    public T unordered() {
        return create(stepManager, notificationCache, CheckStepExecutionType.unordered().merge(checkStepExecutionType));
    }

    /**
     * @return an instance of the concrete sub-class of AbstractThenSteps where the steps it creates can occur in any
     * order.  They will not block execution of other steps, but must complete for the test to pass.  The steps are
     * associated with the given name, which may be used to block and wait for them or remove them.  See {@link
     * UnorderedSteps}
     *
     * See OcavaScenarioTest/README.md file for explanation of what notifications the created step will receive.
     *
     * @throws IllegalStateException if called after an incompatible modifier
     */
    public T unordered(String name) {
        return create(stepManager, notificationCache, CheckStepExecutionType.unordered(name).merge(checkStepExecutionType));
    }

    /**
     * @return an instance of the concrete sub-class of AbstractThenSteps where the steps it creates must never occur.
     * They will not block execution of other steps.  If the steps are ever completed, the test will fail.
     *
     * See OcavaScenarioTest/README.md file for explanation of what notifications the created step will receive.
     *
     * @throws IllegalStateException if called after an incompatible modifier
     */
    public T never() {
        return create(stepManager, notificationCache, CheckStepExecutionType.never().merge(checkStepExecutionType));
    }

    /**
     * @return an instance of the concrete sub-class of AbstractThenSteps where the steps it creates must never occur.
     * They will not block execution of other steps.  The steps are associated with the given name, which may be used to
     * remove them if the test requires that they only hold for a portion of the scenario.  See {@link UnorderedSteps}
     *
     * See OcavaScenarioTest/README.md file for explanation of what notifications the created step will receive.
     *
     * @throws IllegalStateException if called after an incompatible modifier
     */
    public T never(String name) {
        return create(stepManager, notificationCache, CheckStepExecutionType.never(name).merge(checkStepExecutionType));
    }

    /**
     * @return an instance of the concrete sub-class of AbstractThenSteps where the steps it creates must complete
     * within the specified duration from the time this step is executed.
     *
     * See OcavaScenarioTest/README.md file for explanation of what notifications the created step will receive.
     *
     * @throws IllegalStateException if called after an incompatible modifier
     */
    public T within(Duration duration) {
        return within(duration.toNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * @return an instance of the concrete sub-class of AbstractThenSteps where the steps it creates must complete
     * within the specified duration from the time this step is executed.
     *
     * See OcavaScenarioTest/README.md file for explanation of what notifications the created step will receive.
     *
     * @throws IllegalStateException if called after an incompatible modifier
     */
    public T within(double magnitude, TimeUnit timeUnit) {
        double timeLimit = TimeThenSteps.convertToUnit(magnitude, timeUnit, stepManager.getTimeUnit());
        return create(stepManager, notificationCache,
                CheckStepExecutionType.within(stepManager.simulation::getEventScheduler, timeLimit).merge(checkStepExecutionType));
    }

    /**
     * @param timeLimit - the time limit specified in the simulation scheduler's time units
     * @return an instance of the concrete sub-class of AbstractThenSteps where the steps it creates must complete
     * within the specified duration from the time this step is executed.
     *
     * See OcavaScenarioTest/README.md file for explanation of what notifications the created step will receive.
     *
     * @throws IllegalStateException if called after an incompatible modifier
     */
    public T within(StepFuture<Double> timeLimit) {
        return create(stepManager, notificationCache,
                CheckStepExecutionType.within(stepManager.simulation::getEventScheduler, timeLimit).merge(checkStepExecutionType));
    }

    /**
     * @return an instance of the concrete sub-class of AbstractThenSteps where the steps it creates must complete
     * after exactly the specified duration from the time this step is executed.
     *
     * See OcavaScenarioTest/README.md file for explanation of what notifications the created step will receive.
     *
     * @throws IllegalStateException if called after an incompatible modifier
     */
    public T afterExactly(double magnitude, TimeUnit timeUnit) {
        double delay = TimeThenSteps.convertToUnit(magnitude, timeUnit, stepManager.getTimeUnit());
        return afterExactly(StepFuture.of(delay));
    }

    /**
     * @param delay - the delay specified in the simulation scheduler's time units
     * @return an instance of the concrete sub-class of AbstractThenSteps where the steps it creates must complete
     * after exactly the specified duration from the time this step is executed.
     *
     * See OcavaScenarioTest/README.md file for explanation of what notifications the created step will receive.
     *
     * @throws IllegalStateException if called after an incompatible modifier
     */
    public T afterExactly(StepFuture<Double> delay) {
        return create(stepManager, notificationCache,
                CheckStepExecutionType.afterExactly(stepManager.simulation::getEventScheduler, delay).merge(checkStepExecutionType));
    }

    /**
     * @return an instance of the concrete sub-class of AbstractThenSteps where the steps it creates must complete
     * after exactly the specified duration from the time this step is executed.
     *
     * See OcavaScenarioTest/README.md file for explanation of what notifications the created step will receive.
     *
     * @throws IllegalStateException if called after an incompatible modifier
     */
    public T afterExactly(Duration duration) {
        return afterExactly(duration.toNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * @return an instance of the concrete sub-class of AbstractThenSteps where the steps it creates must complete
     * after at least the specified duration from the time this step is executed.
     *
     * See OcavaScenarioTest/README.md file for explanation of what notifications the created step will receive.
     *
     * @throws IllegalStateException if called after an incompatible modifier
     */
    public T afterAtLeast(double magnitude, TimeUnit timeUnit) {
        double delay = TimeThenSteps.convertToUnit(magnitude, timeUnit, stepManager.getTimeUnit());
        return afterAtLeast(StepFuture.of(delay));
    }

    /**
     * @param delay - the delay specified in the simulation scheduler's time units
     * @return an instance of the concrete sub-class of AbstractThenSteps where the steps it creates must complete
     * after at least the specified duration from the time this step is executed.
     *
     * See OcavaScenarioTest/README.md file for explanation of what notifications the created step will receive.
     *
     * @throws IllegalStateException if called after an incompatible modifier
     */
    public T afterAtLeast(StepFuture<Double> delay) {
        return create(stepManager, notificationCache,
                CheckStepExecutionType.afterAtLeast(stepManager.simulation::getEventScheduler, delay).merge(checkStepExecutionType));
    }

    /**
     * @return an instance of the concrete sub-class of AbstractThenSteps where the steps it creates must complete
     * after at least the specified duration from the time this step is executed.
     *
     * See OcavaScenarioTest/README.md file for explanation of what notifications the created step will receive.
     *
     * @throws IllegalStateException if called after an incompatible modifier
     */
    public T afterAtLeast(Duration duration) {
        return afterAtLeast(duration.toNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * @return an instance of the concrete sub-class of AbstractThenSteps where the steps it creates has the
     * {@code CheckStepExecutionType.isFailingStep} flag set to true. The failingStep flag is checked after the scenario test has completed
     * successfully or exceptionally and should be used in conjunction with {@link FixRequired}
     *
     * @throws IllegalStateException if called after a previous invocation of this method
     */
    public T failingStep() {
        return create(stepManager, notificationCache, checkStepExecutionType.merge(CheckStepExecutionType.failing()));
    }

    protected abstract T create(StepManager<S> stepManager, NotificationCache notificationCache, CheckStepExecutionType executionType);

    protected <N extends Notification> void addCheckStep(Class<N> notificationType, Predicate<N> predicate) {
        addCheckStep(new CheckStep<>(notificationType, notificationCache, predicate));
    }

    <N extends Notification> void addCheckStep(CheckStep<N> checkStep) {
        stepManager.add(checkStep, checkStepExecutionType);
    }

    protected void addExecuteStep(Runnable runnable) {
        Preconditions.checkState(checkStepExecutionType.isBasicOrderedStep(),
                "Execute steps must be basic ordered steps.  Remove any modification method calls other than failingStep from this line.");

        stepManager.add(new SimpleExecuteStep(runnable), checkStepExecutionType.isFailingStep());
    }

    public void notificationsReceived(ImmutableSet<Class<? extends Notification>> notifications) {
        notifications.forEach(this::notificationReceived);
    }

    public void notificationReceived(Class<? extends Notification> notificationClass) {
        addCheckStep(notificationClass, n -> true);
    }

    protected S getSimulation() {
        return stepManager.getSimulation();
    }
}
