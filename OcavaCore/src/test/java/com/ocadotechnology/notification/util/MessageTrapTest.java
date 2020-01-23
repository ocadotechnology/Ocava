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
package com.ocadotechnology.notification.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ocadotechnology.notification.Notification;
import com.ocadotechnology.notification.NotificationRouter;

public class MessageTrapTest {
    private static class TestNotification  implements Notification {}

    @Test
    public void getCapture() {
        MessageTrap<TestNotification> trap = new MessageTrap<>(TestNotification.class);
        NotificationRouter.get().broadcast(new TestNotification());
        Assertions.assertTrue(trap.getCapture().isPresent(), "Expected a Notification");
    }

    @Test
    public void verifyNotificationNotBroadcast_noBroadcast() {
        MessageTrap.verifyNotificationNotBroadcast(TestNotification.class, () -> {});
    }

    @Test
    public void verifyNotificationNotBroadcast_withBroadcast() {
        try {
            MessageTrap.verifyNotificationNotBroadcast(TestNotification.class, () -> NotificationRouter.get().broadcast(new TestNotification()));
            Assertions.fail("Expected to capture Notification");
        } catch (AssertionError ignored) {
        }
    }

    @AfterEach
    public void after() {
        NotificationRouter.get().clearAllHandlers();
    }

}