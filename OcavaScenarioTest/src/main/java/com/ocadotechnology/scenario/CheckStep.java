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
package com.ocadotechnology.scenario;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

class CheckStep<T> extends NamedStep implements Executable {
    protected final Class<T> type;
    protected final NotificationCache notificationCache;
    private final boolean required;
    final Predicate<T> predicate;

    protected final AtomicBoolean finished = new AtomicBoolean(false);
    private T lastSeen;

    CheckStep(Class<T> type, NotificationCache notificationCache, boolean required, Predicate<T> predicate) {
        super(type);
        this.type = type;
        this.notificationCache = notificationCache;
        this.required = required;
        this.predicate = predicate;
    }

    CheckStep(Class<T> type, NotificationCache notificationCache, Predicate<T> predicate) {
        this(type, notificationCache, true, predicate);
    }

    Class<T> getType() {
        return type;
    }

    @Override
    public final boolean isRequired() {
        return required;
    }

    @Override
    public boolean isFinished() {
        return finished.get();
    }

    public T getLastSeen() {
        return lastSeen;
    }

    @Override
    public void execute() {
        Object notification = getNotification();
        if (notification != null && type.isAssignableFrom(notification.getClass())) {
            lastSeen = type.cast(notification);
            finished.set(predicate.test(lastSeen));
        }
    }

    Object getNotification() {
        return notificationCache.getNotificationAndReset();
    }

    @Override
    public boolean isMergeable() {
        return false;
    }

    @Override
    public void merge(Executable step) {}

    @Override
    public String info() {
        return "Check step last saw " + lastSeen;
    }
}
