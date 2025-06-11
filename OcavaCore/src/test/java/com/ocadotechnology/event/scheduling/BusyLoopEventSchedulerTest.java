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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.util.concurrent.Runnables;
import com.ocadotechnology.notification.TestSchedulerType;
import com.ocadotechnology.time.TimeProvider;
import com.ocadotechnology.time.TimeProviderWithUnit;
import com.ocadotechnology.time.UtcTimeProvider;

class BusyLoopEventSchedulerTest {
    private BusyLoopEventScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new BusyLoopEventScheduler(TimeProvider.NULL, "BusyLoopEventSchedulerTest", TestSchedulerType.TEST_SCHEDULER_TYPE);
        scheduler.start();
    }

    @AfterEach
    void tearDown() {
        if (scheduler != null) {
            scheduler.stop();
        }
    }

    @Test
    void testGetQueueSize() {
        Assertions.assertEquals(0, scheduler.getQueueSize());

        scheduler.doAt(1000, Runnables.doNothing());
        Assertions.assertEquals(1, scheduler.getQueueSize());
    }

    @Test
    void testCancelledEventIsNotRemovedFromQueueUntilScheduledTime() throws InterruptedException {
        scheduler = new BusyLoopEventScheduler(new UtcTimeProvider(MILLISECONDS), "BusyLoopEventSchedulerTest", TestSchedulerType.TEST_SCHEDULER_TYPE);
        scheduler.start();

        Assertions.assertEquals(0, scheduler.getQueueSize());

        CountDownLatch cancelLatch = new CountDownLatch(1);
        CountDownLatch eventTimePassedLatch = new CountDownLatch(1);
        AtomicInteger testValue = new AtomicInteger(0);

        Cancelable eventToBeCanceled = scheduler.doIn(1000, testValue::incrementAndGet);
        scheduler.doIn(1001, eventTimePassedLatch::countDown); // Guaranteed to be scheduled after the above, will not be cancelled

        Assertions.assertEquals(2, scheduler.getQueueSize());

        scheduler.doNow(() -> {
            eventToBeCanceled.cancel();
            cancelLatch.countDown();
        });

        assertTrue(cancelLatch.await(999, MILLISECONDS));
        // Both scheduled events still exist on the queue, even though eventToBeCanceled has been canceled
        Assertions.assertEquals(2, scheduler.getQueueSize());

        assertTrue(eventTimePassedLatch.await(10, SECONDS));
        // All events have expired from the queue
        Assertions.assertEquals(0, scheduler.getQueueSize());
        // eventToBeCancelled was short-circuited due to being canceled, so it did not increment the testValue
        Assertions.assertEquals(0, testValue.get());
    }

    @Test
    void whenCreatedWithoutUnits_thenDoesNotAcceptObjectMethodCalls() {
        Assertions.assertThrows(
                TimeUnitNotSpecifiedException.class,
                () -> scheduler.doAt(Instant.ofEpochSecond(1), () -> {}, "Test event", true));
    }

    @Test
    void whenCreatedWithUnits_thenAcceptsObjectMethodCalls() {
        EventScheduler scheduler = new BusyLoopEventScheduler(
                TimeProviderWithUnit.NULL, //Also testing that TimeProviderWithUnit.NULL is correctly hiding TimeProvider.NULL
                "BusyLoopEventSchedulerTest",
                TestSchedulerType.TEST_SCHEDULER_TYPE);
        scheduler.doAt(Instant.ofEpochSecond(1), () -> {}, "Test event", true);
    }
}
