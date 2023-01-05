/*
 * Copyright © 2017-2023 Ocado (Ocava)
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

import org.junit.jupiter.api.Test;

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
                    ConstantJerkTraversalSectionsFactoryTest.checkJoinedUpVelocities(message, traversal.getSections());
                }
            }
        }
    }

    @Test
    void create_whenRequiredDistanceIsZero_thenFindsBreakingDistance() {
        double d = 0d, a = 0d, u = 2d;
        String message = "distance=" + d + ", initial-acceleration=" + a + ", initial-speed=" + u;
        Traversal traversal = factory.create(d, u, a, vehicleMotionProperties);
        ConstantJerkTraversalSectionsFactoryTest.checkJoinedUp(message, traversal.getSections());
    }

    @Test
    void create_whenStartingWithNegativeDecelerationWithASubstantialDistanceToGo() {
        double d = 100d, a = -2d, u = 6d;
        String message = "distance=" + d + ", initial-acceleration=" + a + ", initial-speed=" + u;
        Traversal traversal = factory.create(d, u, a, vehicleMotionProperties);
        ConstantJerkTraversalSectionsFactoryTest.checkJoinedUp(message, traversal.getSections());
    }

    @Test
    void create_whenStartingWithNegativeDecelerationWithShortDistanceToGo() {
        double d = 0.5d, a = -3d, u = 0.5;
        String message = "distance=" + d + ", initial-acceleration=" + a + ", initial-speed=" + u;
        Traversal traversal = factory.create(d, u, a, vehicleMotionProperties);
        ConstantJerkTraversalSectionsFactoryTest.checkJoinedUpVelocities(message, traversal.getSections());
    }

    @Test
    void create_when() {
        double d = 26d, a = 0.5d, u = 8.5d;
        String message = "distance=" + d + ", initial-acceleration=" + a + ", initial-speed=" + u;
        Traversal traversal = factory.create(d, u, a, vehicleMotionProperties);
        ConstantJerkTraversalSectionsFactoryTest.checkJoinedUpVelocities(message, traversal.getSections());
    }
}
