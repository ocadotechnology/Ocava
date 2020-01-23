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
package com.ocadotechnology.physics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.ocadotechnology.physics.units.LengthUnit;
import com.ocadotechnology.physics.units.ValuesInSIUnits;

class TraversalTimeCalculatorTest {
    private static final double EPSILON = 0.01;

    @Test
    void testCalcTraversalTime_notGoingAnywhere() {
        VehicleMotionProperties vehicleMotionProperties = new VehicleMotionProperties(1E-6, -1E-6, 1E-3, 1E-9, -1E-9, -1E-9, 1E-9);
        List<TraversalSection> traversal = ConstantAccelerationTraversalTimeCalculator.calcTraversalTime(0, 0, 0, vehicleMotionProperties);
        assertThat(ValuesInSIUnits.convertDuration(calcTime(traversal), TimeUnit.MILLISECONDS)).isZero();
    }

    @Test
    void testCalcTraversalTime_nearInstantAccelDecel() {

        VehicleMotionProperties vehicleMotionProperties = new VehicleMotionProperties(999999999, -999999999, 1E-3, 1E-9, -1E-9, -1E-9, 1E-9);
        List<TraversalSection> traversal = ConstantAccelerationTraversalTimeCalculator.calcTraversalTime(
                15,
                0,
                0,
                vehicleMotionProperties);
        assertThat(ValuesInSIUnits.convertDuration(calcTime(traversal), TimeUnit.MILLISECONDS)).isCloseTo(15, within(EPSILON));
    }

    @Test
    void testCalcTraversalTime_reachingMaxSpeed() {
        VehicleMotionProperties vehicleMotionProperties = new VehicleMotionProperties(1E-6, -1E-6, 1E-3, 1E-9, -1E-9, -1E-9, 1E-9);
        List<TraversalSection> traversal = ConstantAccelerationTraversalTimeCalculator.calcTraversalTime(15, 0, 0, vehicleMotionProperties);
        assertThat(ValuesInSIUnits.convertDuration(calcTime(traversal), TimeUnit.MILLISECONDS)).isEqualTo(16);

    }

    @Test
    void testCalcTraversalTime_notReachingMaxSpeed() {
        VehicleMotionProperties vehicleMotionProperties = new VehicleMotionProperties(1E-6, -1E-6, 9999, 1E-9, -1E-9, -1E-9, 1E-9);
        List<TraversalSection> traversal = ConstantAccelerationTraversalTimeCalculator.calcTraversalTime(15, 0, 0, vehicleMotionProperties);
        assertThat(ValuesInSIUnits.convertDuration(calcTime(traversal), TimeUnit.MILLISECONDS)).isCloseTo(7.75, within(EPSILON));
    }

    @Test
    void testCalcTraversalTime_wonkyAccelerationDeceleration() {
        VehicleMotionProperties vehicleMotionProperties = new VehicleMotionProperties(3E-6, -1E-6, 1E-3, 1E-9, -1E-9, -1E-9, 1E-9);
        List<TraversalSection> traversal = ConstantAccelerationTraversalTimeCalculator.calcTraversalTime(
                15,
                0,
                0,
                vehicleMotionProperties);
        assertThat(ValuesInSIUnits.convertDuration(calcTime(traversal), TimeUnit.MILLISECONDS)).isCloseTo(15.66, within(EPSILON));
    }

    @Test
    void testCalcTraversalTime_fractionalVals() {
        VehicleMotionProperties vehicleMotionProperties = new VehicleMotionProperties(3.5E-6, -1.5E-6, 1.25E-3, 1E-9, -1E-9, -1E-9, 1E-9);
        List<TraversalSection> traversal = ConstantAccelerationTraversalTimeCalculator.calcTraversalTime(
                15.5,
                0,
                0,
                vehicleMotionProperties);
        assertThat(ValuesInSIUnits.convertDuration(calcTime(traversal), TimeUnit.MILLISECONDS)).isCloseTo(13, within(EPSILON));
    }

    private double calcTime(List<TraversalSection> traversal) {
        double time = 0;
        for (TraversalSection part : traversal) {
            time += part.getDuration();
        }
        return time;
    }

