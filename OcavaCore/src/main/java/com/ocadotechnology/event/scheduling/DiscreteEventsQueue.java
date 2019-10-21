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

import java.util.LinkedList;
import java.util.Queue;
import java.util.TreeSet;

import com.google.common.base.Preconditions;

public class DiscreteEventsQueue implements EventsQueue {
    private final TreeSet<Event> scheduledEvents = new TreeSet<>(Event.EVENT_COMPARATOR);
    private final Queue<Event> doNowEvents = new LinkedList<>();

    @Override
    public void addDoNow(Event event) {
        doNowEvents.add(event);
    }

    @Override
    public void addDoAt(Event event) {
        scheduledEvents.add(event);
    }

    @Override
    public boolean hasOnlyDaemonEvents() {
        return doNowEvents.isEmpty() && scheduledEvents.stream().allMatch(Event::isDaemon);
    }

    @Override
    public void clear() {
        doNowEvents.clear();
        scheduledEvents.clear();
    }

    @Override
    public void cancel(Event event) {
        Preconditions.checkNotNull(event, "Null event cannot be cancelled");
        doNowEvents.remove(event);
        scheduledEvents.remove(event);
    }

    @Override
    public Event getNextEvent() {
        if (hasDoNow()) {
            return pollDoNow();
        }
        if (hasDoAt()) {
            return pollDoAt();
        }
        return null;
    }

    @Override
    public double getNextScheduledEventTime() {
        if (hasDoNow()) {
            return doNowEvents.peek().time;
        }
        return nextDoAtTime();
    }

    protected double nextDoAtTime() {
        return hasDoAt() ? scheduledEvents.first().time : Double.NaN;
    }

    protected boolean hasDoNow() {
        return !doNowEvents.isEmpty();
    }

    protected boolean hasDoAt() {
        return !scheduledEvents.isEmpty();
    }

    protected Event pollDoNow() {
        return doNowEvents.poll();
    }

    protected Event pollDoAt() {
        return scheduledEvents.pollFirst();
    }
}
