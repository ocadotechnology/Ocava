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
package com.ocadotechnology.notification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.eventbus.Subscribe;
import com.ocadotechnology.event.scheduling.EventSchedulerType;
import com.ocadotechnology.event.scheduling.NonExecutingEventScheduler;
import com.ocadotechnology.time.TimeProvider;

public class NotificationBusTest {

    @AfterEach
    public void cleanUpNotificationRouter() {
        NotificationRouter.get().clearAllHandlers();
    }

    @Test
    public void testIsNotificationRegistered() {
        NotificationBus<Notification> bus = new TestBus();
        NotificationHandler handler = new NotificationHandler();

        bus.addHandler(handler);
        Assertions.assertTrue(bus.isNotificationRegistered(Notification.class));
        Assertions.assertTrue(bus.isNotificationRegistered(TestOneNotification.class));
        Assertions.assertTrue(bus.hasCorrectType(Notification.class));
        Assertions.assertTrue(bus.hasCorrectType(TestOneNotification.class));

        bus.broadcast(new TestOneNotification());
        Assertions.assertTrue(handler.handled);
    }

    /** Check that if we fire a notification, then add a handler for its superclass, then the handler
     *  will receive the notification.
     */
    @Test
    public void testDelayedAddHandler() {
        WithinAppNotificationRouter router = WithinAppNotificationRouter.get();
        NotificationBus<Notification> bus = new TestBus();
        router.registerExecutionLayer(new NonExecutingEventScheduler(TestSchedulerType.TEST_SCHEDULER_TYPE, TimeProvider.NULL), bus);
        NotificationHandler handler = new NotificationHandler();

        router.broadcast(new TestOneNotification());
        Assertions.assertFalse(handler.handled);
        router.addHandler(handler);
        router.broadcast(new TestOneNotification());
        Assertions.assertTrue(handler.handled);
    }

    /** Check that adding an intermediate handler does interfere when later adding a base handler. */
    @Test
    public void testComplexDelayedAddHandler() {
        WithinAppNotificationRouter router = WithinAppNotificationRouter.get();
        NotificationBus<Notification> bus = new TestBus();
        router.registerExecutionLayer(new NonExecutingEventScheduler(TestSchedulerType.TEST_SCHEDULER_TYPE, TimeProvider.NULL), bus);
        NotificationHandler handler = new NotificationHandler();
        OuterNotificationHandler outerHandler = new OuterNotificationHandler();

        router.broadcast(new TestOneNotification());
        router.addHandler(outerHandler);
        router.broadcast(new TestTwoNotification());
        Assertions.assertFalse(handler.handled);
        Assertions.assertTrue(outerHandler.handled);
        outerHandler.handled = false;
        router.addHandler(handler);
        router.broadcast(new TestOneNotification());
        Assertions.assertTrue(handler.handled);
        Assertions.assertTrue(outerHandler.handled);
    }

    @Test
    public void testCallingBroadcastFromAtLeastTwoThreadsFails() throws InterruptedException {
        WithinAppNotificationRouter router = WithinAppNotificationRouter.get();
        NotificationBus<Notification> bus = new TestBus();
        router.registerExecutionLayer(new NonExecutingEventScheduler(TestSchedulerType.TEST_SCHEDULER_TYPE, TimeProvider.NULL), bus);

        OuterNotificationHandler outerHandler = new OuterNotificationHandler();
        router.addHandler(outerHandler);

        int numberOfBogusThreads = 10;  // should be at least 1
        List<LocalBroadcastRunnable> notifications = new ArrayList<>();
        try {
            for (int i = 0; i < numberOfBogusThreads + 1; i++) {
                LocalBroadcastRunnable r = new LocalBroadcastRunnable(router);
                notifications.add(r);
                new Thread(r).start();
            }

            for (LocalBroadcastRunnable r : notifications) {
                r.waitForRunning();
            }
            Thread.sleep(50);

            for (LocalBroadcastRunnable r : notifications) {
                r.stop = true;
            }
        } finally {
            boolean success = false;
            int exceptionCount = 0;
            for (LocalBroadcastRunnable r : notifications) {
                Throwable t = r.waitForCompletion();
                if (t != null) {
                    ++exceptionCount;
                    Assertions.assertTrue(t instanceof IllegalStateException);
                } else {
                    Assertions.assertFalse(success, "At most one thread should succeed");
                    success = true;
                }
            }
            Assertions.assertTrue(success, "At least one thread should succeed");
            Assertions.assertEquals(numberOfBogusThreads, exceptionCount, "All bogus threads should have failed");
        }
    }

