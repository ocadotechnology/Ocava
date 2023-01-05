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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.concurrent.GuardedBy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.eventbus.BlockingEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.reflect.TypeToken;
import com.ocadotechnology.validation.Failer;

public abstract class NotificationBus<N> {
    public static final String NOTIFICATION_BUS_ID = "NOTIFICATION_BUS";

    private final Logger logger = LoggerFactory.getLogger(NOTIFICATION_BUS_ID);

    private final Class<N> notificationClass;

    private final AtomicReference<Thread> thread = new AtomicReference<>(null);

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    @GuardedBy("rwLock")
    private BlockingEventBus eventBus;

    /** All the explicit registrations via {@link #addHandler(Object)}. */
    @GuardedBy("rwLock")
    private final Map<Class<?>, Class<?>> registeredNotifications = new HashMap<>();

    /** Cache: Only touched by {@link #isParentOfNotificationRegistered}<br>
     *  Can be cleared at any time using {@link #clearCache()} with no behavioural change.
     */
    @GuardedBy("rwLock")
    private final Map<Class<?>, Boolean> cacheOfImpliedNotifications = new HashMap<>();

    private final PointToPointValidator pointToPointValidator = new PointToPointValidator();

    protected NotificationBus(Class<N> notificationClass) {
        this.notificationClass = notificationClass;
        eventBus = new BlockingEventBus();
    }

    /** This method is and needs to remain ThreadSafe. */
    protected void addHandler(Object handler) {
        List<Class<?>> newNotifications = collectSubscribingTypes(handler);
        pointToPointValidator.validate(handler, newNotifications);
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            newNotifications.forEach(type -> registeredNotifications.put(type, type));
            clearCache();

            // Not sure that eventBus is thread-safe, so we'll include it in our lock
            eventBus.register(handler);
        } finally {
            lock.unlock();
        }
    }

    private List<Class<?>> collectSubscribingTypes(Object handler) {
        List<Class<?>> newNotifications = new ArrayList<>(8);
        Class<?> clazz = handler.getClass();
        Set<? extends Class<?>> supertypes = TypeToken.of(clazz).getTypes().rawTypes();
        for (Class<?> supertype : supertypes) {
            for (Method method : supertype.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Subscribe.class) && !method.isSynthetic()) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    Preconditions.checkArgument(parameterTypes.length == 1,
                            "@Subscribe-annotated handler method %s should have a single parameter",
                            method.getName());
                    Class<?> notificationType = parameterTypes[0];
                    Preconditions.checkArgument(notificationClass.isAssignableFrom(notificationType),
                            "Can not register notification %s from %s handler %s. Only %s notifications are allowed",
                            notificationType.getSimpleName(), clazz.getSimpleName(), method.getName(), notificationClass.getSimpleName());

                    newNotifications.add(notificationType);
                }
            }
        }
        return newNotifications;
    }

    public void clearAllHandlers() {
        pointToPointValidator.reset();
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            registeredNotifications.clear();
            clearCache();
            clearThread();
            eventBus = new BlockingEventBus();
        } finally {
            lock.unlock();
        }
    }

    public void broadcast(N notification) {
        EventBus bus;
        Lock lock = rwLock.readLock();
        try {
            lock.lock();

            // This has to go in a readLock (even though we use a volatile because of clearAllHandler)
            checkThatThisBusHasOnlyBeenUsedByOneThread(notification);
            bus = eventBus;
        } finally {
            lock.unlock();
        }

        if (logger.isTraceEnabled()) {
            logger.trace("{} broadcasting {}", getClass().getSimpleName(), notification);
        }
        bus.post(notification);
    }

    public void clearThread() {
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            thread.set(null);
        } finally {
            lock.unlock();
        }
    }

    protected void replaceAllNotifications(Collection<Class<?>> newNotifications) {
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();

            registeredNotifications.clear();
            newNotifications.forEach(n -> registeredNotifications.put(n, n));
            clearCache();
        } finally {
            lock.unlock();
        }
    }

    protected void checkThatThisBusHasOnlyBeenUsedByOneThread(N notification) {
        Thread current = Thread.currentThread();
        if (current == thread.get()) {
            return;  // expected path (always true, except first time and errors).  Having this short-cut saves approx 5ns per call
        }

        Thread permitted = thread.updateAndGet(t -> (t == null) ? current : t);
        if (current == permitted) {
            return;
        }

        throw Failer.fail("first Thread: %s [%s] current Thread: %s [%s] %s", permitted, permitted.getId(), current, current.getId(), notification);
    }

    protected boolean isNotificationRegistered(Class<?> notification) {
        Lock lock = rwLock.readLock();
        try {
            lock.lock();

            if (registeredNotifications.containsKey(notification)) {
                return true;
            }

            Boolean result = cacheOfImpliedNotifications.get(notification);
            if (result != null) {
                return result;
            }
        } finally {
            lock.unlock();
        }

        // Has to be done outside readLock as can't upgrade read to write
        return isParentOfNotificationRegistered(notification);
    }

    /** Have we already asked this question?<br>
     *  If not, check if we've registered any parents (and cache the answer).
     */
    private <T> boolean isParentOfNotificationRegistered(Class<T> notification) {
        Set<Class<? super T>> classes = TypeToken.of(notification).getTypes().rawTypes();

        Lock lock = rwLock.writeLock();
        try {
            lock.lock();

            for (Class<?> type : classes) {
                if (registeredNotifications.containsKey(type)) {  // check must be in lock
                    // registeredNotification contains only notifications we want, so this is always true:
                    cacheOfImpliedNotifications.put(notification, Boolean.TRUE);
                    return true;
                }
            }
            // We haven't registered this notification or any of it's parents, so cache the failure:
            cacheOfImpliedNotifications.put(notification, Boolean.FALSE);
            return false;
        } finally {
            lock.unlock();
        }
    }

    private void clearCache() {
        cacheOfImpliedNotifications.clear();
    }

    protected boolean canHandleNotification(Class<?> notification) {
        return hasCorrectType(notification);
    }

    protected abstract boolean hasCorrectType(Class<?> notification);

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("notificationClass", notificationClass)
                .add("registeredNotifications", registeredNotifications)
                .toString();
    }
}
