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

public class BusyLoopSplitRingBufferQueue implements BusyLoopQueue {
    private final BusyLoopRingBufferQueue priorityRingBufferQueue;
    private final RingBufferQueue doNowRingBuffer;

    public BusyLoopSplitRingBufferQueue(TimeProvider timeProvider, int size) {
        priorityRingBufferQueue = new BusyLoopRingBufferQueue(timeProvider, size);
        doNowRingBuffer = new RingBufferQueue(new ScheduledQueue(timeProvider), size);
    }

    @Override
    public void addToNow(Event event) {
        doNowRingBuffer.add(event);
    }

    @Override
    public void addToSchedule(Event event) {
        priorityRingBufferQueue.addToSchedule(event);
    }

    @Override
    public void remove(Event event) {
        doNowRingBuffer.remove(event);
        priorityRingBufferQueue.remove(event);
    }

    @Override
    public @CheckForNull Event getNextEvent() {
        Event doNowEvent = doNowRingBuffer.timedPoll(Double.POSITIVE_INFINITY);
        return doNowEvent != null ? doNowEvent : priorityRingBufferQueue.getNextEvent();
    }

    @Override
    public Event getNextEvent(double now) {
        Event doNowEvent = doNowRingBuffer.timedPoll(now);
        return doNowEvent != null ? doNowEvent : priorityRingBufferQueue.getNextEvent(now);
    }

    @Override
    public boolean hasOnlyDaemonEvents() {
        return priorityRingBufferQueue.hasOnlyDaemonEvents();
    }

    @Override
    public int size() {
        return doNowRingBuffer.size() + priorityRingBufferQueue.size();
    }

    @Override
    public boolean isEmptyNow() {
        return doNowRingBuffer.isEmpty();
    }

    @Override
    public @CheckForNull Event getNextNowEvent() {
        return doNowRingBuffer.timedPoll(Double.POSITIVE_INFINITY);
    }

    @Override
    public @CheckForNull Event getNextScheduledEvent(double now) {
        return priorityRingBufferQueue.getNextEvent(now);
    }
}
