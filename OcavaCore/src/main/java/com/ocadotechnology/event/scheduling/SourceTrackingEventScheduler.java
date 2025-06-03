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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AtomicDouble;
import com.ocadotechnology.event.EventUtil;
import com.ocadotechnology.time.TimeProvider;
import com.ocadotechnology.time.TimeProviderWithUnit;

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
        delayExecutionUntil(delayEndTime, false);
    }

    /**
     * Stops this scheduler (simulated thread) from executing any tasks before dealyEndTime.<br>
     * <em>Other schedulers (simulated threads) are unaffected.</em><br><br>
     *
     * There are two return-from-this-call behaviours:<br>
     * Blocking behaviour:<br>
     *     "This method returns when the timeProvider time equals delayEndTime.
     *      No events will have run for this scheduler, but other schedulers (simulated threads)
     *      will have run through all their events up to delayEndTime.<br>
     *      Any doNow events the caller then adds will run after all overdue delayed events have run
     *      (but before any doNow events those delayed events may add).
     *      Any doAt events the caller adds will run after all delayed events
     *      (we only add doAt events in the future, so this is expected behaviour).
     *      The delayed events will only start executing once the caller event has completed."
     * <br>
     * Non-blocking behaviour:<br>
     *     "This method returns immediately (with time unchanged).
     *      No events will have run for this scheduler (or any others) as no time has passed.<br>
     *      Any doNow events the caller then adds will be run first.
     *      Any doAt events the caller adds will be run in time order (as expected),
     *      The caller can schedule events earlier than delayedEndTime (as long as they are after "now")
     *      and they will be executed in order at the maximum of delayedEndTime and their scheduled time.
     * <br><br>
     * <em>This method is reentrant safe.</em>
     */
    public void delayExecutionUntil(double delayEndTime, boolean blocking) {
        logger.info("Delaying execution on thread {}: {} until {}",
                type, (blocking ? "blocking" : "non-blocking"), EventUtil.logTime(delayEndTime));

        if (this.delayEndTime.get() >= delayEndTime) {
            // Either we're trying to delay in the past (in which case delayedEndEvent will be null and we should not delay)
            // or, we're delaying within a delay (in which case delayedEndEvent will not be null and we're already delaying)
            return;
        }

        delayed.set(true);

        if (delayEndEvent != null) {
            delayEndEvent.cancel();
        }

        this.delayEndTime.set(delayEndTime);
        double delayStartTime = getTimeProvider().getTime();
        // Use the backing scheduler so that ending the delay is not itself delayed.  Would cause an infinite loop.
        delayEndEvent = backingScheduler.doAt(delayEndTime, () -> delayFinished(delayStartTime), "Scheduler " + type + (blocking ? " blocking" : " delayed"));
        if (blocking) {
            // This causes the scheduler to run through all events up to delayEndTime and call "wrappedDoAt/Now" for each
            // which adds to the delayedDoNowRunnables.
            backingScheduler.blockEvent(() -> !delayed.get());  // exitCondition should be true when the delay is over
        }
    }

    private void delayFinished(double delayStartTime) {
        logger.info("Resuming execution on thread {}. Pause started at {}", type, EventUtil.logTime(delayStartTime));

        delayed.set(false);
        delayEndEvent = null;

        // The doNow queue will be empty (otherwise we wouldn't have run our doAt).
        // We don't want to actually run the delayed Runnables now as they'll interfere with the blocking Runnable (if any)
        // So, we can schedule them on the doNow queue to preserve ordering.
        delayedDoNowRunnables.stream()
                .filter(h -> !h.wasCancelled)
                .forEach(this::doNow);  // schedules
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
    public void prepareToStop() {
        backingScheduler.prepareToStop();
    }

    @Override
    public void stop() {
        backingScheduler.stop();
    }

    @Override
    public boolean isStopping() {
        return backingScheduler.isStopping();
    }

    @Override
    public TimeProvider getTimeProvider() {
        return backingScheduler.getTimeProvider();
    }

    @Override
    public TimeProviderWithUnit getTimeProviderWithUnit() {
        return backingScheduler.getTimeProviderWithUnit();
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
        return doNow(cancelableHolder);
    }

    private Cancelable doNow(MutableCancelableHolder cancelableHolder) {
        Runnable newRunnable = wrappedForDoNow(cancelableHolder);
        Cancelable inner = backingScheduler.doNow(newRunnable, cancelableHolder.description);
        return inner == null ? null : cancelableHolder.setCancelable(inner);
    }

    @Override
    public Cancelable doAt(double time, Runnable r, String description, boolean isDaemon) {
        MutableCancelableHolder cancelableHolder = new MutableCancelableHolder(r, description);
        return doAt(time, isDaemon, cancelableHolder);
    }

    private Cancelable doAt(double time, boolean isDaemon, MutableCancelableHolder cancelableHolder) {
        Runnable newRunnable = wrappedForDoAt(cancelableHolder, isDaemon);
        Cancelable inner = backingScheduler.doAt(time, newRunnable, cancelableHolder.description, isDaemon);
        return inner == null ? null : cancelableHolder.setCancelable(inner);
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

        public Cancelable setCancelable(Cancelable cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        @Override
        public void cancel() {
            cancelable.cancel();
            wasCancelled = true;
        }
    }
}
