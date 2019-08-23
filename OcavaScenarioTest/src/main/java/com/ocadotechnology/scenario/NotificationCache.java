/*
 * Copyright © 2017 Ocado (Ocava)
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

import java.util.LinkedList;
import java.util.List;

public class NotificationCache extends Cleanable {
    private List<Class<?>> knownNotifications = new LinkedList<>();
    private Object notification;
    // we have to keep both as there are cases where we have unordered steps check value A in notification and ordered step wait for value B in the same notification
    private Object unorderedNotification;

    public void addKnownNotification(Class<?> notificationClass) {
        this.knownNotifications.add(notificationClass);
    }

    public void set(Object notification) {
        this.notification = notification;
        this.unorderedNotification = notification;
    }

    public Object getNotificationAndReset() {
        Object lastNotification = notification;
        notification = null;
        return lastNotification;
    }

    public Object getUnorderedNotification() {
        return unorderedNotification;
    }

    public boolean knownNotification(Object notification) {
        return knownNotifications.stream().anyMatch(c -> c.isAssignableFrom(notification.getClass()));
    }

    public void resetUnorderedNotification() {
        unorderedNotification = null;
    }

    public List<Class<?>> getKnownNotifications() {
        return knownNotifications;
    }

    @Override
    public void clean() {
        knownNotifications = new LinkedList<>();
        notification = null;
        unorderedNotification = null;
    }
}
