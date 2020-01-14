package com.ocadotechnology.trafficlightsimulation.simulation;

import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.IdGenerator;
import com.ocadotechnology.id.SimpleLongIdentified;

public class Vehicle extends SimpleLongIdentified<Vehicle> {

    private final double arrivalTime;
    private final boolean isMoving;
    private final Colour colour;

    public Vehicle(double arrivalTime, Colour colour) {
        this(IdGenerator.getId(Vehicle.class), arrivalTime, false, colour);
    }

    private Vehicle(Id<Vehicle> vehicleId, double arrivalTime, boolean isMoving, Colour colour) {
        super(vehicleId);
        this.arrivalTime = arrivalTime;
        this.isMoving = isMoving;
        this.colour = colour;
    }

    public Vehicle startsMoving() {
        return new Vehicle(getId(), arrivalTime, true, colour);
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
