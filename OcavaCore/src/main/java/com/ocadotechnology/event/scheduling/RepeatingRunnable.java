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
package com.ocadotechnology.event.scheduling;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.ocadotechnology.time.TimeConverter;

/**
 * This class is used to set up an action on a scheduler that will repeat with the specified frequency
 */
public class RepeatingRunnable implements Runnable {
    private final double time;
    private final double period;
    private final String description;
    private final Consumer<Double> timeConsumingAction;
    protected final EventScheduler eventScheduler;
    private final boolean isDaemon;
    private final AtomicBoolean canceled;
    private final boolean fixedDelay;

    /**
     * Perform the provided action in {@code delay} time from now, then repeat the action every {@code period} time.
     * The time unit of {@code delay} and {@code period} is that of the provided {@link EventScheduler}.
     *
     * @param delay the time before performing the provided action for the first time, interpreted in the time units of the provided {@link EventScheduler}.
     * @param period the time between the start of each execution of the provided action, interpreted in the time units of the provided {@link EventScheduler}.
     * @param description a description to attach to the scheduled event.
     * @param timeConsumingAction the action to execute, consuming the time the action executed at.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @see RepeatingRunnable#startInWithFixedDelay(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startIn(double delay, double period, String description, Consumer<Double> timeConsumingAction, EventScheduler eventScheduler) {
        return startAt(eventScheduler.getTimeProvider().getTime() + delay, period, description, timeConsumingAction, eventScheduler);
    }

    /**
     * Perform the provided action in {@code delay} time from now, then repeat the action every {@code period} time.
     * The time unit of {@code delay} and {@code period} is that of the provided {@link EventScheduler}.
     *
     * @param delay the time before performing the provided action for the first time
     * @param period the time between the start of each execution of the provided action
     * @param description a description to attach to the scheduled event.
     * @param timeConsumingAction the action to execute, consuming the time the action executed at.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @throws TimeUnitNotSpecifiedException if the {@link EventScheduler} was not initialised with a {@link com.ocadotechnology.time.TimeProviderWithUnit}.
     * @see RepeatingRunnable#startInWithFixedDelay(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startIn(Duration delay, Duration period, String description, Consumer<Instant> timeConsumingAction, EventScheduler eventScheduler) {
        TimeConverter timeConverter = eventScheduler.getTimeProviderWithUnit().getConverter();
        Consumer<Double> wrappedAction = t -> timeConsumingAction.accept(timeConverter.convertToInstant(t));
        double primitiveDelay = timeConverter.convertFromDuration(delay);
        double primitivePeriod = timeConverter.convertFromDuration(period);
        return startAt(eventScheduler.getTimeProvider().getTime() + primitiveDelay, primitivePeriod, description, wrappedAction, eventScheduler);
    }

    /**
     * Perform the provided action in {@code delay} time from now, then repeat the action every {@code period} time.
     * The time unit of {@code delay} and {@code period} is that of the provided {@link EventScheduler}.
     *
     * @param delay the time before performing the provided action for the first time, interpreted in the time units of the provided {@link EventScheduler}.
     * @param period the time between the start of each execution of the provided action, interpreted in the time units of the provided {@link EventScheduler}.
     * @param description a description to attach to the scheduled event.
     * @param runnable the action to execute.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @see RepeatingRunnable#startInWithFixedDelay(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startIn(double delay, double period, String description, Runnable runnable, EventScheduler eventScheduler) {
        return startAt(eventScheduler.getTimeProvider().getTime() + delay, period, description, runnable, eventScheduler);
    }

    /**
     * Perform the provided action in {@code delay} time from now, then repeat the action every {@code period} time.
     * The time unit of {@code delay} and {@code period} is that of the provided {@link EventScheduler}.
     *
     * @param delay the time before performing the provided action for the first time.
     * @param period the time between the start of each execution of the provided action.
     * @param description a description to attach to the scheduled event.
     * @param runnable the action to execute.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @throws TimeUnitNotSpecifiedException if the {@link EventScheduler} was not initialised with a {@link com.ocadotechnology.time.TimeProviderWithUnit}.
     * @see RepeatingRunnable#startInWithFixedDelay(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startIn(Duration delay, Duration period, String description, Runnable runnable, EventScheduler eventScheduler) {
        TimeConverter timeConverter = eventScheduler.getTimeProviderWithUnit().getConverter();
        double primitiveDelay = timeConverter.convertFromDuration(delay);
        double primitivePeriod = timeConverter.convertFromDuration(period);
        return startAt(eventScheduler.getTimeProvider().getTime() + primitiveDelay, primitivePeriod, description, runnable, eventScheduler);
    }

    /**
     * Perform the provided action at the specified {@code time}, then repeat the action every {@code period} time after each execution starts.
     * The time unit of {@code period} is that of the provided {@link EventScheduler}.
     *
     * @param time the time at which to perform the provided action for the first time, interpreted as an instantaneous point in time on the provided {@link EventScheduler}.
     * @param period the time between the start of each execution of the provided action, interpreted in the time units of the provided {@link EventScheduler}.
     * @param description a description to attach to the scheduled event.
     * @param timeConsumingAction the action to execute, consuming the time the action executed at.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @see RepeatingRunnable#startAtWithFixedDelay(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startAt(double time, double period, String description, Consumer<Double> timeConsumingAction, EventScheduler eventScheduler) {
        return start(time, period, description, timeConsumingAction, eventScheduler, false, false);
    }

    /**
     * Perform the provided action at the specified {@code time}, then repeat the action every {@code period} time after each execution starts.
     * The time unit of {@code period} is that of the provided {@link EventScheduler}.
     *
     * @param time the time at which to perform the provided action for the first time.
     * @param period the time between the start of each execution of the provided action.
     * @param description a description to attach to the scheduled event.
     * @param timeConsumingAction the action to execute, consuming the time the action executed at.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @throws TimeUnitNotSpecifiedException if the {@link EventScheduler} was not initialised with a {@link com.ocadotechnology.time.TimeProviderWithUnit}.
     * @see RepeatingRunnable#startAtWithFixedDelay(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startAt(Instant time, Duration period, String description, Consumer<Instant> timeConsumingAction, EventScheduler eventScheduler) {
        TimeConverter timeConverter = eventScheduler.getTimeProviderWithUnit().getConverter();
        Consumer<Double> wrappedAction = t -> timeConsumingAction.accept(timeConverter.convertToInstant(t));
        double primitiveTime = timeConverter.convertFromInstant(time);
        double primitivePeriod = timeConverter.convertFromDuration(period);
        return start(primitiveTime, primitivePeriod, description, wrappedAction, eventScheduler, false, false);
    }

    /**
     * Perform the provided action at the specified {@code time}, then repeat the action every {@code period} time after each execution starts.
     * The time unit of {@code period} is that of the provided {@link EventScheduler}.
     *
     * @param time the time at which to perform the provided action for the first time, interpreted as an instantaneous point in time on the provided {@link EventScheduler}.
     * @param period the time between the start of each execution of the provided action, interpreted in the time units of the provided {@link EventScheduler}.
     * @param description a description to attach to the scheduled event.
     * @param runnable the action to execute.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @see RepeatingRunnable#startAtWithFixedDelay(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startAt(double time, double period, String description, Runnable runnable, EventScheduler eventScheduler) {
        return start(time, period, description, eventTime -> runnable.run(), eventScheduler, false, false);
    }

    /**
     * Perform the provided action at the specified {@code time}, then repeat the action every {@code period} time after each execution starts.
     * The time unit of {@code period} is that of the provided {@link EventScheduler}.
     *
     * @param time the time at which to perform the provided action for the first time.
     * @param period the time between the start of each execution of the provided action.
     * @param description a description to attach to the scheduled event.
     * @param runnable the action to execute.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @throws TimeUnitNotSpecifiedException if the {@link EventScheduler} was not initialised with a {@link com.ocadotechnology.time.TimeProviderWithUnit}.
     * @see RepeatingRunnable#startAtWithFixedDelay(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startAt(Instant time, Duration period, String description, Runnable runnable, EventScheduler eventScheduler) {
        TimeConverter timeConverter = eventScheduler.getTimeProviderWithUnit().getConverter();
        double primitiveTime = timeConverter.convertFromInstant(time);
        double primitivePeriod = timeConverter.convertFromDuration(period);
        return start(primitiveTime, primitivePeriod, description, eventTime -> runnable.run(), eventScheduler, false, false);
    }

    /**
     * Perform the provided action at the specified {@code time}, then repeat the action every {@code period} time after each execution starts.
     * The time unit of {@code period} is that of the provided {@link EventScheduler}.
     * <p>
     * Each event will be flagged as a daemon task.
     *
     * @param time the time at which to perform the provided action for the first time, interpreted as an instantaneous point in time on the provided {@link EventScheduler}.
     * @param period the time between the start of each execution of the provided action, interpreted in the time units of the provided {@link EventScheduler}.
     * @param description a description to attach to the scheduled event.
     * @param r the action to execute.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @see RepeatingRunnable#startAt(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startAtDaemon(double time, double period, String description, Runnable r, EventScheduler eventScheduler) {
        return startAtDaemon(time, period, description, t -> r.run(), eventScheduler);
    }

    /**
     * Perform the provided action at the specified {@code time}, then repeat the action every {@code period} time after each execution starts.
     * The time unit of {@code period} is that of the provided {@link EventScheduler}.
     * <p>
     * Each event will be flagged as a daemon task.
     *
     * @param time the time at which to perform the provided action for the first time.
     * @param period the time between the start of each execution of the provided action.
     * @param description a description to attach to the scheduled event.
     * @param r the action to execute.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @throws TimeUnitNotSpecifiedException if the {@link EventScheduler} was not initialised with a {@link com.ocadotechnology.time.TimeProviderWithUnit}.
     * @see RepeatingRunnable#startAt(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startAtDaemon(Instant time, Duration period, String description, Runnable r, EventScheduler eventScheduler) {
        return startAtDaemon(time, period, description, t -> r.run(), eventScheduler);
    }

    /**
     * Perform the provided action at the specified {@code time}, then repeat the action every {@code period} time after each execution starts.
     * The time unit of {@code period} is that of the provided {@link EventScheduler}.
     * <p>
     * Each event will be flagged as a daemon task.
     *
     * @param time the time at which to perform the provided action for the first time, interpreted as an instantaneous point in time on the provided {@link EventScheduler}.
     * @param period the time between the start of each execution of the provided action, interpreted in the time units of the provided {@link EventScheduler}.
     * @param description a description to attach to the scheduled event.
     * @param timeConsumingAction the action to execute, consuming the time the action executed at.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @see RepeatingRunnable#startAt(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startAtDaemon(double time, double period, String description, Consumer<Double> timeConsumingAction, EventScheduler eventScheduler) {
        return start(time, period, description, timeConsumingAction, eventScheduler, true, false);
    }

    /**
     * Perform the provided action at the specified {@code time}, then repeat the action every {@code period} time after each execution starts.
     * The time unit of {@code period} is that of the provided {@link EventScheduler}.
     * <p>
     * Each event will be flagged as a daemon task.
     *
     * @param time the time at which to perform the provided action for the first time.
     * @param period the time between the start of each execution of the provided action.
     * @param description a description to attach to the scheduled event.
     * @param timeConsumingAction the action to execute, consuming the time the action executed at.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @throws TimeUnitNotSpecifiedException if the {@link EventScheduler} was not initialised with a {@link com.ocadotechnology.time.TimeProviderWithUnit}.
     * @see RepeatingRunnable#startAt(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startAtDaemon(Instant time, Duration period, String description, Consumer<Instant> timeConsumingAction, EventScheduler eventScheduler) {
        TimeConverter timeConverter = eventScheduler.getTimeProviderWithUnit().getConverter();
        Consumer<Double> wrappedAction = t -> timeConsumingAction.accept(timeConverter.convertToInstant(t));
        double primitiveTime = timeConverter.convertFromInstant(time);
        double primitivePeriod = timeConverter.convertFromDuration(period);
        return start(primitiveTime, primitivePeriod, description, wrappedAction, eventScheduler, true, false);
    }

    /**
     * Perform the provided action in {@code delay} time from now, then repeat the action every {@code period} time.
     * The time unit of {@code delay} and {@code period} is that of the provided {@link EventScheduler}.
     * <p>
     * Each event will be flagged as a daemon task.
     *
     * @param delay the time before performing the provided action for the first time, interpreted in the time units of the provided {@link EventScheduler}.
     * @param period the time between the start of each execution of the provided action, interpreted in the time units of the provided {@link EventScheduler}.
     * @param description a description to attach to the scheduled event.
     * @param r the action to execute.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @see RepeatingRunnable#startIn(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startInDaemon(double delay, double period, String description, Runnable r, EventScheduler eventScheduler) {
        return startInDaemon(delay, period, description, eventTime -> r.run(), eventScheduler);
    }

    /**
     * Perform the provided action in {@code delay} time from now, then repeat the action every {@code period} time.
     * The time unit of {@code delay} and {@code period} is that of the provided {@link EventScheduler}.
     * <p>
     * Each event will be flagged as a daemon task.
     *
     * @param delay the time before performing the provided action for the first time.
     * @param period the time between the start of each execution of the provided action.
     * @param description a description to attach to the scheduled event.
     * @param r the action to execute.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @throws TimeUnitNotSpecifiedException if the {@link EventScheduler} was not initialised with a {@link com.ocadotechnology.time.TimeProviderWithUnit}.
     * @see RepeatingRunnable#startIn(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startInDaemon(Duration delay, Duration period, String description, Runnable r, EventScheduler eventScheduler) {
        return startInDaemon(delay, period, description, eventTime -> r.run(), eventScheduler);
    }

    /**
     * Perform the provided action in {@code delay} time from now, then repeat the action every {@code period} time.
     * The time unit of {@code delay} and {@code period} is that of the provided {@link EventScheduler}.
     * <p>
     * Each event will be flagged as a daemon task.
     *
     * @param delay the time before performing the provided action for the first time, interpreted in the time units of the provided {@link EventScheduler}.
     * @param period the time between the start of each execution of the provided action, interpreted in the time units of the provided {@link EventScheduler}.
     * @param description a description to attach to the scheduled event.
     * @param timeConsumingAction the action to execute, consuming the time the action executed at.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @see RepeatingRunnable#startIn(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startInDaemon(double delay, double period, String description, Consumer<Double> timeConsumingAction, EventScheduler eventScheduler) {
        return startAtDaemon(eventScheduler.getTimeProvider().getTime() + delay, period, description, timeConsumingAction, eventScheduler);
    }

    /**
     * Perform the provided action in {@code delay} time from now, then repeat the action every {@code period} time.
     * The time unit of {@code delay} and {@code period} is that of the provided {@link EventScheduler}.
     * <p>
     * Each event will be flagged as a daemon task.
     *
     * @param delay the time before performing the provided action for the first time.
     * @param period the time between the start of each execution of the provided action.
     * @param description a description to attach to the scheduled event.
     * @param timeConsumingAction the action to execute, consuming the time the action executed at.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @throws TimeUnitNotSpecifiedException if the {@link EventScheduler} was not initialised with a {@link com.ocadotechnology.time.TimeProviderWithUnit}.
     * @see RepeatingRunnable#startIn(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startInDaemon(Duration delay, Duration period, String description, Consumer<Instant> timeConsumingAction, EventScheduler eventScheduler) {
        TimeConverter timeConverter = eventScheduler.getTimeProviderWithUnit().getConverter();
        Consumer<Double> wrappedAction = t -> timeConsumingAction.accept(timeConverter.convertToInstant(t));
        double primitiveDelay = timeConverter.convertFromDuration(delay);
        double primitivePeriod = timeConverter.convertFromDuration(period);
        return startAtDaemon(eventScheduler.getTimeProvider().getTime() + primitiveDelay, primitivePeriod, description, wrappedAction, eventScheduler);
    }

    /**
     * Perform the provided action in {@code delay} time from now, then repeat the action {@code period} time after the last execution of the action finishes.
     * The time unit of {@code delay} and {@code period} is that of the provided {@link EventScheduler}.
     *
     * @param delay the time before performing the provided action for the first time, interpreted in the time units of the provided {@link EventScheduler}.
     * @param period the time between the end of one execution of the provided action and the start of the next, interpreted in the time units of the provided {@link EventScheduler}.
     * @param description a description to attach to the scheduled event.
     * @param timeConsumingAction the action to execute, consuming the time the action executed at.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @see RepeatingRunnable#startIn(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startInWithFixedDelay(double delay, double period, String description, Consumer<Double> timeConsumingAction, EventScheduler eventScheduler) {
        return startAtWithFixedDelay(eventScheduler.getTimeProvider().getTime() + delay, period, description, timeConsumingAction, eventScheduler);
    }

    /**
     * Perform the provided action in {@code delay} time from now, then repeat the action {@code period} time after the last execution of the action finishes.
     * The time unit of {@code delay} and {@code period} is that of the provided {@link EventScheduler}.
     *
     * @param delay the time before performing the provided action for the first time.
     * @param period the time between the end of one execution of the provided action and the start of the next.
     * @param description a description to attach to the scheduled event.
     * @param timeConsumingAction the action to execute, consuming the time the action executed at.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @throws TimeUnitNotSpecifiedException if the {@link EventScheduler} was not initialised with a {@link com.ocadotechnology.time.TimeProviderWithUnit}.
     * @see RepeatingRunnable#startIn(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startInWithFixedDelay(Duration delay, Duration period, String description, Consumer<Instant> timeConsumingAction, EventScheduler eventScheduler) {
        TimeConverter timeConverter = eventScheduler.getTimeProviderWithUnit().getConverter();
        Consumer<Double> wrappedAction = t -> timeConsumingAction.accept(timeConverter.convertToInstant(t));
        double primitiveDelay = timeConverter.convertFromDuration(delay);
        double primitivePeriod = timeConverter.convertFromDuration(period);
        return startAtWithFixedDelay(eventScheduler.getTimeProvider().getTime() + primitiveDelay, primitivePeriod, description, wrappedAction, eventScheduler);
    }

    /**
     * Perform the provided action at the specified {@code time}, then repeat the action {@code period} time after the last execution of the action finishes.
     * The time unit of {@code period} is that of the provided {@link EventScheduler}.
     *
     * @param time the time at which to perform the provided action for the first time, interpreted as an instantaneous point in time on the provided {@link EventScheduler}.
     * @param period the time between the end of one execution of the provided action and the start of the next, interpreted in the time units of the provided {@link EventScheduler}.
     * @param description a description to attach to the scheduled event.
     * @param timeConsumingAction the action to execute, consuming the time the action executed at.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @see RepeatingRunnable#startAt(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startAtWithFixedDelay(double time, double period, String description, Consumer<Double> timeConsumingAction, EventScheduler eventScheduler) {
        return start(time, period, description, timeConsumingAction, eventScheduler, false, true);
    }

    /**
     * Perform the provided action at the specified {@code time}, then repeat the action {@code period} time after the last execution of the action finishes.
     * The time unit of {@code period} is that of the provided {@link EventScheduler}.
     *
     * @param time the time at which to perform the provided action for the first time.
     * @param period the time between the end of one execution of the provided action and the start of the next.
     * @param description a description to attach to the scheduled event.
     * @param timeConsumingAction the action to execute, consuming the time the action executed at.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @throws TimeUnitNotSpecifiedException if the {@link EventScheduler} was not initialised with a {@link com.ocadotechnology.time.TimeProviderWithUnit}.
     * @see RepeatingRunnable#startAt(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startAtWithFixedDelay(Instant time, Duration period, String description, Consumer<Instant> timeConsumingAction, EventScheduler eventScheduler) {
        TimeConverter timeConverter = eventScheduler.getTimeProviderWithUnit().getConverter();
        Consumer<Double> wrappedAction = t -> timeConsumingAction.accept(timeConverter.convertToInstant(t));
        double primitiveTime = timeConverter.convertFromInstant(time);
        double primitivePeriod = timeConverter.convertFromDuration(period);
        return start(primitiveTime, primitivePeriod, description, wrappedAction, eventScheduler, false, true);
    }

    /**
     * Perform the provided action at the specified {@code time}, then repeat the action {@code period} time after the last execution of the action finishes.
     * The time unit of {@code period} is that of the provided {@link EventScheduler}.
     * <p>
     * Each event will be flagged as a daemon task.
     *
     * @param time the time at which to perform the provided action for the first time, interpreted as an instantaneous point in time on the provided {@link EventScheduler}.
     * @param period the time between the end of one execution of the provided action and the start of the next, interpreted in the time units of the provided {@link EventScheduler}.
     * @param description a description to attach to the scheduled event.
     * @param r the action to execute.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @see RepeatingRunnable#startAt(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startAtDaemonWithFixedDelay(double time, double period, String description, Runnable r, EventScheduler eventScheduler) {
        return startAtDaemonWithFixedDelay(time, period, description, t -> r.run(), eventScheduler);
    }

    /**
     * Perform the provided action at the specified {@code time}, then repeat the action {@code period} time after the last execution of the action finishes.
     * The time unit of {@code period} is that of the provided {@link EventScheduler}.
     * <p>
     * Each event will be flagged as a daemon task.
     *
     * @param time the time at which to perform the provided action for the first time.
     * @param period the time between the end of one execution of the provided action and the start of the next.
     * @param description a description to attach to the scheduled event.
     * @param r the action to execute.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @throws TimeUnitNotSpecifiedException if the {@link EventScheduler} was not initialised with a {@link com.ocadotechnology.time.TimeProviderWithUnit}.
     * @see RepeatingRunnable#startAt(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startAtDaemonWithFixedDelay(Instant time, Duration period, String description, Runnable r, EventScheduler eventScheduler) {
        return startAtDaemonWithFixedDelay(time, period, description, t -> r.run(), eventScheduler);
    }

    /**
     * Perform the provided action at the specified {@code time}, then repeat the action {@code period} time after the last execution of the action finishes.
     * The time unit of {@code period} is that of the provided {@link EventScheduler}.
     * <p>
     * Each event will be flagged as a daemon task.
     *
     * @param time the time at which to perform the provided action for the first time, interpreted as an instantaneous point in time on the provided {@link EventScheduler}.
     * @param period the time between the end of one execution of the provided action and the start of the next, interpreted in the time units of the provided {@link EventScheduler}.
     * @param description a description to attach to the scheduled event.
     * @param timeConsumingAction the action to execute, consuming the time the action executed at.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @see RepeatingRunnable#startAt(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startAtDaemonWithFixedDelay(double time, double period, String description, Consumer<Double> timeConsumingAction, EventScheduler eventScheduler) {
        return start(time, period, description, timeConsumingAction, eventScheduler, true, true);
    }

    /**
     * Perform the provided action at the specified {@code time}, then repeat the action {@code period} time after the last execution of the action finishes.
     * The time unit of {@code period} is that of the provided {@link EventScheduler}.
     * <p>
     * Each event will be flagged as a daemon task.
     *
     * @param time the time at which to perform the provided action for the first time.
     * @param period the time between the end of one execution of the provided action and the start of the next.
     * @param description a description to attach to the scheduled event.
     * @param timeConsumingAction the action to execute, consuming the time the action executed at.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @throws TimeUnitNotSpecifiedException if the {@link EventScheduler} was not initialised with a {@link com.ocadotechnology.time.TimeProviderWithUnit}.
     * @see RepeatingRunnable#startAt(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startAtDaemonWithFixedDelay(Instant time, Duration period, String description, Consumer<Instant> timeConsumingAction, EventScheduler eventScheduler) {
        TimeConverter timeConverter = eventScheduler.getTimeProviderWithUnit().getConverter();
        Consumer<Double> wrappedAction = t -> timeConsumingAction.accept(timeConverter.convertToInstant(t));
        double primitiveTime = timeConverter.convertFromInstant(time);
        double primitivePeriod = timeConverter.convertFromDuration(period);
        return start(primitiveTime, primitivePeriod, description, wrappedAction, eventScheduler, true, true);
    }

    /**
     * Perform the provided action in {@code delay} time from now, then repeat the action {@code period} time after the last execution of the action finishes.
     * The time unit of {@code delay} and {@code period} is that of the provided {@link EventScheduler}.
     * <p>
     * Each event will be flagged as a daemon task.
     *
     * @param delay the time before performing the provided action for the first time, interpreted in the time units of the provided {@link EventScheduler}.
     * @param period the time between the end of one execution of the provided action and the start of the next, interpreted in the time units of the provided {@link EventScheduler}.
     * @param description a description to attach to the scheduled event.
     * @param r the action to execute.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @see RepeatingRunnable#startIn(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startInDaemonWithFixedDelay(double delay, double period, String description, Runnable r, EventScheduler eventScheduler) {
        return startInDaemonWithFixedDelay(delay, period, description, eventTime -> r.run(), eventScheduler);
    }

    /**
     * Perform the provided action in {@code delay} time from now, then repeat the action {@code period} time after the last execution of the action finishes.
     * The time unit of {@code delay} and {@code period} is that of the provided {@link EventScheduler}.
     * <p>
     * Each event will be flagged as a daemon task.
     *
     * @param delay the time before performing the provided action for the first time.
     * @param period the time between the end of one execution of the provided action and the start of the next.
     * @param description a description to attach to the scheduled event.
     * @param r the action to execute.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @throws TimeUnitNotSpecifiedException if the {@link EventScheduler} was not initialised with a {@link com.ocadotechnology.time.TimeProviderWithUnit}.
     * @see RepeatingRunnable#startIn(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startInDaemonWithFixedDelay(Duration delay, Duration period, String description, Runnable r, EventScheduler eventScheduler) {
        return startInDaemonWithFixedDelay(delay, period, description, eventTime -> r.run(), eventScheduler);
    }

    /**
     * Perform the provided action in {@code delay} time from now, then repeat the action {@code period} time after the last execution of the action finishes.
     * The time unit of {@code delay} and {@code period} is that of the provided {@link EventScheduler}.
     * <p>
     * Each event will be flagged as a daemon task.
     *
     * @param delay the time before performing the provided action for the first time, interpreted in the time units of the provided {@link EventScheduler}.
     * @param period the time between the end of one execution of the provided action and the start of the next, interpreted in the time units of the provided {@link EventScheduler}.
     * @param description a description to attach to the scheduled event.
     * @param timeConsumingAction the action to execute, consuming the time the action executed at.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @see RepeatingRunnable#startIn(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startInDaemonWithFixedDelay(double delay, double period, String description, Consumer<Double> timeConsumingAction, EventScheduler eventScheduler) {
        return startAtDaemonWithFixedDelay(eventScheduler.getTimeProvider().getTime() + delay, period, description, timeConsumingAction, eventScheduler);
    }

    /**
     * Perform the provided action in {@code delay} time from now, then repeat the action {@code period} time after the last execution of the action finishes.
     * The time unit of {@code delay} and {@code period} is that of the provided {@link EventScheduler}.
     * <p>
     * Each event will be flagged as a daemon task.
     *
     * @param delay the time before performing the provided action for the first time.
     * @param period the time between the end of one execution of the provided action and the start of the next.
     * @param description a description to attach to the scheduled event.
     * @param timeConsumingAction the action to execute, consuming the time the action executed at.
     * @param eventScheduler the {@link EventScheduler} to perform the repeating action on.
     * @return a cancelable representing the repeating event.
     * @throws TimeUnitNotSpecifiedException if the {@link EventScheduler} was not initialised with a {@link com.ocadotechnology.time.TimeProviderWithUnit}.
     * @see RepeatingRunnable#startIn(double, double, String, Consumer, EventScheduler)
     */
    public static Cancelable startInDaemonWithFixedDelay(Duration delay, Duration period, String description, Consumer<Instant> timeConsumingAction, EventScheduler eventScheduler) {
        TimeConverter timeConverter = eventScheduler.getTimeProviderWithUnit().getConverter();
        Consumer<Double> wrappedAction = t -> timeConsumingAction.accept(timeConverter.convertToInstant(t));
        double primitiveDelay = timeConverter.convertFromDuration(delay);
        double primitivePeriod = timeConverter.convertFromDuration(period);
        return startAtDaemonWithFixedDelay(eventScheduler.getTimeProvider().getTime() + primitiveDelay, primitivePeriod, description, wrappedAction, eventScheduler);
    }

