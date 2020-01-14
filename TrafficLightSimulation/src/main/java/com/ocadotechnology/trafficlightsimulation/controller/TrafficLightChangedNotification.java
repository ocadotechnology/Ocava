package com.ocadotechnology.trafficlightsimulation.controller;

import com.ocadotechnology.notification.Notification;
import com.ocadotechnology.trafficlightsimulation.controller.TrafficLightController.State;

public class TrafficLightChangedNotification implements Notification {

    public final State newState;

    public TrafficLightChangedNotification(State newState) {
        this.newState = newState;
    }
}
