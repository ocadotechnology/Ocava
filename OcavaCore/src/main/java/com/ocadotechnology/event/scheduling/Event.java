/*
 * Copyright © 2017-2020 Ocado (Ocava)
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

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;

import com.ocadotechnology.id.IdGenerator;

public class Event implements Cancelable {
    private static final AtomicLong idGenerator = IdGenerator.getRawIdGenerator(Event.class);

    static final Comparator<Event> EVENT_COMPARATOR = (o1, o2) -> {
        if (o1.time < o2.time) {
            return -1;
        }
        if (o1.time > o2.time) {
            return 1;
        }
        return Long.compare(o1.id, o2.id);
    };

    /**
     * This is a long rather than Id<Event> for performance. It's private, so the extra type-safety of Id<Event> is
     * of limited use anyway.
     */
    private final long id = idGenerator.getAndIncrement();
    public final double time;
    public final String description;
    protected final EventSchedulerWithCanceling eventScheduler;
    private final Runnable runnable;
    private final boolean isDaemon;

    public Event(double time, String description, Runnable runnable, EventSchedulerWithCanceling eventScheduler, boolean isDaemon) {
        this.runnable = runnable;
        this.eventScheduler = eventScheduler;
        this.time = time;
        this.description = description;
        this.isDaemon = isDaemon;
    }

    public final void execute() {
        runnable.run();
    }

    @Override
    public void cancel() {
        eventScheduler.cancel(this);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    public boolean isDaemon() {
        return isDaemon;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        Event other = (Event) obj;
        return id == other.id;
    }

    @Override
    public String toString() {
        return "event [id=" + id + ", time=" + time + ", desc=" + description + "]";
    }
}
