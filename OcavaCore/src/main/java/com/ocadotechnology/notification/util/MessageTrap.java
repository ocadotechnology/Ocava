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
package com.ocadotechnology.notification.util;

import java.util.Optional;

import com.google.common.eventbus.Subscribe;
import com.ocadotechnology.event.scheduling.EventSchedulerType;
import com.ocadotechnology.notification.Notification;
import com.ocadotechnology.notification.Subscriber;

public class MessageTrap<T extends Notification> implements Subscriber {
    private T trappedNotification;
    private final Class<T> type;
    private final EventSchedulerType schedulerType;

    private MessageTrap(Class<T> type, EventSchedulerType schedulerType) {
        this.type = type;
        this.schedulerType = schedulerType;
    }

    public static <T extends Notification> MessageTrap<T> createAndSubscribe(Class<T> type, EventSchedulerType schedulerType) {
        MessageTrap<T> messageTrap = new MessageTrap<>(type, schedulerType);
        messageTrap.subscribeForNotifications();
        return messageTrap;
    }

    @Subscribe
    public void anyNotificationOfType(T n) {
        if (!type.isAssignableFrom(n.getClass())) {
            return;
        }
        trappedNotification = n;
    }

    public Optional<T> getCapture() {
        return Optional.ofNullable(trappedNotification);
    }

    public void reset() {
        trappedNotification = null;
    }

    @Override
    public EventSchedulerType getSchedulerType() {
        return schedulerType;
    }

    public static <N extends Notification> void verifyNotificationNotBroadcast(Class<N> notificationClass, Runnable action, EventSchedulerType schedulerType) {
        MessageTrap<N> messageTrap = MessageTrap.createAndSubscribe(notificationClass, schedulerType);
        action.run();
        messageTrap.getCapture().ifPresent(n -> {
            throw new AssertionError("Unexpected notification of type [" + notificationClass.getName() + "] broadcast: " + n);
        });
    }

}
