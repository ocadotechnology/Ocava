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
package com.ocadotechnology.trafficlights.simulation.entities;

import com.google.common.base.MoreObjects;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.IdGenerator;
import com.ocadotechnology.id.SimpleLongIdentified;

public class SimulatedPedestrian extends SimpleLongIdentified<SimulatedPedestrian> {

    private final double arrivalTime;
    private final boolean isCrossing;

    public SimulatedPedestrian(double arrivalTime) {
        this(IdGenerator.getId(SimulatedPedestrian.class), arrivalTime, false);
    }

    private SimulatedPedestrian(Id<SimulatedPedestrian> pedestrianId, double arrivalTime, boolean isCrossing) {
        super(pedestrianId);
        this.arrivalTime = arrivalTime;
        this.isCrossing = isCrossing;
    }

    public SimulatedPedestrian startsCrossing() {
        return new SimulatedPedestrian(getId(), arrivalTime, true);
    }

    public double getArrivalTime() {
        return arrivalTime;
    }

    public boolean isStationary() {
        return !isCrossing();
    }

    public boolean isCrossing() {
        return isCrossing;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", getId())
                .add("arrivalTime", arrivalTime)
                .add("isCrossing", isCrossing)
                .toString();
    }
}
