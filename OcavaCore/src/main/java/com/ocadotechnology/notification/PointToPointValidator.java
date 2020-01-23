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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableList;

/**
 * Enforces that there is at most ONE subscribing class for any PointToPointNotification.
 * We do not check broadcasts (there may be multiple broadcasters). We check classes rather
 * than instances (we allow multiple instances of a subscriber).
 *
 * Formally, it raises an error if this weak check passes:
 *  exists sub1(A), sub2(B) in Subscriptions, A, B in Classes : sub1 != sub2 & A >= B & P2P >= A
 */
class PointToPointValidator {
    //Map from P2P notification class to subscribing class
    private final Map<Class<?>, Class<?>> subscriptions = new HashMap<>();

    PointToPointValidator() {
    }

    void validate(Object subscriber, List<Class<?>> subscribedNotifications) {
        Class<?> subscriberClass = subscriber.getClass();

        ImmutableList<Class<?>> subscribedP2PNotifications = subscribedNotifications.stream()
                .filter(PointToPointNotification.class::isAssignableFrom)
                .collect(ImmutableList.toImmutableList());

        for (Class<?> notification : subscribedP2PNotifications) {
            for (Entry<Class<?>, Class<?>> entry : subscriptions.entrySet()) {
                Class<?> otherNotification = entry.getKey();
                Class<?> otherSubscriber = entry.getValue();
                if (notification.isAssignableFrom(otherNotification)) {
                    throw new IllegalStateException(getErrorMessage(subscriberClass, otherNotification, otherSubscriber));
                } else if (otherNotification.isAssignableFrom(notification)) {
                    throw new IllegalStateException(getErrorMessage(subscriberClass, notification, otherSubscriber));
                }
            }
            subscriptions.put(notification, subscriberClass);
        }
    }

    private String getErrorMessage(Class<?> subscriberClass, Class<?> notification, Class<?> otherSubscriberClass) {
        if (subscriberClass.equals(otherSubscriberClass)) {
            return String.format("Too many P2P subscribers. PointToPointNotification %s is subscribed to twice by %s",
                    notification.getSimpleName(),
                    subscriberClass.getSimpleName());
        }
        return String.format("Too many P2P subscribers. PointToPointNotification %s is subscribed to by both %s and %s",
                notification.getSimpleName(),
                subscriberClass.getSimpleName(),
                otherSubscriberClass.getSimpleName());
    }

    void reset() {
        subscriptions.clear();
    }
}
