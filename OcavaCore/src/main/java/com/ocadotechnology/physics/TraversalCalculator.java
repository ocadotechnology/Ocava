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
package com.ocadotechnology.physics;

/**
 * Interface for creating traversal objects matching different definitions.
 */
public interface TraversalCalculator {

    /**
     * @param length how far to traverse
     * @param initialSpeed initial speed
     * @param initialAcceleration initial acceleration
     * @param vehicleProperties vehicles physical properties
     * @return A traversal that will cover length, starting from the specified speed and acceleration and ending with
     * the vehicle at rest. If the length is shorter than the minimal braking traversal, then this braking traversal is
     * instead returned.
     * @throws TraversalCalculationException if the traversal is physically impossible.
     */
    Traversal create(
            double length,
            double initialSpeed,
            double initialAcceleration,
            VehicleMotionProperties vehicleProperties);

    /**
     * @param initialSpeed initial speed
     * @param initialAcceleration initial acceleration
     * @param vehicleProperties vehicle properties
     * @return a traversal that will bring the vehicle to rest as quickly as possible.
     */
    Traversal getBrakingTraversal(
            double initialSpeed,
            double initialAcceleration,
            VehicleMotionProperties vehicleProperties);

    /**
     * @param fullTraversal original traversal
     * @param distanceAlongTraversal initial acceleration
     * @param vehicleProperties vehicle properties
     * @return a traversal that brings the bot to rest as quickly as possible given it has already travelled distanceAlongTraversal
     * along the traversal specified by fullTraversal
     * @throws TraversalCalculationException
     */
    default Traversal getBrakingTraversal(Traversal fullTraversal, double distanceAlongTraversal, VehicleMotionProperties vehicleProperties) {
        double initialSpeed = fullTraversal.getSpeedAtDistance(distanceAlongTraversal);
        double initialAcceleration = fullTraversal.getAccelerationAtDistance(distanceAlongTraversal);
        return getBrakingTraversal(initialSpeed, initialAcceleration, vehicleProperties);
    }

}
