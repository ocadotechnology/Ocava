/*
 * Copyright © 2017 Ocado (Ocava)
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ocadotechnology.testing.UtilityClassTest;

public class JerkKinematicsTest implements UtilityClassTest{
    @Override
    public Class<?> getTestSubject() {
        return JerkKinematics.class;
    }

    @Test
    void getDisplacement_whenSuitableValuesGiven_thenGivesCorrectAnswer() {
        double displacement = JerkKinematics.getDisplacement(3.0, 2.5, 3, 12.0);
        Assertions.assertEquals(1080, displacement);
    }

    @Test
    void getTimeToReachDisplacement_whenSuitableValuesGiven_thenGivesCorrectAnswer() {
        double time = JerkKinematics.getTimeToReachDisplacement(20, 0, 3, 1);
        Assertions.assertEquals(3.14353, time, 1e-5);
    }

    @Test
    void getTimeToReachDisplacement_whenUnsuitableValuesGiven_thenThrowsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> JerkKinematics.getTimeToReachDisplacement(20, 1, 1, -5));
    }

    @Test
    void getFinalVelocity_whenSuitableValuesGiven_thenGivesCorrectAnswer() {
        double velocity = JerkKinematics.getFinalVelocity(5, 1, 7, 13);
        Assertions.assertEquals(609.5, velocity);
    }

    @Test
    void getTimeToReachVelocity_whenSuitableValuesGiven_thenGivesCorrectAnswer() {
        double time = JerkKinematics.getTimeToReachVelocity(0, 20, 3, 1);
        Assertions.assertEquals(4, time, 1e-5);
    }

    @Test
    void getTimeToReachVelocity_whenUnsuitableValuesGiven_thenThrowsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> JerkKinematics.getTimeToReachVelocity(0, 20, 1, -5));
    }
}
