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
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import javax.annotation.CheckForNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.ocadotechnology.event.EventUtil;
import com.ocadotechnology.time.ModifiableTimeProvider;
import com.ocadotechnology.time.TimeProvider;
import com.ocadotechnology.time.TimeProviderWithUnit;

public class SimpleDiscreteEventScheduler implements EventSchedulerWithCanceling {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ModifiableTimeProvider timeProvider;

    private final boolean enforceStrictEventOrdering;

    private final EventExecutor simpleEventExecutor;
    private final Consumer<Throwable> endCallBack;

    private final EventsQueue discreteEventsQueue;
    private final EventSchedulerType type;

    private final RunState runState = new RunState();

    private boolean shouldLogExceptions = true;

    public SimpleDiscreteEventScheduler(EventExecutor simpleEventExecutor, Runnable endCallBack, EventSchedulerType type, ModifiableTimeProvider timeProvider, boolean enforceStrictEventOrdering, EventsQueue discreteEventsQueue) {
        this(simpleEventExecutor, t -> endCallBack.run(), type, timeProvider, enforceStrictEventOrdering, discreteEventsQueue);
    }

    /**
     * @param endCallBack The endCallback is always called in the event of the scheduler being stopped regardless of whether there is an exception or not.
     *                    If there is no exception the callback will receive null as the exception parameter
     */
    public SimpleDiscreteEventScheduler(EventExecutor simpleEventExecutor, Consumer<Throwable> endCallBack, EventSchedulerType type, ModifiableTimeProvider timeProvider, boolean enforceStrictEventOrdering, EventsQueue discreteEventsQueue) {
        this.simpleEventExecutor = simpleEventExecutor;
        this.endCallBack = endCallBack;
        this.type = type;
        this.timeProvider = timeProvider;
        this.enforceStrictEventOrdering = enforceStrictEventOrdering;
        this.discreteEventsQueue = discreteEventsQueue;
    }

    public SimpleDiscreteEventScheduler(EventExecutor simpleEventExecutor, Runnable endCallBack, EventSchedulerType type, ModifiableTimeProvider timeProvider, boolean enforceStrictEventOrdering) {
        this(simpleEventExecutor, endCallBack, type, timeProvider, enforceStrictEventOrdering, new DiscreteEventsQueue());
    }

    public SimpleDiscreteEventScheduler(EventExecutor simpleEventExecutor, Runnable endCallBack, EventSchedulerType type, ModifiableTimeProvider timeProvider) {
        this(simpleEventExecutor, endCallBack, type, timeProvider, true);
    }

    public void dontLogExceptions() {
        shouldLogExceptions = false;
    }

    public void pause() {
        runState.setPause();
    }

    public void unPause() {
        unPause(() -> false);
    }

    public void unPause(BooleanSupplier exitCondition) {
        if (runState.clearPause()) {
            startExecutingEvents(false, exitCondition);
        }
    }

    /**
     * Unpauses the scheduler and allows it to run for a specified duration. This method guarantees that all events
     * scheduled for the exact end of the duration will be executed before the scheduler is paused again.
     * @param duration The duration to run the scheduler for, in scheduler time units.
     * @throws IllegalStateException if the scheduler is not paused or if the provided duration is negative.
     */
    public void runForDuration(double duration) {
        double endTime = timeProvider.getTime() + duration;
        unpauseUntil(endTime, "runForDuration");
    }

    /**
     * Unpauses the scheduler and allows it to run for a specified duration. This method guarantees that all events
     * scheduled for the exact end of the duration will be executed before the scheduler is paused again.
     * @param duration The Duration to run the scheduler for.
     * @throws IllegalStateException if the scheduler is not paused or if the provided duration is negative.
     * @throws TimeUnitNotSpecifiedException if this {@link EventScheduler} was not initialised with a {@link TimeProviderWithUnit}.
     */
    public void runForDuration(Duration duration) {
        double primitiveDuration = getTimeProviderWithUnit().getConverter().convertFromDuration(duration);
        runForDuration(primitiveDuration);
    }

