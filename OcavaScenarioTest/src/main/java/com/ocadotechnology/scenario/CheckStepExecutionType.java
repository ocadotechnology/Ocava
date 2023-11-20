/*
 * Copyright © 2017-2023 Ocado (Ocava)
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

import java.util.function.Supplier;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.id.StringIdGenerator;
import com.ocadotechnology.validation.Failer;
/**
 * Defines the execution mode for one or more associated check steps. This is used with the AbstractThenSteps modifier
 * methods within, afterExactly, etc.
 */
@ParametersAreNonnullByDefault
public class CheckStepExecutionType {
    enum Type {
        ORDERED(true),
        UNORDERED(false),
        NEVER(false),
        WITHIN(true),
        AFTER_EXACTLY(true),
        AFTER_AT_LEAST(true);

        private final boolean isOrdered;

        Type(boolean isOrdered) {
            this.isOrdered = isOrdered;
        }
    }

    /**
     * Internal id to be used if no name is provided for the steps.
     */
    private final String id = nextId();
    /**
     * Optional name for use with the {@link UnorderedSteps}
     */
    @CheckForNull
    private final String name;
    private final Type type;
    /**
     * Optional {@link EventScheduler} supplier, used with the time-constrained step modifiers.
     */
    @CheckForNull
    private final Supplier<EventScheduler> schedulerSupplier;
    /**
     * Optional duration supplier, used with the time-constrained step modifiers.
     */
    @CheckForNull
    private final StepFuture<Double> duration;

    /**
     * Used to mark this check step as expecting to fail
     */
    private final boolean isFailingStep;

    /**
     * Default constructor creates an ORDERED CheckStepExecutionType with the next id.
     * Can be used for dependency injection.
     */
    public CheckStepExecutionType() {
        this(null, Type.ORDERED);
    }

    //Internal utility to make the factory methods nicer.
    private CheckStepExecutionType(@CheckForNull String name, Type type) {
        this(name, type, null, null, false);
    }

    CheckStepExecutionType(Type type, @CheckForNull Supplier<EventScheduler> schedulerSupplier, Double duration) {
        this(null, type, schedulerSupplier, StepFuture.of(duration), false);
    }

    CheckStepExecutionType(Type type, @CheckForNull Supplier<EventScheduler> schedulerSupplier, StepFuture<Double> duration) {
        this(null, type, schedulerSupplier, duration, false);
    }

    private CheckStepExecutionType(
            @CheckForNull String name,
            Type type,
            @CheckForNull Supplier<EventScheduler> schedulerSupplier,
            @CheckForNull StepFuture<Double> duration,
            boolean failingStep) {
        this.name = name;
        this.type = type;
        this.schedulerSupplier = schedulerSupplier;
        this.duration = duration;
        this.isFailingStep = failingStep;
    }

    //region Factory methods
    public static CheckStepExecutionType ordered() {
        return new CheckStepExecutionType(null, Type.ORDERED);
    }

    public static CheckStepExecutionType unordered(String name) {
        return new CheckStepExecutionType(name, Type.UNORDERED);
    }

    public static CheckStepExecutionType unordered() {
        return new CheckStepExecutionType(null, Type.UNORDERED);
    }

    public static CheckStepExecutionType never(String name) {
        return new CheckStepExecutionType(name, Type.NEVER);
    }

    public static CheckStepExecutionType never() {
        return new CheckStepExecutionType(null, Type.NEVER);
    }

    public static CheckStepExecutionType within(Supplier<EventScheduler> schedulerSupplier, double duration) {
        return new CheckStepExecutionType(Type.WITHIN, schedulerSupplier, duration);
    }

    public static CheckStepExecutionType within(Supplier<EventScheduler> schedulerSupplier, StepFuture<Double> duration) {
        return new CheckStepExecutionType(Type.WITHIN, schedulerSupplier, duration);
    }

    public static CheckStepExecutionType afterExactly(Supplier<EventScheduler> schedulerSupplier, StepFuture<Double> duration) {
        return new CheckStepExecutionType(Type.AFTER_EXACTLY, schedulerSupplier, duration);
    }

