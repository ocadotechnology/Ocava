/*
 * Copyright © 2017-2025 Ocado (Ocava)
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.google.common.util.concurrent.Runnables;
import com.ocadotechnology.notification.TestSchedulerType;
import com.ocadotechnology.time.AdjustableTimeProviderWithUnit;

/**
 * Performance test for the use of time classes in the discrete event scheduler.
 * Disabled because it is not a correctness test I want to run by default.
 */
@ParametersAreNonnullByDefault
@Disabled
class DiscreteEventSchedulerPerformanceTest {
    private static final int NUMBER_OF_EVENTS = 1_000_000;
    private final SimpleDiscreteEventScheduler scheduler = new SimpleDiscreteEventScheduler(
            new EventExecutor(),
            Runnables.doNothing(),
            TestSchedulerType.TEST_SCHEDULER_TYPE,
            new AdjustableTimeProviderWithUnit(1, TimeUnit.MILLISECONDS),
            true);

    @Test
    void runCycles() {
        runEventCycle(counter -> primitiveDoAt(scheduler, counter), "primitive worse case");
        runEventCycle(counter -> objectDoAt(scheduler, counter), "object worse case");
        runEventCycle(counter -> primitiveDynamicDoIn(scheduler, counter), "primitive mid case");
        runEventCycle(counter -> objectDynamicDoIn(scheduler, counter), "object mid case");
        runEventCycle(counter -> primitiveStaticDoIn(scheduler, counter, 100), "primitive best case");
        runEventCycle(counter -> objectStaticDoIn(scheduler, counter, Duration.ofMillis(100)), "object best case");
        runEventCycle(counter -> objectStaticDoIn(scheduler, counter, Duration.ofMillis(100)), "object best case");
        runEventCycle(counter -> primitiveStaticDoIn(scheduler, counter, 100), "primitive best case");
        runEventCycle(counter -> objectDynamicDoIn(scheduler, counter), "object mid case");
        runEventCycle(counter -> primitiveDynamicDoIn(scheduler, counter), "primitive mid case");
        runEventCycle(counter -> objectDoAt(scheduler, counter), "object worse case");
        runEventCycle(counter -> primitiveDoAt(scheduler, counter), "primitive worse case");
    }

    private void runEventCycle(Consumer<AtomicInteger> event, String label) {
        AtomicInteger counter = new AtomicInteger(NUMBER_OF_EVENTS * 10);
        // Warm up
        scheduler.doNow(() -> event.accept(counter));
        assertEquals(0, counter.get()); //Check that the loop ran as expected
        counter.set(NUMBER_OF_EVENTS);

        long start = System.nanoTime();
        scheduler.doNow(() -> event.accept(counter));
        long end = System.nanoTime();
        System.out.println("Time taken for " + NUMBER_OF_EVENTS + " events using " + label + ": " + (end - start) / 1_000_000 + "ms");
    }

    private static void primitiveDoAt(EventScheduler scheduler, AtomicInteger counter) {
        int remaining = counter.decrementAndGet();
        if (remaining <= 0) {
            return;
        }
        double time = scheduler.getTimeProvider().getTime();
        double eventTime = time + remaining;
        scheduler.doAt(eventTime, () -> primitiveDoAt(scheduler, counter));
    }

    private static void primitiveDynamicDoIn(EventScheduler scheduler, AtomicInteger counter) {
        int remaining = counter.decrementAndGet();
        if (remaining <= 0) {
            return;
        }
        scheduler.doIn(remaining, () -> primitiveDynamicDoIn(scheduler, counter));
    }

    private static void primitiveStaticDoIn(EventScheduler scheduler, AtomicInteger counter, double duration) {
        int remaining = counter.decrementAndGet();
        if (remaining <= 0) {
            return;
        }
        scheduler.doIn(duration, () -> primitiveStaticDoIn(scheduler, counter, duration));
    }

