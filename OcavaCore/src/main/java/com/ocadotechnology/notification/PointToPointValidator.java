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
package com.ocadotechnology.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

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
    private final Map<Class<?>, Class<?>> subscriptions = new ConcurrentHashMap<>();

    PointToPointValidator() {
    }

    /** This method is and needs to remain ThreadSafe. */
    void validate(Object subscriber, List<Class<?>> subscribedNotifications) {
        Class<?> subscriberClass = subscriber.getClass();
        subscribedNotifications.stream()
                .filter(PointToPointNotification.class::isAssignableFrom)
                .flatMap(this::streamP2PSupertypes)
                .forEach(classImplementingP2P -> {
                    Class<?> previousSubscriberClass = subscriptions.putIfAbsent(classImplementingP2P, subscriberClass);
                    if (previousSubscriberClass != null) {
                        throw new IllegalStateException(getErrorMessage(subscriberClass, classImplementingP2P, previousSubscriberClass));
                    }
                });
    }

    private Stream<Class<?>> streamP2PSupertypes(Class<?> clazz) {
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

        return classesToCheck.stream();
    }

    private void addIfClassImplementsP2P(Class<?> clazz, List<Class<?>> classesToCheck) {
        if (clazz != null && classImplementsP2P(clazz) && !classesToCheck.contains(clazz)) {
            classesToCheck.add(clazz);
        }
    }

    private boolean classImplementsP2P(Class<?> clazz) {
        return PointToPointNotification.class.isAssignableFrom(clazz) && !clazz.equals(PointToPointNotification.class);
    }

    private String getErrorMessage(Class<?> subscriberClass, Class<?> notification, Class<?> otherSubscriberClass) {
        if (subscriberClass.equals(otherSubscriberClass)) {
            return String.format("Too many P2P subscribers. PointToPointNotification %s (or one of its superclasses) is subscribed to twice by %s",
                    notification.getSimpleName(),
                    subscriberClass.getSimpleName());
        }
        return String.format("Too many P2P subscribers. PointToPointNotification %s (or one of its superclasses) is subscribed to by both %s and %s",
                notification.getSimpleName(),
                subscriberClass.getSimpleName(),
                otherSubscriberClass.getSimpleName());
    }

    void reset() {
        subscriptions.clear();
    }
}
