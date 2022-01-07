/*
 * Copyright © 2017-2022 Ocado (Ocava)
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
     * @throws TraversalCalculationException
     */
    Traversal create(
            double length,
            double initialSpeed,
            double initialAcceleration,
            VehicleMotionProperties vehicleProperties);

    Traversal getBrakingTraversal(
            double initialSpeed,
            double initialAcceleration,
            VehicleMotionProperties vehicleProperties);

    /**
     * @throws TraversalCalculationException
     */
    default Traversal getBrakingTraversal(Traversal fullTraversal, double distanceAlongTraversal, VehicleMotionProperties vehicleProperties) {
        double initialSpeed = fullTraversal.getSpeedAtDistance(distanceAlongTraversal);
        double initialAcceleration = fullTraversal.getAccelerationAtDistance(distanceAlongTraversal);
        return getBrakingTraversal(initialSpeed, initialAcceleration, vehicleProperties);
    }

}