    private static void objectDoAt(EventScheduler scheduler, AtomicInteger counter) {
        int remaining = counter.decrementAndGet();
        if (remaining <= 0) {
            return;
        }
        Instant time = scheduler.getTimeProviderWithUnit().getInstant();
        Instant eventTime = time.plus(Duration.ofMillis(remaining));
        scheduler.doAt(eventTime, () -> objectDoAt(scheduler, counter));
    }

    private static void objectDynamicDoIn(EventScheduler scheduler, AtomicInteger counter) {
        int remaining = counter.decrementAndGet();
        if (remaining <= 0) {
            return;
        }
        scheduler.doIn(Duration.ofMillis(remaining), () -> objectDynamicDoIn(scheduler, counter));
    }

    private static void objectStaticDoIn(EventScheduler scheduler, AtomicInteger counter, Duration duration) {
        int remaining = counter.decrementAndGet();
        if (remaining <= 0) {
            return;
        }
        scheduler.doIn(duration, () -> objectStaticDoIn(scheduler, counter, duration));
    }

    @Test
    void runRepeatingRunnable() {
        runRepeatingRunnable(this::repeatingRunnablePrimitive, "primitive runnable");
        runRepeatingRunnable(this::repeatingRunnableObjects, "objects runnable");
        runRepeatingRunnable(this::repeatingConsumerPrimitive, "primitive consumer");
        runRepeatingRunnable(this::repeatingConsumerObjects, "objects consumer");
        runRepeatingRunnable(this::repeatingConsumerObjects, "objects consumer");
        runRepeatingRunnable(this::repeatingConsumerPrimitive, "primitive consumer");
        runRepeatingRunnable(this::repeatingRunnableObjects, "objects runnable");
        runRepeatingRunnable(this::repeatingRunnablePrimitive, "primitive runnable");
    }

    private Cancelable repeatingRunnablePrimitive(Runnable runnable, EventScheduler scheduler) {
        return RepeatingRunnable.startIn(100, 101, "Test event", runnable, scheduler);
    }

    private Cancelable repeatingRunnableObjects(Runnable runnable, EventScheduler scheduler) {
        return RepeatingRunnable.startIn(Duration.ofMillis(100), Duration.ofMillis(101), "Test event", runnable, scheduler);
    }

    private Cancelable repeatingConsumerPrimitive(Runnable runnable, EventScheduler scheduler) {
        return RepeatingRunnable.startIn(100, 101, "Test event", time -> runnable.run(), scheduler);
    }

    private Cancelable repeatingConsumerObjects(Runnable runnable, EventScheduler scheduler) {
        return RepeatingRunnable.startIn(Duration.ofMillis(100), Duration.ofMillis(101), "Test event", instant -> runnable.run(), scheduler);
    }

    private void runRepeatingRunnable(BiFunction<Runnable, EventScheduler, Cancelable> scheduleRepeatingEvent, String label) {
        AtomicInteger counter = new AtomicInteger(NUMBER_OF_EVENTS * 50);
        scheduler.pause();
        // Warm up
        Cancelable warmupEvent = scheduleRepeatingEvent.apply(() -> runEvent(counter), scheduler);
        scheduler.unPause();
        assertEquals(0, counter.get()); //Check that the loop ran as expected
        warmupEvent.cancel();

        counter.set(NUMBER_OF_EVENTS);
        scheduler.pause();
        long start = System.nanoTime();
        Cancelable mainEvent = scheduleRepeatingEvent.apply(() -> runEvent(counter), scheduler);
        scheduler.unPause();
        long end = System.nanoTime();
        System.out.println("Time taken for " + NUMBER_OF_EVENTS + " events using " + label + ": " + (end - start) / 1_000_000 + "ms");
        mainEvent.cancel();
    }

    private void runEvent(AtomicInteger counter) {
        int remaining = counter.decrementAndGet();
        if (remaining <= 0) {
            scheduler.pause();
        }
    }
}
