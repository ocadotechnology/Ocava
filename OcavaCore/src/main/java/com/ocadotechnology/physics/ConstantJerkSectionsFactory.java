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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.math.DoubleMath;

/**
 * Helper class to generate parts of traversals which we can combine later to build an entire traversal. Because there are
 * many different possible profiles (more than 20), solving the equations analytically in one go is non-trivial.
 * Splitting the traversal into smaller parts and solving those reduces the number of cases quite a bit and is much
 * more manageable.
 */
public class ConstantJerkSectionsFactory {
    private static final double ROUNDING_ERROR_MARGIN = 1E-9;

    /**
     * Helper method that given an initial speed, v0 and acceleration, a0 find the traversal to reach a state where the
     * speed is vTarget and the acceleration is 0. This forms the first half of the overall traversal.
     * Throws an exception if vTarget is low enough that it is not possible to reach it without decelerating or if velocity
     * would go negative. This can intuitively be thought of as the "accelerating" portion of the traversal we are trying to build.
     *
     * <pre>
     *     "Normal" case where distance is large enough that max acceleration can be reached
     *      ^
     *  a   |   ________
     *      |  /        \
     *      | /          \
     *      |/____________\___
     *
     *     Unable to reach max acceleration because vehicle is constrained by its max acceleration or the distance is
     *     too short
     *      ^
     *  a   |
     *      |  /\
     *      | /  \
     *      |/____\___
     *
     *     Initial acceleration is negative but distance is long enough and initial speed low enough to allow for reaching
     *     max acceleration
     *      ^
     *  a   |       ________
     *      |      /        \
     *      |     /          \
     *      |____/____________\___
     *      |   /
     *      |  /
     *      | /
     *
     *      Initial acceleration is negative but vehicle cannot reach max acceleration due to being constrained by the
     *      distance or its max speed
     *      ^
     *  a   |
     *      |      /\
     *      |     /  \
     *      |____/____\___
     *      |   /
     *      |  /
     *      | /
     *
     *      Rare case when initial acceleration is negative and distance is very short
     *      ^
     *      |___________
     *  a   |   /
     *      |  /
     *      | /
     *
     *      Rare case when initial acceleration is positive and just touches max speed by jerking down as much as possible
     *      till acceleration is 0
     *      ^
     *  a   |
     *      |\
     *      | \
     *      |__\___
     *
     *      Case where the initial configuration results in the vehicle exceeding its max speed so some deceleration is
     *      required to bring the vehicle back within its constraints
     *      ^
     *  a   |
     *      |\
     *      | \
     *      |__\_______
     *      |   \    /
     *      |    \  /
     *      |     \/
     *
     *      Similar to case above
     *      ^
     *  a   |
     *      |\
     *      | \
     *      |__\___________
     *      |   \        /
     *      |    \      /
     *      |     \____/
     * </pre>
     */
    @VisibleForTesting
    static List<TraversalSection> buildAcceleratingPhaseOfTraversal(double initialAcc, double initialSpeed, double vTarget, VehicleMotionProperties vehicleProperties) {
        double currentSpeed = initialSpeed;
        double currentAcc = initialAcc;
        List<TraversalSection> sections = new ArrayList<>();
        double jerkDecelerationDown = vehicleProperties.jerkDecelerationDown;
        double jerkAccelerationDown = vehicleProperties.jerkAccelerationDown;
        if (currentAcc < 0) {
            // First check if braking is possible.
            // a_t = currentAcc + jt = 0
            // t = -currentAcc / j
            double timeToReachZeroAcc = -currentAcc / jerkDecelerationDown;
            double velocityAtZeroAcc = JerkKinematics.getFinalVelocity(currentSpeed, currentAcc, jerkDecelerationDown, timeToReachZeroAcc);
            if (velocityAtZeroAcc < 0) {
                // Should not reach this line due to guards earlier
                throw new TraversalCalculationException(String.format("Speed goes negative due to initial speed %s and initial acceleration %s with props: %s", initialSpeed, initialAcc, vehicleProperties));
            }
            double distanceTravelledAtZeroAcc = JerkKinematics.getDisplacement(currentSpeed, currentAcc, jerkDecelerationDown, timeToReachZeroAcc);
            ConstantJerkTraversalSection decelerateDownToZero = new ConstantJerkTraversalSection(timeToReachZeroAcc, distanceTravelledAtZeroAcc, currentSpeed, velocityAtZeroAcc, currentAcc, 0, jerkDecelerationDown);
            sections.add(decelerateDownToZero);
            currentAcc = 0;
            currentSpeed = velocityAtZeroAcc;
        }
        // Return early if we have already reached the target
        if (DoubleMath.fuzzyEquals(currentSpeed, vTarget, ROUNDING_ERROR_MARGIN * vehicleProperties.maxSpeed)
                && DoubleMath.fuzzyEquals(currentAcc, 0, ROUNDING_ERROR_MARGIN * vehicleProperties.acceleration)) {
            return sections;
        }

        // if we reduce acceleration directly to 0, will the speed exceed our target?
        // a_t = currentAcc + jt = 0
        // t = -currentAcc / j
        double timeToReachZeroAcc = -currentAcc / jerkAccelerationDown;
        double velocityAtZeroAcc = JerkKinematics.getFinalVelocity(currentSpeed, currentAcc, jerkAccelerationDown, timeToReachZeroAcc);
        int comparisonResult = DoubleMath.fuzzyCompare(velocityAtZeroAcc, vTarget, ROUNDING_ERROR_MARGIN * vehicleProperties.maxSpeed);
        double distanceTravelledAtZeroAcc = JerkKinematics.getDisplacement(currentSpeed, currentAcc, jerkAccelerationDown, timeToReachZeroAcc);
        if (comparisonResult >= 0 && !DoubleMath.fuzzyEquals(currentAcc, 0, ROUNDING_ERROR_MARGIN * vehicleProperties.acceleration)) {
            sections.add(new ConstantJerkTraversalSection(timeToReachZeroAcc, distanceTravelledAtZeroAcc, currentSpeed, velocityAtZeroAcc, currentAcc, 0, jerkAccelerationDown));
        }
        if (comparisonResult == 0) {
            return sections;
        } else if (comparisonResult == 1) {
            // This handles situations where we go above the max speed due to starting conditions, so we need to bring it down.
            sections.addAll(solveTriangleOrTrapezoidSections(velocityAtZeroAcc, 0, vTarget, vehicleProperties));
            return sections;
        }

        sections.addAll(solveTriangleOrTrapezoidSections(currentSpeed, currentAcc, vTarget, vehicleProperties));
        return sections;
    }

