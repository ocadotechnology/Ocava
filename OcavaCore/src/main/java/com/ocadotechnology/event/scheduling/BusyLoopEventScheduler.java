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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ocadotechnology.ThreadManager;
import com.ocadotechnology.event.EventUtil;
import com.ocadotechnology.event.RecoverableException;
import com.ocadotechnology.event.scheduling.BusyLoopQueue.BusyLoopQueueType;
import com.ocadotechnology.time.TimeProvider;
import com.ocadotechnology.time.TimeProviderWithUnit;

public class BusyLoopEventScheduler extends TypedEventScheduler {
    private static final Logger logger = LoggerFactory.getLogger(BusyLoopEventScheduler.class);
    private final BusyLoopQueue busyLoopQueue;
    private final TimeProvider timeProvider;
    private final String name;
    private final Set<Consumer<Throwable>> failureListeners = new HashSet<>();
    private final Set<Consumer<RecoverableException>> recoverableFailureListeners = new HashSet<>();
    private final Set<Runnable> onShutDowns = new HashSet<>();
    private final ThreadManager threadManager;
    private final boolean heartbeatMonitor;

    private final long parkDurationNanos;
    private final boolean useLowLatencyRunner;

    private volatile boolean shouldStop = false;
    private volatile long threadId;

    /**
     * @param parkDurationNanos an experimental setting to park the thread for a number of nanoseconds when idle to
     *                          prevent an idle thread from overheating the core.  It is expected that a value of ~10ns
     *                          should result in a reduction in idle core heating of 80-90% for an acceptably small
     *                          reduction in responsiveness.  However, in practice, this has been observed to create
     *                          unacceptably large latency spikes.
     * */
    public BusyLoopEventScheduler(
            TimeProvider timeProvider,
            String name,
            EventSchedulerType type,
            ThreadManager threadManager,
            boolean heartbeatMonitor,
            BusyLoopQueueType busyLoopQueueType,
            int size,
            long parkDurationNanos,
            boolean useLowLatencyRunner) {
        super(type);
        this.timeProvider = timeProvider;
        this.name = name;
        this.heartbeatMonitor = heartbeatMonitor;
        this.busyLoopQueue = BusyLoopQueue.constructQueue(busyLoopQueueType, timeProvider, size);
        this.threadManager = threadManager;

        this.parkDurationNanos = parkDurationNanos;
        this.useLowLatencyRunner = useLowLatencyRunner;
    }

    public BusyLoopEventScheduler(TimeProvider timeProvider, String name, EventSchedulerType type, ThreadManager threadManager, boolean heartbeatMonitor, long parkDurationNanos, boolean useLowLatencyRunner) {
        this(timeProvider, name, type, threadManager, heartbeatMonitor, BusyLoopQueueType.SwitchingQueue, 0, parkDurationNanos, useLowLatencyRunner);
    }

    public BusyLoopEventScheduler(TimeProvider timeProvider, String name, EventSchedulerType type) {
        this(timeProvider, name, type, () -> {}, false, BusyLoopQueueType.SwitchingQueue, 0, 0, false);
    }

