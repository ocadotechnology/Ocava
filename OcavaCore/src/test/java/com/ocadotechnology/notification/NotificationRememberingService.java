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
package com.ocadotechnology.notification;

import java.util.ArrayList;
import java.util.List;

import com.google.common.eventbus.Subscribe;
import com.ocadotechnology.event.scheduling.EventSchedulerType;

public class NotificationRememberingService implements Subscriber {
    private final List<ConcreteMessageNotification> receivedNotifications = new ArrayList<>();

    @Subscribe
    public void concreteMessage(ConcreteMessageNotification n) {
        receivedNotifications.add(n);
    }

    public List<ConcreteMessageNotification> getReceivedNotifications() {
        return receivedNotifications;
    }

    @Override
    public EventSchedulerType getSchedulerType() {
        return TestSchedulerType.TEST_SCHEDULER_TYPE;
    }

    public void clear() {
        receivedNotifications.clear();
    }
}
