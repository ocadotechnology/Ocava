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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.eventbus.Subscribe;
import com.ocadotechnology.event.scheduling.EventSchedulerType;

public class NotificationRememberingService implements Subscriber {
    private final List<ConcreteMessageNotification> receivedNotifications = new ArrayList<>();
    private final HashMap<ConcreteMessageNotification, Runnable> notificationTriggers = new HashMap<>();
    private final EventSchedulerType schedulerType;

    public NotificationRememberingService(EventSchedulerType schedulerType) {
        this.schedulerType = schedulerType;
    }

    @Subscribe
    public void concreteMessage(ConcreteMessageNotification n) {
        receivedNotifications.add(n);
        if (notificationTriggers.containsKey(n)) {
            notificationTriggers.remove(n).run();
        }
    }

    public List<ConcreteMessageNotification> getReceivedNotifications() {
        return receivedNotifications;
    }

    public void onReceiptDo(ConcreteMessageNotification notification, Runnable action) {
        notificationTriggers.put(notification, action);
    }

    @Override
    public EventSchedulerType getSchedulerType() {
        return schedulerType;
    }

    public void clear() {
        receivedNotifications.clear();
        notificationTriggers.clear();
    }
}
