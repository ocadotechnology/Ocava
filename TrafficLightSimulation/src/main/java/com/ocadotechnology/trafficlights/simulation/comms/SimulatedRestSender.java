/*
 * Copyright © 2017-2025 Ocado (Ocava)
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

import com.ocadotechnology.notification.NotificationRouter;
import com.ocadotechnology.trafficlights.controller.LightColour;
import com.ocadotechnology.trafficlights.controller.RestSender;

public class SimulatedRestSender implements RestSender {

    @Override
    public void setTrafficColour(LightColour lightColour) {
        NotificationRouter.get().broadcast(new TrafficLightChangedNotification(lightColour));
    }

    @Override
    public void setPedestrianColour(LightColour lightColour) {
        NotificationRouter.get().broadcast(new PedestrianLightChangedNotification(lightColour));
    }
}
