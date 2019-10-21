/*
 * Copyright © 2017-2019 Ocado (Ocava)
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

public class BusyLoopRingBufferQueue implements BusyLoopQueue {
    private final RingBufferQueue ringBufferQueue;
    private final ScheduledQueue scheduledQueue;

    public BusyLoopRingBufferQueue(TimeProvider timeProvider, int size) {
        scheduledQueue = new ScheduledQueue(timeProvider);
        ringBufferQueue = new RingBufferQueue(scheduledQueue, size);
    }

    @Override
    public void addToNow(Event event) {
        ringBufferQueue.add(event);
    }

    @Override
    public void addToSchedule(Event event) {
        ringBufferQueue.add(event);
    }

    @Override
    public void remove(Event event) {
        ringBufferQueue.remove(event);
    }

    @Override
    public @CheckForNull Event getNextEvent() {
        return ringBufferQueue.timedPoll();
    }

    @Override
    public @CheckForNull Event getNextEvent(double now) {
        return ringBufferQueue.timedPoll(now);
    }

    public void updateRingBuffer() {
        ringBufferQueue.update();
    }

    @Override
    public boolean hasOnlyDaemonEvents() {
        updateRingBuffer();
        return scheduledQueue.hasOnlyDaemonEvents();
    }

    @Override
    public int size() {
        return ringBufferQueue.size();
    }

    @Override
    public boolean isEmptyNow() {
        return true;
    }

    @Override
    public @CheckForNull Event getNextNowEvent() {
        return null;
    }

    @Override
    public @CheckForNull Event getNextScheduledEvent(double now) {
        return getNextEvent(now);
    }
}
