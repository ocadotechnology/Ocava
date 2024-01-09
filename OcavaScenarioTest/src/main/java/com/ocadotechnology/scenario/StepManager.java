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

import static com.ocadotechnology.scenario.StepManager.ExecuteStepExecutionType.Type.PERIODIC;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.id.StringIdGenerator;
import com.ocadotechnology.simulation.Simulation;
import com.ocadotechnology.validation.Failer;

public class StepManager<S extends Simulation> {
    private final StepCache stepsCache;
    protected final ScenarioSimulationApi<S> simulation;
    protected final NotificationCache notificationCache;
    protected final ScenarioNotificationListener listener;

    public StepManager(StepCache stepsCache, ScenarioSimulationApi<S> simulation, NotificationCache notificationCache, ScenarioNotificationListener listener) {
        this.stepsCache = stepsCache;
        this.simulation = simulation;
        this.notificationCache = notificationCache;
        this.listener = listener;
    }

    public StepCache getStepsCache() {
        return stepsCache;
    }

    public S getSimulation() {
        return simulation.getSimulation();
    }

    public TimeUnit getTimeUnit() {
        return simulation.getSchedulerTimeUnit();
    }

    public void add(CheckStep<?> checkStep, CheckStepExecutionType checkStepExecutionType) {
        if (checkStepExecutionType.isOrdered()) {
            addOrderedCheckStep(checkStepExecutionType, checkStep);
        } else {
            addUnorderedStepOnExecutionOfStep(checkStepExecutionType, checkStep);
        }
    }

    private void addOrderedCheckStep(CheckStepExecutionType checkStepExecutionType, CheckStep<?> baseStep) {
        CheckStep<?> checkStep = checkStepExecutionType.createOrderedStep(baseStep);
        checkStep.setStepName(LoggerUtil.getStepName());
        checkStep.setStepOrder(stepsCache.getNextStepCounter());
        notificationCache.addKnownNotification(checkStep.getType());
        stepsCache.addOrdered(checkStep);

        if (checkStepExecutionType.isFailingStep()) {
            stepsCache.addFailingStep(checkStep);
        }
    }

    public void add(String name, ValidationStep<?> step) {
        step.setStepName(LoggerUtil.getStepName());
        step.setStepOrder(stepsCache.getNextStepCounter());
        step.setName(name);
        notificationCache.addKnownNotification(step.getType());
        stepsCache.addUnordered(name, step);
    }

    public void add(BroadcastStep broadcastStep) {
        broadcastStep.setStepOrder(stepsCache.getNextStepCounter());
        broadcastStep.setStepName(LoggerUtil.getStepName());
        stepsCache.addOrdered(broadcastStep);
    }

    public void add(ExceptionCheckStep exceptionCheckStep) {
        exceptionCheckStep.setStepOrder(stepsCache.getNextStepCounter());
        exceptionCheckStep.setStepName(LoggerUtil.getStepName());
        stepsCache.addCheckStep(exceptionCheckStep);
    }

    public void add(ExecuteStep executeStep) {
        add(executeStep, false);
    }

    public void add(ExecuteStep executeStep, boolean isFailingStep) {
        executeStep.setStepOrder(stepsCache.getNextStepCounter());
        executeStep.setStepName(LoggerUtil.getStepName());
        stepsCache.addOrdered(executeStep);
        if (isFailingStep) {
            stepsCache.addFailingStep(executeStep);
        }
    }

    public void addExecuteStep(Runnable r) {
        add(new SimpleExecuteStep(r));
    }

    public void add(PeriodicExecuteStep periodicExecuteStep) {
        periodicExecuteStep.setStepOrder(stepsCache.getNextStepCounter());
        periodicExecuteStep.setStepName(LoggerUtil.getStepName());
        stepsCache.addOrdered(periodicExecuteStep);
    }

    public void add(ExecuteStep executeStep, ExecuteStepExecutionType executeStepExecutionType) {
        if (executeStepExecutionType.type == ExecuteStepExecutionType.Type.ORDERED) {
            add(executeStep);
        } else if (executeStepExecutionType.type == PERIODIC) {
            add(new PeriodicExecuteStep(executeStep, executeStepExecutionType.getScheduler(), executeStepExecutionType.getPeriod()));
        } else {
            throw Failer.fail("Unhandled Execute step type");
        }
    }

