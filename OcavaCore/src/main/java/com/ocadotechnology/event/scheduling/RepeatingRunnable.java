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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class RepeatingRunnable implements Runnable {
    private final double time;
    private final double period;
    private final String description;
    private final Consumer<Double> timeConsumingAction;
    protected final EventScheduler eventScheduler;
    private final boolean isDaemon;
    private final AtomicBoolean canceled;
    private final boolean fixedDelay;

    public static Cancelable startIn(double delay, double period, String description, Consumer<Double> timeConsumingAction, EventScheduler eventScheduler) {
        return startAt(eventScheduler.getTimeProvider().getTime() + delay, period, description, timeConsumingAction, eventScheduler);
    }

    public static Cancelable startAt(double time, double period, String description, Consumer<Double> timeConsumingAction, EventScheduler eventScheduler) {
        return start(time, period, description, timeConsumingAction, eventScheduler, false, false);
    }

    public static Cancelable startAtDaemon(double time, double period, String description, Runnable r, EventScheduler eventScheduler) {
        return startAtDaemon(time, period, description, t -> r.run(), eventScheduler);
    }

    public static Cancelable startAtDaemon(double time, double period, String description, Consumer<Double> timeConsumingAction, EventScheduler eventScheduler) {
        return start(time, period, description, timeConsumingAction, eventScheduler, true, false);
    }

    public static Cancelable startInDaemon(double delay, double period, String description, Runnable r, EventScheduler eventScheduler) {
        return startInDaemon(delay, period, description, eventTime -> r.run(), eventScheduler);
    }

    public static Cancelable startInDaemon(double delay, double period, String description, Consumer<Double> timeConsumingAction, EventScheduler eventScheduler) {
        return startAtDaemon(eventScheduler.getTimeProvider().getTime() + delay, period, description, timeConsumingAction, eventScheduler);
    }

    public static Cancelable startInWithFixedDelay(double delay, double period, String description, Consumer<Double> timeConsumingAction, EventScheduler eventScheduler) {
        return startAtWithFixedDelay(eventScheduler.getTimeProvider().getTime() + delay, period, description, timeConsumingAction, eventScheduler);
    }

    public static Cancelable startAtWithFixedDelay(double time, double period, String description, Consumer<Double> timeConsumingAction, EventScheduler eventScheduler) {
        return start(time, period, description, timeConsumingAction, eventScheduler, false, true);
    }

    public static Cancelable startAtDaemonWithFixedDelay(double time, double period, String description, Runnable r, EventScheduler eventScheduler) {
        return startAtDaemonWithFixedDelay(time, period, description, t -> r.run(), eventScheduler);
    }

    public static Cancelable startAtDaemonWithFixedDelay(double time, double period, String description, Consumer<Double> timeConsumingAction, EventScheduler eventScheduler) {
        return start(time, period, description, timeConsumingAction, eventScheduler, true, true);
    }

    public static Cancelable startInDaemonWithFixedDelay(double delay, double period, String description, Runnable r, EventScheduler eventScheduler) {
        return startInDaemonWithFixedDelay(delay, period, description, eventTime -> r.run(), eventScheduler);
    }

    public static Cancelable startInDaemonWithFixedDelay(double delay, double period, String description, Consumer<Double> timeConsumingAction, EventScheduler eventScheduler) {
        return startAtDaemonWithFixedDelay(eventScheduler.getTimeProvider().getTime() + delay, period, description, timeConsumingAction, eventScheduler);
    }

    private static Cancelable start(double time, double period, String description, Consumer<Double> timeConsumingAction, EventScheduler eventScheduler, boolean isDaemon, boolean fixedDelay) {
        AtomicBooleanCancelable atomicBooleanCancelable = new AtomicBooleanCancelable();
        repeat(time, period, description, timeConsumingAction, eventScheduler, isDaemon, atomicBooleanCancelable.cancelled, fixedDelay);
        return atomicBooleanCancelable;
    }

    private static void repeat(
            double time,
            double period,
            String description,
            Consumer<Double> timeConsumingAction,
            EventScheduler eventScheduler,
            boolean isDaemon,
            AtomicBoolean canceled,
            boolean fixedDelay) {
        RepeatingRunnable repeatingRunnable = new RepeatingRunnable(time, period, description, timeConsumingAction, eventScheduler, isDaemon, canceled, fixedDelay);

        if (isDaemon) {
            eventScheduler.doAtDaemon(time, repeatingRunnable, description);
        } else {
            eventScheduler.doAt(time, repeatingRunnable, description);
        }
    }

    private RepeatingRunnable(
            double time,
            double period,
            String description,
            Consumer<Double> timeConsumingAction,
            EventScheduler eventScheduler,
            boolean isDaemon,
            AtomicBoolean canceled,
            boolean fixedDelay) {

        this.time = time;
        this.period = period;
        this.description = description;
        this.timeConsumingAction = timeConsumingAction;
        this.eventScheduler = eventScheduler;
        this.isDaemon = isDaemon;
        this.canceled = canceled;
        this.fixedDelay = fixedDelay;
    }

    @Override
    public void run() {
        if (canceled.get()) {
            return;
        }
        timeConsumingAction.accept(time);

        double next = fixedDelay ? eventScheduler.getTimeProvider().getTime() + period : time + period;
        repeat(next, period, description, timeConsumingAction, eventScheduler, isDaemon, canceled, fixedDelay);
    }

    private static class AtomicBooleanCancelable implements Cancelable {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        AtomicBooleanCancelable() {
        }

        @Override
        public void cancel() {
            cancelled.set(true);
        }
    }

}
