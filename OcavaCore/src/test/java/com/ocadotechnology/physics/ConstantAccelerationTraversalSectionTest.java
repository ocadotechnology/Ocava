/*
 * Copyright © 2017-2019 Ocado (Ocava)
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

class ConstantAccelerationTraversalSectionTest {
    private static final double EPSILON = 1e-6;

    private static final double DISTANCE = 75;
    private static final double DURATION = 5;
    private static final double LOW_SPEED = 10;
    private static final double HIGH_SPEED = 20;
    private static final double ACCELERATION = 2;

    private final ConstantAccelerationTraversalSection acceleratingTraversalSection = new ConstantAccelerationTraversalSection(DISTANCE, ACCELERATION, LOW_SPEED, HIGH_SPEED, DURATION);
    private final ConstantAccelerationTraversalSection deceleratingTraversalSection = new ConstantAccelerationTraversalSection(DISTANCE, -ACCELERATION, HIGH_SPEED, LOW_SPEED, DURATION);

    @Test
    @DisplayName("getDuration() method")
    void testGetDuration_returnsFullDuration() {
        assertThat(acceleratingTraversalSection.getDuration()).isEqualTo(DURATION);
        assertThat(deceleratingTraversalSection.getDuration()).isEqualTo(DURATION);
    }

    @Test
    @DisplayName("getTotalDistance() method")
    void testGetTotalDistance_returnsFullDistance() {
        assertThat(acceleratingTraversalSection.getTotalDistance()).isEqualTo(DISTANCE);
        assertThat(deceleratingTraversalSection.getTotalDistance()).isEqualTo(DISTANCE);
    }

    @Test
    @DisplayName("isConstantSpeed() method")
    void isConstantSpeed_returnsFalse() {
        assertThat(acceleratingTraversalSection.isConstantSpeed()).isFalse();
        assertThat(deceleratingTraversalSection.isConstantSpeed()).isFalse();
    }

    @Test
    @DisplayName("isConstantAcceleration() method")
    void isConstantAcceleration_returnsTrue() {
        assertThat(acceleratingTraversalSection.isConstantAcceleration()).isTrue();
        assertThat(deceleratingTraversalSection.isConstantAcceleration()).isTrue();
    }

    @Nested
    @DisplayName("isAccelerating() method")
    class IsAcceleratingMethod {
        @Test
        @DisplayName("returns true when accelerating")
        void isAccelerating_whenAccelerationPositive_thenReturnsTrue() {
            assertThat(acceleratingTraversalSection.isAccelerating()).isTrue();
        }

        @Test
        @DisplayName("returns false when decelerating")
        void isAccelerating_whenAccelerationNegative_thenReturnsFalse() {
            assertThat(deceleratingTraversalSection.isAccelerating()).isFalse();
        }
    }

    @Nested
    @DisplayName("isDecelerating() method")
    class IsDeceleratingMethod {
        @Test
        @DisplayName("returns false when accelerating")
        void isDecelerating_whenAccelerationPositive_thenReturnsFalse() {
            assertThat(acceleratingTraversalSection.isDecelerating()).isFalse();
        }

        @Test
        @DisplayName("returns true when decelerating")
        void isDecelerating_whenAccelerationNegative_thenReturnsTrue() {
            assertThat(deceleratingTraversalSection.isDecelerating()).isTrue();
        }
    }

    @Nested
    @DisplayName("getTimeAtDistance() method")
    class TimeAtDistanceMethodTest {

        @Test
        @DisplayName("calculates correct time")
        void getTimeAtDistance_whenValidDistance_thenCalculatesTheRightTime() {
            assertThat(acceleratingTraversalSection.getTimeAtDistance(5)).isCloseTo(0.477226, within(EPSILON));
        }

        @Test
        @DisplayName("calculates correct time from zero distance")
        void getTimeAtDistance_whenZeroDistance_thenCalculatesTheRightTime() {
            assertThat(acceleratingTraversalSection.getTimeAtDistance(0)).isEqualTo(0);
        }

        @Test
        @DisplayName("calculates correct time from max distance")
        void getTimeAtDistance_whenMaxDistance_thenCalculatesTheRightTime() {
            assertThat(acceleratingTraversalSection.getTimeAtDistance(DISTANCE)).isEqualTo(DURATION);
        }

        @Test
        @DisplayName("throws expected error on negative distance")
        void getTimeAtDistance_whenNegativeDistance_thenThrowsException() {
            assertThatThrownBy(() -> acceleratingTraversalSection.getTimeAtDistance(-1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("throws expected error on too large distance")
        void getTimeAtDistance_whenTooLargeDistance_thenThrowsException() {
            assertThatThrownBy(() -> acceleratingTraversalSection.getTimeAtDistance(DISTANCE + 1)).isInstanceOf(TraversalCalculationException.class);
        }
    }

    @Nested
    @DisplayName("getDistanceAtTime() method")
    class DistanceAtTimeMethodTest {

        @Test
        @DisplayName("calculates correct distance")
        void getDistanceAtTime_whenValidTime_thenCalculatesTheRightDistance() {
            assertThat(acceleratingTraversalSection.getDistanceAtTime(2)).isCloseTo(24, within(EPSILON));
        }

        @Test
        @DisplayName("calculates correct distance from zero time")
        void getDistanceAtTime_whenZeroTime_thenCalculatesTheRightDistance() {
            assertThat(acceleratingTraversalSection.getDistanceAtTime(0)).isEqualTo(0);
        }

        @Test
        @DisplayName("calculates correct distance from max time")
        void getDistanceAtTime_whenMaxTime_thenCalculatesTheRightDistance() {
            assertThat(acceleratingTraversalSection.getDistanceAtTime(DURATION)).isEqualTo(DISTANCE);
        }

        @Test
        @DisplayName("throws expected error on negative time")
        void getDistanceAtTime_whenNegativeTime_thenThrowsException() {
            assertThatThrownBy(() -> acceleratingTraversalSection.getDistanceAtTime(-1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("throws expected error on too large time")
        void getDistanceAtTime_whenTooLargeTime_thenThrowsException() {
            assertThatThrownBy(() -> acceleratingTraversalSection.getDistanceAtTime(DURATION + 1)).isInstanceOf(TraversalCalculationException.class);
        }
    }

    @Nested
    @DisplayName("getSpeedAtDistance() method")
    class SpeedAtDistanceMethodTest {

        @Test
        @DisplayName("calculates correct speed")
        void getSpeedAtDistance_whenValidDistance_thenCalculatesTheRightSpeed() {
            assertThat(acceleratingTraversalSection.getSpeedAtDistance(5)).isCloseTo(10.954451, within(EPSILON));
        }

        @Test
        @DisplayName("calculates correct speed from zero distance")
        void getSpeedAtDistance_whenZeroDistance_thenCalculatesTheRightSpeed() {
            assertThat(acceleratingTraversalSection.getSpeedAtDistance(0)).isEqualTo(LOW_SPEED);
        }

        @Test
        @DisplayName("calculates correct speed from max distance")
        void getSpeedAtDistance_whenMaxDistance_thenCalculatesTheRightSpeed() {
            assertThat(acceleratingTraversalSection.getSpeedAtDistance(DISTANCE)).isEqualTo(HIGH_SPEED);
        }

        @Test
        @DisplayName("throws expected error on negative distance")
        void getSpeedAtDistance_whenNegativeDistance_thenThrowsException() {
            assertThatThrownBy(() -> acceleratingTraversalSection.getSpeedAtDistance(-1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("throws expected error on too large distance")
        void getSpeedAtDistance_whenTooLargeDistance_thenThrowsException() {
            assertThatThrownBy(() -> acceleratingTraversalSection.getSpeedAtDistance(DISTANCE + 1)).isInstanceOf(TraversalCalculationException.class);
        }
    }

    @Nested
    @DisplayName("getSpeedAtTime() method")
    class SpeedAtTimeMethodTest {

        @Test
        @DisplayName("calculates correct speed")
        void getSpeedAtTime_whenValidTime_thenCalculatesTheRightSpeed() {
            assertThat(acceleratingTraversalSection.getSpeedAtTime(2)).isCloseTo(14, within(EPSILON));
        }

        @Test
        @DisplayName("calculates correct speed from zero time")
        void getSpeedAtTime_whenZeroTime_thenCalculatesTheRightSpeed() {
            assertThat(acceleratingTraversalSection.getSpeedAtTime(0)).isEqualTo(LOW_SPEED);
        }

        @Test
        @DisplayName("calculates correct speed from max time")
        void getSpeedAtTime_whenMaxTime_thenCalculatesTheRightSpeed() {
            assertThat(acceleratingTraversalSection.getSpeedAtTime(DURATION)).isEqualTo(HIGH_SPEED);
        }

        @Test
        @DisplayName("throws expected error on negative time")
        void getSpeedAtTime_whenNegativeTime_thenThrowsException() {
            assertThatThrownBy(() -> acceleratingTraversalSection.getSpeedAtTime(-1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("throws expected error on too large time")
        void getSpeedAtTime_whenTooLargeTime_thenThrowsException() {
            assertThatThrownBy(() -> acceleratingTraversalSection.getSpeedAtTime(DURATION + 1)).isInstanceOf(TraversalCalculationException.class);
        }
    }

    @Nested
    @DisplayName("getAccelerationAtDistance() method")
    class AccelerationAtDistanceMethodTest {

        @Test
        @DisplayName("calculates correct acceleration")
        void getAccelerationAtDistance_whenValidDistance_thenCalculatesTheRightAcceleration() {
            assertThat(acceleratingTraversalSection.getAccelerationAtDistance(DISTANCE / 2)).isEqualTo(ACCELERATION);
        }

        @Test
        @DisplayName("calculates correct acceleration from zero distance")
        void getAccelerationAtDistance_whenZeroDistance_thenCalculatesTheRightAcceleration() {
            assertThat(acceleratingTraversalSection.getAccelerationAtDistance(0)).isEqualTo(ACCELERATION);
        }

        @Test
        @DisplayName("calculates correct acceleration from max distance")
        void getAccelerationAtDistance_whenMaxDistance_thenCalculatesTheRightAcceleration() {
            assertThat(acceleratingTraversalSection.getAccelerationAtDistance(DISTANCE)).isEqualTo(ACCELERATION);
        }

        @Test
        @DisplayName("throws expected error on negative distance")
        void getAccelerationAtDistance_whenNegativeDistance_thenThrowsException() {
            assertThatThrownBy(() -> acceleratingTraversalSection.getAccelerationAtDistance(-1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("throws expected error on too large distance")
        void getAccelerationAtDistance_whenTooLargeDistance_thenThrowsException() {
            assertThatThrownBy(() -> acceleratingTraversalSection.getAccelerationAtDistance(DISTANCE + 1)).isInstanceOf(TraversalCalculationException.class);
        }
    }

    @Nested
    @DisplayName("getAccelerationAtTime() method")
    class AccelerationAtTimeMethodTest {

        @Test
        @DisplayName("calculates correct acceleration")
        void getAccelerationAtDistance_whenValidDistance_thenCalculatesTheRightAcceleration() {
            assertThat(acceleratingTraversalSection.getAccelerationAtTime(DURATION / 2)).isEqualTo(ACCELERATION);
        }

        @Test
        @DisplayName("calculates correct acceleration from zero time")
        void getAccelerationAtTime_whenZeroTime_thenCalculatesTheRightAcceleration() {
            assertThat(acceleratingTraversalSection.getAccelerationAtTime(0)).isEqualTo(ACCELERATION);
        }

        @Test
        @DisplayName("calculates correct acceleration from max time")
        void getAccelerationAtTime_whenMaxTime_thenCalculatesTheRightAcceleration() {
            assertThat(acceleratingTraversalSection.getAccelerationAtTime(DURATION)).isEqualTo(ACCELERATION);
        }

        @Test
        @DisplayName("throws expected error on negative distance")
        void getAccelerationAtDistance_whenNegativeDistance_thenThrowsException() {
            assertThatThrownBy(() -> acceleratingTraversalSection.getAccelerationAtTime(-1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("throws expected error on too large distance")
        void getAccelerationAtDistance_whenTooLargeDistance_thenThrowsException() {
            assertThatThrownBy(() -> acceleratingTraversalSection.getAccelerationAtTime(DURATION + 1)).isInstanceOf(TraversalCalculationException.class);
        }
    }

}
