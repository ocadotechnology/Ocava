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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.within;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ConstantJerkTraversalSectionTest {
    private static final double EPSILON = 1e-5;
    private static final double ROUNDING_ERROR_FRACTION = 1E-9;

    private final double DURATION = 1;
    private final double LOW_SPEED = 0;
    private final double HIGH_SPEED = 1.5;
    private final double LOW_ACCELERATION = 1;
    private final double HIGH_ACCELERATION = 2;
    private final double JERK = 1;

    private final double JERK_UP_DISTANCE = 2.0 / 3.0;
    private final double JERK_DOWN_DISTANCE = 5.0 / 6.0;

    private final ConstantJerkTraversalSection jerkUpAcceleratingSection = new ConstantJerkTraversalSection(DURATION, JERK_UP_DISTANCE, LOW_SPEED, HIGH_SPEED, LOW_ACCELERATION, HIGH_ACCELERATION, JERK);
    private final ConstantJerkTraversalSection jerkDownAcceleratingSection = new ConstantJerkTraversalSection(DURATION, JERK_DOWN_DISTANCE, LOW_SPEED, HIGH_SPEED, HIGH_ACCELERATION, LOW_ACCELERATION, -JERK);
    private final ConstantJerkTraversalSection jerkUpDeceleratingSection = new ConstantJerkTraversalSection(DURATION, JERK_UP_DISTANCE, HIGH_SPEED, LOW_SPEED, -HIGH_ACCELERATION, -LOW_ACCELERATION, JERK);
    private final ConstantJerkTraversalSection jerkDownDeceleratingSection = new ConstantJerkTraversalSection(DURATION, JERK_DOWN_DISTANCE, HIGH_SPEED, LOW_SPEED, -LOW_ACCELERATION, -HIGH_ACCELERATION, -JERK);

    @Test
    @DisplayName("getDuration() method")
    void testGetDuration_returnsFullDuration() {
        assertThat(jerkUpAcceleratingSection.getDuration()).isEqualTo(DURATION);
        assertThat(jerkDownAcceleratingSection.getDuration()).isEqualTo(DURATION);
        assertThat(jerkUpDeceleratingSection.getDuration()).isEqualTo(DURATION);
        assertThat(jerkDownDeceleratingSection.getDuration()).isEqualTo(DURATION);
    }

    @Test
    @DisplayName("getTotalDistance() method")
    void testGetTotalDistance_returnsFullDistance() {
        assertThat(jerkUpAcceleratingSection.getTotalDistance()).isEqualTo(JERK_UP_DISTANCE);
        assertThat(jerkDownAcceleratingSection.getTotalDistance()).isEqualTo(JERK_DOWN_DISTANCE);
        assertThat(jerkUpDeceleratingSection.getTotalDistance()).isEqualTo(JERK_UP_DISTANCE);
        assertThat(jerkDownDeceleratingSection.getTotalDistance()).isEqualTo(JERK_DOWN_DISTANCE);
    }

    @Test
    @DisplayName("isConstantSpeed() method")
    void isConstantSpeed_returnsFalse() {
        assertThat(jerkUpAcceleratingSection.isConstantSpeed()).isFalse();
        assertThat(jerkDownAcceleratingSection.isConstantSpeed()).isFalse();
        assertThat(jerkUpDeceleratingSection.isConstantSpeed()).isFalse();
        assertThat(jerkDownDeceleratingSection.isConstantSpeed()).isFalse();
    }

    @Test
    @DisplayName("isConstantAcceleration() method")
    void isConstantAcceleration_returnsFalse() {
        assertThat(jerkUpAcceleratingSection.isConstantAcceleration()).isFalse();
        assertThat(jerkDownAcceleratingSection.isConstantAcceleration()).isFalse();
        assertThat(jerkUpDeceleratingSection.isConstantAcceleration()).isFalse();
        assertThat(jerkDownDeceleratingSection.isConstantAcceleration()).isFalse();
    }

    @Nested
    @DisplayName("isAccelerating() method")
    class IsAcceleratingMethod {
        @Test
        @DisplayName("returns true when accelerating")
        void isAccelerating_whenAccelerationPositive_thenReturnsTrue() {
            assertThat(jerkUpAcceleratingSection.isAccelerating()).isTrue();
            assertThat(jerkDownAcceleratingSection.isAccelerating()).isTrue();
        }

        @Test
        @DisplayName("returns false when decelerating")
        void isAccelerating_whenAccelerationNegative_thenReturnsFalse() {
            assertThat(jerkUpDeceleratingSection.isAccelerating()).isFalse();
            assertThat(jerkDownDeceleratingSection.isAccelerating()).isFalse();
        }
    }

    @Nested
    @DisplayName("isDecelerating() method")
    class IsDeceleratingMethod {
        @Test
        @DisplayName("returns false when accelerating")
        void isDecelerating_whenAccelerationPositive_thenReturnsFalse() {
            assertThat(jerkUpAcceleratingSection.isDecelerating()).isFalse();
            assertThat(jerkDownAcceleratingSection.isDecelerating()).isFalse();
        }

        @Test
        @DisplayName("returns true when decelerating")
        void isDecelerating_whenAccelerationNegative_thenReturnsTrue() {
            assertThat(jerkUpDeceleratingSection.isDecelerating()).isTrue();
            assertThat(jerkDownDeceleratingSection.isDecelerating()).isTrue();
        }
    }

    @Nested
    @DisplayName("getTimeAtDistance() method")
    class TimeAtDistanceMethodTest {

        @Test
        @DisplayName("calculates correct time")
        void getTimeAtDistance_whenValidDistance_thenCalculatesTheRightTime() {
            assertThat(jerkUpAcceleratingSection.getTimeAtDistance(0.5)).isCloseTo(0.87939, within(EPSILON));
        }

        @Test
        @DisplayName("calculates correct time from zero distance")
        void getTimeAtDistance_whenZeroDistance_thenCalculatesTheRightTime() {
            assertThat(jerkUpAcceleratingSection.getTimeAtDistance(0)).isEqualTo(0);
        }

        @Test
        @DisplayName("Calculates zero time on distances smaller than the tolerance")
        void getTimeAtDistance_whenDistanceSmallerThanTolerance_thenThenTimeZero() {
            double distance = JERK_UP_DISTANCE * ROUNDING_ERROR_FRACTION * 0.99;
            assertThat(jerkUpAcceleratingSection.getTimeAtDistance(distance)).isEqualTo(0);
            assertThat(jerkUpAcceleratingSection.getTimeAtDistance(-distance)).isEqualTo(0);
        }

        @Test
        @DisplayName("Calculates correct time when distance is very small BUT not smaller than the tolerance")
        void getTimeAtDistance_whenDistanceSmallButLargerThanTolerance_thenThenTimeCorrect() {
            double distance = JERK_UP_DISTANCE * ROUNDING_ERROR_FRACTION * 1.01;
            assertThat(jerkUpAcceleratingSection.getTimeAtDistance(distance))
                    .isCloseTo(3.669E-5, Percentage.withPercentage(0.1));
        }

        @Test
        @DisplayName("calculates correct time from max distance")
        void getTimeAtDistance_whenMaxDistance_thenCalculatesTheRightTime() {
            assertThat(jerkUpAcceleratingSection.getTimeAtDistance(JERK_UP_DISTANCE)).isEqualTo(DURATION);
        }

        @Test
        @DisplayName("calculates correct time from max distance")
        void getTimeAtDistance_whenDistanceWithinToleranceOfMaxDistance_thenCalculatesTheRightTime() {
            double tolerance = JERK_UP_DISTANCE * ROUNDING_ERROR_FRACTION * 0.99;
            assertThat(jerkUpAcceleratingSection.getTimeAtDistance(JERK_UP_DISTANCE + tolerance)).isEqualTo(DURATION);
            assertThat(jerkUpAcceleratingSection.getTimeAtDistance(JERK_UP_DISTANCE - tolerance)).isEqualTo(DURATION);
        }

        @Test
        @DisplayName("throws expected error on negative distance")
        void getTimeAtDistance_whenNegativeDistance_thenThrowsException() {
            assertThatThrownBy(() -> jerkUpAcceleratingSection.getTimeAtDistance(-1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("throws expected error on too large distance")
        void getTimeAtDistance_whenTooLargeDistance_thenThrowsException() {
            assertThatThrownBy(() -> jerkUpAcceleratingSection.getTimeAtDistance(JERK_UP_DISTANCE + 1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("calculates correct time when total distance is very low magnitude")
        void getTimeAtDistance_whenDistanceVeryLowMagnitude_thenCorrectTimeCalculated() {
            // distance = 5E-13
            // time = 1
            ConstantJerkTraversalSection section = ConstantJerkSectionFactory.jerkAccelerationUp(0, 0, 3E-12, 3E-12);
            assertThat(section.getTimeAtDistance(2.5E-13)).isCloseTo(0.7937, Percentage.withPercentage(0.01));
        }

        @Test
        @DisplayName("calculates correct time when total distance and duration is very low magnitude")
        void getTimeAtDistance_whenDistanceAndTimeVeryLowMagnitude_thenCorrectTimeCalculated() {
            // distance = 3E-12
            // time = 3E-12
            ConstantJerkTraversalSection section = ConstantJerkSectionFactory.jerkAccelerationUp(1, 0, 3E-12, 1);
            assertThat(section.getTimeAtDistance(1.5E-12)).isCloseTo(1.4999E-12, Percentage.withPercentage(0.01));
        }
    }

    @Nested
    @DisplayName("getDistanceAtTime() method")
    class DistanceAtTimeMethodTest {

        @Test
        @DisplayName("calculates correct distance")
        void getDistanceAtTime_whenValidTime_thenCalculatesTheRightDistance() {
            assertThat(jerkUpAcceleratingSection.getDistanceAtTime(0.5)).isCloseTo(7.0 / 48.0, within(EPSILON));
        }

        @Test
        @DisplayName("calculates correct distance from zero time")
        void getDistanceAtTime_whenZeroTime_thenCalculatesTheRightDistance() {
            assertThat(jerkUpAcceleratingSection.getDistanceAtTime(0)).isEqualTo(0);
        }

        @Test
        @DisplayName("Calculates zero distance on times smaller than the tolerance")
        void getDistanceAtTime_whenTimeSmallerThanTolerance_thenThenDistanceZero() {
            double time = DURATION * ROUNDING_ERROR_FRACTION * 0.99;
            assertThat(jerkUpAcceleratingSection.getDistanceAtTime(time)).isEqualTo(0);
            assertThat(jerkUpAcceleratingSection.getDistanceAtTime(-time)).isEqualTo(0);
        }

        @Test
        @DisplayName("Calculates correct distance when time is very small BUT not smaller than the tolerance")
        void getDistanceAtTime_whenTimeSmallButLargerThanTolerance_thenThenDistanceCorrect() {
            double time = DURATION * ROUNDING_ERROR_FRACTION * 1.01;
            assertThat(jerkUpAcceleratingSection.getDistanceAtTime(time))
                    .isCloseTo(5.100E-19, Percentage.withPercentage(0.1));
        }

        @Test
        @DisplayName("calculates correct distance from max time")
        void getDistanceAtTime_whenMaxTime_thenCalculatesTheRightDistance() {
            assertThat(jerkUpAcceleratingSection.getDistanceAtTime(DURATION)).isEqualTo(JERK_UP_DISTANCE);
        }

        @Test
        @DisplayName("calculates correct distance when time within tolerance of max")
        void getDistanceAtTime_whenTimeWithinToleranceOfMaxTime_thenCalculatesTheRightDistance() {
            double tolerance = DURATION * ROUNDING_ERROR_FRACTION * 0.99;
            assertThat(jerkUpAcceleratingSection.getDistanceAtTime(DURATION + tolerance)).isEqualTo(JERK_UP_DISTANCE);
            assertThat(jerkUpAcceleratingSection.getDistanceAtTime(DURATION - tolerance)).isEqualTo(JERK_UP_DISTANCE);
        }

        @Test
        @DisplayName("throws expected error on negative time")
        void getDistanceAtTime_whenNegativeTime_thenThrowsException() {
            assertThatThrownBy(() -> jerkUpAcceleratingSection.getDistanceAtTime(-1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("throws expected error on too large time")
        void getDistanceAtTime_whenTooLargeTime_thenThrowsException() {
            assertThatThrownBy(() -> jerkUpAcceleratingSection.getDistanceAtTime(DURATION + 1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("Calculates correct distance when duration is very low magnitude")
        void getDistanceAtTime_whenTimeVeryLowMagnitude_thenCorrectDistanceCalculated() {
            // distance = 3
            // time = 3E-12
            ConstantJerkTraversalSection section = ConstantJerkSectionFactory.jerkAccelerationUp(1E12, 0, 3E-12, 1);
            assertThat(section.getDistanceAtTime(1.5E-12)).isCloseTo(1.5, Percentage.withPercentage(0.01));
        }

        @Test
        @DisplayName("Calculates correct distance when duration and distance is very low magnitude")
        void getDistanceAtTime_whenDistanceAndTimeVeryLowMagnitude_thenCorrectTimeCalculated() {
            // distance = 3E-12
            // time = 3E-12
            ConstantJerkTraversalSection section = ConstantJerkSectionFactory.jerkAccelerationUp(1, 0, 3E-12, 1);
            assertThat(section.getDistanceAtTime(1.5E-12)).isCloseTo(1.4999E-12, Percentage.withPercentage(0.01));
        }
    }

    @Nested
    @DisplayName("getSpeedAtDistance() method")
    class SpeedAtDistanceMethodTest {

        @Test
        @DisplayName("calculates correct speed")
        void getSpeedAtDistance_whenValidDistance_thenCalculatesTheRightSpeed() {
            assertThat(jerkUpAcceleratingSection.getSpeedAtDistance(0.5)).isCloseTo(1.26605, within(EPSILON));
        }

        @Test
        @DisplayName("calculates correct speed from zero distance")
        void getSpeedAtDistance_whenZeroDistance_thenCalculatesTheRightSpeed() {
            assertThat(jerkUpAcceleratingSection.getSpeedAtDistance(0)).isEqualTo(LOW_SPEED);
        }

        @Test
        @DisplayName("calculates correct speed when distance is smaller than tolerance")
        void getSpeedAtDistance_whenDistanceSmallerThanTolerance_thenCalculatesTheRightSpeed() {
            double distance = JERK_UP_DISTANCE * ROUNDING_ERROR_FRACTION * 0.99;
            assertThat(jerkUpAcceleratingSection.getSpeedAtDistance(distance)).isEqualTo(LOW_SPEED);
            assertThat(jerkUpAcceleratingSection.getSpeedAtDistance(-distance)).isEqualTo(LOW_SPEED);
        }

        @Test
        @DisplayName("calculates correct speed when distance is small but larger than the tolerance")
        void getSpeedAtDistance_whenDistanceSmallButLargerThanTolerance_thenCalculatesTheRightSpeed() {
            double distance = JERK_UP_DISTANCE * ROUNDING_ERROR_FRACTION * 1.01;
            assertThat(jerkUpAcceleratingSection.getSpeedAtDistance(distance)).isCloseTo(3.6697E-5, Percentage.withPercentage(0.01));
        }

        @Test
        @DisplayName("calculates correct speed from max distance")
        void getSpeedAtDistance_whenMaxDistance_thenCalculatesTheRightSpeed() {
            assertThat(jerkUpAcceleratingSection.getSpeedAtDistance(JERK_UP_DISTANCE)).isEqualTo(HIGH_SPEED);
        }

        @Test
        @DisplayName("calculates correct speed when distance is within tolerance of max")
        void getSpeedAtDistance_whenDistanceWithinToleranceOfMaxDistance_thenCalculatesTheRightSpeed() {
            double tolerance = JERK_UP_DISTANCE * ROUNDING_ERROR_FRACTION * 0.99;
            assertThat(jerkUpAcceleratingSection.getSpeedAtDistance(JERK_UP_DISTANCE + tolerance)).isEqualTo(HIGH_SPEED);
            assertThat(jerkUpAcceleratingSection.getSpeedAtDistance(JERK_UP_DISTANCE - tolerance)).isEqualTo(HIGH_SPEED);
        }

        @Test
        @DisplayName("throws expected error on negative distance")
        void getSpeedAtDistance_whenNegativeDistance_thenThrowsException() {
            assertThatThrownBy(() -> jerkUpAcceleratingSection.getSpeedAtDistance(-1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("throws expected error on too large distance")
        void getSpeedAtDistance_whenTooLargeDistance_thenThrowsException() {
            assertThatThrownBy(() -> jerkUpAcceleratingSection.getSpeedAtDistance(JERK_UP_DISTANCE + 1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("Calculates correct speed when distance and speed is very low magnitude")
        void getSpeedAtDistance_whenDistanceAndSpeedVeryLowMagnitude_thenCorrectTimeCalculated() {
            // v = 4.5E-12
            // distance = 4.5E-18
            ConstantJerkTraversalSection section = ConstantJerkSectionFactory.jerkAccelerationUp(0, 0, 3E-6, 1);
            assertThat(section.getSpeedAtDistance(2.25E-18)).isCloseTo(2.8348E-12, Percentage.withPercentage(0.01));
        }
    }

    @Nested
    @DisplayName("getSpeedAtTime() method")
    class SpeedAtTimeMethodTest {

        @Test
        @DisplayName("calculates correct speed")
        void getSpeedAtTime_whenValidTime_thenCalculatesTheRightDistance() {
            assertThat(jerkUpAcceleratingSection.getSpeedAtTime(0.5)).isCloseTo(5.0 / 8.0, within(EPSILON));
        }

        @Test
        @DisplayName("calculates correct speed from zero time")
        void getSpeedAtTime_whenZeroTime_thenCalculatesTheRightSpeed() {
            assertThat(jerkUpAcceleratingSection.getSpeedAtTime(0)).isEqualTo(LOW_SPEED);
        }

        @Test
        @DisplayName("calculates correct speed when time is smaller than the tolerance")
        void getSpeedAtTime_whenTimeSmallerThanTolerance_thenCalculatesTheRightSpeed() {
            double time = DURATION * ROUNDING_ERROR_FRACTION * 0.99;
            assertThat(jerkUpAcceleratingSection.getSpeedAtTime(time)).isEqualTo(LOW_SPEED);
            assertThat(jerkUpAcceleratingSection.getSpeedAtTime(-time)).isEqualTo(LOW_SPEED);
        }

        @Test
        @DisplayName("calculates correct speed hen time is small but larger than the tolerance")
        void getSpeedAtTime_whenTimeSmallButLargerThanTolerance_thenCalculatesTheRightSpeed() {
            double time = DURATION * ROUNDING_ERROR_FRACTION * 1.01;
            assertThat(jerkUpAcceleratingSection.getSpeedAtTime(time)).isCloseTo(1.01E-9, Percentage.withPercentage(0.01));
        }

        @Test
        @DisplayName("calculates correct speed from max time")
        void getSpeedAtTime_whenMaxTime_thenCalculatesTheRightSpeed() {
            assertThat(jerkUpAcceleratingSection.getSpeedAtTime(DURATION)).isEqualTo(HIGH_SPEED);
        }

        @Test
        @DisplayName("calculates correct speed from max time")
        void getSpeedAtTime_whenTimeWithinToleranceOfMaxTime_thenCalculatesTheRightSpeed() {
            double tolerance = DURATION * ROUNDING_ERROR_FRACTION * 0.99;
            assertThat(jerkUpAcceleratingSection.getSpeedAtTime(DURATION + tolerance)).isEqualTo(HIGH_SPEED);
            assertThat(jerkUpAcceleratingSection.getSpeedAtTime(DURATION - tolerance)).isEqualTo(HIGH_SPEED);
        }

        @Test
        @DisplayName("throws expected error on negative time")
        void getSpeedAtTime_whenNegativeTime_thenThrowsException() {
            assertThatThrownBy(() -> jerkUpAcceleratingSection.getSpeedAtTime(-1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("throws expected error on too large time")
        void getSpeedAtTime_whenTooLargeTime_thenThrowsException() {
            assertThatThrownBy(() -> jerkUpAcceleratingSection.getSpeedAtTime(DURATION + 1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("Calculates correct speed when the total duration is of very low magnitude")
        void getSpeedAtTime_whenTimeVeryLowMagnitude_thenCorrectDistanceCalculated() {
            // v = 1.0000000000005
            // time = 1E-12
            ConstantJerkTraversalSection section = ConstantJerkSectionFactory.jerkAccelerationUp(0, 1E12, 1E12 + 1, 1E12);
            assertThat(section.getSpeedAtTime(0.5E-12)).isCloseTo(0.500000000000125, Percentage.withPercentage(0.01));
        }

        @Test
        @DisplayName("Calculates correct speed when the final speed and total duration is of very low magnitude")
        void getSpeedAtTime_whenSpeedAndTimeVeryLowMagnitude_thenCorrectTimeCalculated() {
            // v = 5E-13
            // time = 1E-12
            ConstantJerkTraversalSection section = ConstantJerkSectionFactory.jerkAccelerationUp(0, 0, 1, 1E12);
            assertThat(section.getSpeedAtTime(0.5E-12)).isCloseTo(1.25E-13, Percentage.withPercentage(0.01));
        }
    }

    @Nested
    @DisplayName("getAccelerationAtDistance() method")
    class AccelerationAtDistanceMethodTest {

        @Test
        @DisplayName("calculates correct acceleration")
        void getAccelerationAtDistance_whenValidDistance_thenCalculatesTheRightSpeed() {
            assertThat(jerkUpAcceleratingSection.getAccelerationAtDistance(0.5)).isCloseTo(1.87939, within(EPSILON));
        }

        @Test
        @DisplayName("calculates correct acceleration from zero distance")
        void getAccelerationAtDistance_whenZeroDistance_thenCalculatesTheRightAcceleration() {
            assertThat(jerkUpAcceleratingSection.getAccelerationAtDistance(0)).isEqualTo(LOW_ACCELERATION);
        }

        @Test
        @DisplayName("calculates correct acceleration when distance smaller than the tolerance")
        void getAccelerationAtDistance_whenDistanceSmallerThanTolerance_thenCalculatesTheRightAcceleration() {
            double distance = JERK_UP_DISTANCE * ROUNDING_ERROR_FRACTION * 0.99;
            assertThat(jerkUpAcceleratingSection.getAccelerationAtDistance(distance)).isEqualTo(LOW_ACCELERATION);
            assertThat(jerkUpAcceleratingSection.getAccelerationAtDistance(-distance)).isEqualTo(LOW_ACCELERATION);
        }

        @Test
        @DisplayName("calculates correct acceleration when the distance is small but still larger than the tolerance")
        void getAccelerationAtDistance_whenDistanceSmallButLargerThanTolerance_thenCalculatesTheRightAcceleration() {
            double distance = JERK_UP_DISTANCE * ROUNDING_ERROR_FRACTION * 1.01;
            assertThat(jerkUpAcceleratingSection.getAccelerationAtDistance(distance)).isCloseTo(1.00003669, Percentage.withPercentage(0.01));
        }

        @Test
        @DisplayName("calculates correct acceleration from max distance")
        void getAccelerationAtDistance_whenMaxDistance_thenCalculatesTheRightAcceleration() {
            assertThat(jerkUpAcceleratingSection.getAccelerationAtDistance(JERK_UP_DISTANCE)).isEqualTo(HIGH_ACCELERATION);
        }

        @Test
        @DisplayName("calculates correct acceleration when distance with tolerance of the max distance")
        void getAccelerationAtDistance_whenDistanceWithinToleranceOfMaxDistance_thenCalculatesTheRightAcceleration() {
            double tolerance = JERK_UP_DISTANCE * ROUNDING_ERROR_FRACTION * 0.99;
            assertThat(jerkUpAcceleratingSection.getAccelerationAtDistance(JERK_UP_DISTANCE + tolerance)).isEqualTo(HIGH_ACCELERATION);
            assertThat(jerkUpAcceleratingSection.getAccelerationAtDistance(JERK_UP_DISTANCE - tolerance)).isEqualTo(HIGH_ACCELERATION);
        }

        @Test
        @DisplayName("throws expected error on negative distance")
        void getAccelerationAtDistance_whenNegativeDistance_thenThrowsException() {
            assertThatThrownBy(() -> jerkUpAcceleratingSection.getAccelerationAtDistance(-1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("throws expected error on too large distance")
        void getAccelerationAtDistance_whenTooLargeDistance_thenThrowsException() {
            assertThatThrownBy(() -> jerkUpAcceleratingSection.getAccelerationAtDistance(JERK_UP_DISTANCE + 1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("Calculates correct acceleration when total distance is very low magnitude")
        void getAccelerationAtDistance_whenDistanceVeryLowMagnitude_thenCorrectTimeCalculated() {
            // a = 1
            // distance = 1E-12
            ConstantJerkTraversalSection section = ConstantJerkSectionFactory.jerkAccelerationUp(1, 0, 1, 1E12);
            assertThat(section.getAccelerationAtDistance(0.5E-12)).isCloseTo(0.49999, Percentage.withPercentage(0.01));
        }

        @Test
        @DisplayName("Calculates correct acceleration when final acceleration and total distance is very low magnitude")
        void getAccelerationAtDistance_whenDistanceAndAccelerationVeryLowMagnitude_thenCorrectTimeCalculated() {
            // a = 3E-12
            // distance = 4.5E-18
            ConstantJerkTraversalSection section = ConstantJerkSectionFactory.jerkAccelerationUp(0, 0, 3E-12, 3E-12);
            assertThat(section.getAccelerationAtDistance(2.5E-13)).isCloseTo(2.3811E-12, Percentage.withPercentage(0.01));
        }
    }

    @Nested
    @DisplayName("getAccelerationAtTime() method")
    class AccelerationAtTimeMethodTest {

        @Test
        @DisplayName("calculates correct acceleration")
        void getAccelerationAtTime_whenValidTime_thenCalculatesTheRightDistance() {
            assertThat(jerkUpAcceleratingSection.getAccelerationAtTime(0.5)).isCloseTo(1.5, within(EPSILON));
        }

        @Test
        @DisplayName("calculates correct acceleration from zero time")
        void getAccelerationAtTime_whenZeroTime_thenCalculatesTheRightAcceleration() {
            assertThat(jerkUpAcceleratingSection.getAccelerationAtTime(0)).isEqualTo(LOW_ACCELERATION);
        }

        @Test
        @DisplayName("calculates correct acceleration when time smaller than tolerance")
        void getAccelerationAtTime_whenTimeSmallerThanTolerance_thenCalculatesTheRightAcceleration() {
            double time = DURATION * ROUNDING_ERROR_FRACTION * 0.99;
            assertThat(jerkUpAcceleratingSection.getAccelerationAtTime(time)).isEqualTo(LOW_ACCELERATION);
        }

        @Test
        @DisplayName("calculates correct acceleration when time small but larger than tolerance")
        void getAccelerationAtTime_whenTimeSmallButLargerThanTolerance_thenCalculatesTheRightAcceleration() {
            double time = DURATION * ROUNDING_ERROR_FRACTION * 1.01;
            assertThat(jerkUpAcceleratingSection.getAccelerationAtTime(time)).isCloseTo(1.00000000101, Percentage.withPercentage(0.000000001));
        }

        @Test
        @DisplayName("calculates correct acceleration from max time")
        void getAccelerationAtTime_whenMaxTime_thenCalculatesTheRightAcceleration() {
            assertThat(jerkUpAcceleratingSection.getAccelerationAtTime(DURATION)).isEqualTo(HIGH_ACCELERATION);
        }

        @Test
        @DisplayName("calculates correct acceleration when time is within tolerances of the max time")
        void getAccelerationAtTime_whenTimeWithinToleranceOfMaxTime_thenCalculatesTheRightAcceleration() {
            double tolerance = DURATION * ROUNDING_ERROR_FRACTION * 0.99;
            assertThat(jerkUpAcceleratingSection.getAccelerationAtTime(DURATION + tolerance)).isEqualTo(HIGH_ACCELERATION);
            assertThat(jerkUpAcceleratingSection.getAccelerationAtTime(DURATION - tolerance)).isEqualTo(HIGH_ACCELERATION);
        }

        @Test
        @DisplayName("throws expected error on negative time")
        void getAccelerationAtTime_whenNegativeTime_thenThrowsException() {
            assertThatThrownBy(() -> jerkUpAcceleratingSection.getAccelerationAtTime(-1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("throws expected error on too large time")
        void getAccelerationAtTime_whenTooLargeTime_thenThrowsException() {
            assertThatThrownBy(() -> jerkUpAcceleratingSection.getAccelerationAtTime(DURATION + 1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("Calculates correct acceleration when total duration is of very low magnitude")
        void getAccelerationAtTime_whenTimeVeryLowMagnitude_thenCorrectTimeCalculated() {
            // a = 1
            // duration = 1E-12
            ConstantJerkTraversalSection section = ConstantJerkSectionFactory.jerkAccelerationUp(1, 0, 1, 1E12);
            assertThat(section.getAccelerationAtTime(0.5E-12)).isCloseTo(0.5, Percentage.withPercentage(0.01));
        }

        @Test
        @DisplayName("Calculates correct acceleration when final acceleration and total duration is of very low magnitude")
        void getAccelerationAtTime_whenTimeAndAccelerationVeryLowMagnitude_thenCorrectTimeCalculated() {
            // a = 3E-12
            // duration = 3E-12
            ConstantJerkTraversalSection section = ConstantJerkSectionFactory.jerkAccelerationUp(0, 0, 3E-12, 1);
            assertThat(section.getAccelerationAtTime(1.5E-12)).isCloseTo(1.5E-12, Percentage.withPercentage(0.01));
        }
    }
}
