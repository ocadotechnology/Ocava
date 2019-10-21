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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.utils.RememberingSupplier;

class WithinAppNotificationRouter implements NotificationRouter {
    private static final Logger logger = LoggerFactory.getLogger(WithinAppNotificationRouter.class);

    private static class SingletonHolder {
        private static final WithinAppNotificationRouter instance = new WithinAppNotificationRouter();
    }

    static WithinAppNotificationRouter get() {
        return SingletonHolder.instance;
    }

    /** Singleton */
    private WithinAppNotificationRouter(){}

    private volatile List<Broadcaster<?>> broadcasters = new ArrayList<>();

    private volatile boolean defaultMode = true;

    /**
     * The defaultMode uses DefaultBus to send all notifications (no forwarding is provided)
     * DefaultMode is required for all unit tests. It seems that it is
     * the less destructive way of using this class in tests.
     * <p/>
     * DefaultMode is removed with first call to registerExecutionLayer
     */
    @Override
    public <T extends Notification> void broadcast(T notification) {
        RememberingSupplier<T> notificationHolder = new RememberingSupplier<>(notification);
        broadcastImplementation(notificationHolder, notification.getClass(), (clazz, broadcaster) -> broadcaster.canHandleNotification(clazz));
    }

    @Override
    public <T extends Notification> void broadcast(Supplier<T> concreteMessageNotificationSupplier, Class<T> notificationClass) {
        RememberingSupplier<T> rememberingSupplier = new RememberingSupplier<>(concreteMessageNotificationSupplier);
        broadcastImplementation(rememberingSupplier, notificationClass, (clazz, broadcaster) -> broadcaster.isNotificationRegistered(clazz));
    }

    <T extends Notification> void broadcastImplementation(
            RememberingSupplier<T> messageSupplier,
            Class<?> notificationClass,
            BiFunction<Class<?>, Broadcaster<?>, Boolean> shouldHandToBroadcaster) {

        if (logger.isTraceEnabled()) {
            logger.trace("Broadcasting {}", messageSupplier.get());
        }

        if (defaultMode) {
            DefaultBus.get().broadcast(messageSupplier.get());
            return;
        }

        passToBroadcasters(messageSupplier, notificationClass, shouldHandToBroadcaster);
    }

    private <T> void passToBroadcasters(RememberingSupplier<T> messageSupplier, Class<?> notificationClass, BiFunction<Class<?>, Broadcaster<?>, Boolean> shouldHandToBroadcaster) {
        for (Broadcaster<?> broadcaster : broadcasters) {
            if (shouldHandToBroadcaster.apply(notificationClass, broadcaster)) {
                broadcaster.broadcast(messageSupplier.get());
            }
        }
    }

    @Override
    public void addHandler(Subscriber handler) {
        if (defaultMode) {
            DefaultBus.get().addHandler(handler);
            return;
        }
        broadcasters.stream().filter(b -> b.getSchedulerType() == handler.getSchedulerType()).findFirst().ifPresent(b -> b.addHandler(handler));
    }

    /**
     * Insertion order is used to send notification (LOGGING should be first)
     * You have to register your layers to removeAndCancel defaultMode.
     */
    @Override
    public synchronized <T> void registerExecutionLayer(EventScheduler scheduler, NotificationBus<T> notificationBus) {
        defaultMode = false;
        List<Broadcaster<?>> registeredBroadcasters = new ArrayList<>(broadcasters);
        registeredBroadcasters.add(new Broadcaster<>(scheduler, notificationBus, scheduler.getType()));
        //thread handover
        broadcasters = registeredBroadcasters;
    }

    @Override
    public void clearAllHandlers() {
        DefaultBus.get().clearAllHandlers();
        defaultMode = true;
        broadcasters.forEach(Broadcaster::clearAllHandlers);
        broadcasters = new ArrayList<>();
    }
}
