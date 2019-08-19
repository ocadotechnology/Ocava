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

import java.util.Optional;

class ConstantJerkSectionFactory {

    /**
     * @throws TraversalCalculationException
     */
    ConstantSpeedTraversalSection constantSpeed(double s, double v) {
        double t = s / v;

        return new ConstantSpeedTraversalSection(s, v, t);
    }

    /**
     * @throws TraversalCalculationException
     */
    ConstantAccelerationTraversalSection constantAcceleration(double u, double v, double a) {
        // v = u + at
        // v - u = at
        // (v - u)/a = t
        double t = (v - u) / a;

        double s = (u + v) / 2 * t;

        return new ConstantAccelerationTraversalSection(s, a, u, v, t);
    }

    /**
     * @throws TraversalCalculationException
     */
    ConstantJerkTraversalSection jerkAccelerationUp(double finalAcceleration, double j) {
        // a = a. + jt
        // a - a. = jt
        // (a - a.)/j = t
        // a. = 0
        double t = finalAcceleration / j;

        double v = 1 / 2d * j * Math.pow(t, 2);

        double s = JerkKinematics.getDisplacement(0, 0, j, t);

        return new ConstantJerkTraversalSection(t, s, 0, v, 0, finalAcceleration, j);
    }

    /**
     * @throws TraversalCalculationException
     */
    ConstantJerkTraversalSection jerkAccelerationUp(double u, double initialAcceleration, double finalAcceleration, double j) {
        // a = a. + jt
        // a - a. = jt
        // (a - a.)/j = t
        double t = (finalAcceleration - initialAcceleration) / j;

        double v = JerkKinematics.getFinalVelocity(u, initialAcceleration, j, t);

        double s = JerkKinematics.getDisplacement(u, initialAcceleration, j, t);

        return new ConstantJerkTraversalSection(t, s, u, v, initialAcceleration, finalAcceleration, j);
    }

    /**
     * @throws TraversalCalculationException
     */
    ConstantJerkTraversalSection jerkAccelerationUpFrom(double initialAcceleration, double u, double j, double timeToJerkFor) {
        double s = JerkKinematics.getDisplacement(u, initialAcceleration, j, timeToJerkFor);
        double v = u + initialAcceleration * timeToJerkFor + (1 / 2d) * j * Math.pow(timeToJerkFor, 2);
        double a = initialAcceleration + j * timeToJerkFor;

        return new ConstantJerkTraversalSection(timeToJerkFor, s, u, v, initialAcceleration, a, j);
    }

    /**
     * @throws TraversalCalculationException
     */
    ConstantJerkTraversalSection jerkAccelerationUpFromTo(double initialAcceleration, double u, double j, double finalAcceleration) {
        double t = (finalAcceleration - initialAcceleration) / j;
        double s = JerkKinematics.getDisplacement(u, initialAcceleration, j, t);
        double v = u + initialAcceleration * t + (1 / 2d) * j * Math.pow(t, 2);
        return new ConstantJerkTraversalSection(t, s, u, v, initialAcceleration, finalAcceleration, j);
    }

    /**
     * @throws TraversalCalculationException
     */
    ConstantJerkTraversalSection jerkAccelerationDown(double initialAcceleration, double u, double j) {
        // a = a. + jt
        // a - a. = jt
        // (a - a.)/j = t
        // a = 0
        double t = -initialAcceleration / j;

        double v = JerkKinematics.getFinalVelocity(u, initialAcceleration, j, t);

        double s = JerkKinematics.getDisplacement(u, initialAcceleration, j, t);

        return new ConstantJerkTraversalSection(t, s, u, v, initialAcceleration, 0, j);
    }

    /**
     * @throws TraversalCalculationException
     */
    ConstantJerkTraversalSection jerkAccelerationDownToV(double initialAcceleration, double v, double j) {
        // a = a. + jt
        // a - a. = jt
        // (a - a.)/j = t
        // a = 0
        double t = -initialAcceleration / j;

        double u = v - (initialAcceleration * t + 1 / 2d * j * Math.pow(t, 2));

        double s = JerkKinematics.getDisplacement(u, initialAcceleration, j, t);

        return new ConstantJerkTraversalSection(t, s, u, v, initialAcceleration, 0, j);
    }

