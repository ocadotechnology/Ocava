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
package com.ocadotechnology.notification;

import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import com.ocadotechnology.event.scheduling.EventExecutor;
import com.ocadotechnology.event.scheduling.SimpleDiscreteEventScheduler;
import com.ocadotechnology.time.AdjustableTimeProvider;

class NotificationRouterTest {
    private static final AdjustableTimeProvider ADJUSTABLE_TIME_PROVIDER = new AdjustableTimeProvider(0);

    private final NotificationRememberingService notificationRememberingService = new NotificationRememberingService(TestSchedulerType.TEST_SCHEDULER_TYPE);

    @BeforeEach
    void setup() {
        SimpleDiscreteEventScheduler eventScheduler = new SimpleDiscreteEventScheduler(new EventExecutor(), () -> {}, TestSchedulerType.TEST_SCHEDULER_TYPE, ADJUSTABLE_TIME_PROVIDER, true);
        NotificationRouter.get().registerExecutionLayer(eventScheduler, TestBus.get());

        notificationRememberingService.subscribeForNotifications();
    }

    @Test
    void whenMessageBroadcastThatNoServiceListensToAndInfoLogLevel_thenSupplierGetIsNotCalled() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Level previousLevel = root.getLevel();
        root.setLevel(Level.INFO);
        try {
            Supplier<UnlistenedToNotification> unlistenedToNotificationSupplier = () -> {
                throw new UnsupportedOperationException();
            };
            NotificationRouter.get().broadcast(unlistenedToNotificationSupplier, UnlistenedToNotification.class);
        } finally {
            root.setLevel(previousLevel);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void whenMessageBroadcastThatServiceListensTo_thenSupplierIsCalledAndNotificationIsReceivedByService(boolean useStaticBroadcastMethod) {
        ConcreteMessageNotification concreteMessageNotification = new ConcreteMessageNotification("Hello world");
        Supplier<ConcreteMessageNotification> concreteMessageNotificationSupplier = () -> concreteMessageNotification;
        if (useStaticBroadcastMethod) {
            NotificationRouter.publish(concreteMessageNotificationSupplier, ConcreteMessageNotification.class);
        } else {
            NotificationRouter.get().broadcast(concreteMessageNotificationSupplier, ConcreteMessageNotification.class);
        }
        Assertions.assertEquals(1, notificationRememberingService.getReceivedNotifications().size());
        Assertions.assertEquals(concreteMessageNotification, notificationRememberingService.getReceivedNotifications().get(0) );
    }
    @Test
    void whenMessageBroadcastThatEavesDropperListensTo_thenSupplierIsCalledAndNotificationIsReceivedByEavesdropper() {
        ConcreteMessageNotification concreteMessageNotification = new ConcreteMessageNotification("Hello world");
        Supplier<ConcreteMessageNotification> concreteMessageNotificationSupplier = () -> concreteMessageNotification;
        RememberingConsumer<Notification> eavesdropper = new RememberingConsumer<>();
        CrossAppNotificationRouter.get().setEavesDropper(eavesdropper);
        NotificationRouter.get().broadcast(concreteMessageNotificationSupplier, ConcreteMessageNotification.class);
        Assertions.assertEquals(1, eavesdropper.get().size());
        Assertions.assertEquals(concreteMessageNotification, eavesdropper.get().get(0));
    }

    @Test
    void whenTheBroadCastIsDisabled_ThenServiceWillNotReceiveTheMessage(){
        ConcreteMessageNotification concreteMessageNotification = new ConcreteMessageNotification("Hello world");
        Supplier<ConcreteMessageNotification> concreteMessageNotificationSupplier = () -> concreteMessageNotification;
        CrossAppNotificationRouter.get().setShouldSuppressBroadcast(true);
        NotificationRouter.get().broadcast(concreteMessageNotificationSupplier, ConcreteMessageNotification.class);
        Assertions.assertEquals(0, notificationRememberingService.getReceivedNotifications().size());
    }

    @Test
    void handlersCleared_whenNewHandlerRegistered_thenExceptionForMissingBroadcasterThrown(){
        NotificationRouter.get().clearAllHandlers();
        Assertions.assertThrows(IllegalStateException.class, () -> NotificationRouter.get().addHandler(notificationRememberingService));
    }

    @Test
    void whenNoNotifications_ThenNoNotificationsBroadcast() {
        NotificationRouter.get().broadcast();
        Assertions.assertEquals(0, notificationRememberingService.getReceivedNotifications().size());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void whenNotifications_ThenNotificationsBroadcast(boolean useStaticBroadcastMethod) {
        ConcreteMessageNotification concreteMessageNotification = new ConcreteMessageNotification("Hello world");
        ConcreteMessageNotification concreteMessageNotification2 = new ConcreteMessageNotification("Goodbye!");
        if (useStaticBroadcastMethod) {
            NotificationRouter.publish(concreteMessageNotification, concreteMessageNotification2);
        } else {
            NotificationRouter.get().broadcast(concreteMessageNotification, concreteMessageNotification2);
        }
        Assertions.assertEquals(2, notificationRememberingService.getReceivedNotifications().size());
        Assertions.assertEquals(concreteMessageNotification, notificationRememberingService.getReceivedNotifications().get(0));
        Assertions.assertEquals(concreteMessageNotification2, notificationRememberingService.getReceivedNotifications().get(1));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void IfMessageBroadcastViaUnLazyBroadcastMethod_thenSupplierIsCalledAndNotificationIsReceivedByService(boolean useStaticBroadcastMethod) {
        ConcreteMessageNotification concreteMessageNotification = new ConcreteMessageNotification("Hello world");
        if (useStaticBroadcastMethod) {
            NotificationRouter.publish(concreteMessageNotification);
        } else {
            NotificationRouter.get().broadcast(concreteMessageNotification);
        }
        Assertions.assertEquals(1, notificationRememberingService.getReceivedNotifications().size());
        Assertions.assertEquals(concreteMessageNotification, notificationRememberingService.getReceivedNotifications().get(0) );
    }

    @Test
    void whenNotificationAcceptedByLogger_thenLoggerIsCalled() {
        TestLogger logger = new TestLogger();
        CrossAppNotificationRouter.get().setLogger(logger);
        ConcreteMessageNotification concreteMessageNotification = new ConcreteMessageNotification("Hello world");
        NotificationRouter.get().broadcast(concreteMessageNotification);
        Assertions.assertTrue(logger.received);
    }

    @Test
    void whenNotificationNotAcceptedByLogger_thenLoggerIsNotCalled() {
        TestLogger logger = new TestLogger();
        CrossAppNotificationRouter.get().setLogger(logger);
        NotificationRouter.get().broadcast(new UnlistenedToNotification());
        Assertions.assertFalse(logger.received);
    }

    @AfterEach
    void after() {
        notificationRememberingService.clear();
        CrossAppNotificationRouter.get().setShouldSuppressBroadcast(false);
        CrossAppNotificationRouter.get().setEavesDropper(null);
        CrossAppNotificationRouter.get().setLogger(null);
        NotificationRouter.get().clearAllHandlers();
    }

    static class TestLogger implements NotificationLogger {
        boolean received = false;

        @Override
        public boolean accepts(Class<?> notificationClass) {
            return ConcreteMessageNotification.class.isAssignableFrom(notificationClass);
        }

        @Override
        public void log(Notification n) {
            if (n instanceof ConcreteMessageNotification) {
                received = true;
            }
        }
    }
}