    /**
     * Helper method to calculate the second half of a traversal starting at an initial velocity and at 0 acceleration,
     * to reach rest. This can intuitively be thought of as the "decelerating" portion of the traversal we are trying to build.
     * <pre>
     *
     *     Case where vehicle can't reach max deceleration due to the initial speed being too low or the distance being too short
     *      ^
     *      |__________
     *  a   |\    /
     *      | \  /
     *      |  \/
     *
     *      Case where vehicle can reach max deceleration due to initial speed being high enough and/or distance long enough
     *      ^
     *      |___________
     *  a   |\        /
     *      | \      /
     *      |  \____/
     *      </pre>
     */
    @VisibleForTesting
    static List<TraversalSection> buildDeceleratingPhaseOfTraversal(double v0, VehicleMotionProperties vehicleProperties) {
        if (DoubleMath.fuzzyEquals(v0, 0, ROUNDING_ERROR_MARGIN * vehicleProperties.maxSpeed)) {
            return new ArrayList<>();
        }
        return solveTriangleOrTrapezoidSections(v0, 0, 0, vehicleProperties);
    }

    /**
     * Finds a triangular or trapezoid traversal given an initial v0 and a0 to reach a target speed (assumes target acceleration of 0)
     */
    static List<TraversalSection> solveTriangleOrTrapezoidSections(
            double v0,
            double a0,
            double vTarget,
            VehicleMotionProperties vehicleProperties) {
        if (a0 < 0 || (a0 == 0 && vTarget < v0)) {
            return solveTriangleOrTrapezoidSections(
                    v0,
                    a0,
                    dec -> Math.max(dec, vehicleProperties.deceleration),
                    vTarget,
                    vehicleProperties.jerkDecelerationUp,
                    vehicleProperties.jerkDecelerationDown,
                    vehicleProperties);
        } else {
            return solveTriangleOrTrapezoidSections(
                    v0,
                    a0,
                    acc -> Math.min(acc, vehicleProperties.acceleration),
                    vTarget,
                    vehicleProperties.jerkAccelerationUp,
                    vehicleProperties.jerkAccelerationDown,
                    vehicleProperties);
        }
    }