    /**
     * @throws TraversalCalculationException
     */
    public ConstantJerkTraversalSection jerkAccelerationFromUToV(double initialAcceleration, double u, double v, double j) {
        double t = JerkKinematics.getTimeToReachVelocity(u, v, initialAcceleration, j);

        // a = a. + jt
        double a = initialAcceleration + j * t;

        double s = JerkKinematics.getDisplacement(u, initialAcceleration, j, t);

        return new ConstantJerkTraversalSection(t, s, u, v, initialAcceleration, a, j);
    }

    /**
     * @throws TraversalCalculationException
     */
    Optional<ConstantJerkTraversalSection> jerkDecelerationUp(double initialAcceleration, double u, double finalAcceleration, double j) {
        // a = a. + jt
        // a - a. = jt
        // (a - a.)/j = t
        double t = (finalAcceleration - initialAcceleration) / j;

        double v = u + initialAcceleration * t + 1 / 2d * j * Math.pow(t, 2);

        if (v < 0 || t < 0) {
            return Optional.empty();
        }

        double s = JerkKinematics.getDisplacement(u, initialAcceleration, j, t);

        return Optional.of(new ConstantJerkTraversalSection(t, s, u, v, initialAcceleration, finalAcceleration, j));
    }

    /**
     * @throws TraversalCalculationException
     */
    Optional<ConstantJerkTraversalSection> jerkDecelerationUpToV(double initialAcceleration, double v, double finalAcceleration, double j) {
        // a = a. + jt
        // a - a. = jt
        // (a - a.)/j = t
        double t = (finalAcceleration - initialAcceleration) / j;

        double u = v - (initialAcceleration * t + 1 / 2d * j * Math.pow(t, 2));

        if (v < 0) {
            return Optional.empty();
        }

        double s = JerkKinematics.getDisplacement(u, initialAcceleration, j, t);

        return Optional.of(new ConstantJerkTraversalSection(t, s, u, v, initialAcceleration, finalAcceleration, j));
    }

    /**
     * @throws TraversalCalculationException
     */
    ConstantJerkTraversalSection jerkDecelerationDown(double initialAcceleration, double j) {
        // a = a. + jt
        // a - a. = jt
        // (a - a.)/j = t
        // a = 0
        double t = -initialAcceleration / j;

        // v = u + a.t + 1/2jt^2
        // u = v - a.t - 1/2jt^2
        // v = 0
        double u = -initialAcceleration * t - 1 / 2d * j * Math.pow(t, 2);

        double s = JerkKinematics.getDisplacement(u, initialAcceleration, j, t);

        return new ConstantJerkTraversalSection(t, s, u, 0, initialAcceleration, 0, j);
    }

    /**
     * @throws TraversalCalculationException
     */
    ConstantJerkTraversalSection jerkDecelerationDownToZeroV(double u, double initialAcceleration, double j) {
        double t = JerkKinematics.getTimeToReachVelocity(u, 0, initialAcceleration, j);

        // a = a. + jt
        double a = initialAcceleration + j * t;

        double s = JerkKinematics.getDisplacement(u, initialAcceleration, j, t);

        return new ConstantJerkTraversalSection(t, s, u, 0, initialAcceleration, a, j);
    }

    /**
     * @throws TraversalCalculationException
     */
    public Optional<ConstantJerkTraversalSection> jerkAccelerationDownToAcceleration(double initialAcceleration, double finalAcceleration, double u, double j) {
        double t = (finalAcceleration - initialAcceleration) / j;
        double v = JerkKinematics.getFinalVelocity(u, initialAcceleration, j, t);
        if (v < 0) {
            return Optional.empty();
        }
        double s = JerkKinematics.getDisplacement(u, initialAcceleration, j, t);

        return Optional.of(new ConstantJerkTraversalSection(t, s, u, v, initialAcceleration, finalAcceleration, j));
    }

}
