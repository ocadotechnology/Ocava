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
package com.ocadotechnology.trafficlights;

public enum TrafficConfig {
    SCHEDULER_TYPE,
    MAX_SIM_TIME;

    public enum TrafficLight {
        INITIAL_TRAFFIC_STATE,
        INITIAL_PEDESTRIAN_STATE,
        RED_LIGHT_INTERVAL,
        GREEN_LIGHT_INTERVAL,
        PEDESTRIAN_CROSSING_CHANGE_DELAY,
        ENABLE_AUTOMATIC_CHANGE,
    }

    public enum Vehicles {
        INITIAL_VEHICLES,
        MIN_TIME_BETWEEN_ARRIVALS,
        MAX_TIME_BETWEEN_ARRIVALS,
        TIME_TO_LEAVE_JUNCTION,
        ENABLE_RANDOM_ARRIVAL
    }

    public enum Pedestrians {
        MIN_TIME_BETWEEN_ARRIVALS,
        MAX_TIME_BETWEEN_ARRIVALS,
        TIME_TO_LEAVE_JUNCTION,
        ENABLE_RANDOM_ARRIVAL,
    }

}