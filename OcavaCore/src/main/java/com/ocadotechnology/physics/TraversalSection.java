/*
 * Copyright © 2017-2021 Ocado (Ocava)
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
package com.ocadotechnology.physics;

import java.io.Serializable;

/**
 * Definition of a part of a well-defined section of a {@link Traversal}.
 */
public interface TraversalSection extends Serializable {
    /**
     * @return the total time of this section
     */
    double getDuration();

    /**
     * @return the total distance of this section
     */
    double getTotalDistance();

    /**
     * @return the time after the start of this section for the object to reach the given distance after the start of
     * this section.
     * @throws TraversalCalculationException if an invalid distance is provided
     */
    double getTimeAtDistance(double distance);

    /**
     * @return the distance after the start of this section the object will reach by the given time after the start of
     * this section.
     * @throws TraversalCalculationException if an invalid time is provided
     */
    double getDistanceAtTime(double time);

    /**
     * @return the speed the object will have at the given distance after the start of this section
     * @throws TraversalCalculationException if an invalid distance is provided
     */
    default double getSpeedAtDistance(double distance) {
        return getSpeedAtTime(getTimeAtDistance(distance));
    }

    /**
     * @return the speed the object will have at the given time after the start of this section
     * @throws TraversalCalculationException if an invalid time is provided
     */
    double getSpeedAtTime(double time);

    /**
     * @return the acceleration the object will have at the given distance after the start of this section
     * @throws TraversalCalculationException if an invalid distance is provided
     */
    default double getAccelerationAtDistance(double distance) {
        return getAccelerationAtTime(getTimeAtDistance(distance));
    }

    /**
     * @return the acceleration the object will have at the given time after the start of this section
     * @throws TraversalCalculationException if an invalid time is provided
     */
    double getAccelerationAtTime(double time);

    /**
     * @return true if the speed of the object is increasing during this section
     */
    default boolean isAccelerating() {
        return false;
    }

    /**
     * @return true if the speed of the object is decreasing during this section
     */
    default boolean isDecelerating() {
        return false;
    }

    /**
     * @return true if the speed of the object is constant during this section
     */
    default boolean isConstantSpeed() {
        return false;
    }

    /**
     * @return true if the acceleration of the object is not changing during this section
     */
    default boolean isConstantAcceleration() {
        return true;
    }
}
