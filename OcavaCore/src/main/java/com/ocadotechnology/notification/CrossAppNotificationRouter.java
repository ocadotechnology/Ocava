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

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;
import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.utils.RememberingSupplier;

public class CrossAppNotificationRouter implements NotificationRouter {
    private static class SingletonHolder {
        private static final CrossAppNotificationRouter instance = new CrossAppNotificationRouter();
    }

    public static CrossAppNotificationRouter get() {
        return SingletonHolder.instance;
    }

    /** Singleton */
    private CrossAppNotificationRouter(){}

    private volatile Consumer<Notification> eavesDropper = null;
    private volatile NotificationLogger logger = null;

    private volatile boolean disableBroadcast = false;

    public void setEavesDropper(Consumer<Notification> eavesDropper) {
        Preconditions.checkState(this.eavesDropper == null || eavesDropper == null, "Attempting to override eavesdropper: old %s new %s", this.eavesDropper, eavesDropper);
        this.eavesDropper = eavesDropper;
    }

    public void setLogger(NotificationLogger logger) {
        Preconditions.checkState(this.logger == null || logger == null, "Attempting to override logger: old %s new %s", this.logger, logger);
        this.logger = logger;
    }

    public void setShouldSuppressBroadcast(boolean disableBroadcast) {
        this.disableBroadcast = disableBroadcast;
    }

    /**
     * The defaultMode uses DefaultBus to send all notifications (no forwarding is provided)
     * DefaultMode is required for all unit tests. It seems that it is
     * the less destructive way of using this class in tests.
     * <p>
     * DefaultMode is removed with first call to registerExecutionLayer
     * </p>
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

    private <T extends Notification> void broadcastImplementation(
            RememberingSupplier<T> messageSupplier,
            Class<?> notificationClass,
            BiFunction<Class<?>, Broadcaster<?>, Boolean> shouldHandToBroadcaster) {

        if (logger != null && logger.accepts(notificationClass)) {
            logger.log(messageSupplier.get());
        }

        if (eavesDropper != null) {
            eavesDropper.accept(messageSupplier.get());
        }

        if (disableBroadcast) {
            return;
        }

        WithinAppNotificationRouter.get().broadcastImplementation(messageSupplier, notificationClass, shouldHandToBroadcaster);
    }

    public void broadcastWithinAppOnly(Notification message) {
        WithinAppNotificationRouter.get().broadcast(message);
    }

    @Override
    public void addHandler(Subscriber handler) {
        WithinAppNotificationRouter.get().addHandler(handler);
    }

    @Override
    public synchronized <T> void registerExecutionLayer(EventScheduler scheduler, NotificationBus<T> notificationBus) {
        WithinAppNotificationRouter.get().registerExecutionLayer(scheduler, notificationBus);
    }

    @Override
    public <T> void registerExecutionLayer(Broadcaster<T> newBroadcaster) {
        WithinAppNotificationRouter.get().registerExecutionLayer(newBroadcaster);
    }

    @Override
    public void clearAllHandlers() {
        WithinAppNotificationRouter.get().clearAllHandlers();
    }
}