    @Test
    public void testWhenTwoThreadsBroadcast_thenTheSecondFails() throws InterruptedException {
        WithinAppNotificationRouter router = WithinAppNotificationRouter.get();
        NotificationBus<Notification> bus = new TestBus();
        router.registerExecutionLayer(new NonExecutingEventScheduler(TestSchedulerType.TEST_SCHEDULER_TYPE, TimeProvider.NULL), bus);

        OuterNotificationHandler outerHandler = new OuterNotificationHandler();
        router.addHandler(outerHandler);

        AtomicReference<Throwable> threadThrowable = new AtomicReference<>();
        Thread t1 = new Thread(() -> {
            try {
                router.broadcast(new TestOneNotification());
            } catch (Throwable t) {
                threadThrowable.set(t);
            }
        });
        t1.start();
        t1.join();

        Assertions.assertNull(threadThrowable.get(), "First broadcast should be fine");

        Thread t2 = new Thread(() -> {
            try {
                router.broadcast(new TestTwoNotification());
            } catch (Throwable t) {
                threadThrowable.set(t);
            }
        });
        t2.start();
        t2.join();

        Assertions.assertNotNull(threadThrowable.get(), "Second broadcast should fail");
        Assertions.assertTrue(threadThrowable.get() instanceof IllegalStateException);
    }

    @Test
    public void testWhenTwoThreadsBroadcast_thenTheFirstAlwaysSucceedsAndTheSecondAlwaysFails() throws InterruptedException {
        WithinAppNotificationRouter router = WithinAppNotificationRouter.get();
        NotificationBus<Notification> bus = new TestBus();
        router.registerExecutionLayer(new NonExecutingEventScheduler(TestSchedulerType.TEST_SCHEDULER_TYPE, TimeProvider.NULL), bus);

        OuterNotificationHandler outerHandler = new OuterNotificationHandler();
        router.addHandler(outerHandler);

        TwoThreadTestBroadcaster firstBroadcaster = new TwoThreadTestBroadcaster(router);
        new Thread(firstBroadcaster::runExpectingSuccess).start();

        firstBroadcaster.waitForCount(10);  // Ensure that thread 1 calls broadcast at least once *before* starting thread 2

        TwoThreadTestBroadcaster secondBroadcaster = new TwoThreadTestBroadcaster(router);
        new Thread(secondBroadcaster::runExpectingFailure).start();;

        secondBroadcaster.waitForCount(100);  // Ensure thread 2 has called broadcast at least once
        firstBroadcaster.waitForCount(firstBroadcaster.broadcastCount + 100);  // Make sure we're still running thread 1

        firstBroadcaster.waitToStop();
        secondBroadcaster.waitToStop();
        Assertions.assertFalse(firstBroadcaster.failed, "Should never throw as first caller");
        Assertions.assertFalse(secondBroadcaster.failed, "Should never succeed as second caller");
    }

    private static class TwoThreadTestBroadcaster {
        private final NotificationRouter router;

        public volatile boolean failed;
        public volatile int broadcastCount;

        private volatile boolean mustStop;
        private volatile boolean hasCompleted;

        private TwoThreadTestBroadcaster(NotificationRouter router) {
            this.router = router;
        }

        public void runExpectingSuccess() {
            try {
                while (!mustStop) {
                    ++broadcastCount;  // ++ ok as only writer
                    router.broadcast(new TestOneNotification());
                }
            } catch (Throwable t) {
                failed = true;
            }
            hasCompleted = true;
        }

