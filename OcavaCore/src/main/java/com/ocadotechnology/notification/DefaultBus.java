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

/**
 * This is a test-specific implementation of the NotificationBus, which is only used when nothing has been registered
 * on the NotificationRouter.
 */
public class DefaultBus extends NotificationBus<Object> {
    private DefaultBus(Class<Object> notificationClass) {
        super(notificationClass);
    }

    private static class SingletonHolder {
        public static final DefaultBus instance = new DefaultBus(Object.class);
    }

    public static DefaultBus get() {
        return SingletonHolder.instance;
    }
    
    @Override
    protected boolean hasCorrectType(Class<?> notification) {
        return true;
    }
}
