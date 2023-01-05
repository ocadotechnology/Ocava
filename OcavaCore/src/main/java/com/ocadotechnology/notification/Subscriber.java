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

import org.slf4j.LoggerFactory;

import com.ocadotechnology.event.scheduling.EventSchedulerType;

/** Use this if you want to @Subscribe to something and use the {@link NotificationRouter}. */
public interface Subscriber {
    EventSchedulerType getSchedulerType();

    /**
     * Attaches this subscriber to the notification router.
     */
    default void subscribeForNotifications() {
        LoggerFactory.getLogger(getClass()).debug("Registering {} to receive notification updates.", this);
        NotificationRouter.get().addHandler(this);
    }

    /**
     * This method is a leftover from when this interface was called "Service". It assumes
     * that implementing classes are services that would sensibly have a method "startService".
     *
     * You should instead use {@link #subscribeForNotifications()}
     */
    @Deprecated
    default void startService() {
        LoggerFactory.getLogger(getClass()).debug("Starting service: {}", this);
        this.subscribeForNotifications();
    }
}
