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
    enum OrderedModifier {
        WITHIN,
        AFTER_EXACTLY,
        AFTER_AT_LEAST
    }

    enum UnorderedModifier {
        UNORDERED,
        NEVER
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
    /**
     * Optional modifier which changes the behaviour of the steps.
     */
    @CheckForNull
    private final OrderedModifier orderedModifier;
    /**
     * Optional modifier which forces the steps to behave in an unordered manner.
     */
    @CheckForNull
    private final UnorderedModifier unorderedModifier;
    /**
     * Used to mark this check step as expecting to fail
     */
    private final boolean isFailingStep;
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
     * Default constructor creates an ORDERED CheckStepExecutionType with the next id.
     * Can be used for dependency injection.
     */
    public CheckStepExecutionType() {
        this(null, null, null, false, null, null);
    }

    private CheckStepExecutionType(@CheckForNull String name, UnorderedModifier modifier) {
        this(name, null, modifier, false, null, null);
    }

    private CheckStepExecutionType(OrderedModifier modifier, Supplier<EventScheduler> schedulerSupplier, StepFuture<Double> duration) {
        this(null, modifier, null, false, schedulerSupplier, duration);
    }

    private CheckStepExecutionType(
            @CheckForNull String name,
            @CheckForNull OrderedModifier orderedModifier,
            @CheckForNull UnorderedModifier unorderedModifier,
            boolean isFailingStep,
            @CheckForNull Supplier<EventScheduler> schedulerSupplier,
            @CheckForNull StepFuture<Double> duration) {
        this.name = name;
        this.orderedModifier = orderedModifier;
        this.unorderedModifier = unorderedModifier;
        this.isFailingStep = isFailingStep;
        this.schedulerSupplier = schedulerSupplier;
        this.duration = duration;
    }

    //region Factory methods
    public static CheckStepExecutionType ordered() {
        return new CheckStepExecutionType();
    }

    public static CheckStepExecutionType unordered(@CheckForNull String name) {
        return new CheckStepExecutionType(name, UnorderedModifier.UNORDERED);
    }

    public static CheckStepExecutionType unordered() {
        return unordered(null);
    }

    public static CheckStepExecutionType never(@CheckForNull String name) {
        return new CheckStepExecutionType(name, UnorderedModifier.NEVER);
    }

    public static CheckStepExecutionType never() {
        return never(null);
    }

    public static CheckStepExecutionType within(Supplier<EventScheduler> schedulerSupplier, double duration) {
        return within(schedulerSupplier, StepFuture.of(duration));
    }

    public static CheckStepExecutionType within(Supplier<EventScheduler> schedulerSupplier, StepFuture<Double> duration) {
        return new CheckStepExecutionType(OrderedModifier.WITHIN, schedulerSupplier, duration);
    }

    public static CheckStepExecutionType afterExactly(Supplier<EventScheduler> schedulerSupplier, double duration) {
        return afterExactly(schedulerSupplier, StepFuture.of(duration));
    }

    public static CheckStepExecutionType afterExactly(Supplier<EventScheduler> schedulerSupplier, StepFuture<Double> duration) {
        return new CheckStepExecutionType(OrderedModifier.AFTER_EXACTLY, schedulerSupplier, duration);
    }

    public static CheckStepExecutionType afterAtLeast(Supplier<EventScheduler> schedulerSupplier, double duration) {
        return afterAtLeast(schedulerSupplier, StepFuture.of(duration));
    }

    public static CheckStepExecutionType afterAtLeast(Supplier<EventScheduler> schedulerSupplier, StepFuture<Double> duration) {
        return new CheckStepExecutionType(OrderedModifier.AFTER_AT_LEAST, schedulerSupplier, duration);
    }

    /**
     * Create an ordered, failing execution type instance
     */
    public static CheckStepExecutionType failing() {
        return new CheckStepExecutionType(null, null, null, true, null, null);
    }
    //endregion

    /**
     * @return Either the user-defined name for this group of steps, or an auto-generated numerical id.
     */
    public String getName() {
        return name != null ? name : id;
    }

    public Supplier<EventScheduler> getSchedulerSupplier() {
        return Preconditions.checkNotNull(schedulerSupplier, "SchedulerSupplier not provided for a %s step.", orderedModifier);
    }

    private StepFuture<Double> getDuration() {
        Preconditions.checkNotNull(duration, "Duration not provided for a %s step", orderedModifier);
        return duration;
    }

    public boolean isOrdered() {
        return unorderedModifier == null;
    }

    public boolean isFailingStep() {
        return isFailingStep;
    }

    public boolean isBasicOrderedStep() {
        return orderedModifier == null && unorderedModifier == null;
    }

    CheckStep<?> createOrderedStep(CheckStep<?> baseStep) {
        if (orderedModifier == null) {
            return baseStep;
        }

        switch (orderedModifier) {
            case WITHIN:
                return new WithinStep<>(baseStep, getSchedulerSupplier(), getDuration());
            case AFTER_EXACTLY:
                return new AfterExactlyStep<>(baseStep, getSchedulerSupplier(), getDuration());
            case AFTER_AT_LEAST:
                return new AfterAtLeastStep<>(baseStep, getSchedulerSupplier(), getDuration());
            default:
                throw Failer.fail("Unsupported ordered check step modifier %s", orderedModifier);
        }
    }

    UnorderedCheckStep<?> createUnorderedStep(CheckStep<?> baseStep) {
        CheckStep<?> orderedStep = createOrderedStep(baseStep);
        switch (Preconditions.checkNotNull(unorderedModifier, "Attempted to create an unordered step from an ordered execution type instance")) {
            case UNORDERED:
                return new UnorderedCheckStep<>(orderedStep);
            case NEVER:
                return new NeverStep<>(orderedStep);
            default:
                throw Failer.fail("Unsupported unordered check step modifier %s", unorderedModifier);
        }
    }

    /**
     * Combines two instances of CheckStepExecutionType, if they have compatible modifiers. This currently means that
     *
     * - An unordered modifier can be combined with any one of afterAtLeast, afterExactly or within
     * - A "failing step" modifier can be combined with any other non-failing modifier
     *
     * @throws IllegalStateException if the instances are incompatible due to having incompatible modifiers.
     * @throws IllegalStateException if the instances have inconsistent data defined such that merging them would result
     *          in data loss. This is not expected to happen, as the checks for incompatible modifiers should catch any
     *          such case.
     */
    public CheckStepExecutionType merge(CheckStepExecutionType other) {
        Preconditions.checkState(this.orderedModifier == null || other.orderedModifier == null,
                "Cannot merge a CheckStepExecutionType %s with %s", this.orderedModifier, other.orderedModifier);
        Preconditions.checkState(this.unorderedModifier == null || other.unorderedModifier == null,
                "Cannot merge a CheckStepExecutionType %s with %s", this.unorderedModifier, other.unorderedModifier);

        OrderedModifier mergedOrderedModifier = this.orderedModifier == null ? other.orderedModifier : this.orderedModifier;
        UnorderedModifier mergedUnorderedModifier = this.unorderedModifier == null ? other.unorderedModifier : this.unorderedModifier;
        Preconditions.checkState(areCompatible(mergedOrderedModifier, mergedUnorderedModifier),
                "Cannot merge a CheckStepExecutionType %s with %s", mergedOrderedModifier, mergedUnorderedModifier);

        Preconditions.checkState(!this.isFailingStep || !other.isFailingStep,
                "Cannot merge two failing CheckStepExecutionType instances");

        Preconditions.checkState(this.name == null || other.name == null,
                "Cannot merge two CheckStepExecutionType instances with defined names");
        Preconditions.checkState(this.duration == null || other.duration == null,
                "Cannot merge two CheckStepExecutionType instances with defined durations");

        return new CheckStepExecutionType(
                this.name == null ? other.name : this.name,
                mergedOrderedModifier,
                mergedUnorderedModifier,
                this.isFailingStep || other.isFailingStep,
                this.schedulerSupplier != null ? this.schedulerSupplier : other.schedulerSupplier,
                this.duration != null ? this.duration : other.duration);
    }

    private boolean areCompatible(@CheckForNull OrderedModifier orderedModifier, @CheckForNull UnorderedModifier unorderedModifier) {
        return orderedModifier == null
                || unorderedModifier == null
                || UnorderedModifier.UNORDERED.equals(unorderedModifier);
    }

    private static String nextId() {
        return StringIdGenerator.getId(CheckStepExecutionType.class).id;
    }

    @VisibleForTesting
    @CheckForNull
    OrderedModifier getOrderedModifierForTesting() {
        return orderedModifier;
    }

    @VisibleForTesting
    @CheckForNull
    UnorderedModifier getUnorderedModifierForTesting() {
        return unorderedModifier;
    }
}
