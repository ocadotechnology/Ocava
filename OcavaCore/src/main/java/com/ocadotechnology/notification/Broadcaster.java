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
package com.ocadotechnology.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.event.scheduling.EventSchedulerType;

class Broadcaster<T> {
    private static final Logger logger = LoggerFactory.getLogger(Broadcaster.class);

    private final EventScheduler eventScheduler;
    private final NotificationBus<T> notificationBus;

    Broadcaster(EventScheduler eventScheduler, NotificationBus<T> notificationBus, EventSchedulerType type) {
        this.eventScheduler = eventScheduler;
        this.notificationBus = notificationBus;
    }

    /**
     * Expensive lookup to see whether a service is registered to handle a notification.
     *
     * @return true if any service is registered that listens to the notification with the broadcasters notification bus;
     * otherwise false.
     */
    boolean isNotificationRegistered(Class<?> notification) {
        return notificationBus.isNotificationRegistered(notification);
    }

    /**
     * Shortcut for checking whether a broadcaster can handle a notification, for example this could be implemented
     * as a check on the interfaces the notification implements.
     *
     * @return true if the notification has the right type to be handled by the broadcaster; otherwise false.
     */
    boolean canHandleNotification(Class<?> notification) {
        return notificationBus.canHandleNotification(notification);
    }

    /**
     * @return whether thread handover was required
     */
    @SuppressWarnings("unchecked")
    boolean broadcast(Object notification) {
        if (eventScheduler.isThreadHandoverRequired()) {
            // Optimization: do not use String.format in event name - too expensive
            eventScheduler.doNow(() -> notificationBus.broadcast((T) notification), "Broadcasting event across thread");
            return true;
        } else {
            notificationBus.broadcast((T) notification);
            return false;
        }
    }

    void clearAllHandlers() {
        notificationBus.clearAllHandlers();
    }

    void addHandler(Object handler) {
        if (eventScheduler.isThreadHandoverRequired()) {
            logger.warn("Handler {} should be registered from scheduler {} not from thread: {}.", handler.getClass().getSimpleName(), eventScheduler.getType(), Thread.currentThread().getName());
        }
        //can not be executed with Scheduler.doNow (some messages can be skipped).
        //Multi-thread data visibility is guaranteed.
        notificationBus.addHandler(handler);
    }

    public EventSchedulerType getSchedulerType() {
        return eventScheduler.getType();
    }
}
