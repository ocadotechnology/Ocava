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

import javax.annotation.CheckForNull;

import com.ocadotechnology.time.TimeProvider;
import com.ocadotechnology.validation.Failer;

public interface BusyLoopQueue {
    /**
     * This is thread safe for any thread
     */
    void addToNow(Event event);

    /**
     * This is thread safe for any thread
     */
    void addToSchedule(Event event);

    /**
     * This is thread safe for any thread
     */
    void remove(Event event);

    /**
     * This method can be only access by scheduler
     */
    @CheckForNull Event getNextEvent();

    /**
     * This method can be only access by scheduler
     */
    @CheckForNull Event getNextEvent(double now);

    /**
     * This method can be only access by scheduler
     */
    boolean hasOnlyDaemonEvents();

    /**
     * This method can be only access by scheduler
     */
    int size();

    /**
     * Will a call to {@link #getNextNowEvent()} return null?<br>
     * This method can be only access by scheduler
     */
    boolean isEmptyNow();

    /**
     * Return the first event supplied to {@link #addToNow(Event)}} not already returned by
     * {@link #getNextEvent()}, {@link #getNextScheduledEvent(double)} or {@link #getNextNowEvent()}}.<br>
     * This method can be only access by scheduler
     */
    @CheckForNull Event getNextNowEvent();

    /**
     * Return the first event supplied to {@link #addToNow(Event)}} not already returned by
     * {@link #getNextEvent()}, {@link #getNextEvent(double)} or {@link #getNextScheduledEvent(double)} ()}}.<br>
     * This method can be only access by scheduler
     */
    @CheckForNull Event getNextScheduledEvent(double now);

    enum BusyLoopQueueType {
        SwitchingQueue,
        PriorityQueue,
        RingBufferQueue,
        SplitRingBufferQueue,
    }

    static BusyLoopQueue constructQueue(BusyLoopQueueType busyLoopQueueType, TimeProvider timeProvider, int size) {
        switch (busyLoopQueueType) {
            case SwitchingQueue:
                return new BusyLoopSwitchingQueue(timeProvider);
            case PriorityQueue:
                return new BusyLoopCombinedQueue(timeProvider);
            case RingBufferQueue:
                return new BusyLoopRingBufferQueue(timeProvider, size);
            case SplitRingBufferQueue:
                return new BusyLoopSplitRingBufferQueue(timeProvider, size);

            default:
                throw Failer.fail("Unknown BusyLoopQueue type %s", busyLoopQueueType);
        }
    }
}
