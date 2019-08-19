/*
 * Copyright © 2017 Ocado (Ocava)
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * This double queue is optimized for BusyLoopScheduler.
 * There are two queues. One belongs to external threads (externalQueue) and it is
 * synchronized. Second queue is internal to scheduler (internalQueue) and it is not synchronized.
 * As soon as internalQueue is empty we replace external queue with internal queue within synchronized block.
 *
 * Notes:
 * 1. ReentrantLock is more expensive then synchronized (java8). Do not used it here
 */
public class SwitchingQueue {
    private final List<Event> eventsToRemove = new ArrayList<>();

    // LinkedLists are very slow (18ns vs 12ns per event in total throughput tests)
    private Queue<Event> externalQueue = new ArrayDeque<>();
    private volatile Queue<Event> internalQueue =  new ArrayDeque<>();

    private volatile boolean toRemove;
    private volatile boolean bothQueuesEmpty;

    public synchronized void add(Event event) {
        externalQueue.add(event);
        bothQueuesEmpty = false;
    }

    public synchronized void remove(Event event) {
        if (bothQueuesEmpty) {
            return;
        }
        externalQueue.remove(event);
        eventsToRemove.add(event);
        toRemove = true;
    }

    public Event poll() {
        if (bothQueuesEmpty) {
            return null;
        }
        return updateQueues().poll();
    }

    public Event peek() {
        if (bothQueuesEmpty) {
            return null;
        }
        return updateQueues().peek();
    }

    private Queue<Event> updateQueues() {
        if (toRemove) {
            removeEventsFromInternalQueue();
        }
        if (internalQueue.isEmpty()) {
            return switchQueues();
        }
        return internalQueue;
    }

    private synchronized void removeEventsFromInternalQueue() {
        internalQueue.removeAll(eventsToRemove);
        eventsToRemove.clear();
        toRemove = false;
    }

    private synchronized Queue<Event> switchQueues() {
        Queue<Event> tmpSchedulerQueue = externalQueue;
        externalQueue = internalQueue;
        internalQueue = tmpSchedulerQueue;

        // We only get here if the internalQueuei is empty, so bothQueue must be empty if the external queue is also empty.
        // Also, we want to do this in a synchronized block, so the setters can't race:
        bothQueuesEmpty = tmpSchedulerQueue.isEmpty();

        return tmpSchedulerQueue;
    }

    public int size() {
        int size = internalQueue.size();
        synchronized (this) {
            size += externalQueue.size();
        }
        return size;
    }

    public boolean isEmpty() {
        return bothQueuesEmpty;
    }
}
