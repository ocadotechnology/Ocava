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
package com.ocadotechnology.notification;

import java.util.ArrayList;
import java.util.List;

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
        router.clearAllHandlers();
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
        router.clearAllHandlers();
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
    public void testCallingBroadcastFromTwoThreadsFails() throws InterruptedException {
        WithinAppNotificationRouter router = WithinAppNotificationRouter.get();
        router.clearAllHandlers();
        NotificationBus<Notification> bus = new TestBus();
        router.registerExecutionLayer(new NonExecutingEventScheduler(TestSchedulerType.TEST_SCHEDULER_TYPE, TimeProvider.NULL), bus);

        OuterNotificationHandler outerHandler = new OuterNotificationHandler();
        router.addHandler(outerHandler);

        List<LocalBroadcastRunnable> notifications = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                LocalBroadcastRunnable r = new LocalBroadcastRunnable(router);
                notifications.add(r);
                new Thread(r).start();
            }

            Thread.sleep(1000);
        } finally {
            boolean foundException = false;
            for (LocalBroadcastRunnable r : notifications) {
                r.stop = true;
                if (r.exception != null) {
                    Assertions.assertTrue(!foundException);
                    foundException = true;
                    Assertions.assertTrue(r.exception instanceof IllegalStateException);
                }
            }
        }
    }

    @Test
    public void testThreaded() throws InterruptedException {
        WithinAppNotificationRouter router = WithinAppNotificationRouter.get();
        NotificationBus<Notification> bus = new TestBus();
        router.registerExecutionLayer(new NonExecutingEventScheduler(TestSchedulerType.TEST_SCHEDULER_TYPE, TimeProvider.NULL), bus);

        List<LocalHandlerRunnable> handlers = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            LocalHandlerRunnable r = new LocalHandlerRunnable(router);
            handlers.add(r);
            new Thread(r).start();
        }

        LocalBroadcastRunnable r = new LocalBroadcastRunnable(router);
        new Thread(r).start();

        Thread.sleep(1000);
        r.stop = true;
        handlers.forEach(h -> h.stop = true);
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

    private static class LocalHandlerRunnable implements Runnable {
        private final WithinAppNotificationRouter router;
        public volatile boolean stop = false;

        public LocalHandlerRunnable(WithinAppNotificationRouter router) {
            this.router = router;
        }

        public void run() {
            while (!stop) {
                router.addHandler(new OuterNotificationHandler());
                try {
                    Thread.sleep(17);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class LocalBroadcastRunnable implements Runnable {
        private final WithinAppNotificationRouter router;
        public volatile boolean stop = false;
        public volatile Throwable exception = null;

        public LocalBroadcastRunnable(WithinAppNotificationRouter router) {
            this.router = router;
        }

        public void run() {
            while (!stop) {
                try {
                    router.broadcast(new TestOneNotification());
                } catch (Throwable t) {
                    exception = t;
                    break;
                }
                try {
                    Thread.sleep(13);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}