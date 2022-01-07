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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ocadotechnology.event.scheduling.BusyLoopQueue.BusyLoopQueueType;

public class BusyLoopQueueTest {
    private Event nowEvent1 = new Event(0, "TestNowEvent1", () -> {}, null, false);
    private Event nowEvent2 = new Event(0, "TestNowEvent2", () -> {}, null, false);
    private Event nowEvent3 = new Event(0, "TestNowEvent3", () -> {}, null, false);
    private Event schedEvent1 = new Event(3, "TestScheduledEvent1", () -> {}, null, false);
    private Event schedEvent2 = new Event(2, "TestScheduledEvent2", () -> {}, null, false);
    private Event schedEvent3 = new Event(0, "TestScheduledEvent3", () -> {}, null, false);
    private Event schedDaemonEvent1 = new Event(0, "TestDaemonEvent1", () -> {}, null, true);

    @Test
    public void switchingQueueTest() {
        validateQueue(BusyLoopQueueType.SwitchingQueue, 0);
    }

    @Test
    public void priorityQueueTest() {
        validateQueue(BusyLoopQueueType.PriorityQueue, 0);
    }

    @Test
    public void ringBufferQueueTest() {
        validateQueue(BusyLoopQueueType.RingBufferQueue, 0);
    }

    @Test
    public void ringBufferWithOverflowQueueTest() {
        validateQueue(BusyLoopQueueType.RingBufferQueue, 1);
    }

    @Test
    public void splitRingBufferQueueTest() {
        validateQueue(BusyLoopQueueType.SplitRingBufferQueue, 0);
    }

    public void validateQueue(BusyLoopQueueType busyLoopQueueType, int size) {
        BusyLoopQueue busyLoopQueue = BusyLoopQueue.constructQueue(busyLoopQueueType, () -> 4, size);
        validateQueueSize(busyLoopQueue, 0);
        populateQueue(busyLoopQueue);
        validateQueueSize(busyLoopQueue, 6);
        validateGetNextEvent(busyLoopQueue);
        validateQueueSize(busyLoopQueue, 0);

        busyLoopQueue = BusyLoopQueue.constructQueue(busyLoopQueueType, () -> 4, size);
        populateQueue(busyLoopQueue);
        validateRemove(busyLoopQueue);

        busyLoopQueue = BusyLoopQueue.constructQueue(busyLoopQueueType, () -> 2, size);
        populateQueue(busyLoopQueue);
        validateScheduledEvent(busyLoopQueue);

        busyLoopQueue = BusyLoopQueue.constructQueue(busyLoopQueueType, () -> 4, size);
        validateHasOnlyDaemonEvents(busyLoopQueue);
    }

    private void validateQueueSize(BusyLoopQueue busyLoopQueue, int size) {
        Assertions.assertEquals(size, busyLoopQueue.size());
    }

    private void validateHasOnlyDaemonEvents(BusyLoopQueue busyLoopQueue) {
        //empty queue
        Assertions.assertTrue(busyLoopQueue.hasOnlyDaemonEvents());
        busyLoopQueue.addToSchedule(schedDaemonEvent1);
        Assertions.assertTrue(busyLoopQueue.hasOnlyDaemonEvents());
        populateQueue(busyLoopQueue);
        Assertions.assertFalse(busyLoopQueue.hasOnlyDaemonEvents());
    }

    private void validateScheduledEvent(BusyLoopQueue busyLoopQueue) {
        Assertions.assertEquals(nowEvent1, busyLoopQueue.getNextEvent());
        Assertions.assertEquals(nowEvent2, busyLoopQueue.getNextEvent());
        Assertions.assertEquals(nowEvent3, busyLoopQueue.getNextEvent());
        Assertions.assertEquals(schedEvent3, busyLoopQueue.getNextEvent());
        Assertions.assertEquals(schedEvent2, busyLoopQueue.getNextEvent());
        Assertions.assertNull(busyLoopQueue.getNextEvent());
    }

    private void validateRemove(BusyLoopQueue busyLoopQueue) {
        busyLoopQueue.remove(nowEvent2);
        busyLoopQueue.remove(schedEvent3);
        Assertions.assertEquals(nowEvent1, busyLoopQueue.getNextEvent());
        Assertions.assertEquals(nowEvent3, busyLoopQueue.getNextEvent());
        Assertions.assertEquals(schedEvent2, busyLoopQueue.getNextEvent());
        Assertions.assertEquals(schedEvent1, busyLoopQueue.getNextEvent());
    }

    private void validateGetNextEvent(BusyLoopQueue busyLoopQueue) {
        Assertions.assertEquals(nowEvent1, busyLoopQueue.getNextEvent());
        Assertions.assertEquals(nowEvent2, busyLoopQueue.getNextEvent());
        Assertions.assertEquals(nowEvent3, busyLoopQueue.getNextEvent());
        Assertions.assertEquals(schedEvent3, busyLoopQueue.getNextEvent());
        Assertions.assertEquals(schedEvent2, busyLoopQueue.getNextEvent());
        Assertions.assertEquals(schedEvent1, busyLoopQueue.getNextEvent());
    }

    private void populateQueue(BusyLoopQueue busyLoopQueue) {
        busyLoopQueue.addToNow(nowEvent1);
        busyLoopQueue.addToNow(nowEvent2);
        busyLoopQueue.addToNow(nowEvent3);
        busyLoopQueue.addToSchedule(schedEvent1);
        busyLoopQueue.addToSchedule(schedEvent2);
        busyLoopQueue.addToSchedule(schedEvent3);
    }
}
