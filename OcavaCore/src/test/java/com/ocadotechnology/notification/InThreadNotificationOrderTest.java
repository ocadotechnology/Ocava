/*
 * Copyright © 2017-2019 Ocado (Ocava)
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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Runnables;
import com.ocadotechnology.event.scheduling.EventExecutor;
import com.ocadotechnology.event.scheduling.EventSchedulerType;
import com.ocadotechnology.event.scheduling.SimpleDiscreteEventScheduler;
import com.ocadotechnology.event.scheduling.SourceSchedulerTracker;
import com.ocadotechnology.event.scheduling.SourceTrackingEventScheduler;
import com.ocadotechnology.time.AdjustableTimeProvider;

class InThreadNotificationOrderTest {

    private enum TestSchedulerType implements EventSchedulerType {
        T1, T2;
    }

    private SourceSchedulerTracker tracker = new SourceSchedulerTracker();
    private final SimpleDiscreteEventScheduler backingScheduler = new SimpleDiscreteEventScheduler(new EventExecutor(), Runnables.doNothing(), TestSchedulerType.T1, new AdjustableTimeProvider(0), true);
    private final SourceTrackingEventScheduler t1Scheduler = new SourceTrackingEventScheduler(tracker, TestSchedulerType.T1, backingScheduler);
    private final SourceTrackingEventScheduler t2Scheduler = new SourceTrackingEventScheduler(tracker, TestSchedulerType.T2, backingScheduler);

    private final NotificationRememberingService notificationRememberingServiceOnT1 = new NotificationRememberingService(TestSchedulerType.T1);
    private final NotificationRememberingService notificationRememberingServiceOnT2 = new NotificationRememberingService(TestSchedulerType.T2);

    /**
     * Configures the broadcast order in WithinAppNotificationRouter to 'CROSS_THREAD_FIRST' by setting 'SCHEDULE_CROSS_THREAD_BROADCAST_FIRST' to true.
     */
    @BeforeAll
    static void setBroadcastImplementation() {
        try {
            Field privateField = WithinAppNotificationRouter.class.getDeclaredField("SCHEDULE_CROSS_THREAD_BROADCAST_FIRST");
            privateField.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(privateField, privateField.getModifiers() & ~Modifier.FINAL);

            privateField.set(null, true);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to setup test state.");
        }
    }

    /**
     * Reverts the broadcast order in WithinAppNotificationRouter to the default (BROADCASTER_REGISTRATION_ORDER) by setting 'SCHEDULE_CROSS_THREAD_BROADCAST_FIRST' to false.
     */
    @AfterAll
    static void revertBroadcastImplementation() {
        try {
            Field privateField = WithinAppNotificationRouter.class.getDeclaredField("SCHEDULE_CROSS_THREAD_BROADCAST_FIRST");
            privateField.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(privateField, privateField.getModifiers() & Modifier.FINAL);

            privateField.set(null, false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to cleanup test state.");
        }
    }

    private void setup() {
        NotificationRouter.get().registerExecutionLayer(t1Scheduler, new TestBus(Notification.class));
        NotificationRouter.get().registerExecutionLayer(t2Scheduler, new TestBus(Notification.class));

        notificationRememberingServiceOnT1.subscribeForNotifications();
        notificationRememberingServiceOnT2.subscribeForNotifications();
    }

    @Test
    void whenSubscriberBroadcastsNotification_thenSubscribersOnOtherDiscreteEventSchedulersReceiveNotificationsInOrder() {
        setup();

        ConcreteMessageNotification notification1 = new ConcreteMessageNotification("Notification 1");
        ConcreteMessageNotification notification2 = new ConcreteMessageNotification("Notification 2");

        notificationRememberingServiceOnT1.onReceiptDo(notification1, () -> NotificationRouter.get().broadcast(notification2));
        t1Scheduler.doNow(() -> NotificationRouter.get().broadcast(notification1));

        ImmutableList<ConcreteMessageNotification> expected = ImmutableList.of(notification1, notification2);
        Assertions.assertEquals(expected, notificationRememberingServiceOnT1.getReceivedNotifications());
        Assertions.assertEquals(expected, notificationRememberingServiceOnT2.getReceivedNotifications());
    }

}