        public void runExpectingFailure() {
            while (!mustStop) {
                try {
                    ++broadcastCount;  // ++ ok as only writer
                    router.broadcast(new TestTwoNotification());
                    failed = true;

                } catch (Throwable t) {
                    ;  // expected
                }
            }
            hasCompleted = true;
        }

        public void waitForCount(int count) throws InterruptedException {
            while (broadcastCount  < count) {
                Thread.sleep(1);
            }
        }

        public void waitToStop() throws InterruptedException {
            mustStop = true;
            while (!hasCompleted) {
                Thread.sleep(1);
            }
        }
    }

    @Test
    void testThrowsIllegalArgument_whenNotificationMissedFromSubscriber() {
        NotificationBus<Notification> bus = new TestBus();
        assertThatThrownBy(() -> bus.addHandler(new Subscriber() {

            @Subscribe
            public void invalidHandler() {
            }

            @Override
            public EventSchedulerType getSchedulerType() {
                return TestSchedulerType.TEST_SCHEDULER_TYPE;
            }
        }))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("invalidHandler");
    }

    @Test
    void testThrowsIllegalArgument_whenSubscriberParameterNotANotification() {
        NotificationBus<Notification> bus = new TestBus();
        assertThatThrownBy(() -> bus.addHandler(new Subscriber() {

            @Subscribe
            public void invalidHandler(String notANotification) {
            }

            @Override
            public EventSchedulerType getSchedulerType() {
                return TestSchedulerType.TEST_SCHEDULER_TYPE;
            }
        }))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("invalidHandler")
        .hasMessageContaining("String");
    }

    private static class TestBus extends NotificationBus<Notification> {
        public TestBus() {
            super(Notification.class);
        }

        @Override
        protected boolean hasCorrectType(Class<?> notification) {
            // Matches the way we sub-class:
            return isNotificationRegistered(notification);
        }
    }

    private static class NotificationHandler implements Subscriber {
        public boolean handled = false;

        @Subscribe
        public void handle(Notification n) {
            handled = true;
        }

        @Override
        public void startService() {
            throw new UnsupportedOperationException();
        }

        @Override
        public EventSchedulerType getSchedulerType() {
            return TestSchedulerType.TEST_SCHEDULER_TYPE;
        }
    }

    private static class OuterNotificationHandler implements Subscriber {
        public boolean handled = false;

        @Subscribe
        public void handle(OuterNotification n) {
            handled = true;
        }

        @Override
        public void startService() {
            throw new UnsupportedOperationException();
        }

        @Override
        public EventSchedulerType getSchedulerType() {
            return TestSchedulerType.TEST_SCHEDULER_TYPE;
        }
    }

    private static class TestOneNotificationHandler {
        public boolean handled = false;

        @Subscribe
        public void handle(TestOneNotification n) {
            handled = true;
        }
    }

    private static class TestTwoNotificationHandler {
        public boolean handled = false;

        @Subscribe
        public void handle(TestTwoNotification n) {
            handled = true;
        }
    }

    public static class TestOneNotification  implements OuterNotification {}

    public static class TestTwoNotification  implements OuterNotification {}

    public interface OuterNotification extends Notification {}

    private static class LocalBroadcastRunnable implements Runnable {
        private final WithinAppNotificationRouter router;
        public volatile boolean stop = false;
        public volatile boolean started = false;
        public volatile boolean stopped = false;
        public volatile Throwable exception = null;

        public LocalBroadcastRunnable(WithinAppNotificationRouter router) {
            this.router = router;
        }

        public void run() {
            started = true;
            do {
                try {
                    Thread.sleep(13);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    router.broadcast(new TestOneNotification());
                } catch (Throwable t) {
                    exception = t;
                    break;
                }
            } while (!stop);  // must have at least 1 iteration
            stopped = true;
        }

        public void waitForRunning() throws InterruptedException {
            while (!started) {
                Thread.sleep(1);
            }
        }

        public Throwable waitForCompletion() throws InterruptedException {
            stop = true;
            while (!stopped) {
                Thread.sleep(4);
            }
            return exception;
        }
    }
}