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
import static org.assertj.core.api.Assertions.within;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.ocadotechnology.physics.units.LengthUnit;
import com.ocadotechnology.physics.units.ValuesInSIUnits;
import com.ocadotechnology.validation.Failer;

@DisplayName("ConstantJerkSectionsFactory test")
class ConstantJerkTraversalSectionsFactoryTest {
    private static final double EPSILON = 0.01;

    private final double acceleration = 2.5E-6;
    private final double deceleration = -2E-6;
    private final double maxSpeed = 8E-3;

    private final double jerkAccelerationUp = 1E-9;
    private final double jerkAccelerationDown = -1E-9;
    private final double jerkDecelerationUp = -1E-9;
    private final double jerkDecelerationDown = 1E-9;

    private final VehicleMotionProperties vehicleMotionProperties = new VehicleMotionProperties(acceleration, deceleration, maxSpeed, jerkAccelerationUp, jerkAccelerationDown, jerkDecelerationUp, jerkDecelerationDown);

    @Test
    void neitherMaxAccelerationNorMaxDecelerationReached_whenTotalDistanceTooLongForThisCase_thenReturnAbsent() {
        Optional<ImmutableList<TraversalSection>> sections = ConstantJerkSectionsFactory.neitherMaxAccelerationNorMaxDecelerationReached(16.1d, vehicleMotionProperties);
        assertThat(sections).isEmpty();
    }

    @Test
    void neitherMaxAccelerationNorMaxDecelerationReached_whenTotalDistanceNotTooLongForCase_thenReturnFourSections() {
        Optional<ImmutableList<TraversalSection>> result = ConstantJerkSectionsFactory.neitherMaxAccelerationNorMaxDecelerationReached(1d, vehicleMotionProperties);
        assertThat(result).hasValueSatisfying(sections -> assertThat(sections).hasSize(4));
    }

