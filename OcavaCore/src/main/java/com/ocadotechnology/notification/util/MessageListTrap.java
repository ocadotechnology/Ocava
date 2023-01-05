/*
 * Copyright © 2017-2023 Ocado (Ocava)
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

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;
import com.ocadotechnology.event.scheduling.EventSchedulerType;
import com.ocadotechnology.notification.Notification;
import com.ocadotechnology.notification.Subscriber;

/**
 * Subscribe to the notification bus to capture a list of notifications
 * @param <T> Type of the captured notifications
 */
public class MessageListTrap<T extends Notification> implements Subscriber {
    private final Class<T> type;
    private final boolean acceptSubclasses;
    private final EventSchedulerType schedulerType;

    private final List<T> trappedNotifications = new ArrayList<>();

    private MessageListTrap(Class<T> type, boolean acceptSubclasses, EventSchedulerType schedulerType) {
        this.type = type;
        this.acceptSubclasses = acceptSubclasses;
        this.schedulerType = schedulerType;
    }

    /**
     * Create the trap and subscribe to the event bus
     * @param type Class to listen for on the Notification bus
     * @param acceptSubclasses set to true to capture subclasses
     * @param schedulerType type of Scheduler to subscribe to
     * @param <T> Type of the captured notification
     * @return new {@link MessageListTrap}
     */
    public static <T extends Notification> MessageListTrap<T> createAndSubscribe(Class<T> type, boolean acceptSubclasses, EventSchedulerType schedulerType) {
        MessageListTrap<T> messageListTrap = new MessageListTrap<>(type, acceptSubclasses, schedulerType);
        messageListTrap.subscribeForNotifications();
        return messageListTrap;
    }

    /**
     * Capture notifications from the event bus, and store notifications matching the captured class
     * @param n Notification
     */
    @Subscribe
    public void anyNotificationOfType(T n) {
        if (n.getClass() == type || (acceptSubclasses && type.isAssignableFrom(n.getClass()))) {
            trappedNotifications.add(n);
        }
    }

    /**
     * Returns an immutable copy of the trapped notifications
     * @return {@link ImmutableList} of the trapped notifications
     */
    public ImmutableList<T> getCapturedNotifications() {
        return ImmutableList.copyOf(trappedNotifications);
    }

    @Override
    public EventSchedulerType getSchedulerType() {
        return schedulerType;
    }

    /**
     * Clears the captured list of notifications
     */
    public void reset() {
        trappedNotifications.clear();
    }
}
