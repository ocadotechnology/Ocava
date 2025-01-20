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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces that there is at most ONE subscribing class for any PointToPointNotification.
 * We do not check broadcasts (there may be multiple broadcasters). We do not allow multiple instances of a subscriber.
 *
 * Formally, it raises an error if this weak check passes:
 *  exists sub1(A), sub2(B) in Subscriptions, A, B in Classes : sub1 != sub2 & A >= B & P2P >= A
 */
class PointToPointValidator {
    //Map from P2P notification class to subscribing class
    private final Map<Class<?>, Subscription> subscriptions = new ConcurrentHashMap<>();

    private static final class Subscription {
        final Class<?> subscriberClass;
        final Class<?> notificationClass;

        private Subscription(Class<?> subscriberClass, Class<?> notificationClass) {
            this.subscriberClass = subscriberClass;
            this.notificationClass = notificationClass;
        }
    }

    PointToPointValidator() {
    }

    /** This method is and needs to remain ThreadSafe. */
    void validate(Object subscriber, List<Class<?>> subscribedNotifications) {
        Class<?> subscriberClass = subscriber.getClass();
        for (Class<?> subscribedNotification : subscribedNotifications) {
            if (PointToPointNotification.class.isAssignableFrom(subscribedNotification)) {
                for (Class<?> supertypeClass : getP2PSupertypes(subscribedNotification)) {
                    Subscription newSubscription = new Subscription(subscriberClass, subscribedNotification);
                    Subscription oldSubscription = subscriptions.putIfAbsent(supertypeClass, newSubscription);
                    if (oldSubscription != null) {
                        throw new IllegalStateException(getErrorMessage(newSubscription, oldSubscription));
                    }
                }
            }
        }
    }

    private List<Class<?>> getP2PSupertypes(Class<?> clazz) {
        List<Class<?>> classesToCheck = new ArrayList<>();
        classesToCheck.add(clazz);

        for (int i = 0; i < classesToCheck.size(); i++) {
            Class<?> next = classesToCheck.get(i);

            Class<?> superclass = next.getSuperclass();
            addIfClassImplementsP2P(superclass, classesToCheck);

            for (Class<?> inter : next.getInterfaces()) {
                addIfClassImplementsP2P(inter, classesToCheck);
            }
        }

        return classesToCheck;
    }

    private void addIfClassImplementsP2P(Class<?> clazz, List<Class<?>> classesToCheck) {
        if (clazz != null && classImplementsP2P(clazz) && !classesToCheck.contains(clazz)) {
            classesToCheck.add(clazz);
        }
    }

    private boolean classImplementsP2P(Class<?> clazz) {
        return PointToPointNotification.class.isAssignableFrom(clazz) && !clazz.equals(PointToPointNotification.class);
    }

    private String getErrorMessage(Subscription newSubscription, Subscription oldSubscription) {
        if (newSubscription.subscriberClass.equals(oldSubscription.subscriberClass)) {
            return String.format(
                    "Too many P2P subscribers. PointToPointNotification %s is subscribed to twice by %s",
                    newSubscription.notificationClass.getSimpleName(),
                    newSubscription.subscriberClass.getSimpleName());
        }
        if (newSubscription.notificationClass.equals(oldSubscription.notificationClass)) {
            return String.format(
                    "Too many P2P subscribers. A subscriber of type %s, and one of type %s (which both listen to notification %s, which is P2P) have been registered",
                    newSubscription.subscriberClass.getSimpleName(),
                    oldSubscription.subscriberClass.getSimpleName(),
                    newSubscription.notificationClass.getSimpleName());
        }

        String ancestorDescendant = newSubscription.notificationClass.isAssignableFrom(oldSubscription.notificationClass) ?
                "descendant" :
                "ancestor";

        return String.format(
                "Too many P2P subscribers. A subscriber of type %s which listens to notifications of type %s, and one of type %s which listens to its P2P %s %s have been registered",
                newSubscription.subscriberClass.getSimpleName(),
                newSubscription.notificationClass.getSimpleName(),
                oldSubscription.subscriberClass.getSimpleName(),
                ancestorDescendant,
                oldSubscription.notificationClass.getSimpleName());
    }

    void reset() {
        subscriptions.clear();
    }
}