    /**
     * Unpauses the scheduler and allows it to run until a specified time. This method guarantees that all events
     * scheduled for the exact end time will be executed before the scheduler is paused again.
     * @param endTime The time to run the scheduler to, in scheduler time units.
     * @throws IllegalStateException if the scheduler is not paused or if the provided time is in the past.
     */
    public void runUntilTime(double endTime) {
        unpauseUntil(endTime, "runUntilTime");
    }

    /**
     * Unpauses the scheduler and allows it to run until a specified time. This method guarantees that all events
     * scheduled for the exact end time will be executed before the scheduler is paused again.
     * @param endTime The Instant to run the scheduler to.
     * @throws IllegalStateException if the scheduler is not paused or if the provided time is in the past.
     * @throws TimeUnitNotSpecifiedException if this {@link EventScheduler} was not initialised with a {@link TimeProviderWithUnit}.
     */
    public void runUntilTime(Instant endTime) {
        runUntilTime(getTimeProviderWithUnit().getConverter().convertFromInstant(endTime));
    }

    private void unpauseUntil(double endTime, String methodName) {
        Preconditions.checkState(runState.isPaused(), "Method SimpleDiscreteEventScheduler#%s can only be called when the scheduler is paused", methodName);
        Preconditions.checkState(!runState.isRunningUnpauseUntil(), "Method SimpleDiscreteEventScheduler#%s cannot be called while still executing another similar method call", methodName);
        Preconditions.checkState(endTime >= getTime(), "Method SimpleDiscreteEventScheduler#%s cannot be called with an end time in the past", methodName);

        doAt(endTime, () -> pauseIfNextEventIsAfter(endTime), "Scheduled pause event"); //More efficient than using an exitCondition in the unpause call
        try {
            runState.setRunningUnpauseUntil();
            unPause();
        } finally {
            runState.clearRunningUnpauseUntil();
        }
    }

    private void pauseIfNextEventIsAfter(double endTime) {
        double nextEventTime = discreteEventsQueue.getNextScheduledEventTime();
        if (Double.isNaN(nextEventTime) || nextEventTime > endTime) {
            pause();
        } else {
            doAt(endTime, () -> pauseIfNextEventIsAfter(endTime), "Scheduled pause event");
        }
    }

    @Override
    public Cancelable doNow(Runnable r, String description) {
        Cancelable event = scheduleDoNowDontStart(r, description);
        if (event != null) {
            startExecutingEvents(true, () -> false);
        }
        return event;
    }

    public @CheckForNull Cancelable scheduleDoNowDontStart(Runnable r, String description) {
        if (!runState.canDoNow()) {
            return null;
        }
        Event event = new Event(timeProvider.getTime(), description, r, this, false);
        discreteEventsQueue.addDoNow(event);
        return event;
    }

    @Override
    public Cancelable doAt(double time, Runnable r, String description, boolean isDaemon) {
        Cancelable event = scheduleDoAtDontStart(time, r, description, isDaemon);
        if (event != null) {
            startExecutingEvents(true, () -> false);
        }
        return event;
    }

    public Cancelable scheduleDoAtDontStart(double time, Runnable r, String description, boolean isDaemon) {
        if (!runState.canDoAt()) {
            return null;
        }
        double now = timeProvider.getTime();
        if (time < now) {
            Preconditions.checkState(!enforceStrictEventOrdering, "Attempted to schedule [%s @ %s < %s] in the past", description, time, now);
            time = now;
        }
        Event event = new Event(time, description, r, this, isDaemon);
        discreteEventsQueue.addDoAt(event);
        return event;
    }

    private void startExecutingEvents(boolean isReentrant, BooleanSupplier exitCondition) {
        if (runState.setExecuting(isReentrant)) {
            executeEvents(exitCondition);
            runState.setIdle();
        }
    }

    private void executeEvents(BooleanSupplier exitCondition) {
        Event nextEvent;
        while (runState.canExecute() && !exitCondition.getAsBoolean() && (nextEvent = discreteEventsQueue.getNextEvent()) != null) {
            timeProvider.setTime(nextEvent.time);
            try {
                simpleEventExecutor.execute(nextEvent, timeProvider.getTime());
            } catch (Throwable t) {
                if (shouldLogExceptions) {
                    logger.error("Throwable caught at {} whilst processing {}", EventUtil.logTime(timeProvider.getTime()), nextEvent, t);
                }
                stop(t);

                throw t;
            }
        }
    }

