/*
 * Copyright © 2017-2026 Ocado (Ocava)
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

/// Enforces that there is at most ONE subscribing class for any PointToPointNotification.
/// We do not check broadcasts (there may be multiple broadcasters). We do not allow multiple instances of a subscriber.
///
/// Formally, it raises an error if this weak check passes:
///  exists sub1(A), sub2(B) in Subscriptions, A, B in Classes : sub1 != sub2 & A >= B & P2P >= A
class PointToPointValidator {
    // Map from the P2P notification class subscribed to, to the class of its subscriber
    private final Map<Class<?>, Class<?>> subscriptions = new HashMap<>();

    // Guards all access to subscriptions: a single check-then-insert needs to compare the new
    // notification against every existing subscription, so per-key atomicity is not sufficient.
    private final Object lock = new Object();

    PointToPointValidator() {
    }

    /// This method is and needs to remain ThreadSafe.
    void validate(Object subscriber, List<Class<?>> subscribedNotifications) {
        Class<?> subscriberClass = subscriber.getClass();
        synchronized (lock) {
            for (Class<?> subscribedNotification : subscribedNotifications) {
                if (PointToPointNotification.class.isAssignableFrom(subscribedNotification)) {

                    // Two subscriptions are only ambiguous at dispatch time if their notification classes are
                    // equal, or one is an ancestor of the other - i.e. some runtime notification instance could
                    // be delivered to both. Sharing a common ancestor (e.g. two sibling subtypes) is fine.
                    for (Map.Entry<Class<?>, Class<?>> existingSubscription : subscriptions.entrySet()) {
                        if (isRelated(existingSubscription.getKey(), subscribedNotification)) {
                            throw new IllegalStateException(getErrorMessage(subscriberClass, subscribedNotification, existingSubscription));
                        }
                    }

                    subscriptions.put(subscribedNotification, subscriberClass);

                    // Weak validation that no notification is both FNF and P2P
                    if (FireAndForgetNotification.class.isAssignableFrom(subscribedNotification)) {
                        throw new IllegalStateException(String.format("%s cannot be both a FireAndForgetNotification and a PointToPointNotification", subscribedNotification.getSimpleName()));
                    }
                }
            }
        }
    }

    private boolean isRelated(Class<?> a, Class<?> b) {
        return a.isAssignableFrom(b) || b.isAssignableFrom(a);
    }

    private String getErrorMessage(Class<?> newSubscriberClass, Class<?> newNotificationClass, Map.Entry<Class<?>, Class<?>> oldSubscription) {
        Class<?> oldNotificationClass = oldSubscription.getKey();
        Class<?> oldSubscriberClass = oldSubscription.getValue();

        if (newSubscriberClass.equals(oldSubscriberClass)) {
            return String.format(
                    "Too many P2P subscribers. PointToPointNotification %s is subscribed to twice by %s",
                    newNotificationClass.getSimpleName(),
                    newSubscriberClass.getSimpleName());
        }
        if (newNotificationClass.equals(oldNotificationClass)) {
            return String.format(
                    "Too many P2P subscribers. A subscriber of type %s, and one of type %s (which both listen to notification %s, which is P2P) have been registered",
                    newSubscriberClass.getSimpleName(),
                    oldSubscriberClass.getSimpleName(),
                    newNotificationClass.getSimpleName());
        }

        String ancestorDescendant = newNotificationClass.isAssignableFrom(oldNotificationClass) ?
                "descendant" :
                "ancestor";

        return String.format(
                "Too many P2P subscribers. A subscriber of type %s which listens to notifications of type %s, and one of type %s which listens to its P2P %s %s have been registered",
                newSubscriberClass.getSimpleName(),
                newNotificationClass.getSimpleName(),
                oldSubscriberClass.getSimpleName(),
                ancestorDescendant,
                oldNotificationClass.getSimpleName());
    }

    void reset() {
        synchronized (lock) {
            subscriptions.clear();
        }
    }
}
