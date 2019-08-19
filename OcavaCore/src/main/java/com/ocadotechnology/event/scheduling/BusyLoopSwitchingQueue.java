/*
 * Copyright © 2017 Ocado (Ocava)
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

import com.ocadotechnology.time.TimeProvider;

public class BusyLoopSwitchingQueue implements BusyLoopQueue {
    private final SwitchingQueue switchingQueue = new SwitchingQueue();
    private final SynchronizedScheduledQueue busyLoopPriorityQueue;

    public BusyLoopSwitchingQueue(TimeProvider timeProvider) {
        // A synchronized queue is fine here (an implementation using cas was slightly faster
        // (but far more complex due to memory minimisation))
        // Very low contention was measured (< 1e7 calls).
        busyLoopPriorityQueue = new SynchronizedScheduledQueue(timeProvider);
    }

    @Override
    public void addToNow(Event event) {
        switchingQueue.add(event);
    }

    @Override
    public void addToSchedule(Event event) {
        busyLoopPriorityQueue.addToSchedule(event);
    }

    @Override
    public void remove(Event event) {
        switchingQueue.remove(event);
        busyLoopPriorityQueue.remove(event);
    }

    @Override
    public Event getNextEvent(double now) {
        Event nextEvent = switchingQueue.poll();
        if (nextEvent == null) {
            return busyLoopPriorityQueue.timedPoll(now);
        }
        return nextEvent;
    }

    @Override
    public Event getNextEvent() {
        Event nextEvent = switchingQueue.poll();
        if (nextEvent == null) {
            return busyLoopPriorityQueue.timedPoll();
        }
        return nextEvent;
    }

    @Override
    public boolean isEmptyNow() {
        return switchingQueue.isEmpty();
    }

    @Override
    public Event getNextNowEvent() {
        return switchingQueue.poll();
    }

    @Override
    public Event getNextScheduledEvent(double now) {
        return busyLoopPriorityQueue.timedPoll(now);
    }

    @Override
    public int size() {
        return switchingQueue.size() + busyLoopPriorityQueue.size();
    }

    @Override
    public boolean hasOnlyDaemonEvents() {
        return busyLoopPriorityQueue.hasOnlyDaemonEvents();
    }
}