    @Test
    void testCalcTraversalTimeAndSpeedAtDistance() {
        VehicleMotionProperties vehicleMotionProperties = new VehicleMotionProperties(1E-6, -1E-6, 1E-3, 1E-9, -1E-9, -1E-9, 1E-9);
        ImmutableList<TraversalSection> parts = ConstantAccelerationTraversalTimeCalculator.calcTraversalTime(15, 0, 0, vehicleMotionProperties);

        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 0)).containsExactly(new double[]{0, 0}, within(EPSILON));
        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 0.00001)).containsExactly(new double[]{0, 0}, within(EPSILON));
        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 0.5)).containsExactly(new double[]{1, 1}, within(EPSILON));
        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 0.25)).containsExactly(new double[]{0.71, 0.71}, within(EPSILON));
        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 1)).containsExactly(new double[]{1.5, 1}, within(EPSILON));
        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 14.75)).containsExactly(new double[]{15.29, 0.71}, within(EPSILON));
        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 14.99999)).containsExactly(new double[]{16, 0}, within(EPSILON));
        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 15)).containsExactly(new double[]{16, 0}, within(EPSILON));
    }

    @Test
    void testCalcTraversalTimeAndSpeedAtDistance_startAndEndAtMaxSpeed() {
        VehicleMotionProperties vehicleMotionProperties = new VehicleMotionProperties(1E-6, -1E-6, 1E-3, 1E-9, -1E-9, -1E-9, 1E-9);
        ImmutableList<TraversalSection> parts = ConstantAccelerationTraversalTimeCalculator.calcTraversalTime(15, 1E-3, 1E-3, vehicleMotionProperties);

        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 0)).containsExactly(new double[]{0, 1}, within(EPSILON));
        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 0.00001)).containsExactly(new double[]{0, 1}, within(EPSILON));
        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 0.5)).containsExactly(new double[]{0.5, 1}, within(EPSILON));
        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 0.25)).containsExactly(new double[]{0.25, 1}, within(EPSILON));
        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 1)).containsExactly(new double[]{1, 1}, within(EPSILON));
        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 14.75)).containsExactly(new double[]{14.75, 1}, within(EPSILON));
        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 14.99999)).containsExactly(new double[]{15, 1}, within(EPSILON));
        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 15)).containsExactly(new double[]{15, 1}, within(EPSILON));
    }

    @Test
    void testCalcTraversalTimeAndSpeedAtDistance_nonZeroVelocities() {
        VehicleMotionProperties vehicleMotionProperties = new VehicleMotionProperties(1E-6, -2E-6, 5E-3, 1E-9, -1E-9, -1E-9, 1E-9);

        ImmutableList<TraversalSection> parts = ConstantAccelerationTraversalTimeCalculator.calcTraversalTime(15, 1E-3, 2E-3, vehicleMotionProperties);

        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 0)).containsExactly(new double[]{0, 1}, within(EPSILON));
        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 0.00001)).containsExactly(new double[]{0, 1}, within(EPSILON));
        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 0.25)).containsExactly(new double[]{0.22, 1.22}, within(EPSILON));
        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 0.5)).containsExactly(new double[]{0.41, 1.41}, within(EPSILON));
        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 1)).containsExactly(new double[]{0.73, 1.73}, within(EPSILON));
        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 14.75)).containsExactly(new double[]{4.91, 2.24}, within(EPSILON));
        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 14.99999)).containsExactly(new double[]{5.04, 2}, within(EPSILON));
        assertThat(calcTraversalTimeAndSpeedAtDistanceInSIUnits(parts, 15)).containsExactly(new double[]{5.04, 2}, within(EPSILON));
    }

    @Test
    void calculateAccelerationTraversal_whenMaxSpeedReached_thenReturnsPartsWithCorrectTimes() {
        ImmutableList<TraversalSection> parts = ConstantAccelerationTraversalTimeCalculator.calcAccelerationTraversal(5, 1e-3, 1.5e-6, 3e-3);

        assertThat(parts).as("Unexpected number of traversal parts").hasSize(2);
        assertThat(getFinalSpeed(parts.get(0))).as("Unexpected top speed reached").isCloseTo(3e-3, within(1e-9));
        assertThat(parts.get(0).getAccelerationAtTime(0)).as("Unexpected acceleration value").isCloseTo(1.5e-6, within(1e-9));
        assertThat(parts.get(0).getDuration()).as("Unexpected acceleration duration").isCloseTo(1333.333333333, within(1e-9));
        assertThat(parts.get(0).getTotalDistance()).as("Unexpected acceleration distance").isCloseTo(2.666666666, within(1e-9));
        assertThat(parts.get(1).getAccelerationAtTime(0)).as("Expected constant speed segment").isCloseTo(0, within(1e-9));
        assertThat(parts.get(1).getSpeedAtTime(0)).as("Expected equal speed between parts").isCloseTo(getFinalSpeed(parts.get(0)), within(1e-9));
        assertThat(parts.get(1).getDuration()).as("Unexpected constant speed duration").isCloseTo(777.777777777, within(1e-9));
        assertThat(parts.get(1).getTotalDistance()).as("Unexpected constant speed distance").isCloseTo(2.333333333, within((1e-9)));
    }

    @Test
    void calculateAccelerationTraversal_whenMaxSpeedNotReached_thenReturnsPartsWithCorrectTimes() {
        ImmutableList<TraversalSection> parts = ConstantAccelerationTraversalTimeCalculator.calcAccelerationTraversal(2, 1e-3, 1.5e-6, 3e-3);

        assertThat(parts).as("Unexpected number of traversal rts").hasSize(1);
        assertThat(getFinalSpeed(parts.get(0))).as("Unexpected top speed reached").isCloseTo(2.645751311e-3, within(1e-9));
        assertThat(parts.get(0).getAccelerationAtTime(0)).as("Unexpected acceleration value").isCloseTo(1.5e-6, within(1e-9));
        assertThat(parts.get(0).getDuration()).as("Unexpected acceleration duration").isCloseTo(1097.167540709, within(1e-9));
        assertThat(parts.get(0).getTotalDistance()).as("Unexpected acceleration distance").isCloseTo(2, within(1e-9));
    }

    @Test
    void calculateAccelerationTraversal_whenMaxSpeedLessThanInitial_thenIllegalArgumentExceptionThrown() {
        assertThatThrownBy(() -> ConstantAccelerationTraversalTimeCalculator.calcAccelerationTraversal(2, 3e-3, 1.5e-6, 1e-3))
                .isInstanceOf(TraversalCalculationException.class);
    }

    @Test
    void calculateAccelerationTraversal_whenInitialSpeedNegative_thenIllegalArgumentExceptionThrown() {
        assertThatThrownBy(() -> ConstantAccelerationTraversalTimeCalculator.calcAccelerationTraversal(2, -1e-3, 1.5e-6, 1e-3))
                .isInstanceOf(TraversalCalculationException.class);
    }

    @Test
    void calculateAccelerationTraversal_whenMaxSpeedNegative_thenIllegalArgumentExceptionThrown() {
        assertThatThrownBy(() -> ConstantAccelerationTraversalTimeCalculator.calcAccelerationTraversal(2, 1e-3, 1.5e-6, -1e-3))
                .isInstanceOf(TraversalCalculationException.class);
    }

    @Test
    void calculateAccelerationTraversal_whenDistanceNegative_thenIllegalArgumentExceptionThrown() {
        assertThatThrownBy(() -> ConstantAccelerationTraversalTimeCalculator.calcAccelerationTraversal(-2, 1e-3, 1.5e-6, 1e-3))
                .isInstanceOf(TraversalCalculationException.class);
    }

    private double[] calcTraversalTimeAndSpeedAtDistanceInSIUnits(ImmutableList<TraversalSection> constantAccelerationTraversalSections, double distance) {
        Traversal traversal = new Traversal(constantAccelerationTraversalSections);
        return new double[]{
                ValuesInSIUnits.convertDuration(traversal.getTimeAtDistance(distance), TimeUnit.MILLISECONDS),
                ValuesInSIUnits.convertSpeed(traversal.getSpeedAtDistance(distance), LengthUnit.METERS, TimeUnit.MILLISECONDS)
        };
    }

    private double getFinalSpeed(TraversalSection section) {
        return section.getSpeedAtTime(section.getDuration());
    }
}
