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

import static com.ocadotechnology.scenario.StepManager.ExecuteStepExecutionType.Type.PERIODIC;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;
import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.id.StringIdGenerator;
import com.ocadotechnology.validation.Failer;

public class StepManager {
    protected StepCache stepsCache;
    protected ScenarioSimulationApi simulation;
    public NotificationCache notificationCache;
    protected ScenarioNotificationListener listener;

    public StepManager(StepCache stepsCache, ScenarioSimulationApi simulation, NotificationCache notificationCache, ScenarioNotificationListener listener) {
        this.stepsCache = stepsCache;
        this.simulation = simulation;
        this.notificationCache = notificationCache;
        this.listener = listener;
    }

    public StepCache getStepsCache() { return stepsCache; }

    public TimeUnit getTimeUnit() {
        return simulation.getSchedulerTimeUnit();
    }

    public void add(CheckStep<?> checkStep, CheckStepExecutionType checkStepExecutionType) {
        if (checkStepExecutionType.type == CheckStepExecutionType.Type.ORDERED) {
            addOrderedCheckStep(checkStep);
        } else {
            addUnorderedStepOnExecutionOfStep(checkStepExecutionType, checkStep);
        }
    }

    private void addOrderedCheckStep(CheckStep<?> checkStep) {
        checkStep.setStepName(LoggerUtil.getStepName());
        checkStep.setStepOrder(stepsCache.getNextStepCounter());
        notificationCache.addKnownNotification(checkStep.getType());
        stepsCache.addOrdered(checkStep);
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
        executeStep.setStepOrder(stepsCache.getNextStepCounter());
        executeStep.setStepName(LoggerUtil.getStepName());
        stepsCache.addOrdered(executeStep);
    }

    public void addExecuteStep(Runnable r) {
        add(new ExecuteStep() {
            @Override
            protected void executeStep() {
                r.run();
            }
        });
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
                CheckStep<?> step = createStepToAddLater(checkStepExecutionType, testStep);
                if (checkStepExecutionType.isFailingStep) {
                    // Remove the original step added and replace with the new wrapped step
                    stepsCache.removeFailingStep(testStep);
                    stepsCache.addFailingStep(step);
                }

                notificationCache.resetUnorderedNotification();
                addUnordered(checkStepExecutionType.name, step, getStepName(), getStepOrder());
            }
        });
    }

    private CheckStep<?> createStepToAddLater(CheckStepExecutionType checkStepExecutionType, CheckStep<?> testStep) {
        switch (checkStepExecutionType.type) {
            case UNORDERED:
                return new UnorderedCheckStep<>(testStep, true);
            case NEVER:
                return new NeverStep<>(testStep);
            case WITHIN:
                return new WithinStep<>(testStep, checkStepExecutionType.getScheduler(), checkStepExecutionType.getDurationInMillis());
            default:
                throw Failer.fail("Unhandled Check step type %s", checkStepExecutionType.type);
        }
    }

    private void addUnordered(String name, CheckStep<?> testStep, String stepName, int stepOrder) {
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

    public static class CheckStepExecutionType {
        enum Type {ORDERED, UNORDERED, NEVER, WITHIN}

        private final String name;
        private final Type type;
        private final Supplier<EventScheduler> schedulerSupplier;
        private final Long duration;

        /** Used to mark this check step as expecting to fail */
        private boolean isFailingStep;

        /**
         * Default constructor creates an ORDERED CheckStepExecutionType with the next id.
         * Can be use for dependency injection.
         */
        public CheckStepExecutionType() {
            this(nextId(), Type.ORDERED);
        }

        private CheckStepExecutionType(String name, Type type) {
            this(name, type, null, null);
        }

        /**
         * The main constructor for creating a CheckStepExecutionType
         */
        public CheckStepExecutionType(String name, Type type, Supplier<EventScheduler> schedulerSupplier, Long duration) {
            this(name, type, schedulerSupplier, duration, false);
        }

        /**
         * Construct a CheckStepExecutionType, the failingStep argument is used to allow marking a step as expected failure,
         * this is normally only used by {@link #markFailingStep()}, other cases for constructing
         * this class should make use of {@link #CheckStepExecutionType(String, Type, Supplier, Long)}
         */
        private CheckStepExecutionType(String name, Type type, Supplier<EventScheduler> schedulerSupplier, Long duration, boolean failingStep) {
            this.name = name;
            this.type = type;
            this.schedulerSupplier = schedulerSupplier;
            this.duration = duration;
            this.isFailingStep = failingStep;
        }

        public EventScheduler getScheduler() {
            return Preconditions.checkNotNull(schedulerSupplier, "SchedulerSupplier not provided.").get();
        }

        public long getDurationInMillis() {
            return Preconditions.checkNotNull(duration, "Duration not provided");
        }

        public Type getType(){
            return type;
        }

        public static CheckStepExecutionType ordered() {
            return new CheckStepExecutionType(nextId(), Type.ORDERED);
        }

        public static CheckStepExecutionType unordered(String name) {
            return new CheckStepExecutionType(name, Type.UNORDERED);
        }

        public static CheckStepExecutionType unordered() {
            return unordered(nextId());
        }

        public static CheckStepExecutionType never(String name) {
            return new CheckStepExecutionType(name, Type.NEVER);
        }

        public static CheckStepExecutionType never() {
            return never(nextId());
        }

        public static CheckStepExecutionType within(Supplier<EventScheduler> schedulerSupplier, long duration) {
            return new CheckStepExecutionType(nextId(), Type.WITHIN, schedulerSupplier, duration);
        }

        /**
         * Mark this check step as expected to fail, this part of the FixStep and FixRequired functionality
         */
        public CheckStepExecutionType markFailingStep() {
            return new CheckStepExecutionType(name, type, schedulerSupplier, duration, true);
        }

        private static String nextId() {
            return StringIdGenerator.getId(CheckStepExecutionType.class).id;
        }
    }
}
