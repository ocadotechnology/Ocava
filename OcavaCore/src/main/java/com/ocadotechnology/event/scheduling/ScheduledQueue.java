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

import java.util.PriorityQueue;

import javax.annotation.CheckForNull;

import com.ocadotechnology.time.TimeProvider;

public class ScheduledQueue {
    private final TimeProvider timeProvider;

    private final PriorityQueue<Event> queue = new PriorityQueue<>(15_000, Event.EVENT_COMPARATOR);

    public ScheduledQueue(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    public boolean add(Event e) {
        return queue.offer(e);
    }

    public void remove(Event event) {
        queue.remove(event);
    }

    // If there is a bin, this code is faster.  If not, we don't care about a small additional cost
    // (NB: offer(e) where e is the smallest element is O(1), not O(log n), ie is similar cost to peek)
    public @CheckForNull Event timedPoll() {
        Event e = queue.poll();
        if (e != null) {
            double now = timeProvider.getTime();
            if (e.time <= now) {
                return e;
            }
            queue.offer(e);
        }
        return null;
    }

    // If there is a bin, this code is faster.  If not, we don't care about a small additional cost
    // (NB: offer(e) where e is the smallest element is O(1), not O(log n), ie is similar cost to peek)
    public @CheckForNull Event timedPoll(double now) {
        Event e = queue.poll();
        if (e != null) {
            if (e.time <= now) {
                return e;
            }
            queue.offer(e);
        }
        return null;
    }

    public int size() {
        return queue.size();
    }

    public Event peek() {
        return queue.peek();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public boolean hasOnlyDaemonEvents() {
        return queue.stream().allMatch(Event::isDaemon);
    }

    public void addAll(Iterable<Event> eventsToAdd) {
        eventsToAdd.forEach(this::add);
    }

    public void removeAll(Iterable<Event> eventsToRemove) {
        eventsToRemove.forEach(this::remove);
    }
}
