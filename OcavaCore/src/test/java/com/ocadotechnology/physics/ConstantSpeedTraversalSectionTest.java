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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("The ConstantSpeedTraversalSection class")
class ConstantSpeedTraversalSectionTest {
    private static final double DURATION = 1000;
    private static final double DISTANCE = 100;
    private static final double SPEED = 0.1;

    private final ConstantSpeedTraversalSection traversalSection = new ConstantSpeedTraversalSection(DISTANCE, SPEED, DURATION);

    @Test
    @DisplayName("getDuration() method")
    void testGetDuration_returnsFullDuration() {
        assertThat(traversalSection.getDuration()).isEqualTo(DURATION);
    }

    @Test
    @DisplayName("getTotalDistance() method")
    void testGetTotalDistance_returnsFullDistance() {
        assertThat(traversalSection.getTotalDistance()).isEqualTo(DISTANCE);
    }

    @Test
    @DisplayName("isAccelerating() method")
    void isAccelerating_returnsFalse() {
        assertThat(traversalSection.isAccelerating()).isFalse();
    }

    @Test
    @DisplayName("isDecelerating() method")
    void isDecelerating_returnsFalse() {
        assertThat(traversalSection.isDecelerating()).isFalse();
    }

    @Test
    @DisplayName("isConstantSpeed() method")
    void isConstantSpeed_returnsTrue() {
        assertThat(traversalSection.isConstantSpeed()).isTrue();
    }

    @Test
    @DisplayName("isConstantAcceleration() method")
    void isConstantAcceleration_returnsTrue() {
        assertThat(traversalSection.isConstantAcceleration()).isTrue();
    }

    @Nested
    @DisplayName("getTimeAtDistance() method")
    class TimeAtDistanceMethodTest {

        @Test
        @DisplayName("calculates correct time")
        void getTimeAtDistance_whenValidDistance_thenCalculatesTheRightTime() {
            assertThat(traversalSection.getTimeAtDistance(DISTANCE / 2)).isEqualTo(DURATION / 2);
        }

        @Test
        @DisplayName("calculates correct time from zero distance")
        void getTimeAtDistance_whenZeroDistance_thenCalculatesTheRightTime() {
            assertThat(traversalSection.getTimeAtDistance(0)).isEqualTo(0);
        }

        @Test
        @DisplayName("calculates correct time from max distance")
        void getTimeAtDistance_whenMaxDistance_thenCalculatesTheRightTime() {
            assertThat(traversalSection.getTimeAtDistance(DISTANCE)).isEqualTo(DURATION);
        }

