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

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;
import com.ocadotechnology.event.scheduling.EventSchedulerType;
import com.ocadotechnology.notification.Notification;
import com.ocadotechnology.notification.Subscriber;

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

    public static <T extends Notification> MessageListTrap<T> createAndSubscribe(Class<T> type, boolean acceptSubclasses, EventSchedulerType schedulerType) {
        MessageListTrap<T> messageListTrap = new MessageListTrap<>(type, acceptSubclasses, schedulerType);
        messageListTrap.subscribeForNotifications();
        return messageListTrap;
    }
    
    @Subscribe
    public void anyNotificationOfType(T n) {
        if (n.getClass() == type || (acceptSubclasses && type.isAssignableFrom(n.getClass()))) {
            trappedNotifications.add(n);
        }
    }
    
    public ImmutableList<T> getCapturedNotifications() {
        return ImmutableList.copyOf(trappedNotifications);
    }

    @Override
    public EventSchedulerType getSchedulerType() {
        return schedulerType;
    }
}
