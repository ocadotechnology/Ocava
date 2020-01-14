package com.ocadotechnology.trafficlightsimulation.simulation;

import com.ocadotechnology.notification.Notification;

public class CarLeavesNotification implements Notification {

    public final Vehicle vehicle;

    public CarLeavesNotification(Vehicle vehicle) {
        this.vehicle = vehicle;
    }
}
