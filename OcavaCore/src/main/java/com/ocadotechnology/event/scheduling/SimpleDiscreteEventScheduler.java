/*
 * Copyright © 2017-2022 Ocado (Ocava)
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

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.ocadotechnology.event.EventUtil;
import com.ocadotechnology.time.ModifiableTimeProvider;
import com.ocadotechnology.time.TimeProvider;

public class SimpleDiscreteEventScheduler implements EventSchedulerWithCanceling {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ModifiableTimeProvider timeProvider;

    private final boolean enforceStrictEventOrdering;

    private final EventExecutor simpleEventExecutor;
    private final Consumer<Throwable> endCallBack;

    private final EventsQueue discreteEventsQueue;
    private final EventSchedulerType type;

    private boolean isPaused = false;
    private boolean isExecuting = false;
    private boolean isStopped = false;

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
        isPaused = true;
    }

    public void unPause() {
        unPause(() -> false);
    }

    public void unPause(BooleanSupplier exitCondition) {
        if (isPaused) {
            isPaused = false;
            startExecutingEvents(exitCondition);
        }
    }

    public Cancelable scheduleDoNowDontStart(Runnable r, String description) {
        if (isStopped) {
            return null;
        }
        Event event = new Event(timeProvider.getTime(), description, r, this, false);
        discreteEventsQueue.addDoNow(event);
        return event;
    }

    @Override
    public Cancelable doNow(Runnable r, String description) {
        if (isStopped) {
            return null;
        }
        Cancelable event = scheduleDoNowDontStart(r, description);
        if (!isExecuting && !isPaused) {
            startExecutingEvents(() -> false);
        }
        return event;
    }

    @Override
    public final Cancelable doAt(double time, Runnable r, String description, boolean isDaemon) {
        if (isStopped) {
            return null;
        }
        Cancelable event = scheduleDoAtDontStart(time, r, description, isDaemon);
        if (!isExecuting && !isPaused) {
            startExecutingEvents(() -> false);
        }
        return event;
    }

    public Cancelable scheduleDoAtDontStart(double time, Runnable r, String description, boolean isDaemon) {
        if (isStopped) {
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

    private void startExecutingEvents(BooleanSupplier exitCondition) {
        executeEvents(exitCondition);
        isExecuting = false;
    }

    private void executeEvents(BooleanSupplier exitCondition) {
        Event nextEvent;
        while (!isStopped && !isPaused && !exitCondition.getAsBoolean() && (nextEvent = discreteEventsQueue.getNextEvent()) != null) {
            isExecuting = true;
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
    public void stop() {
        stop(null);
    }

    public void stop(Throwable t) {
        isStopped = true;
        discreteEventsQueue.clear();
        endCallBack.accept(t);
    }

    public boolean isStopped() {
        return isStopped;
    }

    @Override
    public TimeProvider getTimeProvider() {
        return timeProvider;
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
}
