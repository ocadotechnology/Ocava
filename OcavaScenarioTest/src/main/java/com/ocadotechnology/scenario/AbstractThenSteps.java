/*
 * Copyright © 2017-2020 Ocado (Ocava)
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
import com.ocadotechnology.scenario.StepManager.CheckStepExecutionType;
import com.ocadotechnology.scenario.StepManager.CheckStepExecutionType.Type;
import com.ocadotechnology.simulation.Simulation;

/**
 * An abstract class which should be extended by each distinct set of then conditions that need to be implemented as
 * part of the testing package.  Each implementation should be generic on itself so that it can be correctly modified by
 * the {@link AbstractThenSteps#unordered}, {@link AbstractThenSteps#never} and {@link AbstractThenSteps#within} methods
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
     */
    public T unordered() {
        return create(stepManager, notificationCache, CheckStepExecutionType.unordered().applyModifiersFrom(checkStepExecutionType));
    }

    /**
     * @return an instance of the concrete sub-class of AbstractThenSteps where the steps it creates can occur in any
     * order.  They will not block execution of other steps, but must complete for the test to pass.  The steps are
     * associated with the given name, which may be used to block and wait for them or remove them.  See {@link
     * UnorderedSteps}
     *
     * See OcavaScenarioTest/README.md file for explanation of what notifications the created step will receive.
     */
    public T unordered(String name) {
        return create(stepManager, notificationCache, CheckStepExecutionType.unordered(name).applyModifiersFrom(checkStepExecutionType));
    }

    /**
     * @return an instance of the concrete sub-class of AbstractThenSteps where the steps it creates must never occur.
     * They will not block execution of other steps.  If the steps are ever completed, the test will fail.
     *
     * See OcavaScenarioTest/README.md file for explanation of what notifications the created step will receive.
     */
    public T never() {
        return create(stepManager, notificationCache, CheckStepExecutionType.never().applyModifiersFrom(checkStepExecutionType));
    }

    /**
     * @return an instance of the concrete sub-class of AbstractThenSteps where the steps it creates must never occur.
     * They will not block execution of other steps.  The steps are associated with the given name, which may be used to
     * remove them if the test requires that they only hold for a portion of the scenario.  See {@link UnorderedSteps}
     *
     * See OcavaScenarioTest/README.md file for explanation of what notifications the created step will receive.
     */
    public T never(String name) {
        return create(stepManager, notificationCache, CheckStepExecutionType.never(name).applyModifiersFrom(checkStepExecutionType));
    }

    /**
     * @return an instance of the concrete sub-class of AbstractThenSteps where the steps it creates must complete
     * within the specified duration from the time this step is executed.
     *
     * See OcavaScenarioTest/README.md file for explanation of what notifications the created step will receive.
     */
    public T within(Duration duration) {
        return within(duration.toNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * @return an instance of the concrete sub-class of AbstractThenSteps where the steps it creates must complete
     * within the specified duration from the time this step is executed.
     *
     * See OcavaScenarioTest/README.md file for explanation of what notifications the created step will receive.
     */
    public T within(double magnitude, TimeUnit timeUnit) {
        double timeLimit = TimeThenSteps.convertToUnit(magnitude, timeUnit, stepManager.getTimeUnit());
        return create(stepManager, notificationCache,
                CheckStepExecutionType.within(stepManager.simulation::getEventScheduler, timeLimit).applyModifiersFrom(checkStepExecutionType));
    }

    /**
     * @return an instance of the concrete sub-class of AbstractThenSteps where the steps it creates has the
     * {@code CheckStepExecutionType.isFailingStep} flag set to true. The failingStep flag is checked after the scenario test has completed
     * successfully or exceptionally and should be used in conjunction with {@link FixRequired}
     */
    public T failingStep() {
        return create(stepManager, notificationCache, checkStepExecutionType.markFailingStep());
    }

    protected abstract T create(StepManager<S> stepManager, NotificationCache notificationCache, CheckStepExecutionType executionType);

    protected <N extends Notification> void addCheckStep(Class<N> notificationType, Predicate<N> predicate) {
        addCheckStep(new CheckStep<>(notificationType, notificationCache, predicate));
    }

    protected <N extends Notification> void addCheckStep(CheckStep<N> checkStep) {
        if (checkStepExecutionType.isFailingStep()) {
            stepManager.getStepsCache().addFailingStep(checkStep);
            stepManager.add(checkStep, checkStepExecutionType.markFailingStep());
        } else {
            stepManager.add(checkStep, checkStepExecutionType);
        }
    }

    protected void addExecuteStep(Runnable runnable) {
        Preconditions.checkState(checkStepExecutionType.getType() == Type.ORDERED, "Execute steps must be ORDERED.  Remove any within, unordered or never modification method calls from this line.");

        ExecuteStep step = new SimpleExecuteStep(runnable);
        if (checkStepExecutionType.isFailingStep()) {
            stepManager.getStepsCache().addFailingStep(step);
        }
        stepManager.add(step);
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
