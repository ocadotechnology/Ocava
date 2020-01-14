package com.ocadotechnology.trafficlightsimulation;

public enum TrafficConfig {
    SCHEDULER_TYPE,
    MAX_SIM_TIME;

    public enum TrafficLight {
        INITIAL_STATE,
        RED_LIGHT_INTERVAL,
        GREEN_LIGHT_INTERVAL,
        ENABLE_AUTOMATIC_CHANGE
    }

    public enum Vehicles {
        INITIAL_VEHICLES,
        MIN_TIME_BETWEEN_ARRIVALS,
        MAX_TIME_BETWEEN_ARRIVALS,
        TIME_TO_LEAVE_JUNCTION,
        ENABLE_RANDOM_ARRIVAL
    }

}
