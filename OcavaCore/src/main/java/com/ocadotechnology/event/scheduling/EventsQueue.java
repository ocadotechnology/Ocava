/*
 * Copyright © 2017-2021 Ocado (Ocava)
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

/**
 * Discrete scheduler queue contains two collections of events. DoNow events are stored in insertion order collection.
 * DoAt events are sorted according to execution time and then event id.
 */
public interface EventsQueue {
    /**
     * Adds DoNow events. Those events will be executed before any DoAt events and will not modify time.
     * @param event doNow event
     */
    void addDoNow(Event event);

    /**
     * Adds DoAt events. Those events will be executed only if the DoNow collection is empty.
     * Every execution of DoAt will progress time
     * @param event DoAt event
     */
    void addDoAt(Event event);

    /**
     * Daemon events represents periodic events which are not crucial part of the simulation.
     * If we have only Daemon events we can terminate simulation.
     * @return true if all events are daemon, false otherwise.
     */
    boolean hasOnlyDaemonEvents();

    /**
     * Clear the content of DoAt and DoNow queues (for tests purpose).
     */
    void clear();

    /**
     * Cancel selected event.
     */
    void cancel(Event event);

    /**
     * @return next event from event queue, this can be a blocking method in case of distributed execution.
     */
    Event getNextEvent();

    /**
     * @return next schedule event time.
     */
    double getNextScheduledEventTime();
}
