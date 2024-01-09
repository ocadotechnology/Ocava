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
package com.ocadotechnology.event.scheduling;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ocadotechnology.event.EventUtil;
import com.ocadotechnology.event.RecoverableException;
import com.ocadotechnology.time.TimeProvider;
import com.ocadotechnology.time.UtcTimeProvider;
import com.ocadotechnology.utils.Types;
import com.ocadotechnology.validation.Failer;

/**
 * A realtime event scheduler using a {@link ScheduledThreadPoolExecutor} to schedule the execution of events for a
 * given time in the future.
 */
public class ExecutorEventScheduler extends TypedEventScheduler {
    private static final Logger logger = LoggerFactory.getLogger(ExecutorEventScheduler.class);

    private static final PlaceholderScheduledFuture PLACEHOLDER_FUTURE = new PlaceholderScheduledFuture();

    private final UtcTimeProvider timeProvider;

    private final ScheduledThreadPoolExecutor executor;
    private final Set<Consumer<Throwable>> failureListeners = new HashSet<>();
    private final Set<Consumer<RecoverableException>> recoverableFailureListeners = new HashSet<>();
    private final Set<Runnable> onShutDowns = new HashSet<>();

    private final Map<Event, ScheduledFuture<?>> eventsMap = new ConcurrentHashMap<>();
    private final long threadId;

    private final AtomicBoolean failed = new AtomicBoolean(false);