    private static Cancelable start(double time, double period, String description, Consumer<Double> timeConsumingAction, EventScheduler eventScheduler, boolean isDaemon, boolean fixedDelay) {
        AtomicBooleanCancelable atomicBooleanCancelable = new AtomicBooleanCancelable();
        repeat(time, period, description, timeConsumingAction, eventScheduler, isDaemon, atomicBooleanCancelable.cancelled, fixedDelay);
        return atomicBooleanCancelable;
    }

    private static void repeat(
            double time,
            double period,
            String description,
            Consumer<Double> timeConsumingAction,
            EventScheduler eventScheduler,
            boolean isDaemon,
            AtomicBoolean canceled,
            boolean fixedDelay) {
        RepeatingRunnable repeatingRunnable = new RepeatingRunnable(time, period, description, timeConsumingAction, eventScheduler, isDaemon, canceled, fixedDelay);

        if (isDaemon) {
            eventScheduler.doAtDaemon(time, repeatingRunnable, description);
        } else {
            eventScheduler.doAt(time, repeatingRunnable, description);
        }
    }

    private RepeatingRunnable(
            double time,
            double period,
            String description,
            Consumer<Double> timeConsumingAction,
            EventScheduler eventScheduler,
            boolean isDaemon,
            AtomicBoolean canceled,
            boolean fixedDelay) {

        this.time = time;
        this.period = period;
        this.description = description;
        this.timeConsumingAction = timeConsumingAction;
        this.eventScheduler = eventScheduler;
        this.isDaemon = isDaemon;
        this.canceled = canceled;
        this.fixedDelay = fixedDelay;
    }

    /**
     * Public due to the functional interface, however this should never be visible outside the class as it's not possible to obtain an instance of a RepeatingRunnable
     */
    @Override
    public void run() {
        if (canceled.get()) {
            return;
        }
        timeConsumingAction.accept(time);

        double next = fixedDelay ? eventScheduler.getTimeProvider().getTime() + period : time + period;
        repeat(next, period, description, timeConsumingAction, eventScheduler, isDaemon, canceled, fixedDelay);
    }

    private static class AtomicBooleanCancelable implements Cancelable {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        AtomicBooleanCancelable() {
        }

        @Override
        public void cancel() {
            cancelled.set(true);
        }
    }

}