    public static CheckStepExecutionType afterExactly(Supplier<EventScheduler> schedulerSupplier, double duration) {
        return new CheckStepExecutionType(Type.AFTER_EXACTLY, schedulerSupplier, duration);
    }

    public static CheckStepExecutionType afterAtLeast(Supplier<EventScheduler> schedulerSupplier, StepFuture<Double> duration) {
        return new CheckStepExecutionType(Type.AFTER_AT_LEAST, schedulerSupplier, duration);
    }

    public static CheckStepExecutionType afterAtLeast(Supplier<EventScheduler> schedulerSupplier, double duration) {
        return new CheckStepExecutionType(Type.AFTER_AT_LEAST, schedulerSupplier, duration);
    }

    /**
     * Create an ordered, failing execution type instance
     */
    public static CheckStepExecutionType failing() {
        return new CheckStepExecutionType(null, Type.ORDERED, null, null, true);
    }
    //endregion

    /**
     * @return Either the user-defined name for this group of steps, or an auto-generated numerical id.
     */
    public String getName() {
        return name != null ? name : id;
    }

    public Supplier<EventScheduler> getSchedulerSupplier() {
        return Preconditions.checkNotNull(schedulerSupplier, "SchedulerSupplier not provided for a %s step.", type);
    }

    private StepFuture<Double> getDuration() {
        Preconditions.checkNotNull(duration, "Duration not provided for a %s step", type);
        return duration;
    }

    public boolean isOrdered() {
        return type.isOrdered;
    }

    public boolean isFailingStep() {
        return isFailingStep;
    }

    public boolean isBasicOrderedStep() {
        return type == Type.ORDERED;
    }

    CheckStep<?> createOrderedStep(CheckStep<?> baseStep) {
        switch (type) {
            case ORDERED:
                return baseStep;
            case WITHIN:
                return new WithinStep<>(baseStep, getSchedulerSupplier(), getDuration());
            case AFTER_EXACTLY:
                return new AfterExactlyStep<>(baseStep, getSchedulerSupplier(), getDuration());
            case AFTER_AT_LEAST:
                return new AfterAtLeastStep<>(baseStep, getSchedulerSupplier(), getDuration());
            default:
                throw Failer.fail("Unsupported ordered check step type %s", type);
        }
    }

    UnorderedCheckStep<?> createUnorderedStep(CheckStep<?> orderedStep) {
        switch (type) {
            case UNORDERED:
                return new UnorderedCheckStep<>(orderedStep, true);
            case NEVER:
                return new NeverStep<>(orderedStep);
            default:
                throw Failer.fail("Unsupported unordered check step type %s", type);
        }
    }

    /**
     * Combines two instances of CheckStepExecutionType, if possible.
     *
     * @throws IllegalStateException if the instances are incompatible due to having incompatible types or independently
     *          defined fields
     */
    public CheckStepExecutionType merge(CheckStepExecutionType other) {
        Preconditions.checkState(Type.ORDERED.equals(this.type) || Type.ORDERED.equals(other.type),
                "Cannot merge a CheckStepExecutionType %s with %s", this.type, other.type);
        Preconditions.checkState(!this.isFailingStep || !other.isFailingStep,
                "Cannot merge two failing CheckStepExecutionType instances");
        Preconditions.checkState(this.name == null || other.name == null,
                "Cannot merge two CheckStepExecutionType instances with defined names");
        Preconditions.checkState(this.duration == null || other.duration == null,
                "Cannot merge two CheckStepExecutionType instances with defined durations");

        return new CheckStepExecutionType(
                this.name == null ? other.name : this.name,
                Type.ORDERED.equals(this.type) ? other.type : this.type,
                this.schedulerSupplier != null ? this.schedulerSupplier : other.schedulerSupplier,
                this.duration != null ? this.duration : other.duration,
                other.isFailingStep || this.isFailingStep);
    }

    private static String nextId() {
        return StringIdGenerator.getId(CheckStepExecutionType.class).id;
    }

    @VisibleForTesting
    Type getTypeForTesting() {
        return type;
    }
}
