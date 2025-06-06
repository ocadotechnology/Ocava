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
import java.util.function.Consumer;

import com.ocadotechnology.time.TimeProvider;
import com.ocadotechnology.time.TimeProviderWithUnit;

public interface EventScheduler {

    double ONE_CLOCK_CYCLE = 0.001;

    boolean hasOnlyDaemonEvents();

    /**
     * Signal to the scheduler that a "safe shutdown" should be started.
     *
     * Once called, doAt events will be rejected (return null) and existing doAt events will NOT be executed.
     * doNow events are unaffected (will continue to execute and can be scheduled).
     * This allows existing events to 'flush' through (including scheduling more doNow events),
     * and other processes to schedule doNow events as part of the shutdown sequence.
     *
     * The scheduler will continue to run like this until {@link #stop()} is called, at which point
     * when the doNow queue becomes empty, the scheduler will actually stop.
     *
     * Calling prepareToStop more than once has no additional effect.
     */
    void prepareToStop();

    /**
     * Default (previous) behaviour: calling stop will prevent any more "now" or "at" events from starting.
     * Calls to doNow and doAt will return null until the scheduler is set to idle.
     *
     * New behaviour: if {@link #prepareToStop()} has been called (and {@link #isStopping()} returns true,
     * then existing "now" events will be called until the "now" queue is empty (flushing phase).
     * If more "now" events are added during this time, they will also run.
     * Calls to doNow will (obviously) succeed, but calls to doAt will return null.
     * Once the "now" queue is empty, the scheduler will stop (and doNow calls will also return null).
     * The new behaviour is intended as a "safe shutdown".
     *
     * Calling stop more than once has no additional effect.
     */
    void stop();

    /** @return true if {@link #prepareToStop()} has been called, but {@link #stop()} has not (yet). */
    boolean isStopping();

    TimeProvider getTimeProvider();

    /**
     * @return this scheduler's time provider, if it is a TimeProviderWithUnit.
     * @throws TimeUnitNotSpecifiedException if this {@link EventScheduler} was not initialised with a {@link TimeProviderWithUnit}.
     */
    TimeProviderWithUnit getTimeProviderWithUnit();

    /**
     * Schedules an event to occur at the current time.
     */
    default Cancelable doNow(Runnable r) {
        return doNow(r, r.getClass().getName());
    }

    /**
     * Schedules an event to occur at the specified time.
     * May throw IllegalStateException if the time is in the past, or may execute the event immediately, depending on implementation.
     */
    default Cancelable doAt(double time, Runnable r) {
        return doAt(time, r, r.getClass().getName());
    }

    /**
     * Schedules an event to occur at the specified time.
     * May throw IllegalStateException if the time is in the past, or may execute the event immediately, depending on implementation.
     * @throws TimeUnitNotSpecifiedException if this {@link EventScheduler} was not initialised with a {@link TimeProviderWithUnit}.
     */
    default Cancelable doAt(Instant time, Runnable r) {
        return doAt(time, r, r.getClass().getName());
    }

    /**
     * Schedules an event to occur at the current time or at the specified time, whichever is later.
     */
    default Cancelable doAtOrNow(double time, Runnable r) {
        return doAtOrNow(time, r, r.getClass().getName());
    }

    /**
     * Schedules an event to occur at the current time or at the specified time, whichever is later.
     * @throws TimeUnitNotSpecifiedException if this {@link EventScheduler} was not initialised with a {@link TimeProviderWithUnit}.
     */
    default Cancelable doAtOrNow(Instant time, Runnable r) {
        return doAtOrNow(time, r, r.getClass().getName());
    }

    Cancelable doNow(Runnable r, String description);

    /**
     * Schedules an event to occur at the specified time.
     * May throw IllegalStateException if the time is in the past, or may execute the event immediately, depending on implementation.
     */
    default Cancelable doAt(double time, Runnable r, String description) {
        return doAt(time, r, description, false);
    }

    /**
     * Schedules an event to occur at the specified time.
     * May throw IllegalStateException if the time is in the past, or may execute the event immediately, depending on implementation.
     * @throws TimeUnitNotSpecifiedException if this {@link EventScheduler} was not initialised with a {@link TimeProviderWithUnit}.
     */
    default Cancelable doAt(Instant time, Runnable r, String description) {
        return doAt(time, r, description, false);
    }