    public void addFinal(ExecuteStep executeStep) {
        executeStep.setStepOrder(stepsCache.getNextStepCounter());
        executeStep.setStepName(LoggerUtil.getStepName());
        stepsCache.addFinalStep(executeStep);
    }

    public void add(String name, ExecuteStep executeStep) {
        executeStep.setStepOrder(stepsCache.getNextStepCounter());
        executeStep.setStepName(LoggerUtil.getStepName());
        executeStep.setName(name);
        stepsCache.addUnordered(name, executeStep);
    }

    public void add(WaitStep scheduledStep) {
        scheduledStep.setStepOrder(stepsCache.getNextStepCounter());
        scheduledStep.setStepName(LoggerUtil.getStepName());
        stepsCache.addOrdered(scheduledStep);
    }

    public void add(ScheduledStep scheduledStep) {
        scheduledStep.setStepOrder(stepsCache.getNextStepCounter());
        scheduledStep.setStepName(LoggerUtil.getStepName());
        stepsCache.addOrdered(scheduledStep);
    }

    private void addUnorderedStepOnExecutionOfStep(CheckStepExecutionType checkStepExecutionType, CheckStep<?> testStep) {
        notificationCache.addKnownNotification(testStep.getType());

        add(new ExecuteStep() {
            @Override
            protected void executeStep() {
                UnorderedCheckStep<?> step = checkStepExecutionType.createUnorderedStep(testStep);
                if (checkStepExecutionType.isFailingStep()) {
                    // Remove the original step added and replace with the new wrapped step
                    stepsCache.removeFailingStep(testStep);
                    stepsCache.addFailingStep(step);
                }

                notificationCache.resetUnorderedNotification();
                addUnordered(checkStepExecutionType.getName(), step, getStepName(), getStepOrder());
            }
        });
    }

    private void addUnordered(String name, UnorderedCheckStep<?> testStep, String stepName, int stepOrder) {
        testStep.setStepOrder(stepOrder);
        testStep.setStepName(stepName);
        testStep.setName(name);
        notificationCache.addKnownNotification(testStep.getType());
        stepsCache.addUnordered(name, testStep);
    }

    public static class ExecuteStepExecutionType {
        enum Type {ORDERED, PERIODIC}

        private final Optional<Supplier<EventScheduler>> schedulerSupplier;
        private final OptionalDouble period;
        private final String name;
        private final Type type;

        private ExecuteStepExecutionType(String name, Type type) {
            this.name = name;
            this.type = type;
            this.schedulerSupplier = Optional.empty();
            this.period = OptionalDouble.empty();
        }

        private ExecuteStepExecutionType(String name, Type type, Optional<Supplier<EventScheduler>> schedulerSupplier, OptionalDouble period) {
            this.name = name;
            this.type = type;
            this.schedulerSupplier = schedulerSupplier;
            this.period = period;
        }

        public EventScheduler getScheduler() {
            return schedulerSupplier.orElseThrow(() -> Failer.fail("Scheduler was not set")).get();
        }

        public double getPeriod() {
            return period.orElseThrow(() -> Failer.fail("Period not provided"));
        }

        public static ExecuteStepExecutionType ordered() {
            return new ExecuteStepExecutionType(nextId(), Type.ORDERED);
        }

        public static ExecuteStepExecutionType periodic(String name, Supplier<EventScheduler> schedulerSupplier, double period) {
            return new ExecuteStepExecutionType(name, PERIODIC, Optional.of(schedulerSupplier), OptionalDouble.of(period));
        }

        public static ExecuteStepExecutionType periodic(Supplier<EventScheduler> schedulerSupplier, double period) {
            return periodic(nextId(), schedulerSupplier, period);
        }

        private static String nextId() {
            return StringIdGenerator.getId(ExecuteStepExecutionType.class).id;
        }
    }
}
