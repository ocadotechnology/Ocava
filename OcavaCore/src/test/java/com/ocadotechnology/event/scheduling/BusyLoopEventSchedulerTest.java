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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.util.concurrent.Runnables;
import com.ocadotechnology.notification.TestSchedulerType;
import com.ocadotechnology.time.TimeProvider;

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
    void testCancelledEventIsImmediatelyRemovedFromQueue() {
        Assertions.assertEquals(0, scheduler.getQueueSize());

        Cancelable event = scheduler.doIn(Double.MAX_VALUE, Runnables.doNothing());
        Assertions.assertEquals(1, scheduler.getQueueSize());

        event.cancel();

        Assertions.assertEquals(0, scheduler.getQueueSize());
    }
}
