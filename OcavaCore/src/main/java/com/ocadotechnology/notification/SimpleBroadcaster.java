package com.ocadotechnology.notification;

import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.event.scheduling.EventSchedulerType;

public class SimpleBroadcaster<T> extends Broadcaster<T> {

    private SimpleBroadcaster(EventScheduler eventScheduler, NotificationBus<T> notificationBus) {
        super(eventScheduler, notificationBus);
    }

    public static SimpleBroadcaster<?> createWithSimpleBus(EventScheduler eventScheduler) {
        return new SimpleBroadcaster<>(eventScheduler, SimpleBus.create());
    }

    @Override
    boolean handlesSubscriber(EventSchedulerType ignored) {
        return true;
    }
}