    private static List<TraversalSection> solveTriangleOrTrapezoidSections(
            double v0,
            double a0,
            Function<Double, Double> accelerationThresholder,
            double vTarget,
            double initialJerk,
            double finalJerk,
            VehicleMotionProperties vehicleProperties) {
        List<TraversalSection> sections = new ArrayList<>();
        // Comes froms solving for a1 from the following kinematic equations:
        // a1 = a0 + j1*t1
        // a2 = a1 + j2*t2 = 0
        // v1 = v0 + (a0*t2) + (0.5 * j1 * t1^2)
        // v2 = v1 + (a1*t2) + (0.5 * j2 * t2^2) = vTarget
        double numerator = vTarget - v0 + (Math.pow(a0, 2) / (2 * initialJerk));
        double denominator = (1.0 / (2 * initialJerk)) - (1 / (2 * finalJerk));
        double aPeak = Math.sqrt(numerator / denominator);
        // Flip the sign to negative if the positive solution results in negative time.
        int sign = (aPeak - a0) / initialJerk < 0 ? -1 : 1;
        aPeak *= sign;

        // Cap the acceleration to the min / max value.
        aPeak = accelerationThresholder.apply(aPeak);

        double timeToReachPeakAcc = (aPeak - a0) / initialJerk;
        double distanceTravelledToReachMaxAcc = JerkKinematics.getDisplacement(v0, a0, initialJerk, timeToReachPeakAcc);
        double vAtPeakAcc = JerkKinematics.getFinalVelocity(v0, a0, initialJerk, timeToReachPeakAcc);
        if (!DoubleMath.fuzzyEquals(a0, aPeak, ROUNDING_ERROR_MARGIN * vehicleProperties.acceleration)) {
            sections.add(new ConstantJerkTraversalSection(timeToReachPeakAcc, distanceTravelledToReachMaxAcc, v0, vAtPeakAcc, a0, aPeak, initialJerk));
        }

        double timeToReachZeroAcc = -aPeak / finalJerk;
        double vDelta = vTarget - JerkKinematics.getFinalVelocity(vAtPeakAcc, aPeak, finalJerk, timeToReachZeroAcc);
        if (DoubleMath.fuzzyCompare(vDelta, 0, ROUNDING_ERROR_MARGIN * vAtPeakAcc) != 0) {
            // Need to add a constant acc / dec section
            double timeAtPeakAcceleration = vDelta / aPeak;
            double distanceToCruise = JerkKinematics.getDisplacement(vAtPeakAcc, aPeak, 0, timeAtPeakAcceleration);
            sections.add(new ConstantAccelerationTraversalSection(distanceToCruise, aPeak, vAtPeakAcc, vAtPeakAcc + vDelta, timeAtPeakAcceleration));
            vAtPeakAcc = vAtPeakAcc + vDelta;
        }
        double distanceTravelledToReachZeroAcc = JerkKinematics.getDisplacement(vAtPeakAcc, aPeak, finalJerk, timeToReachZeroAcc);
        sections.add(new ConstantJerkTraversalSection(timeToReachZeroAcc, distanceTravelledToReachZeroAcc, vAtPeakAcc, vTarget, aPeak, 0, finalJerk));
        return sections;
    }
}
