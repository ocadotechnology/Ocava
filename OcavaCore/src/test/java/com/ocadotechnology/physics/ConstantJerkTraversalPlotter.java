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

import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.ocadotechnology.validation.Failer;

public class ConstantJerkTraversalPlotter {
    public static void main(String[] args) {
        double acceleration = 2.5E-6;
        double deceleration = -2E-6;
        double maxSpeed = 8E-3;

        double jerkAccelerationUp = 2E-9;
        double jerkAccelerationDown = -2E-9;
        double jerkDecelerationUp = -2E-9;
        double jerkDecelerationDown = 2E-9;

        double errorFraction = 0.01;

        VehicleMotionProperties vehicleMotionProperties = new VehicleMotionProperties(maxSpeed, acceleration, jerkDecelerationDown, 0.01);

        Optional<ImmutableList<TraversalSection>> constantJerkSections = ConstantJerkSectionsFactory.maxAccelerationDecelerationAndSpeedReached(100d, vehicleMotionProperties);
        Traversal traversal = new Traversal(constantJerkSections.orElseThrow(Failer::valueExpected));

        System.out.println(traversal);

        TraversalGraphPlotter.INSTANCE.plot(traversal);
    }
}
