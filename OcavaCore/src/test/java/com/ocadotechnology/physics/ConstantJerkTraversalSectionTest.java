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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ConstantJerkTraversalSectionTest {
    private static final double EPSILON = 1e-5;

    private final double DURATION = 1;
    private final double LOW_SPEED = 0;
    private final double HIGH_SPEED = 1.5;
    private final double LOW_ACCELERATION = 1;
    private final double HIGH_ACCELERATION = 2;
    private final double JERK = 1;

    private final double JERK_UP_DISTANCE = 2.0/3.0;
    private final double JERK_DOWN_DISTANCE = 5.0/6.0;

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
        @DisplayName("calculates correct time from max distance")
        void getTimeAtDistance_whenMaxDistance_thenCalculatesTheRightTime() {
            assertThat(jerkUpAcceleratingSection.getTimeAtDistance(JERK_UP_DISTANCE)).isEqualTo(DURATION);
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
    }

    @Nested
    @DisplayName("getDistanceAtTime() method")
    class DistanceAtTimeMethodTest {

        @Test
        @DisplayName("calculates correct distance")
        void getDistanceAtTime_whenValidTime_thenCalculatesTheRightDistance() {
            assertThat(jerkUpAcceleratingSection.getDistanceAtTime(0.5)).isCloseTo(7.0/48.0, within(EPSILON));
        }

        @Test
        @DisplayName("calculates correct distance from zero time")
        void getDistanceAtTime_whenZeroTime_thenCalculatesTheRightDistance() {
            assertThat(jerkUpAcceleratingSection.getDistanceAtTime(0)).isEqualTo(0);
        }

        @Test
        @DisplayName("calculates correct distance from max time")
        void getDistanceAtTime_whenMaxTime_thenCalculatesTheRightDistance() {
            assertThat(jerkUpAcceleratingSection.getDistanceAtTime(DURATION)).isEqualTo(JERK_UP_DISTANCE);
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
        @DisplayName("calculates correct speed from max distance")
        void getSpeedAtDistance_whenMaxDistance_thenCalculatesTheRightSpeed() {
            assertThat(jerkUpAcceleratingSection.getSpeedAtDistance(JERK_UP_DISTANCE)).isEqualTo(HIGH_SPEED);
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
    }

    @Nested
    @DisplayName("getSpeedAtTime() method")
    class SpeedAtTimeMethodTest {

        @Test
        @DisplayName("calculates correct speed")
        void getSpeedAtTime_whenValidTime_thenCalculatesTheRightDistance() {
            assertThat(jerkUpAcceleratingSection.getSpeedAtTime(0.5)).isCloseTo(5.0/8.0, within(EPSILON));
        }

        @Test
        @DisplayName("calculates correct speed from zero time")
        void getSpeedAtTime_whenZeroTime_thenCalculatesTheRightSpeed() {
            assertThat(jerkUpAcceleratingSection.getSpeedAtTime(0)).isEqualTo(LOW_SPEED);
        }

        @Test
        @DisplayName("calculates correct speed from max time")
        void getSpeedAtTime_whenMaxTime_thenCalculatesTheRightSpeed() {
            assertThat(jerkUpAcceleratingSection.getSpeedAtTime(DURATION)).isEqualTo(HIGH_SPEED);
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
        @DisplayName("calculates correct acceleration from max distance")
        void getAccelerationAtDistance_whenMaxDistance_thenCalculatesTheRightAcceleration() {
            assertThat(jerkUpAcceleratingSection.getAccelerationAtDistance(JERK_UP_DISTANCE)).isEqualTo(HIGH_ACCELERATION);
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
        @DisplayName("calculates correct acceleration from max time")
        void getAccelerationAtTime_whenMaxTime_thenCalculatesTheRightAcceleration() {
            assertThat(jerkUpAcceleratingSection.getAccelerationAtTime(DURATION)).isEqualTo(HIGH_ACCELERATION);
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
    }
}