    public void blockEvent(BooleanSupplier exitCondition) {
        executeEvents(exitCondition);
    }

    public void blockEvent(double delay) {
        double currentTime = timeProvider.getTime();
        blockEvent(() -> timeProvider.getTime() - currentTime >= delay);
    }

    @Override
    public boolean hasOnlyDaemonEvents() {
        return discreteEventsQueue.hasOnlyDaemonEvents();
    }

    @Override
    public void cancel(Event event) {
        discreteEventsQueue.cancel(event);
    }

    @Override
    public void prepareToStop() {
        if (runState.setStopping()) {
            discreteEventsQueue.clearScheduledEvents();
        }
    }

    @Override
    public void stop() {
        stop(null);
    }

    @Override
    public boolean isStopping() {
        return runState.isStopping;
    }

    public void stop(Throwable t) {
        if (runState.isStopping) {
            flushExistingDoNowEvents();
        }
        if (runState.setStopped()) {
            discreteEventsQueue.clear();
            endCallBack.accept(t);
        }
    }

    private void flushExistingDoNowEvents() {
        double now = timeProvider.getTime();

        Event nextEvent;
        while ((nextEvent = discreteEventsQueue.getNextEvent()) != null) {
            try {
                simpleEventExecutor.execute(nextEvent, now);
            } catch (Throwable t) {
                if (shouldLogExceptions) {
                    logger.error("Throwable caught at {} whilst processing {}", EventUtil.logTime(now), nextEvent, t);
                }
                throw t;
            }
        }
    }

    public boolean isStopped() {
        return runState.isStopped;
    }

    @Override
    public TimeProvider getTimeProvider() {
        return timeProvider;
    }

    @Override
    public TimeProviderWithUnit getTimeProviderWithUnit() {
        if (timeProvider instanceof TimeProviderWithUnit timeProviderWithUnit) {
            return timeProviderWithUnit;
        }
        throw new TimeUnitNotSpecifiedException();
    }

    @Override
    public long getThreadId() {
        return Thread.currentThread().getId();
    }

    @Override
    public EventSchedulerType getType() {
        return type;
    }

    public double getTime() {
        return timeProvider.getTime();
    }

    public double getNextScheduledEventTime() {
        return discreteEventsQueue.getNextScheduledEventTime();
    }

    public double timeToNextEvent() {
        return getNextScheduledEventTime() - timeProvider.getTime();
    }

    private static class RunState {
        private boolean isPaused = false;
        private boolean isExecuting = false;
        private boolean isStopped = false;
        private boolean isStopping = false;
        private boolean isRunningUnpauseUntil = false;

        public boolean isIdle() {
            return !isPaused && !isExecuting && !isStopped && !isStopping;
        }

        public boolean isPaused() {
            return isPaused;
        }

        public void setPause() {
            if (!isStopped && !isStopping) {
                isPaused = true;
            }
        }

        public boolean clearPause() {
            if (isPaused) {
                isPaused = false;
                return true;
            }
            return false;
        }

        public boolean canDoNow() {
            return !isStopped;
        }

        public boolean canDoAt() {
            return !isStopped && !isStopping;
        }

        public boolean canExecute() {
            return !isStopped && !isPaused;
        }

        public boolean setExecuting(boolean isReentrant) {
            if (!isPaused && (!isReentrant || !isExecuting)) {
                isExecuting = true;
                return true;
            }
            return false;
        }

        public void setIdle() {
            if (!isStopped && !isStopping && !isPaused) {
                isExecuting = false;
            }
        }

        public boolean setStopping() {
            if (!isStopped && !isPaused && !isStopping) {
                isStopping = true;
                return true;
            }
            return false;
        }

        public boolean setStopped() {
            if (!isStopped) {
                isStopped = true;
                isStopping = false;
                return true;
            }
            return false;
        }

        public boolean isRunningUnpauseUntil() {
            return isRunningUnpauseUntil;
        }

        public void setRunningUnpauseUntil() {
            isRunningUnpauseUntil = true;
        }

        public void clearRunningUnpauseUntil() {
            isRunningUnpauseUntil = false;
        }
    }
}
