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

import java.util.function.Consumer;

import com.ocadotechnology.time.TimeProvider;

public interface EventScheduler {

    double ONE_CLOCK_CYCLE = 0.001;

    boolean hasOnlyDaemonEvents();

    void stop();

    TimeProvider getTimeProvider();

    default Cancelable doNow(Runnable r) {
        return doNow(r, r.getClass().getName());
    }

    default Cancelable doAt(double time, Runnable r) {
        return doAt(time, r, r.getClass().getName());
    }

    default Cancelable doAtOrNow(double time, Runnable r) {
        return doAtOrNow(time, r, r.getClass().getName());
    }

    Cancelable doNow(Runnable r, String description);

    default Cancelable doAt(double time, Runnable r, String description) {
        return doAt(time, r, description, false);
    }

    /**
     * Creates a Cancelable Event using the provided Runnable, and glags the Event as a 'daemon' task.  Does NOT imply
     * any repetition on the event. Use {@link RepeatingRunnable} to create repeating events.
     *
     * @param time - the time of the event
     * @param r - the action to perform at the indicated time
     * @param description - Human-readable String description for the task.  Important for debugging.
     * @return the Cancellable event created.
     */
    default Cancelable doAtDaemon(double time, Runnable r, String description) {
        return doAt(time, r, description, true);
    }

    Cancelable doAt(double time, Runnable r, String description, boolean isDaemon);

    long getThreadId();

    EventSchedulerType getType();

    default Cancelable doIn(double delay, Runnable r, String description) {
        return doIn(delay, t -> r.run(), description);
    }

    default Cancelable doIn(double delay, Consumer<Double> eventTimeConsumingAction, String description) {
        double eventTime = getTimeProvider().getTime() + delay;
        return doAt(eventTime, () -> eventTimeConsumingAction.accept(eventTime), description);
    }

    default Cancelable doInDaemon(double delay, Runnable r, String description) {
        return doInDaemon(delay, t -> r.run(), description);
    }

    default Cancelable doInDaemon(double delay, Consumer<Double> eventTimeConsumingAction, String description) {
        double eventTime = getTimeProvider().getTime() + delay;
        return doAtDaemon(eventTime, () -> eventTimeConsumingAction.accept(eventTime), description);
    }

    default Cancelable doAtOrNow(double time, Runnable r, String description) {
        return doAtOrNow(time, t -> r.run(), description);
    }

    default Cancelable doAtOrNow(double time, Consumer<Double> eventTimeConsumingAction, String description) {
        double now = getTimeProvider().getTime();
        if (time > now) {
            return doAt(time, () -> eventTimeConsumingAction.accept(time), description);
        } else {
            return doNow(() -> eventTimeConsumingAction.accept(now), description);
        }
    }

    default boolean isThreadHandoverRequired() {
        return Thread.currentThread().getId() != getThreadId();
    }

    default double getMinimumTimeDelta() {
        return ONE_CLOCK_CYCLE;
    }
}
