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
package com.ocadotechnology.scenario;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.reflect.TypeToken;

@ParametersAreNonnullByDefault
public class NotificationCache extends Cleanable {
    private final Set<Class<?>> knownNotifications = new LinkedHashSet<>();
    @CheckForNull
    private Object notification;
    // we have to keep both as there are cases where we have unordered steps check value A in notification and ordered step wait for value B in the same notification
    @CheckForNull
    private Object unorderedNotification;

    public void addKnownNotification(Class<?> notificationClass) {
        this.knownNotifications.add(notificationClass);
    }

    public void set(Object notification) {
        this.notification = notification;
        this.unorderedNotification = notification;
    }

    @CheckForNull
    public Object getNotificationAndReset() {
        Object lastNotification = notification;
        notification = null;
        return lastNotification;
    }

    @CheckForNull
    public Object getUnorderedNotification() {
        return unorderedNotification;
    }

    public boolean knownNotification(Object notification) {
        Class<?> clazz = notification.getClass();

        if (knownNotifications.contains(clazz)) {
            return true;
        }

        for (Class<?> superType : getSuperTypes(clazz)) {
            if (knownNotifications.contains(superType)) {
                knownNotifications.add(clazz);

                return true;
            }
        }

        return false;
    }

    private static <T> Set<Class<? super T>> getSuperTypes(Class<T> clazz) {
        return TypeToken.of(clazz).getTypes().rawTypes();
    }

    public void resetUnorderedNotification() {
        unorderedNotification = null;
    }

    public List<Class<?>> getKnownNotifications() {
        return List.copyOf(knownNotifications);
    }

    @Override
    public void clean() {
        knownNotifications.clear();
        notification = null;
        unorderedNotification = null;
    }
}
