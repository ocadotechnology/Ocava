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
import com.ocadotechnology.notification.NotificationRouter;
import com.ocadotechnology.notification.Subscriber;
import com.ocadotechnology.time.TimeProvider;

public class TimedMessageListTrap<T> implements Subscriber {
    private final TimeProvider timeProvider;
    private final Class<T> type;

    private final List<TimedMessage<T>> trappedNotifications = new ArrayList<>();

    public TimedMessageListTrap(Class<T> type, TimeProvider timeProvider) {
        NotificationRouter.get().addHandler(this);
        this.type = type;
        this.timeProvider = timeProvider;
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
        return null;
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
