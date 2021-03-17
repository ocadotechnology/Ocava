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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.within;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.ocadotechnology.physics.units.ValuesInSIUnits;

class ConstantAccelerationTraversalTimeFactoryTest {
    private static final double EPSILON = 1e-6;

    @Test
    void testGetTimeAtDistance_constantSpeed() {
        VehicleMotionProperties vehicleMotionProperties = new VehicleMotionProperties(9999999E-6, -9999999E-6, 1E-3, 1E-9, -1E-9, -1E-9, 1E-9);
        List<TraversalSection> parts = ConstantAccelerationTraversalTimeCalculator.calcTraversalTime(15, 0, 0, vehicleMotionProperties);

        TraversalSection traversalTimePart = parts.get(1);

        double time = traversalTimePart.getTimeAtDistance(2);
        assertThat(ValuesInSIUnits.convertDuration(time, TimeUnit.MILLISECONDS)).isCloseTo(2, within(EPSILON));
    }

    @Test
    void testGetTimeAtDistance_acceleration() {
        VehicleMotionProperties vehicleMotionProperties = new VehicleMotionProperties(1E-6, -1E-6, 1E-3, 1E-9, -1E-9, -1E-9, 1E-9);
        List<TraversalSection> parts = ConstantAccelerationTraversalTimeCalculator.calcTraversalTime(15, 0, 0, vehicleMotionProperties);

        TraversalSection traversalTimePart = parts.get(0);

        double time = traversalTimePart.getTimeAtDistance(0.5);
        assertThat(ValuesInSIUnits.convertDuration(time, TimeUnit.MILLISECONDS)).isCloseTo(1, within(EPSILON));
    }

    @Test
    void testGetTimeAtDistance_deceleration() {
        VehicleMotionProperties vehicleMotionProperties = new VehicleMotionProperties(1E-6, -1E-6, 1E-3, 1E-9, -1E-9, -1E-9, 1E-9);
        List<TraversalSection> parts = ConstantAccelerationTraversalTimeCalculator.calcTraversalTime(15, 0, 0, vehicleMotionProperties);

        TraversalSection constantAccelerationTraversalSection = parts.get(2);

        double time = constantAccelerationTraversalSection.getTimeAtDistance(0.5);
        assertThat(ValuesInSIUnits.convertDuration(time, TimeUnit.MILLISECONDS)).isCloseTo(1, within(EPSILON));
    }

}
