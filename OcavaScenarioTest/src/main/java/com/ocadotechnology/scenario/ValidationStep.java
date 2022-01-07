/*
 * Copyright © 2017-2022 Ocado (Ocava)
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

import java.util.function.Consumer;

class ValidationStep<T> extends NamedStep implements Executable {
    protected final Class<T> type;
    protected final NotificationCache notificationCache;
    final Consumer<T> consumer;

    private T lastSeen;

    ValidationStep(Class<T> type, NotificationCache notificationCache, Consumer<T> consumer) {
        super(type);
        this.type = type;
        this.notificationCache = notificationCache;
        this.consumer = consumer;
    }

    Class<T> getType() {
        return type;
    }

    @Override
    public final boolean isRequired() {
        return false;
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public void execute() {
        Object notification = notificationCache.getUnorderedNotification();
        if (notification != null && type.isAssignableFrom(notification.getClass())) {
            lastSeen = type.cast(notification);
            consumer.accept(lastSeen);
        }
    }

    @Override
    public boolean isMergeable() {
        return false;
    }

    @Override
    public void merge(Executable step) {}

    @Override
    public String info() {
        return "Validation step last saw " + lastSeen;
    }
}
