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

import com.google.common.base.Preconditions;
import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.utils.RememberingSupplier;

class WithinAppNotificationRouter implements NotificationRouter {
    private static final Logger logger = LoggerFactory.getLogger(WithinAppNotificationRouter.class);

    /**
     * Configuration to allow scheduling the cross-thread broadcasts first.
     */
    private static final String CROSS_THREAD_BROADCAST_TYPE = "CROSS_THREAD_FIRST";
    private static final String CONFIGURED_BROADCAST_TYPE = System.getProperties().getProperty("com.ocadotechnology.notificationrouter.broadcast");
    private static final boolean SCHEDULE_CROSS_THREAD_BROADCAST_FIRST = CROSS_THREAD_BROADCAST_TYPE.equalsIgnoreCase(CONFIGURED_BROADCAST_TYPE);

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

        if (SCHEDULE_CROSS_THREAD_BROADCAST_FIRST) {
            passToBroadcastersCrossThreadFirst(messageSupplier, notificationClass, shouldHandToBroadcaster);
            return;
        }

        passToBroadcastersInOrder(messageSupplier, notificationClass, shouldHandToBroadcaster);
    }

    /**
     * @deprecated (8.15)
     * Provides no guarantees that cross-thread notifications will be received in the order they are sent.
     * Use {@link #passToBroadcastersCrossThreadFirst} passToBroadcastersCrossThreadFirst instead.
     */
    @Deprecated
    private <T> void passToBroadcastersInOrder(RememberingSupplier<T> messageSupplier, Class<?> notificationClass, BiFunction<Class<?>, Broadcaster<?>, Boolean> shouldHandToBroadcaster) {
        for (Broadcaster<?> broadcaster : broadcasters) {
            if (shouldHandToBroadcaster.apply(notificationClass, broadcaster)) {
                broadcaster.broadcast(messageSupplier.get());
            }
        }
    }

    /**
     * Guarantees that cross-thread notifications are received in the order they are sent.
     */
    private <T> void passToBroadcastersCrossThreadFirst(RememberingSupplier<T> messageSupplier, Class<?> notificationClass, BiFunction<Class<?>, Broadcaster<?>, Boolean> shouldHandToBroadcaster) {
        Broadcaster<?> inThreadBroadcaster = null;
        for (Broadcaster<?> broadcaster : broadcasters) {
            if (!shouldHandToBroadcaster.apply(notificationClass, broadcaster)) {
                continue;
            }

            if (broadcaster.requiresScheduling()) {
                broadcaster.scheduleBroadcast(messageSupplier.get());
            } else {
                Preconditions.checkState(inThreadBroadcaster == null, "There should be at most one broadcaster per scheduler/thread.");
                inThreadBroadcaster = broadcaster;
            }
        }

        if (inThreadBroadcaster != null) {
            inThreadBroadcaster.directBroadcast(messageSupplier.get());
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
        if (defaultMode) {
            String broadcasterOrder = SCHEDULE_CROSS_THREAD_BROADCAST_FIRST ? "CROSS_THREAD_FIRST" : "BROADCASTER_REGISTRATION_ORDER";
            logger.info("The configured broadcast order is: {}", broadcasterOrder);
        }
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