    /**
     * @param timeProvider The {@link TimeProvider} that this scheduler will be based on
     * @param name A name to be associated with the thread used by this scheduler
     * @param daemon whether the executor thread should be created as a daemon thread
     * @param type the type of this scheduler (for use with execution layers
     *             - see {@link com.ocadotechnology.notification.NotificationRouter})
     * @deprecated since 6.00
     * Use {@link #ExecutorEventScheduler(UtcTimeProvider timeProvider, String name, boolean daemon, EventSchedulerType type)} instead.
     */
    @Deprecated
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "This object does not contain data that constitutes a security risk")
    public ExecutorEventScheduler(TimeProvider timeProvider, String name, boolean daemon, EventSchedulerType type) {
        this(
                Types.fromTypeOrFail(timeProvider, UtcTimeProvider.class),
                name,
                daemon,
                type);
    }

    /**
     * @param timeUnit The {@link TimeUnit} that this scheduler will run in
     * @param name A name to be associated with the thread used by this scheduler
     * @param daemon whether the executor thread should be created as a daemon thread
     * @param type the type of this scheduler (for use with execution layers
     *             - see {@link com.ocadotechnology.notification.NotificationRouter})
     */
    public ExecutorEventScheduler(TimeUnit timeUnit, String name, boolean daemon, EventSchedulerType type) {
        this(new UtcTimeProvider(timeUnit), name, daemon, type);
    }

    /**
     * @param timeUnit The {@link TimeUnit} that this scheduler will run in
     * @param name A name to be associated with the thread used by this scheduler
     * @param daemon whether the executor thread should be created as a daemon thread
     * @param type the type of this scheduler (for use with execution layers
     *             - see {@link com.ocadotechnology.notification.NotificationRouter})
     * @param removeOnCancel whether cancelled tasks should be immediately removed from the work queue
     *                       - see {@link ScheduledThreadPoolExecutor#setRemoveOnCancelPolicy}
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "This object does not contain data that constitutes a security risk")
    public ExecutorEventScheduler(TimeUnit timeUnit, String name, boolean daemon, EventSchedulerType type, boolean removeOnCancel) {
        this(new UtcTimeProvider(timeUnit), name, daemon, type, removeOnCancel);
    }

    /**
     * @param timeProvider The {@link TimeProvider} that this scheduler will be based on
     * @param name A name to be associated with the thread used by this scheduler
     * @param daemon whether the executor thread should be created as a daemon thread
     * @param type the type of this scheduler (for use with execution layers
     *             - see {@link com.ocadotechnology.notification.NotificationRouter})
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "This object does not contain data that constitutes a security risk")
    public ExecutorEventScheduler(UtcTimeProvider timeProvider, String name, boolean daemon, EventSchedulerType type) {
        this(timeProvider, name, daemon, type, false);
    }

    /**
     * @param timeProvider The {@link TimeProvider} that this scheduler will be based on
     * @param name A name to be associated with the thread used by this scheduler
     * @param daemon whether the executor thread should be created as a daemon thread
     * @param type the type of this scheduler (for use with execution layers
     *             - see {@link com.ocadotechnology.notification.NotificationRouter})
     * @param removeOnCancel whether cancelled tasks should be immediately removed from the work queue
     *                       - see {@link ScheduledThreadPoolExecutor#setRemoveOnCancelPolicy}
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "This object does not contain data that constitutes a security risk")
    public ExecutorEventScheduler(UtcTimeProvider timeProvider, String name, boolean daemon, EventSchedulerType type, boolean removeOnCancel) {
        super(type);
        this.timeProvider = timeProvider;

        // This class still offers only single threaded execution - event ordering, concurrency issues etc
        executor = new ScheduledThreadPoolExecutor(1,
                new ThreadFactoryBuilder()
                        .setNameFormat("ExecutorEventScheduler-" + name + "-%d")
                        .setDaemon(daemon)
                        .build());

        executor.setRemoveOnCancelPolicy(removeOnCancel);

        //The delay is 0 so the TimeUnit is irrelevant.
        ScheduledFuture<Long> future = executor.schedule(() -> Thread.currentThread().getId(), 0, TimeUnit.MILLISECONDS);
        try {
            threadId = future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw Failer.fail("ThreadId was not initialized");
        }
    }

    /**
     * Schedule a task to be executed as soon as possible. No guarantees are provided as to when this task will
     * actually be executed.  <br><em>Or even if it will be executed (if the scheduler has already shutdown).</em>
     *
     * @param r The Runnable encapsulating the work to be scheduled now
     * @param description A description of the task to be scheduled
     * @return A Cancelable allowing the task to be cancelled
     */
    @Override
    public Cancelable doNow(Runnable r, String description) {
        double now = timeProvider.getTime();
        return doAt(now, now, r, description, false);
    }

    /**
     * Schedule a task to be executed at a specific time. The task will be executed as soon as possible after the
     * specified time. No guarantees are provided as to when after the specified time the task will be executed.
     * <br><em>Or even if it will be executed (if the scheduler has already shutdown).</em>
     *
     * @param time The time, in milliseconds, to execute the task. This time should be consistent with that provided
     *             by the TimeProvider given at construction.
     * @param r The Runnable encapsulating the work to be scheduled
     * @param description A description of the task to be scheduled
     * @return A Cancelable allowing the task to be cancelled
     */
    @Override
    public Cancelable doAt(double time, Runnable r, String description, boolean isDaemon) {
        // We don't really care if time < now
        // THe executor will clamp the delay to zero (can't run something in the past).
        return doAt(time, timeProvider.getTime(), r, description, isDaemon);
    }

    private Cancelable doAt(double eventTime, double now, Runnable r, String description, boolean isDaemon) {
        Event event = new Event(eventTime, description, r, this, isDaemon);
        try {
            // NOTE - The scheduled event can complete before we've put a Future in eventsMap, so we first put a placeholder future then replace it (if present, atomic operation)
            eventsMap.put(event, PLACEHOLDER_FUTURE);
            ScheduledFuture<?> future = executor.schedule(() -> executeEvent(event), (long) ((event.time - now) / timeProvider.getMillisecondMultiplier()), TimeUnit.MILLISECONDS);
            eventsMap.replace(event, PLACEHOLDER_FUTURE, future);
        } catch (RejectedExecutionException e) {
            eventsMap.remove(event, PLACEHOLDER_FUTURE);
            if (!executor.isShutdown() && failed.compareAndSet(false, true)) {
                logger.error("Failed to schedule event [{}]", event, e);
            }
        }
        return event;
    }

    @Override
    public boolean hasOnlyDaemonEvents() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancel(Event e) {
        ScheduledFuture<?> future = eventsMap.get(e);
        if (future != null) {
            future.cancel(false);
            eventsMap.remove(e);
        }
    }

    @Override
    public void prepareToStop() {
        // nop
    }

    /**
     * Stop the EventScheduler. This method will attempt to stop any actively executing tasks, and will stop any
     * scheduled tasks. It will wait for 1 second to allow any currently executing tasks to complete before returning.
     */
    @Override
    public void stop() {
        onShutDowns.forEach(Runnable::run);
        executor.shutdownNow();
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            logger.error("Interrupted while waiting for tasks to complete after a shutdown");
        }
        eventsMap.clear();
    }

    @Override
    public boolean isStopping() {
        return false;
    }

    public boolean isStopped() {
        return eventsMap.isEmpty() && executor.isTerminated();
    }

    @Override
    public TimeProvider getTimeProvider() {
        return timeProvider;
    }

    private void executeEvent(Event event) {
        double currentTime = timeProvider.getTime(); // Note that event.time is usually 1970 because of eventScheduler.doNow()
        try {
            event.execute();
        } catch (IllegalStateException e) {
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof RecoverableException) {
                    logger.error("Scheduler attempting to recover at {} from failure processing {}", EventUtil.logTime(currentTime), event, cause);
                    RecoverableException exception = (RecoverableException) cause;
                    recoverableFailureListeners.forEach(l -> l.accept(exception));
                    break;
                }
                cause = cause.getCause();
            }
            if (cause == null) {
                logger.error("Scheduler failed at {} whilst processing {}", EventUtil.logTime(currentTime), event, e);
                failureListeners.forEach(l -> l.accept(e));
                stop();
            }
        } catch (Throwable t) {
            logger.error("Scheduler failed at {} whilst processing {}", EventUtil.logTime(currentTime), event, t);
            failureListeners.forEach(l -> l.accept(t));
            stop();
        } finally {
            eventsMap.remove(event);
        }
    }

    @Override
    public long getThreadId() {
        return threadId;
    }

    public int getQueueSize() {
        return executor.getQueue().size();
    }

    public void registerFailureListener(Consumer<Throwable> failureListener) {
        failureListeners.add(failureListener);
    }

    public void registerRecoverableFailureListener(Consumer<RecoverableException> failureListener) {
        recoverableFailureListeners.add(failureListener);
    }

    public void registerOnShutDown(Runnable onShutDown) {
        onShutDowns.add(onShutDown);
    }

    @ParametersAreNonnullByDefault
    @SuppressFBWarnings(value = "EQ_COMPARETO_USE_OBJECT_EQUALS", justification = "Equals method is correct for this singleton class and compareTo throws UnsupportedOperationException")
    private static class PlaceholderScheduledFuture implements ScheduledFuture<Void> {

        @Override
        public long getDelay(TimeUnit unit) {
            return 0;
        }

        /**
         * This object is only ever placed into a ConcurrentHashMap as a value and so compareTo is never called
         */
        @Override
        public int compareTo(Delayed o) {
            throw new UnsupportedOperationException("compareTo should never be called for this placeholder class");
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public Void get() {
            return null;
        }

        @Override
        public Void get(long timeout, TimeUnit unit) {
            return null;
        }
    }
}

