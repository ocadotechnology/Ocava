/*
 * Copyright © 2017-2024 Ocado (Ocava)
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
package com.ocadotechnology.trafficlights.simulation.comms;

import com.google.common.eventbus.Subscribe;
import com.ocadotechnology.notification.SimpleSubscriber;
import com.ocadotechnology.trafficlights.controller.RestHandler;

public class SimulatedRestForwarder implements SimpleSubscriber {

    private final RestHandler restHandler;

    private SimulatedRestForwarder(RestHandler restHandler) {
        this.restHandler = restHandler;
    }

    public static SimulatedRestForwarder createAndSubscribe(RestHandler restHandler) {
        SimulatedRestForwarder simulatedRestForwarder = new SimulatedRestForwarder(restHandler);
        simulatedRestForwarder.subscribeForNotifications();
        return simulatedRestForwarder;
    }

    @Subscribe
    public void pedestrianLightsButtonPressed(PedestrianCrossingRequestedNotification n) {
        restHandler.pedestrianLightsButtonPressed();
    }
}
