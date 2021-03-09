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
package com.ocadotechnology.indexedcache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ocadotechnology.id.Id;

public class ConcurrentAccessTest {
    private static final Id<TestState> ID_1 = Id.create(1);
    private static final Id<TestState> ID_2 = Id.create(2);

    private final IndexedImmutableObjectCache<TestState, TestState> cache = IndexedImmutableObjectCache.createHashMapBackedCache();

    @BeforeEach
    void setup() {
        cache.add(new TestState(ID_1, true, 10));
        cache.add(new TestState(ID_2, true, 13));
    }

    @Test
    void updateConcurrentlyFromSameThread_fails() {
        cache.registerStateChangeListener((oldState, newState) -> cache.delete(ID_2));

        ConcurrentModificationException e = assertThrows(ConcurrentModificationException.class, () -> cache.delete(ID_1));
        assertTrue(e.getMessage().contains(Thread.currentThread().getName()), "Error message should reference current thread");
    }

    @Test
    void queryConcurrentlyFromSameThread_passes() {
        cache.registerStateChangeListener((oldState, newState) -> cache.get(ID_2));
        cache.delete(ID_1);
    }

    @Test
    void queryFromMultipleThreads_passes() throws InterruptedException {
        testWithMultipleThreads(
                () -> cache.get(ID_1),
                () -> cache.get(ID_2),
                false);
    }

    @Test
    void updateFromMultipleThreads_fails() throws InterruptedException {
        testWithMultipleThreads(
                () -> cache.update(cache.get(ID_1), new TestState(ID_1, false, 10)),
                () -> cache.update(cache.get(ID_2), new TestState(ID_2, false, 10)),
                true);
    }

    @Test
    void updateAndQueryFromSeparateThreads_passes() throws InterruptedException {
        testWithMultipleThreads(
                () -> cache.get(ID_1),
                () -> cache.update(cache.get(ID_2), new TestState(ID_2, false, 10)),
                false);
    }

    private void testWithMultipleThreads(Runnable action1, Runnable action2, boolean expectsFailure) throws InterruptedException {
        List<CacheAccessRunnable> threads = new ArrayList<>();
        try {
            threads.add(createThread(action1));
            threads.add(createThread(action2));

            for (CacheAccessRunnable r : threads) {
                r.waitForRunning();
            }
            Thread.sleep(50);

            for (CacheAccessRunnable r : threads) {
                r.stop();
            }
        } finally {
            boolean exceptionObserved = false;
            boolean successObserved = false;
            for (CacheAccessRunnable r : threads) {
                Throwable terminatingThrowable = r.waitForCompletion();
                if (terminatingThrowable != null) {
                    assertTrue(expectsFailure, "Unexpected Exception thrown " + terminatingThrowable);
                    assertTrue(terminatingThrowable instanceof ConcurrentModificationException, "Unexpected exception type " + terminatingThrowable);
                    assertTrue(terminatingThrowable.getMessage().contains("TEST_THREAD"), "Unexpected exception message " + terminatingThrowable);

                    exceptionObserved = true;
                } else {
                    successObserved = true;
                }
            }
            assertTrue(successObserved, "At least one thread should succeed");
            assertEquals(expectsFailure, exceptionObserved, "Expected Exception not thrown");
        }
    }

    private CacheAccessRunnable createThread(Runnable action) {
        CacheAccessRunnable runnable = new CacheAccessRunnable(action);
        Thread t = new Thread(runnable);
        t.setName("TEST_THREAD");
        t.start();
        return runnable;
    }

    private static class CacheAccessRunnable implements Runnable {
        private final Runnable action;
        private boolean isLive = true;
        private boolean hasStarted = false;
        private boolean hasCompleted = false;

        private Throwable throwable = null;

        public CacheAccessRunnable(Runnable action) {
            this.action = action;
        }

        public void stop() {
            isLive = false;
        }

        public void waitForRunning() throws InterruptedException {
            while (!hasStarted) {
                Thread.sleep(1);
            }
        }

        public Throwable waitForCompletion() throws InterruptedException {
            while (!hasCompleted) {
                Thread.sleep(1);
            }
            return throwable;
        }

        @Override
        public void run() {
            hasStarted = true;
            while (isLive) {
                try {
                    action.run();
                } catch (Throwable t) {
                    this.throwable = t;
                    isLive = false;
                }
            }
            hasCompleted = true;
        }
    }
}
