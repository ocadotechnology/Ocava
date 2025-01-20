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

import com.ocadotechnology.event.scheduling.EventScheduler;

public interface NotificationRouter {
    static NotificationRouter get() {
        return CrossAppNotificationRouter.get();
    }

    /**
     * A static call to broadcast a notification. Equivalent to calling:
     * <pre>
     *     NotificationRouter.get().broadcast(notification);
     * </pre>
     *
     * @see #broadcast(Notification)
     */
    static <T extends Notification> void publish(T notification) {
        get().broadcast(notification);
    }

    /**
     * A static call to broadcast multiple notifications. Equivalent to calling:
     * <pre>
     *     NotificationRouter.get().broadcast(notification1, notification2, ...);
     * </pre>
     *
     * @see #broadcast(Notification...)
     */
    static void publish(Notification... notifications) {
        get().broadcast(notifications);
    }

    /**
     * A static call to broadcast a notification created by the provided supplier. Equivalent to calling:
     * <pre>
     *     NotificationRouter.get().broadcast(concreteMessageNotificationSupplier, notificationClass);
     * </pre>
     *
     * @see #broadcast(Supplier, Class)
     */
    static <T extends Notification> void publish(Supplier<T> concreteMessageNotificationSupplier, Class<T> notificationClass) {
        get().broadcast(concreteMessageNotificationSupplier, notificationClass);
    }

    /**
     * Sends the provided notification to all registered handlers.
     *
     * Note: if the Notification is expensive to create, and it may not have any subscribers, consider using the lazy
     * implementation.
     */
    <T extends Notification> void broadcast(T notification);

    /**
     * Sends the provided notifications to all registered handlers, equivalent to calling:
     *
     * broadcast(notification1);
     * broadcast(notification2);
     * broadcast(notification3);
     * ...
     *
     * Note: if the Notifications are expensive to create, and may not have any subscribers, consider using the lazy
     * implementation.
     */
    default void broadcast(Notification... notifications) {
        for (Notification notification : notifications) {
            broadcast(notification);
        }
    }

    /**
     * Lazily sends the notification provided by the supplier to all registered handlers.  The supplier will not be
     * invoked if nothing subscribes to the declared notificationClass
     */
    <T extends Notification> void broadcast(Supplier<T> concreteMessageNotificationSupplier, Class<T> notificationClass);

    void addHandler(Subscriber handler);

    /**
     * Insertion order corresponds to Notification dispatch order.
     */
    <T> void registerExecutionLayer(EventScheduler scheduler, NotificationBus<T> notificationBus);

    /**
     * Insertion order corresponds to Notification dispatch order.
     */
    <T> void registerExecutionLayer(Broadcaster<T> newBroadcaster);

    void clearAllHandlers();
}
