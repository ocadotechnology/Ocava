/*
 * Copyright © 2017-2023 Ocado (Ocava)
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

/** This is a combined queue that schedules 'now' and 'timed' events within the same queue,<br>
 *  ie, it does <em>NOT</em> prioritise now events.
 */
public class BusyLoopCombinedQueue implements BusyLoopQueue {
    private final ScheduledQueue scheduledQueue;

    public BusyLoopCombinedQueue(TimeProvider timeProvider) {
        scheduledQueue = new ScheduledQueue(timeProvider);
    }

    @Override
    public synchronized void addToNow(Event event) {
        scheduledQueue.add(event);
    }

    @Override
    public synchronized void addToSchedule(Event event) {
        scheduledQueue.add(event);
    }

    @Override
    public synchronized void remove(Event event) {
        scheduledQueue.remove(event);
    }

    @Override
    public synchronized Event getNextEvent(double now) {
        return scheduledQueue.timedPoll(now);
    }

    @Override
    public synchronized Event getNextEvent() {
        return scheduledQueue.timedPoll();
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
    public synchronized Event getNextScheduledEvent(double now) {
        return scheduledQueue.timedPoll(now);
    }

    @Override
    public synchronized int size() {
        return scheduledQueue.size();
    }

    @Override
    public synchronized boolean hasOnlyDaemonEvents() {
        return scheduledQueue.hasOnlyDaemonEvents();
    }
}
