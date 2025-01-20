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
package com.ocadotechnology.notification.util;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;
import com.ocadotechnology.event.scheduling.EventSchedulerType;
import com.ocadotechnology.notification.Notification;
import com.ocadotechnology.notification.Subscriber;
import com.ocadotechnology.time.TimeProvider;

public class TimedMessageListTrap<T extends Notification> implements Subscriber {
    private final TimeProvider timeProvider;
    private final Class<T> type;
    private final EventSchedulerType schedulerType;

    private final List<TimedMessage<T>> trappedNotifications = new ArrayList<>();

    private TimedMessageListTrap(Class<T> type, TimeProvider timeProvider, EventSchedulerType schedulerType) {
        this.type = type;
        this.timeProvider = timeProvider;
        this.schedulerType = schedulerType;
    }

    public static <T extends Notification> TimedMessageListTrap<T> createAndSubscribe(Class<T> type, TimeProvider timeProvider, EventSchedulerType schedulerType) {
        TimedMessageListTrap<T> timedMessageListTrap = new TimedMessageListTrap<>(type, timeProvider, schedulerType);
        timedMessageListTrap.subscribeForNotifications();
        return timedMessageListTrap;
    }

    @Subscribe
    public void anyNotificationOfType(T n) {
        if (n.getClass() != type) {
            return;
        }
        trappedNotifications.add(new TimedMessage<>(n, timeProvider.getTime()));
    }

    public ImmutableList<TimedMessage<T>> getCapturedNotifications() {
        return ImmutableList.copyOf(trappedNotifications);
    }

    @Override
    public EventSchedulerType getSchedulerType() {
        return schedulerType;
    }

    public static class TimedMessage<T> {
        public final T msg;
        public final double time;

        public TimedMessage(T msg, double time) {
            this.msg = msg;
            this.time = time;
        }
    }
}
