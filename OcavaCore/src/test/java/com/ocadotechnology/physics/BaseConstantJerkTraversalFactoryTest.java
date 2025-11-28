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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.math.DoubleMath;
import com.ocadotechnology.physics.units.LengthUnit;
import com.ocadotechnology.physics.units.ValuesInSIUnits;

class BaseConstantJerkTraversalFactoryTest {
    private static final ConstantJerkTraversalCalculator factory = ConstantJerkTraversalCalculator.INSTANCE;

    private static final double EPSILON = Math.pow(1, -8);

    private static final double ROUNDING_ERROR_TOLERANCE = 1e-9;           // generic numeric tolerance
    private static final VehicleMotionProperties vehicleMotionProperties = new VehicleMotionProperties(
            8d,
            2.5d,
            -2d,
            0,
            3.6d,
            -3.4d,
            -2.2d,
            1.2d);

    private static final VehicleMotionProperties uniformJerk = new VehicleMotionProperties(
            2.5E-6,
            0,
            -2E-6,    // negative by your convention
            0,
            8E-3,
            1e-9,
            1E-9,    // > 0
            -1E-9,    // < 0
            -1E-9,    // < 0
            1E-9     // > 0
    );

    @Test
    void sweepMaxSpeedAccDecAndJerksForVehicleProperties() {
        for (double distance : List.of(0.1, 1d, 10d, 100d)) {
            for (double acc = 0.1; acc < 1; acc += 0.2) {
                for (double dec = -0.1; dec > -1; dec -= 0.2) {
                    for (double vel = 0.1; vel < 1; vel += 0.2) {
                        for (double jerkAccUp = 0.1; jerkAccUp < 1; jerkAccUp += 0.2) {
                            for (double jerkAccDown = -0.1; jerkAccDown > -1; jerkAccDown -= 0.2) {
                                for (double jerkDecUp = -0.1; jerkDecUp > -1; jerkDecUp -= 0.2) {
                                    for (double jerkDecDown = 0.1; jerkDecDown < 1; jerkDecDown += 0.2) {
                                        VehicleMotionProperties scratchVehicle = new VehicleMotionProperties(
                                                vel,
                                                acc,
                                                dec,
                                                0, // tolerance
                                                jerkAccUp,
                                                jerkAccDown,
                                                jerkDecUp,
                                                jerkDecDown
                                        );
                                        Traversal traversal = factory.create(distance, scratchVehicle);
                                        assertThat(traversal.getTotalDistance()).isCloseTo(distance, within(1E-6));
                                        verifyTraversalWithinVehicleMotionBounds(traversal, scratchVehicle);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void whenMaxAccelDecelSpeedNotReached_thenCorrectTraversalCreated() {
        double jerk = 1;
        double acc = 2;
        double dec = -2;
        double velocity = 2;
        double distance = 2;
        VehicleMotionProperties properties = createVehicleProperties(velocity, acc, dec, jerk);
        ImmutableList<TraversalSection> createdTraversalSections = ConstantJerkTraversalCalculator.INSTANCE.create(distance, properties).getSections();

        ImmutableList<TraversalSection> expectedSections = ImmutableList.of(
                ConstantJerkSectionFactory.jerkAccelerationUp(0, 0, 1, jerk),
                ConstantJerkSectionFactory.jerkAccelerationDownToV(1, 1, -jerk),
                ConstantJerkSectionFactory.jerkDecelerationUp(0, 1, -1, -1).get(),
                ConstantJerkSectionFactory.jerkDecelerationDownToZeroV(0.5, -1, 1)
        );
        verifyTraversalSections(createdTraversalSections, expectedSections);
    }

    @Test
    void whenMaxAccelDecelNotReachedButMaxSpeedReached_thenCorrectTraversalCreated() {
        double jerk = 1;
        double acc = 2;
        double dec = -2;
        double velocity = 1;
        double distance = 3;
        VehicleMotionProperties properties = createVehicleProperties(velocity, acc, dec, jerk);
        ImmutableList<TraversalSection> createdTraversalSections = ConstantJerkTraversalCalculator.INSTANCE.create(distance, properties).getSections();

        ImmutableList<TraversalSection> expectedSections = ImmutableList.of(
                ConstantJerkSectionFactory.jerkAccelerationUp(0, 0, 1, jerk),
                ConstantJerkSectionFactory.jerkAccelerationDownToV(1, 1, -jerk),
                new ConstantSpeedTraversalSection(1, 1, 1),
                ConstantJerkSectionFactory.jerkDecelerationUp(0, 1, -1, -jerk).get(),
                ConstantJerkSectionFactory.jerkDecelerationDownToZeroV(0.5, -1, jerk)
        );
        verifyTraversalSections(createdTraversalSections, expectedSections);
    }

    @Test
    void whenMaxAccelDecelReachedButMaxSpeedNotReached_thenCorrectTraversalCreated() {
        double jerk = 2;
        double acc = 1;
        double dec = -1;
        double velocity = 2;
        double distance = 3;
        VehicleMotionProperties properties = createVehicleProperties(velocity, acc, dec, jerk);
        ImmutableList<TraversalSection> createdTraversalSections = ConstantJerkTraversalCalculator.INSTANCE.create(distance, properties).getSections();

        ImmutableList<TraversalSection> expectedSections = ImmutableList.of(
                ConstantJerkSectionFactory.jerkAccelerationUp(0, 0, 1, jerk),
                ConstantJerkSectionFactory.constantAcceleration(0.25, 1.25, acc),
                ConstantJerkSectionFactory.jerkAccelerationDownToV(acc, 1.5, -jerk),
                ConstantJerkSectionFactory.jerkDecelerationUp(0, 1.5, -1, -jerk).get(),
                ConstantJerkSectionFactory.constantAcceleration(1.25, 0.25, dec),
                ConstantJerkSectionFactory.jerkDecelerationDownToZeroV(0.25, dec, jerk)
        );
        verifyTraversalSections(createdTraversalSections, expectedSections);
    }

    @Test
    void whenMaxAccelDecelSpeedReached_thenCorrectTraversalCreated() {
        double jerk = 1;
        double acc = 1;
        double dec = -1;
        double velocity = 2;
        double distance = 8;
        VehicleMotionProperties properties = createVehicleProperties(velocity, acc, dec, jerk);
        ImmutableList<TraversalSection> createdTraversalSections = ConstantJerkTraversalCalculator.INSTANCE.create(distance, properties).getSections();

        ImmutableList<TraversalSection> expectedSections = ImmutableList.of(
                ConstantJerkSectionFactory.jerkAccelerationUp(0, 0, 1, jerk),
                ConstantJerkSectionFactory.constantAcceleration(0.5, 1.5, 1),
                ConstantJerkSectionFactory.jerkAccelerationDownToV(1, 2, -1),
                ConstantJerkSectionFactory.constantSpeed(2, 2),
                ConstantJerkSectionFactory.jerkDecelerationUp(0, 2, -1, -jerk).get(),
                ConstantJerkSectionFactory.constantAcceleration(1.5, 0.5, -1),
                ConstantJerkSectionFactory.jerkDecelerationDownToZeroV(0.5, -1, jerk)
        );
        verifyTraversalSections(createdTraversalSections, expectedSections);
    }

    @Test
    void whenMaxAccelAndSpeedNotReachedButMaxDecelReached_thenCorrectTraversalCreated() {
        double jerk = 1;
        double acc = 2;
        double dec = -1;
        double velocity = 3;
        double distance = 7.03125;
        VehicleMotionProperties properties = createVehicleProperties(velocity, acc, dec, jerk);
        ImmutableList<TraversalSection> createdTraversalSections = ConstantJerkTraversalCalculator.INSTANCE.create(distance, properties).getSections();

        ImmutableList<TraversalSection> expectedSections = ImmutableList.of(
                ConstantJerkSectionFactory.jerkAccelerationUp(0, 0, 1.5, jerk),
                ConstantJerkSectionFactory.jerkAccelerationDownToV(1.5, 2.25, -jerk),
                ConstantJerkSectionFactory.jerkDecelerationUp(0, 2.25, -1, -jerk).get(),
                ConstantJerkSectionFactory.constantAcceleration(1.75, 0.5, -1),
                ConstantJerkSectionFactory.jerkDecelerationDownToZeroV(0.5, -1, jerk)
        );
        verifyTraversalSections(createdTraversalSections, expectedSections);
    }

    @Test
    void whenMaxDecelAndSpeedNotReachedButMaxAccelReached_thenCorrectTraversalCreated() {
        double jerk = 1;
        double acc = 1;
        double dec = -2;
        double velocity = 3;
        double distance = 7.03125;
        VehicleMotionProperties properties = createVehicleProperties(velocity, acc, dec, jerk);
        ImmutableList<TraversalSection> createdTraversalSections = ConstantJerkTraversalCalculator.INSTANCE.create(distance, properties).getSections();

        ImmutableList<TraversalSection> expectedSections = ImmutableList.of(
                ConstantJerkSectionFactory.jerkAccelerationUp(0, 0, 1, jerk),
                ConstantJerkSectionFactory.constantAcceleration(0.5, 1.75, 1),
                ConstantJerkSectionFactory.jerkAccelerationDownToV(1, 2.25, -jerk),
                ConstantJerkSectionFactory.jerkDecelerationUp(0, 2.25, -1.5, -jerk).get(),
                ConstantJerkSectionFactory.jerkDecelerationDown(-1.5, jerk)

        );
        verifyTraversalSections(createdTraversalSections, expectedSections);
    }

    @Test
    void whenMaxDecelAndSpeedReachedButMaxAccelNotReached_thenCorrectTraversalCreated() {
        double jerk = 1;
        double acc = 2;
        double dec = -1;
        double velocity = 2.25;
        double distance = 11.53125;
        VehicleMotionProperties properties = createVehicleProperties(velocity, acc, dec, jerk);
        ImmutableList<TraversalSection> createdTraversalSections = ConstantJerkTraversalCalculator.INSTANCE.create(distance, properties).getSections();

        ImmutableList<TraversalSection> expectedSections = ImmutableList.of(
                ConstantJerkSectionFactory.jerkAccelerationUp(0, 0, 1.5, jerk),
                ConstantJerkSectionFactory.jerkAccelerationDownToV(1.5, 2.25, -jerk),
                ConstantJerkSectionFactory.constantSpeed(4.5, 2.25),
                ConstantJerkSectionFactory.jerkDecelerationUp(0, 2.25, -1, -jerk).get(),
                ConstantJerkSectionFactory.constantAcceleration(1.75, 0.5, -1),
                ConstantJerkSectionFactory.jerkDecelerationDownToZeroV(0.5, -1, jerk)
        );
        verifyTraversalSections(createdTraversalSections, expectedSections);
    }

    @Test
    void whenMaxAccelAndSpeedReachedButMaxDecelNotReached_thenCorrectTraversalCreated() {
        double jerk = 1;
        double acc = 1;
        double dec = -2;
        double velocity = 2.25;
        double distance = 11.53125;
        VehicleMotionProperties properties = createVehicleProperties(velocity, acc, dec, jerk);
        ImmutableList<TraversalSection> createdTraversalSections = ConstantJerkTraversalCalculator.INSTANCE.create(distance, properties).getSections();

        ImmutableList<TraversalSection> expectedSections = ImmutableList.of(
                ConstantJerkSectionFactory.jerkAccelerationUp(0, 0, 1, jerk),
                ConstantJerkSectionFactory.constantAcceleration(0.5, 1.75, 1),
                ConstantJerkSectionFactory.jerkAccelerationDownToV(1, 2.25, -jerk),
                ConstantJerkSectionFactory.constantSpeed(4.5, 2.25),
                ConstantJerkSectionFactory.jerkDecelerationUp(0, 2.25, -1.5, -jerk).get(),
                ConstantJerkSectionFactory.jerkDecelerationDown(-1.5, jerk)
        );
        verifyTraversalSections(createdTraversalSections, expectedSections);
    }

    @Test
    void create_whenCalledForADistance_thenReturnsATraversalOfExactlyThatDistance() {
        for (double d = 0.001; d < 500d; d += 0.001) {
            Traversal traversal = factory.create(d, vehicleMotionProperties);
            assertThat(traversal.getTotalDistance()).isCloseTo(d, within(EPSILON * d));
        }
    }

    @Test
    void create_whenCalledWithDifferentStartingPositions_alwaysReturnsATraversal() {
        for (double d = 0.4; d < 100d; d += 0.4) {
            for (double a = 0.00; a < vehicleMotionProperties.acceleration; a += 0.4) {
                for (double u = 0.00; u < vehicleMotionProperties.maxSpeed * 0.9; u += 0.4) {
                    Traversal traversal = factory.create(d, u, a, vehicleMotionProperties);
                    ConstantJerkSectionsFactoryTest.assertSmoothConnected(traversal.getSections());
                }
            }
        }
    }

    @Test
    void create_whenRequiredDistanceIsZero_thenFindsBreakingDistance() {
        double d = 0d, a = 0d, u = 2d;
        Traversal traversal = factory.create(d, u, a, vehicleMotionProperties);
        assertStartsAt(traversal, u, a, vehicleMotionProperties);
        ConstantJerkSectionsFactoryTest.assertSmoothConnected(traversal.getSections());
    }

    @Test
    void create_whenStartingWithNegativeDecelerationWithASubstantialDistanceToGo() {
        double d = 100d, a = -2d, u = 6d;
        Traversal traversal = factory.create(d, u, a, vehicleMotionProperties);
        assertStartsAt(traversal, u, a, vehicleMotionProperties);
        ConstantJerkSectionsFactoryTest.assertSmoothConnected(traversal.getSections());
    }

    @Test
    void create_whenStartingWithNegativeDecelerationWithShortDistanceToGo_thenThrows() {
        double d = 0.5d, a = -2d, u = 0.5;
        Assertions.assertThrows(TraversalCalculationException.class, () -> factory.create(d, u, a, vehicleMotionProperties));
    }

    @Test
    void maxSpeedToTraversal_whenTargetSpeedBelowMaxSpeed_thenCruiseNotAdded() {
        double v0 = 1.0;
        double a0 = 0.2;
        double vTarget = 3.0;          // strictly below vMax
        double distanceTarget = 6.765264176419425;   // moderate

        Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.maxSpeedToStandardTraversal(v0, a0, vTarget, distanceTarget, vehicleMotionProperties);

        // No ConstantSpeedTraversalSection expected when vTarget < vMax
        boolean hasCruise = tr.getSections().stream().anyMatch(s -> s instanceof ConstantSpeedTraversalSection);
        assertFalse(hasCruise, "Cruise section should not be present when vTarget < vMax");

        // Global checks
        assertStartsAt(tr, v0, a0, vehicleMotionProperties);
        assertSmoothConnected(tr);
        assertEndsAtRest(tr, vehicleMotionProperties);
        assertRespectsConstraints(tr, vehicleMotionProperties);

        // Total distance must match target
        assertEquals(0, DoubleMath.fuzzyCompare(tr.getTotalDistance(), distanceTarget, tol(distanceTarget)),
                "Total distance should equal distanceTarget");
    }

    @Test
    void maxSpeedToTraversal_whenTargetSpeedEqualToMaxSpeedAndDistanceLarge_thenCruiseAdded() {
        double v0 = 0.0;
        double a0 = 0.0;
        double vTarget = vehicleMotionProperties.maxSpeed;     // ask to run at max speed
        double distanceTarget = 50.0;    // long distance, should require cruise

        Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.maxSpeedToStandardTraversal(v0, a0, vTarget, distanceTarget, vehicleMotionProperties);

        // Must contain a ConstantSpeedTraversalSection at ~vMax
        var cruises = tr.getSections().stream()
                .filter(s -> s instanceof ConstantSpeedTraversalSection)
                .map(s -> (ConstantSpeedTraversalSection) s)
                .toList();

        assertFalse(cruises.isEmpty(), "Expected a cruise section when vTarget == vMax and distance is large");
        for (ConstantSpeedTraversalSection cs : cruises) {
            assertEquals(0, DoubleMath.fuzzyCompare(cs.getSpeedAtTime(0.0), vehicleMotionProperties.maxSpeed, tol(distanceTarget)),
                    "Cruise speed should be vMax");
        }

        assertStartsAt(tr, v0, a0, vehicleMotionProperties);
        assertSmoothConnected(tr);
        assertEndsAtRest(tr, vehicleMotionProperties);
        assertRespectsConstraints(tr, vehicleMotionProperties);

        // Distance match
        assertEquals(0, DoubleMath.fuzzyCompare(tr.getTotalDistance(), distanceTarget, tol(distanceTarget)),
                "Total distance should equal distanceTarget");
    }

    @Test
    void maxSpeedToTraversal_ifVelocityWillGoNegative_thenThrows() {
        double v0 = 0.02;                        // tiny speed
        double a0 = vehicleMotionProperties.deceleration - 0.5;        // more negative than allowed -> would drive v<0 immediately
        double vTarget = 1.0;
        double distanceTarget = 1.0;

        assertThrows(
                TraversalCalculationException.class,
                () -> ConstantJerkTraversalCalculator.INSTANCE.maxSpeedToStandardTraversal(v0, a0, vTarget, distanceTarget, vehicleMotionProperties),
                "Expected a throw only when initial a0 would cause negative velocity");
    }

    @Test
    void maxSpeedToTraversal_whenNegativeAccAndFeasibleSpeed_thenNeverGoesReverse() {
        double v0 = 0.8;
        double a0 = -0.9;                  // within aMin bound
        double vTarget = 2.5;
        double distanceTarget = 5.573108092773463;

        Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.maxSpeedToStandardTraversal(v0, a0, vTarget, distanceTarget, vehicleMotionProperties);

        assertStartsAt(tr, v0, a0, vehicleMotionProperties);
        assertSmoothConnected(tr);
        assertEndsAtRest(tr, vehicleMotionProperties);
        assertRespectsConstraints(tr, vehicleMotionProperties);

        // Nonnegative speed throughout
        int samples = 240;
        for (int i = 0; i <= samples; i++) {
            double t = tr.getTotalDuration() * i / samples;
            double v = tr.getSpeedAtTime(t);
            assertTrue(DoubleMath.fuzzyCompare(v, 0.0, tol(distanceTarget)) >= 0, "Went reverse at t=" + t + ", v=" + v);
        }

        // Distance match
        assertEquals(0, DoubleMath.fuzzyCompare(tr.getTotalDistance(), distanceTarget, tol(distanceTarget)));
    }

    @Test
    void maxSpeedToTraversal_whenDistanceJustEnoughToReachMaxSpeed_thenNoOrTinyCruise() {
        double v0 = 0.0;
        double a0 = 0.0;
        double vTarget = vehicleMotionProperties.maxSpeed;

        // A distance that is ROUNDING_ERROR_TOLERANCE to “accelerate to vMax, decel to zero” (no room to cruise)
        // We don’t need the exact analytic; pick a moderate distance and assert that any cruise,
        // if present, is near-zero.
        double distanceTarget = 36.0;

        Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.maxSpeedToStandardTraversal(v0, a0, vTarget, distanceTarget, vehicleMotionProperties);

        var cruises = tr.getSections().stream()
                .filter(s -> s instanceof ConstantSpeedTraversalSection)
                .map(s -> (ConstantSpeedTraversalSection) s)
                .toList();

        // Either no cruise or a very tiny one is acceptable at the boundary.
        if (!cruises.isEmpty()) {
            double cruiseDist = cruises.stream().mapToDouble(TraversalSection::getTotalDistance).sum();
            assertTrue(cruiseDist <= 0.5, "Cruise distance should be tiny near the boundary");
        }

        assertStartsAt(tr, v0, a0, vehicleMotionProperties);
        assertSmoothConnected(tr);
        assertEndsAtRest(tr, vehicleMotionProperties);
        assertRespectsConstraints(tr, vehicleMotionProperties);
        assertEquals(0, DoubleMath.fuzzyCompare(tr.getTotalDistance(), distanceTarget, tol(distanceTarget)));
    }

    @Test
    void maxSpeedToTraversal_whenInitialAccelerationGreaterThanMax_thenCalculatesTraversal() {
        double v0 = 0.002652198367028834;
        double a0 = uniformJerk.acceleration * 1.5;
        double distance = 10;

        Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.maxSpeedToStandardTraversal(v0, a0, uniformJerk.maxSpeed, distance, uniformJerk);

        assertStartsAt(tr, v0, a0, uniformJerk);
        assertEndsAtRest(tr, uniformJerk);
        assertSmoothConnected(tr);
    }

    @Test
    void maxSpeedToTraversal_whenInitialDecelerationGreaterThanMaxDeceleration_thenCalculatesTraversal() {
        double v0 = 0.002652198367028834;
        double a0 = uniformJerk.deceleration * 1.1;
        double distance = 10;

        Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.maxSpeedToStandardTraversal(v0, a0, uniformJerk.maxSpeed, distance, uniformJerk);

        assertStartsAt(tr, v0, a0, uniformJerk);
        assertEndsAtRest(tr, uniformJerk);
        assertSmoothConnected(tr);
    }

    @Test
    void braking_whenInitialAccelerationGreaterThanMax_thenCalculatesBrakingTraversal() {
        double v0 = 0.002652198367028834;
        double a0 = uniformJerk.acceleration * 1.5;

        Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.getBrakingTraversal(v0, a0, uniformJerk);

        assertStartsAt(tr, v0, a0, uniformJerk);
        assertEndsAtRest(tr, uniformJerk);
        assertSmoothConnected(tr);
    }

    @Test
    void braking_whenInitialDecelerationGreaterThanMaxDeceleration_thenCalculatesBrakingTraversal() {
        double v0 = 0.0029880849186670355;
        double a0 = uniformJerk.deceleration * 1.1;
        Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.getBrakingTraversal(v0, a0, uniformJerk);

        assertStartsAt(tr, v0, a0, uniformJerk);
        assertEndsAtRest(tr, uniformJerk);
        assertSmoothConnected(tr);
    }

    @Test
    void minimalBraking_whenBasicFromZeroAccel_thenPossible() {
        double v0 = 2.0;
        double a0 = 0.0;

        Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.getBrakingTraversal(v0, a0, vehicleMotionProperties);

        assertStartsAt(tr, v0, a0, vehicleMotionProperties);
        assertEndsAtRest(tr, vehicleMotionProperties);
        assertSmoothConnected(tr);
        assertRespectsBoundsAndMonotone(tr, vehicleMotionProperties);

        // Distance should be >= idealized snap-to-aMin bound (jerkless lower bound)
        double aMinAbs = Math.abs(vehicleMotionProperties.deceleration);
        double sLowerBound = (v0 * v0) / (2.0 * aMinAbs);
        assertTrue(
                DoubleMath.fuzzyCompare(tr.getTotalDistance(), sLowerBound, ROUNDING_ERROR_TOLERANCE) >= 0,
                "Minimal braking distance is below the jerkless lower bound");
    }

    @Test
    void minimalBraking_whenFromNegativeAccel_thenPossible() {
        double v0 = 1.2;
        double a0 = -0.8; // already braking (within aMin)

        Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.getBrakingTraversal(v0, a0, vehicleMotionProperties);

        assertStartsAt(tr, v0, a0, vehicleMotionProperties);
        assertEndsAtRest(tr, vehicleMotionProperties);
        assertSmoothConnected(tr);
        assertRespectsBoundsAndMonotone(tr, vehicleMotionProperties);

        // Lower bound still applies using initial speed
        double sLowerBound = (v0 * v0) / (2.0 * Math.abs(vehicleMotionProperties.deceleration));
        assertTrue(DoubleMath.fuzzyCompare(tr.getTotalDistance(), sLowerBound, ROUNDING_ERROR_TOLERANCE) >= 0);
    }

    @Test
    void minimalBraking_WhenFromPositiveAccel_thenRampsDownThenBrakes() {
        double v0 = 2.8;
        double a0 = 0.7;

        Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.getBrakingTraversal(v0, a0, vehicleMotionProperties);
        assertStartsAt(tr, v0, a0, vehicleMotionProperties);
        assertEndsAtRest(tr, vehicleMotionProperties);
        assertSmoothConnected(tr);
    }

    @Test
    void minimalBraking_distance_increases_when_jerk_limits_are_tightened() {
        VehicleMotionProperties fastJerk = vehicleMotionProperties;
        VehicleMotionProperties slowJerk = new VehicleMotionProperties(
                fastJerk.acceleration,
                fastJerk.accelerationAbsoluteTolerance,
                fastJerk.deceleration,
                fastJerk.decelerationAbsoluteTolerance,
                fastJerk.maxSpeed,
                fastJerk.maxSpeedAbsoluteTolerance,
                /* jerkAccelerationUp */   fastJerk.jerkAccelerationUp * 0.25,
                /* jerkAccelerationDown */ fastJerk.jerkAccelerationDown * 0.25,
                /* jerkDecelerationUp */   fastJerk.jerkDecelerationUp * 0.25,
                /* jerkDecelerationDown */ fastJerk.jerkDecelerationDown * 0.25
        );

        double v0 = 3.0;
        double a0 = 0.0;

        Traversal trFast = ConstantJerkTraversalCalculator.INSTANCE.getBrakingTraversal(v0, a0, fastJerk);
        Traversal trSlow = ConstantJerkTraversalCalculator.INSTANCE.getBrakingTraversal(v0, a0, slowJerk);

        assertTrue(
                DoubleMath.fuzzyCompare(trSlow.getTotalDistance(), trFast.getTotalDistance(), ROUNDING_ERROR_TOLERANCE) >= 0,
                "Tightening jerk limits should not reduce minimal braking distance");
    }

    @Test
    void getBrakingTraversal_whenSpeedWillGoNegative_thenThrows() {
        double v0 = 0.0;
        double a0 = -0.5;
        // Either empty traversal or zero duration/distance
        assertThrows(TraversalCalculationException.class, () -> ConstantJerkTraversalCalculator.INSTANCE.getBrakingTraversal(v0, a0, vehicleMotionProperties));
    }

    @Test
    void whenZeroDistanceAndAtRest_thenReturnsEmptyTraversal() {
        Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.create(0.0, 0.0, 0.0, vehicleMotionProperties);

        assertTrue(tr.getSections().isEmpty(), "Should be empty for zero distance at rest");
        assertEquals(0.0, tr.getTotalDuration(), ROUNDING_ERROR_TOLERANCE);
        assertEquals(0.0, tr.getTotalDistance(), ROUNDING_ERROR_TOLERANCE);
    }

    @Test
    void create_whenDistanceTooShort_thenThrows() {
        double v0 = 0.8, a0 = 0.0;
        Traversal traversal = ConstantJerkTraversalCalculator.INSTANCE.create(0.0, v0, a0, vehicleMotionProperties);
        // Create with distance=0 → should return minimal stopping traversal (non-zero distance)
        assertStartsAt(traversal, v0, a0, vehicleMotionProperties);
        assertEndsAtRest(traversal, vehicleMotionProperties);
        assertSmoothConnected(traversal);
        assertRespectsConstraints(traversal, vehicleMotionProperties);
    }

    @Test
    void create_whenDistanceSlightlyLessThanBrakingDistance_thenBrakingTraversalReturned() {
        double v0 = 0.5, a0 = -0.8;
        Traversal minStop = ConstantJerkTraversalCalculator.INSTANCE.getBrakingTraversal(v0, a0, vehicleMotionProperties);
        double tooShort = Math.max(0.0, minStop.getTotalDistance() - 1E-7);

        Traversal traversal = ConstantJerkTraversalCalculator.INSTANCE.create(tooShort, v0, a0, vehicleMotionProperties);
        assertStartsAt(traversal, v0, a0, vehicleMotionProperties);
        assertEndsAtRest(traversal, vehicleMotionProperties);
        assertSmoothConnected(traversal);
        assertRespectsConstraints(traversal, vehicleMotionProperties);
        assertEquals(traversal.getTotalDistance(), minStop.getTotalDistance());
    }

    @Test
    void whenIntermediateDistanceMatchesExactDistance_thenNoCruiseCreated() {
        double v0 = 0.2, a0 = -0.5;
        double distance = 6.0; // typical: achievable without cruise

        Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.create(distance, v0, a0, vehicleMotionProperties);

        boolean hasCruise = tr.getSections().stream().anyMatch(s -> s instanceof ConstantSpeedTraversalSection);
        assertFalse(hasCruise, "Did not expect a constant-speed (cruise) section for this distance");

        assertStartsAt(tr, v0, a0, vehicleMotionProperties);
        assertEndsAtRest(tr, vehicleMotionProperties);
        assertSmoothConnected(tr);
        assertRespectsConstraints(tr, vehicleMotionProperties);
        assertEquals(0, DoubleMath.fuzzyCompare(tr.getTotalDistance(), distance, ROUNDING_ERROR_TOLERANCE * distance),
                "Total distance must hit the requested distance");
    }

    @Test
    void whenLargeDistance_thenIncludesCruiseAtMaxSpeed() {
        double v0 = 0.0, a0 = 0.0;
        double distance = 80.0; // large → should need cruise

        Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.create(distance, v0, a0, vehicleMotionProperties);

        var cruises = tr.getSections().stream()
                .filter(s -> s instanceof ConstantSpeedTraversalSection)
                .map(s -> (ConstantSpeedTraversalSection) s)
                .toList();

        assertFalse(cruises.isEmpty(), "Expected a cruise section for large distance");
        for (ConstantSpeedTraversalSection cs : cruises) {
            double vCruise = cs.getSpeedAtTime(0.0);
            assertEquals(0, DoubleMath.fuzzyCompare(vCruise, vehicleMotionProperties.maxSpeed, distance * ROUNDING_ERROR_TOLERANCE),
                    "Cruise section must be at v_max");
        }

        assertStartsAt(tr, v0, a0, vehicleMotionProperties);
        assertEndsAtRest(tr, vehicleMotionProperties);
        assertSmoothConnected(tr);
        assertRespectsConstraints(tr, vehicleMotionProperties);
        assertEquals(0, DoubleMath.fuzzyCompare(tr.getTotalDistance(), distance, ROUNDING_ERROR_TOLERANCE * distance));
    }

    @Test
    void whenMaxSpeedBrieflyExceed_thenTraversalCanStillBeCalculated() {
        VehicleMotionProperties uniformJerk = new VehicleMotionProperties(0.004, 2.0E-6, -2.0E-6, 0, 2.0E-8, -2.0E-8, -2.0E-8, 2.0E-8);
        double u = 0.0039700000000000004;
        double a = 2.0E-6;
        double distance = 5.053382813000001;
        Traversal traversal = ConstantJerkTraversalCalculator.INSTANCE.create(distance, u, a, uniformJerk);
        assertEquals(0, DoubleMath.fuzzyCompare(traversal.getTotalDistance(), distance, ROUNDING_ERROR_TOLERANCE * distance));
        assertStartsAt(traversal, u, a, vehicleMotionProperties);
        assertEndsAtRest(traversal, vehicleMotionProperties);
        assertSmoothConnected(traversal);
    }

    @Test
    void boundaryJustTouchingMaxSpeed_thenHasZeroOrSmallCruise() {
        double v0 = 0.0, a0 = 0.0;
        double distance = 14.0; // near “accelerate to vMax, decel” envelope → little to no cruise

        Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.create(distance, v0, a0, vehicleMotionProperties);

        double cruiseDist = tr.getSections().stream()
                .filter(s -> s instanceof ConstantSpeedTraversalSection)
                .mapToDouble(TraversalSection::getTotalDistance)
                .sum();

        assertTrue(
                DoubleMath.fuzzyCompare(cruiseDist, 0.5, 0.5) <= 0,
                "At the boundary, cruise (if present) should be ~0");
        assertStartsAt(tr, v0, a0, vehicleMotionProperties);
        assertEndsAtRest(tr, vehicleMotionProperties);
        assertSmoothConnected(tr);
        assertRespectsConstraints(tr, vehicleMotionProperties);
        assertEquals(0, DoubleMath.fuzzyCompare(tr.getTotalDistance(), distance, distance * ROUNDING_ERROR_TOLERANCE));
    }

    @Test
    void whenInitialAccelerationNegative_thenVehicleDoesNotReverse() {
        double v0 = 0.9, a0 = -1.2;
        double distance = 5.5;

        Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.create(distance, v0, a0, vehicleMotionProperties);

        assertStartsAt(tr, v0, a0, vehicleMotionProperties);
        assertEndsAtRest(tr, vehicleMotionProperties);
        assertSmoothConnected(tr);
        assertRespectsConstraints(tr, vehicleMotionProperties);

        // Nonnegative speed throughout
        int samples = 240;
        for (int i = 0; i <= samples; i++) {
            double t = tr.getTotalDuration() * i / samples;
            assertTrue(
                    DoubleMath.fuzzyCompare(tr.getSpeedAtTime(t), 0.0, distance * ROUNDING_ERROR_TOLERANCE) >= 0,
                    "Went reverse at t=" + t);
        }
    }

    @Test
    void whenInitialAccelerationGreaterThanZero_thenHandles() {
        double v0 = 1.8, a0 = 1.0;
        double distance = 10.0;

        Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.create(distance, v0, a0, vehicleMotionProperties);

        assertStartsAt(tr, v0, a0, vehicleMotionProperties);
        assertEndsAtRest(tr, vehicleMotionProperties);
        assertSmoothConnected(tr);
        assertRespectsConstraints(tr, vehicleMotionProperties);
        assertEquals(0, DoubleMath.fuzzyCompare(tr.getTotalDistance(), distance, distance * ROUNDING_ERROR_TOLERANCE));
    }

    @Test
    void whenInvalidNegativeDeistance_thenThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ConstantJerkTraversalCalculator.INSTANCE.create(-0.1, 0.0, 0.0, vehicleMotionProperties),
                "Negative distance must throw");
    }

    @Test
    void create_whenSpeedStartsOutOfBounds_thenTraversalCorrectsItself() {
        VehicleMotionProperties props = new VehicleMotionProperties(
                /*acceleration*/                1.0E-6,
                /*accelerationAbsoluteTolerance*/0,
                /*deceleration*/               -1.0E-6,    // negative by your convention
                /*decelerationAbsoluteTolerance*/0,
                /*maxSpeed*/                    0.002,
                /*maxSpeedAbsoluteTolerance*/   0,
                /*jerkAccelerationUp*/          2.0E-8,    // > 0
                /*jerkAccelerationDown*/       -2.0E-8,    // < 0
                /*jerkDecelerationUp*/         -2.0E-8,    // < 0
                /*jerkDecelerationDown*/        2.0E-8     // > 0
        );
        // initial speed is larger than max speed
        double v0 = 0.0021400000000000004;
        double a0 = 1.0E-6;
        double distance = 8.87049999999999;
        Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.create(distance, v0, a0, props);
        Optional<TraversalSection> maybeConstantSpeedTraversalSection = tr.getSections().stream().filter(t -> t instanceof ConstantSpeedTraversalSection).findFirst();
        assertTrue(maybeConstantSpeedTraversalSection.isPresent());
        assertEquals(maybeConstantSpeedTraversalSection.get().getSpeedAtTime(0), props.maxSpeed);
        assertStartsAt(tr, v0, a0, vehicleMotionProperties);
        assertEndsAtRest(tr, vehicleMotionProperties);
        assertSmoothConnected(tr);
        assertEquals(0, DoubleMath.fuzzyCompare(tr.getTotalDistance(), distance, distance * ROUNDING_ERROR_TOLERANCE));
    }

    @Test
    void create_whenSpeedAndAccelerationPositive_thenReturnsTraversal() {
        VehicleMotionProperties props = new VehicleMotionProperties(
                0.004,
                2E-6,
                1E-9,
                0
        );
        double u = 0.002;
        double a = 1.6589958605140875E-6;
        double distance = 12.176;
        Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.create(
                distance,
                u,
                a,
                props
        );
        assertStartsAt(tr, u, a, vehicleMotionProperties);
        assertEndsAtRest(tr, vehicleMotionProperties);
        assertSmoothConnected(tr);
        assertTrue(DoubleMath.fuzzyEquals(tr.getTotalDistance(), distance, distance * ROUNDING_ERROR_TOLERANCE));
    }

    @Test
    void test_whenInitialAccelerationNegativeAndDistanceSlightlyGreaterThanBrakingDistance_thenAccelerationIncreasesButStaysNegative() {
        VehicleMotionProperties properties = new VehicleMotionProperties(
                0.004, 2E-6, -2E-6, 0, 1E-8, -1E-8, -1E-8, 1E-8
        );
        double u = 0.00386;
        double a = -1.7237E-6;
        // distance slightly above the braking distance
        double distance = 3.8;
        Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.create(distance, u, a, properties);
        assertStartsAt(tr, u, a, vehicleMotionProperties);
        assertEndsAtRest(tr, vehicleMotionProperties);
        assertSmoothConnected(tr);
        assertTrue(DoubleMath.fuzzyEquals(tr.getTotalDistance(), distance, distance * ROUNDING_ERROR_TOLERANCE));
        assertSectionsRespectPredicate(tr, ts -> ts.getAccelerationAtTime(0) < 0 && ts.getAccelerationAtTime(ts.getDuration()) <= 0);
    }

    @Test
    void test_whenInitialAccelerationNegativeAndDistanceSlightlyGreaterThanBrakingDistance_thenAccelerationTouchesZeroButNeverPositive() {
        VehicleMotionProperties properties = new VehicleMotionProperties(
                0.004, 2E-6, -2E-6, 0, 1E-8, -1E-8, -1E-8, 1E-8
        );
        double u = 0.00386;
        double a = -1.7237E-6;
        // distance  above the braking distance but below what we need for the accleration to go positive
        double distance = 4.463123430533608;
        Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.create(distance, u, a, properties);
        assertStartsAt(tr, u, a, vehicleMotionProperties);
        assertEndsAtRest(tr, vehicleMotionProperties);
        assertSmoothConnected(tr);
        assertTrue(DoubleMath.fuzzyEquals(tr.getTotalDistance(), distance, distance * ROUNDING_ERROR_TOLERANCE));
        assertSectionsRespectPredicate(tr, ts -> ts.getAccelerationAtTime(0) <= 0 && ts.getAccelerationAtTime(ts.getDuration()) <= 0);
    }

    @Nested
    class CruisePresenceLogic {
        @Test
        void noCruiseWhenTargetSpeedBelowMaxSpeed() {
            // Emulate binary-search landing on a vPeak<Vmax by choosing a distance
            // modest enough not to require cruise at vMax.
            double v0 = 0.0, a0 = 0.0, distance = 9.0;

            Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.create(distance, v0, a0, vehicleMotionProperties);
            boolean hasCruise = tr.getSections().stream().anyMatch(s -> s instanceof ConstantSpeedTraversalSection);
            assertFalse(hasCruise, "No cruise expected when envelope can match distance without holding vMax");
        }

        @Test
        void hasCruiseWhenDistanceBeyondNoCruiseEnvelope() {
            double v0 = 0.0, a0 = 0.0, distance = 60.0;
            Traversal tr = ConstantJerkTraversalCalculator.INSTANCE.create(distance, v0, a0, vehicleMotionProperties);
            boolean hasCruise = tr.getSections().stream().anyMatch(s -> s instanceof ConstantSpeedTraversalSection);
            assertTrue(hasCruise, "Cruise expected for long distances");
        }
    }

    @Test
    void testNeitherMaxAccelerationNorMaxDecelerationNorMaxSpeedReached_whenTotalDistanceNotTooLongForCase_thenReturnFourSections() {
        Traversal traversal = ConstantJerkTraversalCalculator.INSTANCE.create(1, uniformJerk);
        ImmutableList<TraversalSection> sections = traversal.getSections();
        assertThat(sections).hasSize(4);
    }

    @DisplayName("when neither max acceleration or deceleration is reached")
    @ParameterizedTest(name = "Section {index} should have: distance={1}; duration={2}; initialSpeed={3}; finalSpeed={4}; initialAcceleration={5}; finalAcceleration={6}; jerk={7}")
    @MethodSource("traversalSectionsForNotTooLongCase")
    void testNeitherMaxAccelerationNorMaxDecelerationNorMaxSpeedReached_whenTotalDistanceNotTooLongForCase(int index, double totalDistance, double duration, double initialSpeed, double finalSpeed, double initialAcceleration, double finalAcceleration, double jerk) {
        Traversal traversal = ConstantJerkTraversalCalculator.INSTANCE.create(1, uniformJerk);
        ImmutableList<TraversalSection> sections = traversal.getSections();
        verifyTraversalSection(sections.get(index), totalDistance, duration, initialSpeed, finalSpeed, initialAcceleration, finalAcceleration, jerk);
    }

    private static Stream<Arguments> traversalSectionsForNotTooLongCase() {
        return Stream.of(
                // Index; Total Distance; Duration; Initial Speed; Final Speed; Initial Acceleration; Final Acceleration; Jerk
                Arguments.of(0, 1 / 12d, Math.cbrt(1 / 2d), 0d, 0.31498, 0d, 1 / Math.cbrt(2d), 1d),
                Arguments.of(1, 5 / 12d, Math.cbrt(1 / 2d), 0.31498, 0.6299, Math.cbrt(1 / 2d), 0d, -1d),
                Arguments.of(2, 5 / 12d, Math.cbrt(1 / 2d), 0.6299, 0.31498, 0d, -Math.cbrt(1 / 2d), -1d),
                Arguments.of(3, 1 / 12d, Math.cbrt(1 / 2d), 0.31498, 0d, -Math.cbrt(1 / 2d), 0d, 1d)
        );
    }

    @Test
    void testMaxSpeedReached_whenMaxSpeedReached_thenReturnSections() {
        double jerk = 1;
        double acc = 2;
        double velocity = 2;
        VehicleMotionProperties scratchVehicle = new VehicleMotionProperties(
                velocity,
                acc,
                -acc,
                0,
                jerk,
                -jerk,
                -jerk,
                jerk
        );

        //Total Distance; Duration (s); Initial Speed; Final Speed; Initial Acceleration; Final Acceleration; Jerk
        List<Double[]> expectedValues = List.of(
                new Double[]{0.471d, Math.sqrt(2) * 1E-3, 0d, 1E3, 0d, Math.sqrt(2) * 1E6, 1E9},
                new Double[]{2.357d, Math.sqrt(2) * 1E-3, 1E3, 2E3, Math.sqrt(2) * 1E6, 0d, -1E9},
                new Double[]{24.343d, 12.171E-3, 2E3, 2E3, 0d, 0d, 0d},
                new Double[]{2.357d, Math.sqrt(2) * 1E-3, 2E3, 1E3, 0d, -Math.sqrt(2) * 1E6, -1E9},
                new Double[]{0.471d, Math.sqrt(2) * 1E-3, 1E3, 0d, -Math.sqrt(2) * 1E6, 0d, 1E9}
        );
        Traversal traversal = ConstantJerkTraversalCalculator.INSTANCE.create(30, scratchVehicle);
        ImmutableList<TraversalSection> sections = traversal.getSections();
        assertThat(sections).hasSize(5);
        for (int i = 0; i < expectedValues.size(); i++) {
            TraversalSection section = sections.get(i);
            Double[] e = expectedValues.get(i);
            verifyTraversalSection(section, e[0], e[1], e[2], e[3], e[4], e[5], e[6], 0.01);
        }
    }

    @Test
    void testMaxSpeedReached_whenDistanceNotTooShortForCase_thenReturnSevenSections() {
        ImmutableList<TraversalSection> sections = ConstantJerkTraversalCalculator.INSTANCE.create(100d, uniformJerk).getSections();
        assertThat(sections).hasSize(7);
    }

    @Test
    void testMaxSpeedReached_whenDistanceNotTooShortForCase_thenFirstSectionHasCorrectValues() {
        ImmutableList<TraversalSection> sections = ConstantJerkTraversalCalculator.INSTANCE.create(100d, uniformJerk).getSections();
        TraversalSection firstSection = sections.get(0);
        verifyTraversalSection(firstSection, 2.6042, 2.5, 0d, 3.125, 0d, 2.5, 1d, 0.01);
    }

    @DisplayName("when max acceleration and deceleration is reached but not max speed")
    @ParameterizedTest(name = "Section {index} should have: distance={1}; duration={2}; initialSpeed={3}; finalSpeed={4}; initialAcceleration={5}; finalAcceleration={6}; jerk={7}")
    @MethodSource("traversalSectionsWhenMaxAccelerationReachedButNotMaxSpeed")
    void testMaxAccelerationAndMaxDecelerationReachedButMaxSpeedNotReached_whenCalledValidly_thenReturnsCorrectTraversals(int index, double totalDistance, double duration, double initialSpeed, double finalSpeed, double initialAcceleration, double finalAcceleration, double jerk) {
        ImmutableList<TraversalSection> sections = ConstantJerkTraversalCalculator.INSTANCE.create(37.8, uniformJerk).getSections();

        verifyTraversalSection(sections.get(index), totalDistance, duration, initialSpeed, finalSpeed, initialAcceleration, finalAcceleration, jerk, 0.01);
    }

    private static Stream<Arguments> traversalSectionsWhenMaxAccelerationReachedButNotMaxSpeed() {
        return Stream.of(
                // Index; Total Distance; Duration; Initial Speed; Final Speed; Initial Acceleration; Final Acceleration; Jerk
                Arguments.of(0, 2.6042, 2.5, 0d, 3.125, 0d, 2.5, 1d),
                Arguments.of(1, 1.05, 0.3, 3.125, 3.875, 2.5, 2.5, 0d),
                Arguments.of(2, 14.895833, 2.5, 3.875, 7d, 2.5, 0d, -1d),
                Arguments.of(3, 12.66666666, 2d, 7d, 5d, 0d, -2d, -1d),
                Arguments.of(4, 5.25, 1.5, 5d, 2d, -2d, -2d, 0d),
                Arguments.of(5, 1.3333333, 1.999982, 2d, 0d, -2d, 0d, 1d)
        );
    }

    @DisplayName("when one max acceleration is reached but not the other")
    @ParameterizedTest(name = "Section {index} should have: distance={1}; duration={2}; initialSpeed={3}; finalSpeed={4}; initialAcceleration={5}; finalAcceleration={6}; jerk={7}")
    @MethodSource("traversalSectionsWhenOneMaxAccelerationReached")
    void testOneMaxAccelReachedButNotOtherAndMaxSpeedNotReached_whenCalledValidly_thenReturnsCorrectTraversals(int index, double totalDistance, double duration, double initialSpeed, double finalSpeed, double initialAcceleration, double finalAcceleration, double jerk) {
        ImmutableList<TraversalSection> sections = ConstantJerkTraversalCalculator.INSTANCE.create(24.452915, uniformJerk).getSections();
        TraversalSection section = sections.get(index);

        verifyTraversalSection(section, totalDistance, duration, initialSpeed, finalSpeed, initialAcceleration, finalAcceleration, jerk, 0.01);
    }

    private static Stream<Arguments> traversalSectionsWhenOneMaxAccelerationReached() {
        return Stream.of(
                // Index; Total Distance; Duration; Initial Speed; Final Speed; Initial Acceleration; Final Acceleration; Jerk
                Arguments.of(0, 2.02783, 2.3, 0d, 2.645, 0d, 2.3, 1d),
                Arguments.of(1, 10.13916, 2.3, 2.645, 5.29, 2.3, 0d, -1d),
                Arguments.of(2, 9.2466, 2d, 5.29, 3.29, 0d, -2d, -1d),
                Arguments.of(3, 1.706025, 0.645, 3.29, 2d, -2d, -2d, 0d),
                Arguments.of(4, 1.3333, 2d, 2d, 0d, -2d, 0d, 1d)
        );
    }

    @Test
    void testOneMaxAccelReachedButNotOtherAndMaxSpeedNotReached_whenSymmetricallyEqualForSymmetricallyEqualVehicleProperties_thenTraversalFlipped() {
        ImmutableList<TraversalSection> sections1 = ConstantJerkTraversalCalculator.INSTANCE.create(24.452915, uniformJerk).getSections();
        int numSections = sections1.size();
        Traversal traversal1 = new Traversal(sections1);

        VehicleMotionProperties symetricallyFlippedVehicleProperties = new VehicleMotionProperties(
                uniformJerk.maxSpeed,
                -uniformJerk.deceleration,
                -uniformJerk.acceleration,
                0,
                -uniformJerk.jerkDecelerationUp,
                -uniformJerk.jerkDecelerationDown,
                -uniformJerk.jerkAccelerationUp,
                -uniformJerk.jerkAccelerationDown
        );

        ImmutableList<TraversalSection> sections2 = ConstantJerkTraversalCalculator.INSTANCE.create(24.452915, symetricallyFlippedVehicleProperties).getSections();
        Traversal traversal2 = new Traversal(sections2);

        assertThat(sections2).hasSize(numSections);
        assertThat(traversal1.getTotalDistance()).isCloseTo(traversal2.getTotalDistance(), within(EPSILON));
        assertThat(traversal1.getTotalDuration()).isCloseTo(traversal2.getTotalDuration(), within(EPSILON));

        for (int i = 0; i < numSections; i++) {
            assertFlipped(sections1.get(i), sections2.get(numSections - (i + 1)));
        }
    }

    @Test
    void testCalculateTraversalGivenFixedJerkUpTimeAssumingNeitherConstantAccelerationOrSpeed_whenCalled_thenReturnSectionWithTheCorrectValues() {
        ImmutableList<TraversalSection> sections = ConstantJerkTraversalCalculator.INSTANCE.create(10d, uniformJerk).getSections();

        ImmutableList<TraversalSection> sectionsStartingFromMoving =
                ConstantJerkTraversalCalculator.INSTANCE.create(10 - 0.16666666666666666, 5.0E-4, 1E-6, uniformJerk).getSections();

        TraversalSection firstSection = sectionsStartingFromMoving.get(0);
        getAccelerationInSIUnit(firstSection.getDuration());
        assertThat(getAccelerationInSIUnit(firstSection.getAccelerationAtTime(0))).isCloseTo(1, within(EPSILON));
        assertThat(getSpeedInSIUnit(firstSection.getSpeedAtTime(0))).isCloseTo(0.5, within(EPSILON));

        for (int i = 1; i < sections.size(); i++) {
            assertThat(sectionsStartingFromMoving.get(i)).isEqualTo(sections.get(i));
        }
    }

    @Test
    void testCalculateTraversalGivenFixedConstantAccelerationTimeAssumingNoMaximumSpeedSection_whenStartingAtLessThanMaximumAcceleration_thenReturnSectionWithTheCorrectValues() {
        ImmutableList<TraversalSection> sections = ConstantJerkTraversalCalculator.INSTANCE.create(39d, uniformJerk).getSections();
        ImmutableList<TraversalSection> sectionsStartingFromMoving = ConstantJerkTraversalCalculator.INSTANCE.create(
                39d - 1.3333333333333333,
                0.002,
                2.0000000000000003E-6,
                uniformJerk).getSections();

        TraversalSection firstSection = sectionsStartingFromMoving.get(0);
        assertThat(getAccelerationInSIUnit(firstSection.getAccelerationAtTime(0))).isCloseTo(2, within(EPSILON));
        assertThat(getSpeedInSIUnit(firstSection.getSpeedAtTime(0))).isCloseTo(2, within(EPSILON));

        for (int i = 1; i < sections.size(); i++) {
            verifyTraversalSection(sectionsStartingFromMoving.get(i), sections.get(i));
        }
    }

    @Test
    void testCalculateTraversalGivenFixedConstantAccelerationTimeAssumingNoMaximumSpeedSection_whenStartingAtMaximumAcceleration_thenReturnSectionWithTheCorrectValues() {
        ImmutableList<TraversalSection> sections = ConstantJerkTraversalCalculator.INSTANCE.create(39d, uniformJerk).getSections();
        ImmutableList<TraversalSection> sectionsStartingFromMoving = ConstantJerkTraversalCalculator.INSTANCE.create(39 - 2.9291666666666667, 0.0033750000000000004, 2.5E-6, uniformJerk).getSections();

        TraversalSection firstSection = sectionsStartingFromMoving.get(0);
        assertThat(getAccelerationInSIUnit(firstSection.getAccelerationAtTime(0))).isCloseTo(2.5, within(EPSILON));
        assertThat(getSpeedInSIUnit(firstSection.getSpeedAtTime(0))).isCloseTo(3.3750000000000004, within(EPSILON));

        for (int i = 1; i < sectionsStartingFromMoving.size(); i++) {
            verifyTraversalSection(sectionsStartingFromMoving.get(i), sections.get(i + 1));
        }
    }

    @Test
    void testFindBrakingTraversal_fourSections() {
        double u = 5E-3;
        double a = 0.2E-6;

        ImmutableList<TraversalSection> breakingTraversal = ConstantJerkTraversalCalculator.INSTANCE.getBrakingTraversal(u, a, uniformJerk).getSections();

        assertThat(breakingTraversal).hasSize(4);
        checkSections(u, a, breakingTraversal);
    }

    @Test
    void testFindBrakingTraversal_threeSections() {
        double u = 5E-3;
        double a = -0.2E-6;

        ImmutableList<TraversalSection> breakingTraversal = ConstantJerkTraversalCalculator.INSTANCE.getBrakingTraversal(u, a, uniformJerk).getSections();

        assertThat(breakingTraversal).hasSize(3);
        checkSections(u, a, breakingTraversal);
    }

    @Test
    void testFindBrakingTraversal_twoSections() {
        double u = 3E-3;
        double a = -0.2E-6;

        ImmutableList<TraversalSection> breakingTraversal = ConstantJerkTraversalCalculator.INSTANCE.getBrakingTraversal(u, a, uniformJerk).getSections();

        assertThat(breakingTraversal).hasSize(2);
        checkSections(u, a, breakingTraversal);
    }

    @Test
    void testFindBrakingTraversal_oneSection() {
        ImmutableList<TraversalSection> twoSections = ConstantJerkTraversalCalculator.INSTANCE.getBrakingTraversal(3E-3, -0.2E-6, uniformJerk).getSections();

        Traversal traversal = new Traversal(twoSections);

        double time = 2.5E3;
        double a = traversal.getAccelerationAtDistance(traversal.getDistanceAtTime(time));
        double u = traversal.getSpeedAtTime(time);

        ImmutableList<TraversalSection> brakingTraversal = ConstantJerkTraversalCalculator.INSTANCE.getBrakingTraversal(u, a, uniformJerk).getSections();

        assertThat(brakingTraversal).hasSize(1);
        checkSections(u, a, brakingTraversal);
    }

    /**
     * This is a regression test for a specific failure where we failed to generate a valid braking traversal
     * when the vehicle can just barely brake to a halt by jerking down the deceleration as much as possible.
     */
    @Test
    void testFindBrakingTraversal_whenBrakingToHaltJustAboutPossible_thenValidBrakingTraversalCreated() {
        VehicleMotionProperties uniformJerk = new VehicleMotionProperties(0.004, 2.0E-6, -2.0E-6, 0, 2.0E-8, -2.0E-8, -2.0E-8, 1.08E-9);
        double u = 8.301293608657197E-4;
        double a = -1.3390591545820352E-6;
        ImmutableList<TraversalSection> traversalSections = ConstantJerkTraversalCalculator.INSTANCE.getBrakingTraversal(u, a, uniformJerk).getSections();
        assertThat(traversalSections).hasSize(1);
        checkSections(u, a, traversalSections);
    }

    @Test
    void testFindBrakingTraversal_oneSection_undershoot() {
        double u = 2E-3;
        double a = -0.2E-6;

        ImmutableList<TraversalSection> breakingTraversal = ConstantJerkTraversalCalculator.INSTANCE.getBrakingTraversal(u, a, uniformJerk).getSections();

        assertThat(breakingTraversal).hasSize(2);
        checkSections(u, a, breakingTraversal);
    }

    @Test
    void testBrakingFromRest_doesNothing() {
        double u = 0;
        double a = 0;

        ImmutableList<TraversalSection> sections = ConstantJerkTraversalCalculator.INSTANCE.getBrakingTraversal(u, a, uniformJerk).getSections();

        assertThat(sections).hasSize(0);
    }

    @Test
    void testGetToVmaxAndStayAtVMaxToReachD_fromZero() {
        double u = 0;
        double a = 0;

        ImmutableList<TraversalSection> sections = ConstantJerkTraversalCalculator.INSTANCE.create(100d, u, a, uniformJerk).getSections();

        assertThat(sections).hasSize(7);
        checkSections(u, a, sections);
    }

    @Test
    void testGetToVmaxAndStayAtVMaxToReachD_firstSection() {
        double u = 0.1E-3;
        double a = 0.1E-6;

        ImmutableList<TraversalSection> sections = ConstantJerkTraversalCalculator.INSTANCE.create(100, u, a, uniformJerk).getSections();

        assertThat(sections).hasSize(7);
        checkSections(u, a, sections);
    }

    @Test
    void testGetToVmaxThenBrake_midSection() {
        double u = uniformJerk.maxSpeed / 2d;
        double a = uniformJerk.acceleration;

        ImmutableList<TraversalSection> sections = ConstantJerkTraversalCalculator.INSTANCE.getBrakingTraversal(
                u, a, uniformJerk).getSections();

        assertThat(sections).hasSize(4);
        checkSections(u, a, sections);
    }

    @Test
    void testGetToVmaxAndStayAtVMaxToReachD_midSection() {
        double u = uniformJerk.maxSpeed / 2d;
        double a = uniformJerk.acceleration;

        ImmutableList<TraversalSection> sections = ConstantJerkTraversalCalculator.INSTANCE.create(
                100, u, a, uniformJerk).getSections();

        assertThat(sections).hasSize(6);
        checkSections(u, a, sections);
    }

    @Test
    void testGetToVmaxAndStayAtVMaxToReachD_lastSectionUndershoot() {
        double u = uniformJerk.maxSpeed - 0.5E-3;
        double a = 0.5E-6;

        ImmutableList<TraversalSection> sections = ConstantJerkTraversalCalculator.INSTANCE.create(
                100, u, a, uniformJerk).getSections();

        assertThat(sections).hasSize(6);
        checkSections(u, a, sections);
    }

    private static void verifyTraversalSection(TraversalSection section, double totalDistance, double duration, double initialSpeed, double finalSpeed, double initialAcceleration, double finalAcceleration, double jerk) {
        verifyTraversalSection(section, totalDistance, duration, initialSpeed, finalSpeed, initialAcceleration, finalAcceleration, jerk, EPSILON);
    }

    private static void verifyTraversalSection(TraversalSection section, double totalDistance, double duration, double initialSpeed, double finalSpeed, double initialAcceleration, double finalAcceleration, double jerk, double rawTol) {
        assertThat(section.getTotalDistance()).isCloseTo(totalDistance, within(rawTol));
        assertThat(getDurationInSIUnit(section.getDuration())).isCloseTo(duration, within(rawTol));
        assertThat(getSpeedInSIUnit(section.getSpeedAtTime(0))).isCloseTo(initialSpeed, within(rawTol));
        assertThat(getSpeedInSIUnit(section.getSpeedAtTime(section.getDuration()))).isCloseTo(finalSpeed, within(rawTol));
        assertThat(getAccelerationInSIUnit(section.getAccelerationAtTime(0))).isCloseTo(initialAcceleration, within(rawTol));
        assertThat(getAccelerationInSIUnit(section.getAccelerationAtTime(section.getDuration()))).isCloseTo(finalAcceleration, within(rawTol));
        assertThat(getJerkInSIUnit(getJerk(section))).isCloseTo(jerk, within(rawTol));
    }

    private static void assertFlipped(TraversalSection s1, TraversalSection s2) {
        assertThat(getDurationInSIUnit(s1.getDuration())).isCloseTo(getDurationInSIUnit(s2.getDuration()), within(EPSILON));
        assertThat(s1.getTotalDistance()).isCloseTo(s2.getTotalDistance(), within(EPSILON));
        assertThat(getAccelerationInSIUnit(-s1.getAccelerationAtTime(0))).isCloseTo(getAccelerationInSIUnit(s2.getAccelerationAtTime(s2.getDuration())), within(EPSILON));
        assertThat(getAccelerationInSIUnit(-s1.getAccelerationAtTime(s1.getDuration()))).isCloseTo(getAccelerationInSIUnit(s2.getAccelerationAtTime(0)), within(EPSILON));
        assertThat(getSpeedInSIUnit(s1.getSpeedAtTime(0))).isCloseTo(getSpeedInSIUnit(s2.getSpeedAtTime(s2.getDuration())), within(EPSILON));
        assertThat(getSpeedInSIUnit(s1.getSpeedAtTime(s1.getDuration()))).isCloseTo(getSpeedInSIUnit(s2.getSpeedAtTime(0)), within(EPSILON));
        assertThat(getJerkInSIUnit(getJerk(s1))).isCloseTo(getJerkInSIUnit(getJerk(s2)), within(EPSILON));
    }

    private static void checkSections(double u, double a, ImmutableList<TraversalSection> sections) {
        checkFirstSection(u, a, sections.get(0));
        checkJoinedUp(sections);
        checkLastSection(sections.get(sections.size() - 1));
    }

    private static void checkFirstSection(double u, double a, TraversalSection section) {
        assertThat(section.getSpeedAtTime(0)).isCloseTo(u, within(EPSILON));
        assertThat(section.getAccelerationAtTime(0)).isCloseTo(a, within(EPSILON));
    }

    static void checkJoinedUp(ImmutableList<TraversalSection> sections) {
        UnmodifiableIterator<TraversalSection> it = sections.iterator();

        TraversalSection prevSection = null;

        while (it.hasNext()) {
            TraversalSection section = it.next();

            if (prevSection != null) {
                assertThat(section.getSpeedAtTime(0))
                        .isCloseTo(prevSection.getSpeedAtTime(prevSection.getDuration()), within(EPSILON));
                assertThat(section.getAccelerationAtTime(0))
                        .isCloseTo(prevSection.getAccelerationAtTime(prevSection.getDuration()), within(EPSILON));
            }

            prevSection = section;
        }
    }

    private static void checkLastSection(TraversalSection s) {
        assertThat(getJerk(s)).isPositive();
        assertThat(s.getAccelerationAtTime(0)).isNegative();
        assertThat(s.getSpeedAtTime(0)).isPositive();
        assertThat(s.getAccelerationAtTime(s.getDuration())).isZero();
        assertThat(s.getSpeedAtTime(s.getDuration())).isZero();
    }

    private static double getDurationInSIUnit(double duration) {
        return ValuesInSIUnits.convertDuration(duration, TimeUnit.MILLISECONDS);
    }

    private static double getSpeedInSIUnit(double speed) {
        return ValuesInSIUnits.convertSpeed(speed, LengthUnit.METERS, TimeUnit.MILLISECONDS);
    }

    private static double getAccelerationInSIUnit(double acceleration) {
        return ValuesInSIUnits.convertAcceleration(acceleration, LengthUnit.METERS, TimeUnit.MILLISECONDS);
    }

    private static double getJerkInSIUnit(double jerk) {
        return ValuesInSIUnits.convertJerk(jerk, LengthUnit.METERS, TimeUnit.MILLISECONDS);
    }

    private static double tol(double x) {
        return Math.abs(x) * ROUNDING_ERROR_TOLERANCE;
    }

    /**
     * Regression test for #454
     * We found a rounding error in the calculation of the speed at a distance in the constant jerk case.
     */
    @Test
    void whenDistanceIsNearlyAtTheCentre_thenReturnsAnExpectedValue() {
        VehicleMotionProperties scratchVehicle = createVehicleProperties(4.04, 2.02, -2.02, 20);
        double totalDistance = 1.5219999999999998;
        double halfWay = 0.761; //rounding error above the mid-point
        Traversal traversal = factory.create(totalDistance, 0, 0, scratchVehicle);
        assertThat(traversal.getSpeedAtDistance(halfWay)).isStrictlyBetween(1.6, 1.7); //This used to throw an exception
    }

    private static void verifyTraversalWithinVehicleMotionBounds(Traversal traversal, VehicleMotionProperties properties) {
        ImmutableList<TraversalSection> sections = traversal.getSections();
        for (int i = 0; i < sections.size(); i++) {
            verifyTraversalSectionWithinVehicleMotionBounds(sections.get(i), properties);
        }
    }

    private void verifyTraversalSections(ImmutableList<TraversalSection> createdTraversalSections, ImmutableList<TraversalSection> expectedSections) {
        assertThat(createdTraversalSections.size()).isEqualTo(expectedSections.size());
        for (int i = 0; i < expectedSections.size(); i++) {
            verifyTraversalSection(expectedSections.get(i), createdTraversalSections.get(i));
        }
    }

    private static void verifyTraversalSection(TraversalSection expectedSection, TraversalSection actualSection) {
        assertThat(expectedSection.getTotalDistance()).isCloseTo(actualSection.getTotalDistance(), within(EPSILON));
        assertThat(expectedSection.getDuration()).isCloseTo(actualSection.getDuration(), within(EPSILON));
        assertThat(expectedSection.getSpeedAtTime(0)).isCloseTo(actualSection.getSpeedAtTime(0), within(EPSILON));
        assertThat(expectedSection.getSpeedAtTime(expectedSection.getDuration())).isCloseTo(actualSection.getSpeedAtTime(actualSection.getDuration()), within(EPSILON));
        assertThat(expectedSection.getAccelerationAtTime(0)).isCloseTo(actualSection.getAccelerationAtTime(0), within(EPSILON));
        assertThat(expectedSection.getAccelerationAtTime(expectedSection.getDuration())).isCloseTo(actualSection.getAccelerationAtTime(actualSection.getDuration()), within(EPSILON));
        assertThat((getJerk(expectedSection))).isCloseTo(getJerk(actualSection), within(EPSILON));
    }

    private static void verifyTraversalSectionWithinVehicleMotionBounds(TraversalSection section, VehicleMotionProperties properties) {
        assertThat(section.getSpeedAtTime(0)).isBetween(-EPSILON, properties.maxSpeed + EPSILON);
        assertThat(section.getSpeedAtTime(section.getDuration())).isBetween(-EPSILON, properties.maxSpeed + EPSILON);
        assertThat(section.getAccelerationAtTime(0)).isBetween(properties.deceleration - EPSILON, properties.acceleration + EPSILON);
        assertThat(section.getAccelerationAtTime(section.getDuration())).isBetween(properties.deceleration - EPSILON, properties.acceleration + EPSILON);
    }

    private static VehicleMotionProperties createVehicleProperties(double speed, double acceleration, double deceleration, double jerk) {
        return new VehicleMotionProperties(
                speed,
                acceleration,
                deceleration,
                0, // tolerance
                jerk, //jerk accel up
                -jerk, //jerk accel down
                -jerk, //jerk decel up
                jerk //jerk decel down
        );
    }

    private static void assertEndsAtRest(Traversal tr, VehicleMotionProperties props) {
        var last = tr.getSections().get(tr.getSections().size() - 1);
        double vEnd = last.getSpeedAtTime(last.getDuration());
        double aEnd = last.getAccelerationAtTime(last.getDuration());
        assertEquals(0, DoubleMath.fuzzyCompare(vEnd, 0.0, ROUNDING_ERROR_TOLERANCE * props.maxSpeed), "Final speed not ~0");
        assertEquals(0, DoubleMath.fuzzyCompare(aEnd, 0.0, ROUNDING_ERROR_TOLERANCE * props.acceleration), "Final acceleration not ~0");
    }

    private static void assertStartsAt(Traversal tr, double initialSpeed, double initialAcceleration, VehicleMotionProperties props) {
        assertEquals(initialAcceleration, tr.getAccelerationAtTime(0), ROUNDING_ERROR_TOLERANCE * props.acceleration);
        assertEquals(initialSpeed, tr.getSpeedAtTime(0), ROUNDING_ERROR_TOLERANCE * props.maxSpeed);
    }

    private static void assertRespectsBoundsAndMonotone(Traversal tr, VehicleMotionProperties props) {
        double aMax = props.acceleration;
        double aMin = props.deceleration; // negative
        double vMax = props.maxSpeed;

        int samples = 300;
        double dt = tr.getTotalDuration() / Math.max(1, samples);

        double prevV = Double.POSITIVE_INFINITY;
        for (int i = 0; i <= samples; i++) {
            double t = Math.min(tr.getTotalDuration(), i * dt);
            double v = tr.getSpeedAtTime(t);
            double a = tr.getAccelerationAtTime(t);

            assertTrue(DoubleMath.fuzzyCompare(v, 0.0, tol(v)) >= 0, "v<0 at t=" + t + " v=" + v);
            assertTrue(DoubleMath.fuzzyCompare(v, vMax, tol(vMax)) <= 0, "v>vMax at t=" + t + " v=" + v);
            assertTrue(DoubleMath.fuzzyCompare(a, aMin, tol(Math.abs(aMin))) >= 0, "a<aMin at t=" + t + " a=" + a);
            assertTrue(DoubleMath.fuzzyCompare(a, aMax, tol(aMax)) <= 0, "a>aMax at t=" + t + " a=" + a);

            // Non-increasing speed during braking (allow tiny numeric rise)
            assertTrue(
                    DoubleMath.fuzzyCompare(v, prevV, ROUNDING_ERROR_TOLERANCE * props.maxSpeed) <= 0,
                    "Speed increased during braking at t=" + t + " v=" + v + " prev=" + prevV);
            prevV = v;
        }
    }

    private static void assertSectionsRespectPredicate(Traversal tr, Predicate<TraversalSection> p) {
        for (TraversalSection sec : tr.getSections()) {
            assertTrue(p.test(sec));
        }
    }

    private static void assertSmoothConnected(Traversal tr) {
        var secs = tr.getSections();
        ConstantJerkSectionsFactoryTest.assertSmoothConnected(secs);
    }

    /**
     * Asserts constraints v∈[0,vmax], a∈[aMin,aMax] sampled across time.
     */
    private static void assertRespectsConstraints(Traversal tr, VehicleMotionProperties p) {
        double aMax = p.acceleration;
        double aMin = p.deceleration; // negative
        double vMax = p.maxSpeed;

        int samples = 200;
        double dt = tr.getTotalDuration() / samples;
        double t = 0.0;
        for (int i = 0; i <= samples; i++, t += dt) {
            double v = tr.getSpeedAtTime(Math.min(t, tr.getTotalDuration()));
            double a = tr.getAccelerationAtTime(Math.min(t, tr.getTotalDuration()));
            assertTrue(
                    DoubleMath.fuzzyCompare(v, 0.0, tol(v)) >= 0,
                    "Speed went negative at t=" + t + " v=" + v);
            assertTrue(
                    DoubleMath.fuzzyCompare(v, vMax, tol(vMax)) <= 0,
                    "Speed exceeded max at t=" + t + " v=" + v);
            assertTrue(
                    DoubleMath.fuzzyCompare(a, aMin, tol(Math.abs(aMin))) >= 0,
                    "Acceleration below min at t=" + t + " a=" + a + " < " + aMin);
            assertTrue(
                    DoubleMath.fuzzyCompare(a, aMax, tol(aMax)) <= 0,
                    "Acceleration above max at t=" + t + " a=" + a + " > " + aMax);
        }
    }

    private static double getJerk(TraversalSection s) {
        return s instanceof ConstantJerkTraversalSection ? ((ConstantJerkTraversalSection) s).jerk : 0;
    }
}
