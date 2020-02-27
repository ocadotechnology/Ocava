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

import java.util.function.Supplier;

import com.ocadotechnology.event.scheduling.EventScheduler;

public interface NotificationRouter {
    static NotificationRouter get() {
        return CrossAppNotificationRouter.get();
    }

    /**
     * Sends the provided notification to all registered handlers.
     *
     * Note: if the Notification is expensive to create, and it may not have any subscribers, consider using the lazy
     * implementation.
     */
    <T extends Notification> void broadcast(T notification);

    /**
     * Lazily sends the notification provided by the supplier to all registered handlers.  The supplier will not be
     * invoked if nothing subscribes to the declated notificationClass
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
