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

import org.apache.commons.math3.complex.Complex;

import com.google.common.collect.ImmutableList;
import com.ocadotechnology.maths.CubicRootFinder;
import com.ocadotechnology.maths.PolynomialRootUtils;
import com.ocadotechnology.maths.QuadraticRootFinder;

/**
 * Utility class wrapping some equations of motion fo a constant jerk system.
 */
public final class JerkKinematics {
    private JerkKinematics() {
        throw new UnsupportedOperationException("Should not be instantiating a JerkKinematics");
    }

    /**
     * Calculate the displacement after a given time moving under the given jerk with the given initial velocity and
     * acceleration.
     *
     * s = ut + 1/2a.t^2 + 1/6jt^3
     */
    public static double getDisplacement(double initialVelocity, double initialAcceleration, double jerk, double time) {
        return initialVelocity * time + 1/2d * initialAcceleration * Math.pow(time, 2) + 1/6d * jerk * Math.pow(time, 3);
    }

    /**
     * Calculate the time to reach a given displacement moving under the given jerk with the given initial velocity and
     * acceleration.
     *
     * s = ut + 1/2a.t^2 + 1/6jt^3
     *
     * @throws IllegalArgumentException if the given values cannot reach that displacement
     */
    public static double getTimeToReachDisplacement(double displacement, double initialVelocity, double initialAcceleration, double jerk) {
        ImmutableList<Complex> roots = CubicRootFinder.find(jerk / 6, initialAcceleration / 2, initialVelocity, -displacement);
        return PolynomialRootUtils.getMinimumPositiveRealRoot(roots);
    }

    /**
     * Calculate the velocity after a given time moving under the given jerk with the given initial velocity and
     * acceleration.
     *
     * v = u + a.t + 1/2jt^2
     */
    public static double getFinalVelocity(double initialVelocity, double initialAcceleration, double jerk, double time) {
        return initialVelocity + initialAcceleration * time + (1/2d) * jerk * Math.pow(time, 2);
    }

    /**
     * Calculate the time to reach a given velocity moving under the given jerk with the given initial velocity and
     * acceleration.
     *
     * v = u + a.t + 1/2jt^2
     *
     * @throws IllegalArgumentException if the given values cannot reach that velocity
     */
    public static double getTimeToReachVelocity(double initialVelocity, double targetVelocity, double initialAcceleration, double jerk) {
        ImmutableList<Complex> roots = QuadraticRootFinder.find(jerk / 2d, initialAcceleration, initialVelocity - targetVelocity);
        return PolynomialRootUtils.getMinimumPositiveRealRoot(roots);
    }
}
