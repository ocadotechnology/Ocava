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

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.event.scheduling.EventSchedulerType;
import com.ocadotechnology.utils.RememberingSupplier;
import com.ocadotechnology.validation.Failer;

class WithinAppNotificationRouter implements NotificationRouter {
    private static final Logger logger = LoggerFactory.getLogger(WithinAppNotificationRouter.class);

    /**
     * Configuration to allow scheduling the cross-thread broadcasts first.
     */
    private static final String CROSS_THREAD_BROADCAST_TYPE = "CROSS_THREAD_FIRST";
    private static final String CONFIGURED_BROADCAST_TYPE = System.getProperties().getProperty("com.ocadotechnology.notificationrouter.broadcast");

    private static boolean scheduleCrossThreadBroadcastFirst = CROSS_THREAD_BROADCAST_TYPE.equalsIgnoreCase(CONFIGURED_BROADCAST_TYPE);

    private static class SingletonHolder {
        private static final WithinAppNotificationRouter instance = new WithinAppNotificationRouter();
    }

    static WithinAppNotificationRouter get() {
        return SingletonHolder.instance;
    }

    /** Singleton */
    private WithinAppNotificationRouter(){}

    private final AtomicReference<ImmutableList<Broadcaster<?>>> broadcasters = new AtomicReference<>(ImmutableList.of());

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

        if (scheduleCrossThreadBroadcastFirst) {
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
        for (Broadcaster<?> broadcaster : broadcasters.get()) {
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
        for (Broadcaster<?> broadcaster : broadcasters.get()) {
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
        EventSchedulerType schedulerType = handler.getSchedulerType();
        Broadcaster<?> broadcaster = broadcasters.get().stream()
                .filter(b -> b.handlesSubscriber(schedulerType))
                .findFirst()
                .orElseThrow(() -> Failer.fail("Attempting to register subscriber of scheduler type %s but there are no registered broadcasters for this type.", schedulerType));

        // The callee needs to be threadsafe -- no reason 2 threads can't get here at the same time
        broadcaster.addHandler(handler);
    }

    @Override
    public <T> void registerExecutionLayer(EventScheduler scheduler, NotificationBus<T> notificationBus) {
        registerExecutionLayer(new Broadcaster<>(scheduler, notificationBus));
    }

    /**
     * Insertion order is used to send notification (LOGGING should be first)
     */
    @Override
    public <T> void registerExecutionLayer(Broadcaster<T> newBroadcaster) {
        broadcasters.updateAndGet(broadcasters -> registerExecutionLayer(broadcasters, newBroadcaster));
    }

    // Can be retried multiple times -- must be side-effect free.
    private static <T> ImmutableList<Broadcaster<?>> registerExecutionLayer(ImmutableList<Broadcaster<?>> broadcasters, Broadcaster<T> newBroadcaster) {
        if (broadcasters.isEmpty()) {
            String broadcasterOrder = scheduleCrossThreadBroadcastFirst ? "CROSS_THREAD_FIRST" : "BROADCASTER_REGISTRATION_ORDER";
            logger.info("The configured broadcast order is: {}", broadcasterOrder);
        }

        Preconditions.checkArgument(!alreadyHandlesType(broadcasters, newBroadcaster.getSchedulerType()), "A broadcaster with type %s has already been registered.", newBroadcaster.getSchedulerType());
        return copyAndAdd(broadcasters, newBroadcaster);
    }

    private static <T> ImmutableList<Broadcaster<?>> copyAndAdd(ImmutableList<Broadcaster<?>> broadcasters, Broadcaster<T> newBroadcaster) {
        return ImmutableList.<Broadcaster<?>>builder().addAll(broadcasters).add(newBroadcaster).build();
    }

    private static boolean alreadyHandlesType(ImmutableList<Broadcaster<?>> broadcasters, EventSchedulerType broadcasterType) {
        return broadcasters.stream().anyMatch(b -> b.handlesSubscriber(broadcasterType));
    }

    @Override
    public void clearAllHandlers() {
        broadcasters.getAndSet(ImmutableList.of()).forEach(Broadcaster::clearAllHandlers);
    }

    @VisibleForTesting
    static void setScheduleCrossThreadBroadcastFirst(boolean scheduleCrossThreadBroadcastFirst) {
        WithinAppNotificationRouter.scheduleCrossThreadBroadcastFirst = scheduleCrossThreadBroadcastFirst;
    }

}