    @DisplayName("when neither max acceleration or deceleration is reached")
    @ParameterizedTest(name = "Section {index} should have: distance={1}; duration={2}; initialSpeed={3}; finalSpeed={4}; initialAcceleration={5}; finalAcceleration={6}; jerk={7}")
    @MethodSource("traversalSectionsForNotTooLongCase")
    void neitherMaxAccelerationNorMaxDecelerationReached_whenTotalDistanceNotTooLongForCase(int index, double totalDistance, double duration, double initialSpeed, double finalSpeed, double initialAcceleration, double finalAcceleration, double jerk) {
        Optional<ImmutableList<TraversalSection>> result = ConstantJerkSectionsFactory.neitherMaxAccelerationNorMaxDecelerationReached(1d, vehicleMotionProperties);
        assertThat(result).hasValueSatisfying(sections -> {
            TraversalSection section = sections.get(index);
            verifyTraversalSection(section, totalDistance, duration, initialSpeed, finalSpeed, initialAcceleration, finalAcceleration, jerk);
        });
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
    void maxAccelerationDecelerationAndSpeedReached_whenDistanceTooShortForCase_thenReturnAbsent() {
        Optional<ImmutableList<TraversalSection>> result = ConstantJerkSectionsFactory.maxAccelerationDecelerationAndSpeedReached(25d, vehicleMotionProperties);
        assertThat(result).isEmpty();
    }

    @Test
    void maxAccelerationDecelerationAndSpeedReached_whenDistanceNotTooShortForCase_thenReturnSevenSections() {
        Optional<ImmutableList<TraversalSection>> result = ConstantJerkSectionsFactory.maxAccelerationDecelerationAndSpeedReached(100d, vehicleMotionProperties);
        assertThat(result).hasValueSatisfying(sections -> assertThat(sections).hasSize(7));
    }

    @Test
    void maxAccelerationDecelerationAndSpeedReached_whenDistanceNotTooShortForCase_thenFirstSectionHasCorrectValues() {
        Optional<ImmutableList<TraversalSection>> result = ConstantJerkSectionsFactory.maxAccelerationDecelerationAndSpeedReached(100d, vehicleMotionProperties);
        assertThat(result).hasValueSatisfying(sections -> {
            TraversalSection firstSection = sections.get(0);

            verifyTraversalSection(firstSection, 2.6042, 2.5, 0d, 3.125, 0d, 2.5, 1d);
        });
    }

    // TODO: more sections for maxAccelerationDecelerationAndSpeedReached
    @Test
    void maxAccelerationAndMaxDecelerationReachedButNotMaxSpeed_whenDistanceTooLongForCase_thenReturnAbsent() {
        Optional<ImmutableList<TraversalSection>> sections = ConstantJerkSectionsFactory.maxAccelerationAndMaxDecelerationReachedButNotMaxSpeed(100d, vehicleMotionProperties);
        assertThat(sections).isEmpty();
    }

    @Test
    void maxAccelerationAndMaxDecelerationReachedButNotMaxSpeed_whenDistanceTooShortForCase_thenReturnAbsent() {
        Optional<ImmutableList<TraversalSection>> sections = ConstantJerkSectionsFactory.maxAccelerationAndMaxDecelerationReachedButNotMaxSpeed(20d, vehicleMotionProperties);
        assertThat(sections).isEmpty();
    }

    @DisplayName("when max acceleration and deceleration is reached but not max speed")
    @ParameterizedTest(name = "Section {index} should have: distance={1}; duration={2}; initialSpeed={3}; finalSpeed={4}; initialAcceleration={5}; finalAcceleration={6}; jerk={7}")
    @MethodSource("traversalSectionsWhenMaxAccelerationReachedButNotMaxSpeed")
    void maxAccelerationAndMaxDecelerationReachedButNotMaxSpeed_whenDistanceInCase(int index, double totalDistance, double duration, double initialSpeed, double finalSpeed, double initialAcceleration, double finalAcceleration, double jerk) {
        Optional<ImmutableList<TraversalSection>> result = ConstantJerkSectionsFactory.maxAccelerationAndMaxDecelerationReachedButNotMaxSpeed(37.8, vehicleMotionProperties);
        assertThat(result).hasValueSatisfying(sections -> {
            TraversalSection section = sections.get(index);
            verifyTraversalSection(section, totalDistance, duration, initialSpeed, finalSpeed, initialAcceleration, finalAcceleration, jerk);
        });
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
    void oneMaxAccelerationReachedButNotOther_whenDistanceInCase(int index, double totalDistance, double duration, double initialSpeed, double finalSpeed, double initialAcceleration, double finalAcceleration, double jerk) {
        ImmutableList<TraversalSection> sections = ConstantJerkSectionsFactory.oneMaxAccelReachedButNotOther(24.452915, vehicleMotionProperties);
        TraversalSection section = sections.get(index);

        verifyTraversalSection(section, totalDistance, duration, initialSpeed, finalSpeed, initialAcceleration, finalAcceleration, jerk);
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
    void oneMaxAbdelReachedButNotOther_symmetricallyEqualForSymmetricallyEqualVehicleProperties() {
        ImmutableList<TraversalSection> sections1 = ConstantJerkSectionsFactory.oneMaxAccelReachedButNotOther(24.452915, vehicleMotionProperties);
        int numSections = sections1.size();
        Traversal traversal1 = new Traversal(sections1);

        VehicleMotionProperties symetricallyFlippedVehicleProperties = new VehicleMotionProperties(-deceleration, -acceleration, maxSpeed, jerkAccelerationUp, jerkAccelerationDown, jerkDecelerationUp,
                jerkDecelerationDown);

        ImmutableList<TraversalSection> sections2 = ConstantJerkSectionsFactory.oneMaxAccelReachedButNotOther(24.452915, symetricallyFlippedVehicleProperties);
        Traversal traversal2 = new Traversal(sections2);

        assertThat(sections2).hasSize(numSections);
        assertThat(traversal1.getTotalDistance()).isCloseTo(traversal2.getTotalDistance(), within(EPSILON));
        assertThat(traversal1.getTotalDuration()).isCloseTo(traversal2.getTotalDuration(), within(EPSILON));

        for (int i = 0; i < numSections; i++) {
            assertFlipped(sections1.get(i), sections2.get(numSections - (i + 1)));
        }
    }

    @Test
    void calculateTraversalGivenFixedJerkUpTimeAssumingNeitherConstantAccelerationOrSpeed_whenCalled_thenReturnSectionWithTheCorrectValues() {
        ImmutableList<TraversalSection> sections = ConstantJerkSectionsFactory.neitherMaxAccelerationNorMaxDecelerationReached(10d, vehicleMotionProperties).orElseThrow(Failer::valueExpected);

        ImmutableList<TraversalSection> sectionsStartingFromMoving =
                ConstantJerkSectionsFactory.calculateTraversalGivenFixedJerkUpTimeAssumingNeitherConstantAccelerationOrSpeed(0.371110046512358E3, 0.896280949311433E-3, 1.338865900164339E-6, vehicleMotionProperties);

        TraversalSection firstSection = sectionsStartingFromMoving.get(0);
        getAccelerationInSIUnit(firstSection.getDuration());
        assertThat(getAccelerationInSIUnit(firstSection.getAccelerationAtTime(0))).isCloseTo(1.338865900164339, within(EPSILON));
        assertThat(getSpeedInSIUnit(firstSection.getSpeedAtTime(0))).isCloseTo(0.896280949311433, within(EPSILON));

        for (int i = 1; i < sections.size(); i++) {
            assertThat(sectionsStartingFromMoving.get(i)).isEqualTo(sections.get(i));
        }
    }

    @Test
    void calculateTraversalGivenFixedConstantAccelerationTimeAssumingNoMaximumSpeedSection_whenStartingAtLessThanMaximumAcceleration_thenReturnSectionWithTheCorrectValues() {
        ImmutableList<TraversalSection> sections = ConstantJerkSectionsFactory.maxAccelerationAndMaxDecelerationReachedButNotMaxSpeed(39d, vehicleMotionProperties).orElseThrow(Failer::valueExpected);
        ImmutableList<TraversalSection> sectionsStartingFromMoving = ConstantJerkSectionsFactory.calculateTraversalGivenFixedMaximumAccelerationTimeAssumingNoMaximumSpeedSection(
                0.35573166424566766E3,
                1.040041911525952E-3,
                1.4422495703074083E-6,
                vehicleMotionProperties);

        TraversalSection firstSection = sectionsStartingFromMoving.get(0);
        assertThat(getAccelerationInSIUnit(firstSection.getAccelerationAtTime(0))).isCloseTo(1.4422495703074083, within(EPSILON));
        assertThat(getSpeedInSIUnit(firstSection.getSpeedAtTime(0))).isCloseTo(1.040041911525952, within(EPSILON));

        for (int i = 1; i < sections.size(); i++) {
            assertThat(sectionsStartingFromMoving.get(i)).as("Expect sections " + i).isEqualTo(sections.get(i));
        }
    }

    @Test
    void calculateTraversalGivenFixedConstantAccelerationTimeAssumingNoMaximumSpeedSection_whenStartingAtMaximumAcceleration_thenReturnSectionWithTheCorrectValues() {
        ImmutableList<TraversalSection> sections = ConstantJerkSectionsFactory.maxAccelerationAndMaxDecelerationReachedButNotMaxSpeed(39d, vehicleMotionProperties).orElseThrow(Failer::valueExpected);
        ImmutableList<TraversalSection> sectionsStartingFromMoving = ConstantJerkSectionsFactory.
                calculateTraversalGivenFixedMaximumAccelerationTimeAssumingNoMaximumSpeedSection(0.234904663247122E3, 3.427067502496364E-3, 2.5E-6, vehicleMotionProperties);

        TraversalSection firstSection = sectionsStartingFromMoving.get(0);
        assertThat(getAccelerationInSIUnit(firstSection.getAccelerationAtTime(0))).isCloseTo(2.5, within(EPSILON));
        assertThat(getSpeedInSIUnit(firstSection.getSpeedAtTime(0))).isCloseTo(3.427067502496364, within(EPSILON));

        for (int i = 1; i < sectionsStartingFromMoving.size(); i++) {
            assertThat(sectionsStartingFromMoving.get(i)).isEqualTo(sections.get(i + 1));
        }
    }

    //TODO: make some assertions about the times and lengths of the below solutions

    @Test
    void testFindBrakingTraversal_fourSections() {
        double u = 5E-3;
        double a = 0.2E-6;

        ImmutableList<TraversalSection> breakingTraversal = ConstantJerkSectionsFactory.findBrakingTraversal(u, a, vehicleMotionProperties);

        assertThat(breakingTraversal).hasSize(4);
        checkSections(u, a, breakingTraversal);
    }
    @Test
    void testFindBrakingTraversal_threeSections() {
        double u = 5E-3;
        double a = -0.2E-6;

        ImmutableList<TraversalSection> breakingTraversal = ConstantJerkSectionsFactory.findBrakingTraversal(u, a, vehicleMotionProperties);

        assertThat(breakingTraversal).hasSize(3);
        checkSections(u, a, breakingTraversal);
    }

    @Test
    void testFindBrakingTraversal_twoSections() {
        double u = 3E-3;
        double a = -0.2E-6;

        ImmutableList<TraversalSection> breakingTraversal = ConstantJerkSectionsFactory.findBrakingTraversal(u, a, vehicleMotionProperties);

        assertThat(breakingTraversal).hasSize(2);
        checkSections(u, a, breakingTraversal);
    }

    @Test
    void testFindBrakingTraversal_oneSection() {
        ImmutableList<TraversalSection> twoSections = ConstantJerkSectionsFactory.findBrakingTraversal(3E-3, -0.2E-6, vehicleMotionProperties);

        Traversal traversal = new Traversal(twoSections);

        double time = 2.5E3;
        double a = traversal.getAccelerationAtDistance(traversal.getDistanceAtTime(time));
        double u = traversal.getSpeedAtTime(time);

        ImmutableList<TraversalSection> breakingTraversal = ConstantJerkSectionsFactory.findBrakingTraversal(u, a, vehicleMotionProperties);

        assertThat(breakingTraversal).hasSize(1);
        checkSections(u, a, breakingTraversal);
    }

    @Test
    void testFindBrakingTraversal_oneSection_overshoot() {
        double u = 0.5E-3;
        double a = -2E-6;

        ImmutableList<TraversalSection> breakingTraversal = ConstantJerkSectionsFactory.findBrakingTraversal(u, a, vehicleMotionProperties);

        assertThat(breakingTraversal).hasSize(1);

        checkFirstSection(u, a, breakingTraversal.get(breakingTraversal.size() - 1));

        assertThat(getJerk(breakingTraversal.get(0))).isPositive();
        assertThat(breakingTraversal.get(0).getAccelerationAtTime(0)).isNegative();
        assertThat(breakingTraversal.get(0).getSpeedAtTime(0)).isPositive();
        assertThat(breakingTraversal.get(0).getSpeedAtTime(breakingTraversal.get(0).getDuration())).isCloseTo(0, within(1e-9));
    }

    @Test
    void testFindBrakingTraversal_oneSection_undershoot() {
        double u = 2E-3;
        double a = -0.2E-6;

        ImmutableList<TraversalSection> breakingTraversal = ConstantJerkSectionsFactory.findBrakingTraversal(u, a, vehicleMotionProperties);

        assertThat(breakingTraversal).hasSize(2);
        checkSections(u, a, breakingTraversal);
    }

    @Test
    void testJerkUpToAmaxConstrainedByVmaxThenBrake_fromZero() {
        double u = 0;
        double a = 0;

        ImmutableList<TraversalSection> sections = ConstantJerkSectionsFactory.jerkUpToAmaxConstrainedByVmaxThenBrake(u, a, vehicleMotionProperties);

        assertThat(sections).hasSize(5);
        checkSections(u, a, sections);
    }

    @Test
    void testGetToVmaxThenBrake_fromZero() {
        double u = 0;
        double a = 0;

        ImmutableList<TraversalSection> sections = ConstantJerkSectionsFactory.getToVmaxThenBrake(u, a, vehicleMotionProperties);

        assertThat(sections).hasSize(6);
        checkSections(u, a, sections);
    }

    @Test
    void testGetToVmaxAndStayAtVMaxToReachD_fromZero() {
        double u = 0;
        double a = 0;

        ImmutableList<TraversalSection> sections = ConstantJerkSectionsFactory.getToVmaxAndStayAtVMaxToReachDistance(100d, u, a, vehicleMotionProperties);

        assertThat(sections).hasSize(7);
        checkSections(u, a, sections);
    }

    @Test
    void testJerkUpToAmaxConstrainedByVmaxThenBrake_firstSection() {
        double u = 0.1E-3;
        double a = 0.1E-6;

        ImmutableList<TraversalSection> sections = ConstantJerkSectionsFactory.jerkUpToAmaxConstrainedByVmaxThenBrake(u, a, vehicleMotionProperties);

        assertThat(sections).hasSize(5);
        checkSections(u, a, sections);
    }

    @Test
    void testGetToVmaxThenBrake_firstSection() {
        double u = 0.1E-3;
        double a = 0.1E-6;

        ImmutableList<TraversalSection> sections = ConstantJerkSectionsFactory.getToVmaxThenBrake(u, a, vehicleMotionProperties);

        assertThat(sections).hasSize(6);
        checkSections(u, a, sections);
    }

    @Test
    void testGetToVmaxAndStayAtVMaxToReachD_firstSection() {
        double u = 0.1E-3;
        double a = 0.1E-6;

        ImmutableList<TraversalSection> sections = ConstantJerkSectionsFactory.getToVmaxAndStayAtVMaxToReachDistance(100, u, a, vehicleMotionProperties);

        assertThat(sections).hasSize(7);
        checkSections(u, a, sections);
    }

    @Test
    void testGetToVmaxThenBrake_midSection() {
        double u = vehicleMotionProperties.maxSpeed / 2d;
        double a = vehicleMotionProperties.acceleration;

        ImmutableList<TraversalSection> sections = ConstantJerkSectionsFactory.getToVmaxThenBrake(
                u, a, vehicleMotionProperties);

        assertThat(sections).hasSize(5);
        checkSections(u, a, sections);
    }

    @Test
    void testGetToVmaxAndStayAtVMaxToReachD_midSection() {
        double u = vehicleMotionProperties.maxSpeed / 2d;
        double a = vehicleMotionProperties.acceleration;

        ImmutableList<TraversalSection> sections = ConstantJerkSectionsFactory.getToVmaxAndStayAtVMaxToReachDistance(
                100, u, a, vehicleMotionProperties);

        assertThat(sections).hasSize(6);
        checkSections(u, a, sections);
    }

    @Test
    void testGetToVmaxAndStayAtVMaxToReachD_lastSection() {
        ImmutableList<TraversalSection> sections1 = ConstantJerkSectionsFactory.getToVmaxAndStayAtVMaxToReachDistance(
                100, vehicleMotionProperties.maxSpeed / 2d, vehicleMotionProperties.acceleration, vehicleMotionProperties);

        Traversal traversal = new Traversal(sections1);

        double time = 2E3;
        double a = traversal.getAccelerationAtDistance(traversal.getDistanceAtTime(time));
        double u = traversal.getSpeedAtTime(time);

        ImmutableList<TraversalSection> sections2 = ConstantJerkSectionsFactory.getToVmaxAndStayAtVMaxToReachDistance(
                100, u, a, vehicleMotionProperties);

        assertThat(sections2).hasSize(5);

        checkSections(u, a, sections2);

        assertThat(getJerk(sections2.get(0))).isNegative();

        assertThat(sections2.get(1).getAccelerationAtTime(0)).isCloseTo(0, within(EPSILON));
        assertThat(sections2.get(1).getAccelerationAtTime(sections2.get(1).getDuration())).isCloseTo(0, within(EPSILON));

        assertThat(sections2.get(2)).isEqualTo(sections1.get(3));
        assertThat(sections2.get(3)).isEqualTo(sections1.get(4));
        assertThat(sections2.get(4)).isEqualTo(sections1.get(5));
    }

    @Test
    void testGetToVmaxAndStayAtVMaxToReachD_lastSectionUndershoot() {
        double u = vehicleMotionProperties.maxSpeed - 0.5E-3;
        double a = 0.5E-6;

        ImmutableList<TraversalSection> sections = ConstantJerkSectionsFactory.getToVmaxAndStayAtVMaxToReachDistance(
                100, u, a, vehicleMotionProperties);

        assertThat(sections).hasSize(6);
        checkSections(u, a, sections);
    }

    @Test
    void testGetToVmaxAndStayAtVMaxToReachD_lastSectionOvershoot() {
        double u = vehicleMotionProperties.maxSpeed - 0.5E-3;
        double a = 2.3E-6;

        ImmutableList<TraversalSection> sections = ConstantJerkSectionsFactory.getToVmaxAndStayAtVMaxToReachDistance(
                500, u, a, vehicleMotionProperties);

        assertThat(sections).hasSize(5);
        checkFirstSection(u, a, sections.get(0));
        checkLastSection(sections.get(sections.size() - 1));
    }

    private static void verifyTraversalSection(TraversalSection section, double totalDistance, double duration, double initialSpeed, double finalSpeed, double initialAcceleration, double finalAcceleration, double jerk) {
        assertThat(section.getTotalDistance()).isCloseTo(totalDistance, within(EPSILON));
        assertThat(getDurationInSIUnit(section.getDuration())).isCloseTo(duration, within(EPSILON));
        assertThat(getSpeedInSIUnit(section.getSpeedAtTime(0))).isCloseTo(initialSpeed, within(EPSILON));
        assertThat(getSpeedInSIUnit(section.getSpeedAtTime(section.getDuration()))).isCloseTo(finalSpeed, within(EPSILON));
        assertThat(getAccelerationInSIUnit(section.getAccelerationAtTime(0))).isCloseTo(initialAcceleration, within(EPSILON));
        assertThat(getAccelerationInSIUnit(section.getAccelerationAtTime(section.getDuration()))).isCloseTo(finalAcceleration, within(EPSILON));
        assertThat(getJerkInSIUnit(getJerk(section))).isCloseTo(jerk, within(EPSILON));
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

    private static void checkJoinedUp(ImmutableList<TraversalSection> sections) {
        checkJoinedUp("", sections);
    }

    static void checkJoinedUp(String message, ImmutableList<TraversalSection> sections) {
        UnmodifiableIterator<TraversalSection> it = sections.iterator();

        TraversalSection prevSection = null;

        while (it.hasNext()) {
            TraversalSection section = it.next();

            if (prevSection != null) {
                assertThat(section.getSpeedAtTime(0))
                        .as(message)
                        .isCloseTo(prevSection.getSpeedAtTime(prevSection.getDuration()), within(EPSILON));
                assertThat(section.getAccelerationAtTime(0))
                        .as(message)
                        .isCloseTo(prevSection.getAccelerationAtTime(prevSection.getDuration()), within(EPSILON));
            }

            prevSection = section;
        }
    }

    static void checkJoinedUpVelocities(String message, ImmutableList<TraversalSection> sections) {
        assertThat(sections).as(message).isNotEmpty();

        UnmodifiableIterator<TraversalSection> it = sections.iterator();

        TraversalSection prevSection = null;

        while (it.hasNext()) {
            TraversalSection section = it.next();
            if (prevSection != null) {
                assertThat(section.getSpeedAtTime(0))
                        .as(message)
                        .isCloseTo(prevSection.getSpeedAtTime(prevSection.getDuration()), within(EPSILON));
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

    private static double getJerk(TraversalSection s) {
        return s instanceof ConstantJerkTraversalSection ? ((ConstantJerkTraversalSection) s).jerk : 0;
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
}
        