    /**
     * Creates a Cancelable Event using the provided Runnable, and tags the Event as a 'daemon' task.  Does NOT imply
     * any repetition on the event. Use {@link RepeatingRunnable} to create repeating events.
     *
     * @param time - the time of the event
     * @param r - the action to perform at the indicated time
     * @param description - Human-readable String description for the task.  Important for debugging.
     * @return the Cancellable event created.
     */
    default Cancelable doAtDaemon(double time, Runnable r, String description) {
        return doAt(time, r, description, true);
    }

    /**
     * Creates a Cancelable Event using the provided Runnable, and tags the Event as a 'daemon' task.  Does NOT imply
     * any repetition on the event. Use {@link RepeatingRunnable} to create repeating events.
     *
     * @param time - the time of the event
     * @param r - the action to perform at the indicated time
     * @param description - Human-readable String description for the task.  Important for debugging.
     * @return the Cancellable event created.
     * @throws TimeUnitNotSpecifiedException if this {@link EventScheduler} was not initialised with a {@link TimeProviderWithUnit}.
     */
    default Cancelable doAtDaemon(Instant time, Runnable r, String description) {
        double primitiveTime = getTimeProviderWithUnit().getConverter().convertFromInstant(time);
        return doAtDaemon(primitiveTime, r, description);
    }

    /**
     * Schedules an event to occur at the specified time.
     * May throw IllegalStateException if the time is in the past, or may execute the event immediately, depending on implementation.
     */
    Cancelable doAt(double time, Runnable r, String description, boolean isDaemon);

    /**
     * Schedules an event to occur at the specified time.
     * May throw IllegalStateException if the time is in the past, or may execute the event immediately, depending on implementation.
     * @throws TimeUnitNotSpecifiedException if this {@link EventScheduler} was not initialised with a {@link TimeProviderWithUnit}.
     */
    default Cancelable doAt(Instant time, Runnable r, String description, boolean isDaemon) {
        double primitiveTime = getTimeProviderWithUnit().getConverter().convertFromInstant(time);
        return doAt(primitiveTime, r, description, isDaemon);
    }

    long getThreadId();

    EventSchedulerType getType();

    /**
     * Schedules an event to occur after the specified delay.
     * May throw IllegalStateException if the delay is negative, or may execute the event immediately, depending on implementation.
     */
    default Cancelable doIn(double delay, Runnable r) {
        return doIn(delay, r, r.getClass().getName());
    }

    /**
     * Schedules an event to occur after the specified delay.
     * May throw IllegalStateException if the delay is negative, or may execute the event immediately, depending on implementation.
     * @throws TimeUnitNotSpecifiedException if this {@link EventScheduler} was not initialised with a {@link TimeProviderWithUnit}.
     */
    default Cancelable doIn(Duration delay, Runnable r) {
        return doIn(delay, r, r.getClass().getName());
    }

    /**
     * Schedules an event to occur after the specified delay.
     * May throw IllegalStateException if the delay is negative, or may execute the event immediately, depending on implementation.
     */
    default Cancelable doIn(double delay, Runnable r, String description) {
        return doIn(delay, t -> r.run(), description);
    }
    
    /**
     * Schedules an event to occur after the specified delay.
     * May throw IllegalStateException if the delay is negative, or may execute the event immediately, depending on implementation.
     * @throws TimeUnitNotSpecifiedException if this {@link EventScheduler} was not initialised with a {@link TimeProviderWithUnit}.
     */
    default Cancelable doIn(Duration delay, Runnable r, String description) {
        return doIn(delay, t -> r.run(), description);
    }

    /**
     * Schedules an event to occur after the specified delay.
     * May throw IllegalStateException if the delay is negative, or may execute the event immediately, depending on implementation.
     */
    default Cancelable doIn(double delay, Consumer<Double> eventTimeConsumingAction, String description) {
        double eventTime = getTimeProvider().getTime() + delay;
        return doAt(eventTime, () -> eventTimeConsumingAction.accept(eventTime), description);
    }

    /**
     * Schedules an event to occur after the specified delay.
     * May throw IllegalStateException if the delay is negative, or may execute the event immediately, depending on implementation.
     * @throws TimeUnitNotSpecifiedException if this {@link EventScheduler} was not initialised with a {@link TimeProviderWithUnit}.
     */
    default Cancelable doIn(Duration delay, Consumer<Double> eventTimeConsumingAction, String description) {
        double primitiveDelay = getTimeProviderWithUnit().getConverter().convertFromDuration(delay);
        return doIn(primitiveDelay, eventTimeConsumingAction, description);
    }