        @Test
        @DisplayName("throws expected error on negative distance")
        void getTimeAtDistance_whenNegativeDistance_thenThrowsException() {
            assertThatThrownBy(() -> traversalSection.getTimeAtDistance(-1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("throws expected error on too large distance")
        void getTimeAtDistance_whenTooLargeDistance_thenThrowsException() {
            assertThatThrownBy(() -> traversalSection.getTimeAtDistance(DISTANCE + 1)).isInstanceOf(TraversalCalculationException.class);
        }
    }

    @Nested
    @DisplayName("getDistanceAtTime() method")
    class DistanceAtTimeMethodTest {

        @Test
        @DisplayName("calculates correct distance")
        void getDistanceAtTime_whenValidTime_thenCalculatesTheRightDistance() {
            assertThat(traversalSection.getDistanceAtTime(DURATION / 2)).isEqualTo(DISTANCE / 2);
        }

        @Test
        @DisplayName("calculates correct distance from zero time")
        void getDistanceAtTime_whenZeroTime_thenCalculatesTheRightDistance() {
            assertThat(traversalSection.getDistanceAtTime(0)).isEqualTo(0);
        }

        @Test
        @DisplayName("calculates correct distance from max time")
        void getDistanceAtTime_whenMaxTime_thenCalculatesTheRightDistance() {
            assertThat(traversalSection.getDistanceAtTime(DURATION)).isEqualTo(DISTANCE);
        }

        @Test
        @DisplayName("throws expected error on negative time")
        void getDistanceAtTime_whenNegativeTime_thenThrowsException() {
            assertThatThrownBy(() -> traversalSection.getDistanceAtTime(-1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("throws expected error on too large time")
        void getDistanceAtTime_whenTooLargeTime_thenThrowsException() {
            assertThatThrownBy(() -> traversalSection.getDistanceAtTime(DURATION + 1)).isInstanceOf(TraversalCalculationException.class);
        }
    }

    @Nested
    @DisplayName("getSpeedAtDistance() method")
    class SpeedAtDistanceMethodTest {

        @Test
        @DisplayName("calculates correct speed")
        void getSpeedAtDistance_whenValidDistance_thenCalculatesTheRightSpeed() {
            assertThat(traversalSection.getSpeedAtDistance(DISTANCE / 2)).isEqualTo(SPEED);
        }

        @Test
        @DisplayName("calculates correct speed from zero distance")
        void getSpeedAtDistance_whenZeroDistance_thenCalculatesTheRightSpeed() {
            assertThat(traversalSection.getSpeedAtDistance(0)).isEqualTo(SPEED);
        }

        @Test
        @DisplayName("calculates correct speed from max distance")
        void getSpeedAtDistance_whenMaxDistance_thenCalculatesTheRightSpeed() {
            assertThat(traversalSection.getSpeedAtDistance(DISTANCE)).isEqualTo(SPEED);
        }

        @Test
        @DisplayName("throws expected error on negative distance")
        void getSpeedAtDistance_whenNegativeDistance_thenThrowsException() {
            assertThatThrownBy(() -> traversalSection.getSpeedAtDistance(-1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("throws expected error on too large distance")
        void getSpeedAtDistance_whenTooLargeDistance_thenThrowsException() {
            assertThatThrownBy(() -> traversalSection.getSpeedAtDistance(DISTANCE + 1)).isInstanceOf(TraversalCalculationException.class);
        }
    }

    @Nested
    @DisplayName("getSpeedAtTime() method")
    class SpeedAtTimeMethodTest {

        @Test
        @DisplayName("calculates correct speed")
        void getSpeedAtTime_whenValidTime_thenCalculatesTheRightSpeed() {
            assertThat(traversalSection.getSpeedAtTime(DURATION / 2)).isEqualTo(SPEED);
        }

        @Test
        @DisplayName("calculates correct speed from zero time")
        void getSpeedAtTime_whenZeroTime_thenCalculatesTheRightSpeed() {
            assertThat(traversalSection.getSpeedAtTime(0)).isEqualTo(SPEED);
        }

        @Test
        @DisplayName("calculates correct speed from max time")
        void getSpeedAtTime_whenMaxTime_thenCalculatesTheRightSpeed() {
            assertThat(traversalSection.getSpeedAtTime(DURATION)).isEqualTo(SPEED);
        }

        @Test
        @DisplayName("throws expected error on negative time")
        void getSpeedAtTime_whenNegativeTime_thenThrowsException() {
            assertThatThrownBy(() -> traversalSection.getSpeedAtTime(-1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("throws expected error on too large time")
        void getSpeedAtTime_whenTooLargeTime_thenThrowsException() {
            assertThatThrownBy(() -> traversalSection.getSpeedAtTime(DURATION + 1)).isInstanceOf(TraversalCalculationException.class);
        }
    }

    @Nested
    @DisplayName("getAccelerationAtDistance() method")
    class AccelerationAtDistanceMethodTest {

        @Test
        @DisplayName("calculates correct acceleration")
        void getAccelerationAtDistance_whenValidDistance_thenCalculatesTheRightAcceleration() {
            assertThat(traversalSection.getAccelerationAtDistance(DISTANCE / 2)).isEqualTo(0);
        }

        @Test
        @DisplayName("calculates correct acceleration from zero distance")
        void getAccelerationAtDistance_whenZeroDistance_thenCalculatesTheRightAcceleration() {
            assertThat(traversalSection.getAccelerationAtDistance(0)).isEqualTo(0);
        }

        @Test
        @DisplayName("calculates correct acceleration from max distance")
        void getAccelerationAtDistance_whenMaxDistance_thenCalculatesTheRightAcceleration() {
            assertThat(traversalSection.getAccelerationAtDistance(DISTANCE)).isEqualTo(0);
        }

        @Test
        @DisplayName("throws expected error on negative distance")
        void getAccelerationAtDistance_whenNegativeDistance_thenThrowsException() {
            assertThatThrownBy(() -> traversalSection.getAccelerationAtDistance(-1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("throws expected error on too large distance")
        void getAccelerationAtDistance_whenTooLargeDistance_thenThrowsException() {
            assertThatThrownBy(() -> traversalSection.getAccelerationAtDistance(DISTANCE + 1)).isInstanceOf(TraversalCalculationException.class);
        }
    }

    @Nested
    @DisplayName("getAccelerationAtTime() method")
    class AccelerationAtTimeMethodTest {

        @Test
        @DisplayName("calculates correct acceleration")
        void getAccelerationAtDistance_whenValidDistance_thenCalculatesTheRightAcceleration() {
            assertThat(traversalSection.getAccelerationAtTime(DURATION / 2)).isEqualTo(0);
        }

        @Test
        @DisplayName("calculates correct acceleration from zero time")
        void getAccelerationAtTime_whenZeroTime_thenCalculatesTheRightAcceleration() {
            assertThat(traversalSection.getAccelerationAtTime(0)).isEqualTo(0);
        }

        @Test
        @DisplayName("calculates correct acceleration from max time")
        void getAccelerationAtTime_whenMaxTime_thenCalculatesTheRightAcceleration() {
            assertThat(traversalSection.getAccelerationAtTime(DURATION)).isEqualTo(0);
        }

        @Test
        @DisplayName("throws expected error on negative distance")
        void getAccelerationAtDistance_whenNegativeDistance_thenThrowsException() {
            assertThatThrownBy(() -> traversalSection.getAccelerationAtTime(-1)).isInstanceOf(TraversalCalculationException.class);
        }

        @Test
        @DisplayName("throws expected error on too large distance")
        void getAccelerationAtDistance_whenTooLargeDistance_thenThrowsException() {
            assertThatThrownBy(() -> traversalSection.getAccelerationAtTime(DURATION + 1)).isInstanceOf(TraversalCalculationException.class);
        }
    }
}
