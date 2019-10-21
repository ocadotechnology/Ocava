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

import java.util.Collection;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.ocadotechnology.time.TimeProvider;

public class NonExecutingEventScheduler extends TypedEventScheduler {

    private final TimeProvider timeProvider;
    private final TreeSet<Event> events;

    public NonExecutingEventScheduler(EventSchedulerType schedulerType, TimeProvider timeProvider) {
        super(schedulerType);
        this.timeProvider = timeProvider;
        this.events = new TreeSet<>(Event.EVENT_COMPARATOR);
    }

    @Override
    public TimeProvider getTimeProvider() {
        return timeProvider;
    }

    @Override
    public Cancelable doNow(Runnable r, String description) {
        return doAt(timeProvider.getTime(), r, description, false);
    }

    @Override
    public Cancelable doAt(double time, Runnable r, String description, boolean isDaemon) {
        Event event = new Event(time, description, r, this, isDaemon);
        events.add(event);
        return event;
    }

    @Override
    public long getThreadId() {
        return Thread.currentThread().getId();
    }

    @Override
    public boolean hasOnlyDaemonEvents() {
        return events.stream().allMatch(Event::isDaemon);
    }

    @Override
    public void cancel(Event e) {
        events.remove(e);
    }

    @Override
    public void stop() {
        // Don't clear pending events as this is a test class (use reset() instead)
    }

    public void executeAllEvents() {
        executeEvents(e -> true);
    }

    public void executeOverdueEvents() {
        double now = timeProvider.getTime();
        executeEvents(e -> e.time <= now);
    }

    private void executeEvents(Predicate<Event> selector) {
        // Events may self-cancel upon execution, so we must use a copy of events
        Collection<Event> eventsToExecute = events.stream()
                .filter(selector)
                .collect(Collectors.toList());
        eventsToExecute.forEach(Event::execute);
        events.removeAll(eventsToExecute);
    }

    public void reset() {
        events.clear();
    }
}