    @Override
    public void cancel(Event event) {
        busyLoopQueue.remove(event);
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
    public Cancelable doNow(Runnable r, String description) {
        Event event = new Event(0, description, r, this, false);
        busyLoopQueue.addToNow(event);
        return event;
    }

    @Override
    public Cancelable doAt(double time, Runnable r, String description, boolean isDaemon) {
        Event event = new Event(time, description, r, this, isDaemon);
        busyLoopQueue.addToSchedule(event);
        return event;
    }

    public void start() {
        shouldStop = false;
        if (heartbeatMonitor) {
            addHeartbeatMonitor();
        }
        Thread thread = new Thread(this::threadStart, "BusyLoopScheduler-" + name);
        threadId = thread.getId();
        thread.start();
    }

    private void threadStart() {
        threadManager.manage();

        if (useLowLatencyRunner) {
            runLowLatencyLoop();
        } else {
            runThroughputLoop();
        }
    }

    private void runThroughputLoop() {
        // Tight loop:
        // It doesn't matter that we always call getTime (system call):
        //     if we have an event, we need to call it anyway;
        //     if we don't, then the delay is irrelevant
        // Micro optimisations:
        // 1. Using continue is faster
        // 2. Catching here is faster
        while (!shouldStop) {
            Event e = busyLoopQueue.getNextEvent();
            if (e == null) {
                continue;
            }
            try {
                e.execute();
            } catch (Throwable t) {
                threadExceptionHandler(e, t);
            }
        }
    }

    private void runLowLatencyLoop() {
        while (!shouldStop) {
            runLowLatencyNowLoop();
            runLowLatencyScheduledLoop();
        }
    }

    private void runLowLatencyNowLoop() {
        // See optimisations as for runThroughputLoop
        // 3. Separating now events from timed events is faster
        while (!shouldStop) {
            Event e = busyLoopQueue.getNextNowEvent();
            if (e == null) {
                return;
            }
            try {
                e.execute();
            } catch (Throwable t) {
                threadExceptionHandler(e, t);
            }
        }
    }

    private void runLowLatencyScheduledLoop() {
        // See optimisations as for runThroughputLoop
        while (!shouldStop && busyLoopQueue.isEmptyNow()) {
            double now = timeProvider.getTime();
            Event e = busyLoopQueue.getNextScheduledEvent(now);
            if (e != null) {
                try {
                    e.execute();
                    continue;
                } catch (Throwable t) {
                    threadExceptionHandler(e, t);
                }
            }
            LockSupport.parkNanos(parkDurationNanos);  // Does not park the thread at all if parkDurationNanos == 0
        }
    }

    private void threadExceptionHandler(Event lastEvent, Throwable t) {
        if (t instanceof IllegalStateException) {
            Throwable cause = t.getCause();
            while (cause != null) {
                if (cause instanceof RecoverableException) {
                    String message = String.format("Scheduler %s attempting to recover at %s from failure processing %s", name, EventUtil.logTime(timeProvider.getTime()), lastEvent);
                    logger.error(message, cause);  // required method is error(message, throwable).  error(message, args, throwable) doesn't exist
                    RecoverableException exception = (RecoverableException) cause;
                    try {
                        recoverableFailureListeners.forEach(l -> l.accept(exception));
                    } catch (Throwable inner) {
                        fail(lastEvent, inner);
                    }
                    break;
                }
                cause = cause.getCause();
            }
            if (cause == null) {
                fail(lastEvent, t);
            }
        } else {
            fail(lastEvent, t);
        }
    }

    private void fail(Event event, Throwable t) {
        // Defensive programming required: Things have gone arbitrarily wrong, so assume the worst:
        try {
            // It's most important to get to the call.  The args matter less
            String message = "Scheduler %s failed at %s whilst processing %s";
            try {
                double time = timeProvider.getTime();
                message = String.format(message, name, EventUtil.logTime(time), event);
            } catch (Throwable ignoreMe) {
                // ignore
            }
            // The logger method we need is error(String, Throwable).  There is no error(String, Args, Throwable)
            logger.error(message, t);
        } finally {
            try {
                failureListeners.forEach(l -> l.accept(t));
            } finally {
                onStop();
            }
        }
    }

    @Override
    public boolean hasOnlyDaemonEvents() {
        return busyLoopQueue.hasOnlyDaemonEvents();
    }

    public void registerOnShutDown(Runnable onShutDown) {
        onShutDowns.add(onShutDown);
    }

    public void registerFailureListener(Consumer<Throwable> l) {
        failureListeners.add(l);
    }

    public void registerRecoverableFailureListener(Consumer<RecoverableException> l) {
        recoverableFailureListeners.add(l);
    }

    @Override
    public void prepareToStop() {
        // We could add 'graceful' stop to this scheduler as a future improvement (see SimpleDiscreteEventScheduler)
        // nop
    }

    @Override
    public void stop() {
        // Defensive programming required: Who know why we've called stop.  Could be because the logger has failed...
        try {
            // We are throwing an exception here to see who was responsible for stopping the scheduler.
            // info method we need is info(message, throwable).  There is no method info(message, args, throwable)
            logger.info("Scheduler " + name + " stopping", new Exception("DUMMY"));
        } catch (Throwable ignoreMe) {
            // ignore
        }
        try {
            onStop();
        } finally {
            logger.info("Scheduler " + name + " was stopped");
        }
    }

    private void onStop() {
        try {
            onShutDowns.forEach(Runnable::run);
        } finally {
            shouldStop = true;
        }
    }

    public boolean isStopping() {
        return false;
    }

    @Override
    public long getThreadId() {
        return threadId;
    }

    public int getQueueSize() {
        return busyLoopQueue.size();
    }

    private void addHeartbeatMonitor() {
        double now = timeProvider.getTime();
        long heartbeat = 1_000;
        doAt(
                now + heartbeat,
                () -> {
                    logger.info("Scheduler {} heartbeat was executed with delay {}", name, timeProvider.getTime() - now - heartbeat);
                    addHeartbeatMonitor();
                },
                "Heart Beat Monitor"
        );
    }
}
