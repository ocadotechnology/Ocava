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
package com.ocadotechnology.physics;

import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

public class ConstantAccelerationTraversalCalculator implements TraversalCalculator {
    public static final ConstantAccelerationTraversalCalculator INSTANCE = new ConstantAccelerationTraversalCalculator();

    private ConstantAccelerationTraversalCalculator() {
    }

    @Override
    public Traversal create(double length, double initialSpeed, double initialAcceleration, VehicleMotionProperties vehicleProperties) {
        if (length == 0) {
            return Traversal.EMPTY_TRAVERSAL;
        }
        return new Traversal(
                ConstantAccelerationTraversalTimeCalculator.calcTraversalTime(
                        length, Math.min(vehicleProperties.maxSpeed, initialSpeed), 0, vehicleProperties));
    }

    @Override
    public Traversal getBrakingTraversal(double initialSpeed, double initialAcceleration, VehicleMotionProperties vehicleProperties) {
        if (DoubleMath.fuzzyEquals(initialSpeed, 0d, ConstantAccelerationTraversalTimeCalculator.ROUNDING_ERROR_MARGIN)) {
            return Traversal.EMPTY_TRAVERSAL;
        }

        double time = AccelerationKinematics.getTime(initialSpeed, 0, vehicleProperties.deceleration);
        double distance = AccelerationKinematics.getDistance(initialSpeed, 0, time);

        return new Traversal(ImmutableList.of(new ConstantAccelerationTraversalSection(distance, vehicleProperties.deceleration, initialSpeed, 0, time)));
    }
}