    /**
     * Schedules a daemon event to occur after the specified delay.
     * May throw IllegalStateException if the delay is negative, or may execute the event immediately, depending on implementation.
     */
    default Cancelable doInDaemon(double delay, Runnable r) {
        return doInDaemon(delay, r, r.getClass().getName());
    }
    
    /**
     * Schedules a daemon event to occur after the specified delay.
     * May throw IllegalStateException if the delay is negative, or may execute the event immediately, depending on implementation.
     * @throws TimeUnitNotSpecifiedException if this {@link EventScheduler} was not initialised with a {@link TimeProviderWithUnit}.
     */
    default Cancelable doInDaemon(Duration delay, Runnable r) {
        return doInDaemon(delay, r, r.getClass().getName());
    }

    /**
     * Schedules a daemon event to occur after the specified delay.
     * May throw IllegalStateException if the delay is negative, or may execute the event immediately, depending on implementation.
     */
    default Cancelable doInDaemon(double delay, Runnable r, String description) {
        return doInDaemon(delay, t -> r.run(), description);
    }
    
    /**
     * Schedules a daemon event to occur after the specified delay.
     * May throw IllegalStateException if the delay is negative, or may execute the event immediately, depending on implementation.
     * @throws TimeUnitNotSpecifiedException if this {@link EventScheduler} was not initialised with a {@link TimeProviderWithUnit}.
     */
    default Cancelable doInDaemon(Duration delay, Runnable r, String description) {
        return doInDaemon(delay, t -> r.run(), description);
    }

    /**
     * Schedules a daemon event to occur after the specified delay.
     * May throw IllegalStateException if the delay is negative, or may execute the event immediately, depending on implementation.
     */
    default Cancelable doInDaemon(double delay, Consumer<Double> eventTimeConsumingAction, String description) {
        double eventTime = getTimeProvider().getTime() + delay;
        return doAtDaemon(eventTime, () -> eventTimeConsumingAction.accept(eventTime), description);
    }
    
    /**
     * Schedules a daemon event to occur after the specified delay.
     * May throw IllegalStateException if the delay is negative, or may execute the event immediately, depending on implementation.
     * @throws TimeUnitNotSpecifiedException if this {@link EventScheduler} was not initialised with a {@link TimeProviderWithUnit}.
     */
    default Cancelable doInDaemon(Duration delay, Consumer<Double> eventTimeConsumingAction, String description) {
        double primitiveDelay = getTimeProviderWithUnit().getConverter().convertFromDuration(delay);
        return doInDaemon(primitiveDelay, eventTimeConsumingAction, description);
    }

    /**
     * Schedules an event to occur at the current time or at the specified time, whichever is later.
     */
    default Cancelable doAtOrNow(double time, Runnable r, String description) {
        return doAtOrNow(time, t -> r.run(), description);
    }
    
    /**
     * Schedules an event to occur at the current time or at the specified time, whichever is later.
     * @throws TimeUnitNotSpecifiedException if this {@link EventScheduler} was not initialised with a {@link TimeProviderWithUnit}.
     */
    default Cancelable doAtOrNow(Instant time, Runnable r, String description) {
        double primitiveTime = getTimeProviderWithUnit().getConverter().convertFromInstant(time);
        return doAtOrNow(primitiveTime, r, description);
    }

    /**
     * Schedules an event to occur at the current time or at the specified time, whichever is later.
     */
    default Cancelable doAtOrNow(double time, Consumer<Double> eventTimeConsumingAction, String description) {
        double now = getTimeProvider().getTime();
        if (time > now) {
            return doAt(time, () -> eventTimeConsumingAction.accept(time), description);
        } else {
            return doNow(() -> eventTimeConsumingAction.accept(now), description);
        }
    }

    /**
     * Schedules an event to occur at the current time or at the specified time, whichever is later.
     * @throws TimeUnitNotSpecifiedException if this {@link EventScheduler} was not initialised with a {@link TimeProviderWithUnit}.
     */
    default Cancelable doAtOrNow(Instant time, Consumer<Double> eventTimeConsumingAction, String description) {
        double primitiveTime = getTimeProviderWithUnit().getConverter().convertFromInstant(time);
        return doAtOrNow(primitiveTime, eventTimeConsumingAction, description);
    }

    default boolean isThreadHandoverRequired() {
        return Thread.currentThread().getId() != getThreadId();
    }

    default double getMinimumTimeDelta() {
        return ONE_CLOCK_CYCLE;
    }
}
