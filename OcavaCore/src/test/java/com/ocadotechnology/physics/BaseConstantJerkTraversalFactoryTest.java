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

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

class BaseConstantJerkTraversalFactoryTest {
    private static final ConstantJerkTraversalCalculator factory = ConstantJerkTraversalCalculator.INSTANCE;

    private static final double EPSILON = Math.pow(10, -9);

    private static final double acceleration = 2.5d;
    private static final double deceleration = -2d;
    private static final double maxSpeed = 8d;
    private static final double jerkAccelerationUp = 3.6d;
    private static final double jerkAccelerationDown = -3.4d;
    private static final double jerkDecelerationUp = -2.2d;
    private static final double jerkDecelerationDown = 1.2d;

    private static final VehicleMotionProperties vehicleMotionProperties = new VehicleMotionProperties(maxSpeed, acceleration, deceleration, 0, jerkAccelerationUp, jerkAccelerationDown, jerkDecelerationUp, jerkDecelerationDown);

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
        for (double d = 0.00; d < 500d; d += 0.001) {
            Traversal traversal = factory.create(d, vehicleMotionProperties);
            assertThat(traversal.getTotalDistance()).isCloseTo(d, within(EPSILON));
        }
    }

    @Test
    void create_whenCalledWithDifferentStartingPositions_alwaysReturnsATraversal() {
        for (double d = 0.4; d < 100d; d += 0.4) {
            for (double a = 0.00; a < 3d; a += 0.4) {
                for (double u = 0.00; u < 10d; u += 0.4) {
                    String message = "distance=" + d + ", initial-acceleration=" + a + ", initial-speed=" + u;
                    Traversal traversal = factory.create(d, u, a, vehicleMotionProperties);
                    ConstantJerkSectionsFactoryTest.checkJoinedUpVelocities(message, traversal.getSections());
                }
            }
        }
    }

    @Test
    void create_whenRequiredDistanceIsZero_thenFindsBreakingDistance() {
        double d = 0d, a = 0d, u = 2d;
        String message = "distance=" + d + ", initial-acceleration=" + a + ", initial-speed=" + u;
        Traversal traversal = factory.create(d, u, a, vehicleMotionProperties);
        ConstantJerkSectionsFactoryTest.checkJoinedUp(message, traversal.getSections());
    }

    @Test
    void create_whenStartingWithNegativeDecelerationWithASubstantialDistanceToGo() {
        double d = 100d, a = -2d, u = 6d;
        String message = "distance=" + d + ", initial-acceleration=" + a + ", initial-speed=" + u;
        Traversal traversal = factory.create(d, u, a, vehicleMotionProperties);
        ConstantJerkSectionsFactoryTest.checkJoinedUp(message, traversal.getSections());
    }

    @Test
    void create_whenStartingWithNegativeDecelerationWithShortDistanceToGo() {
        double d = 0.5d, a = -3d, u = 0.5;
        String message = "distance=" + d + ", initial-acceleration=" + a + ", initial-speed=" + u;
        Traversal traversal = factory.create(d, u, a, vehicleMotionProperties);
        ConstantJerkSectionsFactoryTest.checkJoinedUpVelocities(message, traversal.getSections());
    }

    @Test
    void create_when() {
        double d = 26d, a = 0.5d, u = 8.5d;
        String message = "distance=" + d + ", initial-acceleration=" + a + ", initial-speed=" + u;
        Traversal traversal = factory.create(d, u, a, vehicleMotionProperties);
        ConstantJerkSectionsFactoryTest.checkJoinedUpVelocities(message, traversal.getSections());
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

    private static void verifyTraversalWithinVehicleMotionBounds(Traversal traversal, VehicleMotionProperties properties) {
        ImmutableList<TraversalSection> sections = traversal.getSections();
        for (int i = 0; i < sections.size(); i++) {
            verifyTraversalSectionWithinVehicleMotionBounds(sections.get(i), properties);
        }
    }

    private static void verifyTraversalSectionWithinVehicleMotionBounds(TraversalSection section, VehicleMotionProperties properties) {
        assertThat(section.getSpeedAtTime(0)).isBetween(-EPSILON, properties.maxSpeed + EPSILON);
        assertThat(section.getSpeedAtTime(section.getDuration())).isBetween(-EPSILON, properties.maxSpeed + EPSILON);
        assertThat(section.getAccelerationAtTime(0)).isBetween(properties.deceleration - EPSILON, properties.acceleration + EPSILON);
        assertThat(section.getAccelerationAtTime(section.getDuration())).isBetween(properties.deceleration  -EPSILON, properties.acceleration + EPSILON);
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

    private static double getJerk(TraversalSection s) {
        return s instanceof ConstantJerkTraversalSection ? ((ConstantJerkTraversalSection) s).jerk : 0;
    }
}
