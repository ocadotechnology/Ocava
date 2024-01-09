/*
 * Copyright © 2017-2024 Ocado (Ocava)
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

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.CheckForNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.AlertException;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.Sequencer;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.WaitStrategy;
import com.ocadotechnology.validation.Failer;

/**
 * This queue uses LMAX RingBuffer to exchange data between threads.
 * 'add' and 'removeAndCancel' are accessible from multiple threads (produces)
 * 'poll', 'size' and 'update' should be access by one scheduler thread (consumer)
 */
public class RingBufferQueue {
    private static final Logger logger = LoggerFactory.getLogger(RingBufferQueue.class);
    public static final int RING_BUFFER_DEFAULT_SIZE = 16 * 1024;

    private final RingBuffer<EventHolder> ringBuffer;
    private final SequenceBarrier barrier;
    //overflowRemovedQueue and overflowQueue are both synchronized on overflowQueue
    private final LinkedList<Event> overflowQueue = new LinkedList<>();
    private final LinkedList<Event> overflowRemovedQueue = new LinkedList<>();

    private final AtomicInteger ringBufferOccupancy = new AtomicInteger();
    private final ScheduledQueue queue;
    private final Sequence sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE);
    private final AtomicBoolean ringBufferOverflow = new AtomicBoolean();

    private static RingBuffer<EventHolder> createRingBuffer(int size) {
        //New waitStrategy only check for next available slot without waiting
        return RingBuffer.createMultiProducer(new EventHolderFactory(), size, new WaitStrategy() {
            @Override
            public long waitFor(long sequence, Sequence cursor, Sequence dependentSequence, SequenceBarrier barrier) throws AlertException, InterruptedException, TimeoutException {
                long availableSequence = dependentSequence.get();
                barrier.checkAlert();
                return availableSequence;
            }

            @Override
            public void signalAllWhenBlocking() {
            }
        });
    }

    public RingBufferQueue(ScheduledQueue queue, int size) {
        this.queue = queue;

        if (size < 1) {
            ringBuffer = createRingBuffer(RING_BUFFER_DEFAULT_SIZE);
        } else {
            ringBuffer = createRingBuffer(size);
        }

        barrier = ringBuffer.newBarrier();
        ringBuffer.addGatingSequences(sequence);
    }

    //multi producers access
    public void add(Event event) {
        add(event, false);
    }

    public void remove(Event event) {
        add(event, true);
    }

    public int size() {
        int size = ringBufferOccupancy.get() + queue.size();
        if (ringBufferOverflow.get()) {
            synchronized (overflowQueue) {
                return size + overflowQueue.size();
            }
        }
        return size;
    }

    private void add(Event event, boolean eventToRemove) {
        if (!checkOverflow(event, eventToRemove)) {
            tryToAddToBuffer(event, eventToRemove);
        }
    }

    private boolean checkOverflow(Event event, boolean eventToRemove) {
        if (ringBufferOverflow.get()) {
            synchronized (overflowQueue) {
                if (ringBufferOverflow.get()) {
                    addOverflow(event, eventToRemove);
                    return true;
                }
            }
        }
        return false;
    }

    private void addOverflow(Event event, boolean eventToRemove) {
        if (eventToRemove) {
            overflowRemovedQueue.add(event);
        } else {
            overflowQueue.add(event);
        }
    }

    private void tryToAddToBuffer(Event event, boolean eventToRemove) {
        try {
            //ringBuffer.next() will perform busy looping if queue is full
            long sequence = ringBuffer.tryNext();
            try {
                EventHolder eventHolder = ringBuffer.get(sequence);
                eventHolder.setEvent(event, eventToRemove);
            } finally {
                ringBufferOccupancy.incrementAndGet();
                ringBuffer.publish(sequence);
            }
        } catch (InsufficientCapacityException e) {
            synchronized (overflowQueue) {
                ringBufferOverflow.set(true);
                addOverflow(event, eventToRemove);
            }
            logger.warn("There is an insufficient space in RingBuffer, switching to overflowBuffer {}", Thread.currentThread().getName());
        }
    }

    public @CheckForNull Event timedPoll(double now) {
        update();
        return queue.timedPoll(now);
    }

    public @CheckForNull Event timedPoll() {
        update();
        return queue.timedPoll();
    }

    public boolean peek(double now) {
        update();
        Event e = queue.peek();
        return (e != null && e.time <= now);
    }

    public boolean isEmpty() {
        update();
        return queue.isEmpty();
    }

    public void update() {
        tryToRetrieveEvents();
        checkOverflow();
    }

    private void tryToRetrieveEvents() {
        long availableSequence;
        try {
            long nextSequence = sequence.get() + 1;
            availableSequence = barrier.waitFor(nextSequence);

            while (nextSequence <= availableSequence) {
                EventHolder eventHolder = ringBuffer.get(nextSequence);
                ringBufferOccupancy.decrementAndGet();
                if (eventHolder.isRemovedEvent()) {
                    queue.remove(eventHolder.getEvent());
                } else {
                    queue.add(eventHolder.getEvent());
                }
                nextSequence++;
            }

            sequence.set(availableSequence);
        } catch (AlertException | InterruptedException | TimeoutException e) {
            throw Failer.fail("RingBuffer exception: ", e);
        }
    }

    private void checkOverflow() {
        if (ringBufferOverflow.get() && ringBufferOccupancy.get() == 0) {
            synchronized (overflowQueue) {
                queue.addAll(overflowQueue);
                queue.removeAll(overflowRemovedQueue);
                overflowQueue.clear();
                overflowRemovedQueue.clear();
                ringBufferOverflow.set(false);
                logger.warn("RingBuffer has free space, OverflowBuffer has been disabled {}", Thread.currentThread().getName());
            }
        }
    }

    private static class EventHolderFactory implements EventFactory<EventHolder> {
        @Override
        public EventHolder newInstance() {
            return new EventHolder();
        }
    }

    private static class EventHolder {
        private Event event;
        private boolean removed;

        public Event getEvent () {
            return event;
        }

        public boolean isRemovedEvent() {
            return removed;
        }

        public void setEvent(Event event, boolean removed) {
            this.event = event;
            this.removed = removed;
        }
    }
}
