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
package com.ocadotechnology.notification.util;

import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.MoreObjects;
import com.google.common.eventbus.Subscribe;
import com.ocadotechnology.event.scheduling.EventSchedulerType;
import com.ocadotechnology.notification.Notification;
import com.ocadotechnology.notification.Subscriber;

/**
 * Class which allows for a specific action to be executed in response to the broadcast of a particular type of
 * notification. The main purpose of this class is to be able to mimic/customise the behaviour of specific services in
 * unit tests.
 */
@ParametersAreNonnullByDefault
public final class MessageResponder<N extends Notification> implements Subscriber {

    private final Class<N> type;
    private final Consumer<? super N> action;

    private MessageResponder(Class<N> type, Consumer<? super N> action) {
        this.type = type;
        this.action = action;
    }

    public static <N extends Notification> MessageResponder<N> create(Class<N> type, Consumer<? super N> action) {
        MessageResponder<N> responder = new MessageResponder<>(type, action);
        responder.subscribeForNotifications();
        return responder;
    }

    @Override
    public EventSchedulerType getSchedulerType() {
        return null;
    }

    @Subscribe public void notificationReceived(N n) {
        if (type.isAssignableFrom(n.getClass())) {
            action.accept(n);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("type", type.getCanonicalName())
                .toString();
    }
}
