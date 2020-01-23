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
package com.ocadotechnology.event.scheduling;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AtomicDouble;
import com.ocadotechnology.event.EventUtil;
import com.ocadotechnology.time.TimeProvider;

/**
 * A simulation-only implementation of EventScheduler which tracks which simulated thread is active in each event,
 * permitting simulated thread handover to be implemented.
 */
public class SourceTrackingEventScheduler extends TypedEventScheduler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SimpleDiscreteEventScheduler backingScheduler;

    private final SourceSchedulerTracker tracker;

    private final AtomicBoolean delayed = new AtomicBoolean(false);
    private final List<MutableCancelableHolder> delayedDoNowRunnables = new ArrayList<>();

    private final AtomicDouble delayEndTime = new AtomicDouble(Double.NaN);
    private Cancelable delayEndEvent;

    public SourceTrackingEventScheduler(SourceSchedulerTracker tracker, EventSchedulerType type, SimpleDiscreteEventScheduler backingScheduler) {
        super(type);
        this.backingScheduler = backingScheduler;
        this.tracker = tracker;
    }

    public SourceTrackingEventScheduler createSibling(EventSchedulerType type) {
        return new SourceTrackingEventScheduler(tracker, type, backingScheduler);
    }

    /**
     * Simulates a GC or computational pause in this simulated 'thread'.  If the 'thread' is currently paused, it will
     * start running again at the latest of the current and new pause end times.
     */
    public void delayExecutionUntil(double delayEndTime) {
        logger.info("Delaying execution on thread {} until {}", type, EventUtil.eventTimeToString(delayEndTime));

        delayed.set(true);

        if (this.delayEndTime.get() >= delayEndTime) {
            return;
        }

        if (delayEndEvent != null) {
            delayEndEvent.cancel();
        }

        //Use the backing scheduler so that ending the delay is not itself delayed.  Would cause an infinite loop.
        this.delayEndTime.set(delayEndTime);
        double delayStartTime = getTimeProvider().getTime();
        delayEndEvent = backingScheduler.doAt(delayEndTime, () -> delayFinished(delayStartTime));
    }

    private void delayFinished(double delayStartTime) {
        logger.info("Resuming execution on thread {}. Pause started at {}", type, EventUtil.eventTimeToString(delayStartTime));

        delayed.set(false);
        delayEndEvent = null;
        //doNow to preserve ordering.
        delayedDoNowRunnables.stream()
                .filter(h -> !h.wasCancelled)
                .forEach(this::doNow);
        delayedDoNowRunnables.clear();
    }

    @Override
    public boolean hasOnlyDaemonEvents() {
        return backingScheduler.hasOnlyDaemonEvents();
    }

    @Override
    public void cancel(Event e) {
        backingScheduler.cancel(e);
    }

    @Override
    public void stop() {
        backingScheduler.stop();
    }

    @Override
    public TimeProvider getTimeProvider() {
        return backingScheduler.getTimeProvider();
    }

    @Override
    public long getThreadId() {
        return backingScheduler.getThreadId();
    }

    @Override
    public double getMinimumTimeDelta() {
        return backingScheduler.getMinimumTimeDelta();
    }

    @Override
    public Cancelable doNow(Runnable r, String description) {
        MutableCancelableHolder cancelableHolder = new MutableCancelableHolder(r, description);
        doNow(cancelableHolder);
        return cancelableHolder;
    }

    private void doNow(MutableCancelableHolder cancelableHolder) {
        Runnable newRunnable = wrappedForDoNow(cancelableHolder);
        cancelableHolder.setCancelable(backingScheduler.doNow(newRunnable, cancelableHolder.description));
    }

    @Override
    public Cancelable doAt(double time, Runnable r, String description, boolean isDaemon) {
        MutableCancelableHolder cancelableHolder = new MutableCancelableHolder(r, description);
        doAt(time, isDaemon, cancelableHolder);
        return cancelableHolder;
    }

    private void doAt(double time, boolean isDaemon, MutableCancelableHolder cancelableHolder) {
        Runnable newRunnable = wrappedForDoAt(cancelableHolder, isDaemon);
        Cancelable cancelable = backingScheduler.doAt(time, newRunnable, cancelableHolder.description, isDaemon);
        cancelableHolder.setCancelable(cancelable);
    }

    @Override
    public boolean isThreadHandoverRequired() {
        return !type.equals(tracker.getActiveSchedulerType());
    }

    private Runnable wrappedForDoNow(MutableCancelableHolder cancelableHolder) {
        return () -> {
            tracker.setActiveSchedulerType(type);
            if (delayed.get()) {
                delayedDoNowRunnables.add(cancelableHolder);
                return;
            }
            cancelableHolder.runnable.run();
        };
    }

    private Runnable wrappedForDoAt(MutableCancelableHolder cancelableHolder, boolean isDaemon) {
        return () -> {
            tracker.setActiveSchedulerType(type);
            if (delayed.get()) {
                doAt(delayEndTime.get(), isDaemon, cancelableHolder);
                return;
            }
            cancelableHolder.runnable.run();
        };
    }

    private static final class MutableCancelableHolder implements Cancelable {
        private final Runnable runnable;
        private final String description;
        private Cancelable cancelable;
        private boolean wasCancelled = false;

        public MutableCancelableHolder(Runnable runnable, String description) {
            this.runnable = runnable;
            this.description = description;
        }

        public void setCancelable(Cancelable cancelable) {
            this.cancelable = cancelable;
        }

        @Override
        public void cancel() {
            cancelable.cancel();
            wasCancelled = true;
        }
    }
}
