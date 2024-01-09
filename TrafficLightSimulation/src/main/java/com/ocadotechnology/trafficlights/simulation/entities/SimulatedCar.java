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
package com.ocadotechnology.trafficlights.simulation.entities;

import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.IdGenerator;
import com.ocadotechnology.id.SimpleLongIdentified;

public class SimulatedCar extends SimpleLongIdentified<SimulatedCar> {

    private final double arrivalTime;
    private final boolean isMoving;
    private final Colour colour;

    public SimulatedCar(double arrivalTime, Colour colour) {
        this(IdGenerator.getId(SimulatedCar.class), arrivalTime, false, colour);
    }

    private SimulatedCar(Id<SimulatedCar> vehicleId, double arrivalTime, boolean isMoving, Colour colour) {
        super(vehicleId);
        this.arrivalTime = arrivalTime;
        this.isMoving = isMoving;
        this.colour = colour;
    }

    public SimulatedCar startsMoving() {
        return new SimulatedCar(getId(), arrivalTime, true, colour);
    }

    public double getArrivalTime() {
        return arrivalTime;
    }

    public boolean isStationary() {
        return !isMoving;
    }

    public Colour getColour() {
        return colour;
    }

    public enum Colour {
        RED, GREEN, BLUE,
    }

    @Override
    public String toString() {
        return "Vehicle{"
                + "arrivalTime=" + arrivalTime
                + ", isMoving=" + isMoving
                + ", colour=" + colour
                + '}';
    }
}
