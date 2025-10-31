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

import java.util.Objects;

import javax.annotation.Nonnegative;

import org.apache.commons.math3.complex.Complex;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;
import com.ocadotechnology.maths.CubicRootFinder;
import com.ocadotechnology.maths.PolynomialRootUtils;

/**
 * Implementation of TraversalSection for a section with constant, non-zero jerk (rate of change of acceleration).
 */
public class ConstantJerkTraversalSection implements TraversalSection {
    private static final double ROUNDING_ERROR_FRACTION = 1E-9;
    private static final double ABS_EPSILON = 1E-12;
    final double duration;
    final double distance;
    final double initialSpeed;
    final double finalSpeed;
    final double initialAcceleration;
    final double finalAcceleration;
    final double jerk; //rate of change of acceleration

    /**
     * @throws TraversalCalculationException in the following cases: negative duration, negative distance, negative initialSpeed, negative finalSpeed, zero jerk.
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "This object does not contain data that constitutes a security risk")
    ConstantJerkTraversalSection(
            double duration,
            double distance,
            double initialSpeed,
            double finalSpeed,
            double initialAcceleration,
            double finalAcceleration,
            double jerk) {

        // --- Non-negativity checks (fuzzy) ---
        if (DoubleMath.fuzzyCompare(duration, 0.0, getTolerance(duration)) < 0) {
            throw new TraversalCalculationException(
                    "Cannot have a ConstantJerkTraversalSection with a negative duration: " + duration);
        }
        if (DoubleMath.fuzzyCompare(distance, 0.0, getTolerance(distance)) < 0) {
            throw new TraversalCalculationException(
                    "Cannot have a ConstantJerkTraversalSection with a negative distance: " + distance);
        }
        if (DoubleMath.fuzzyCompare(initialSpeed, 0.0, getTolerance(initialSpeed)) < 0) {
            throw new TraversalCalculationException(
                    "Cannot have a ConstantJerkTraversalSection with a negative initialSpeed: " + initialSpeed);
        }
        if (DoubleMath.fuzzyCompare(finalSpeed, 0.0, getTolerance(finalSpeed)) < 0) {
            throw new TraversalCalculationException(
                    "Cannot have a ConstantJerkTraversalSection with a negative finalSpeed: " + finalSpeed);
        }
        // Jerk must be nonzero (fuzzy)
        if (DoubleMath.fuzzyEquals(jerk, 0.0, getTolerance(jerk))) {
            throw new TraversalCalculationException("Cannot have a ConstantJerkTraversalSection with zero jerk");
        }

        if (!DoubleMath.fuzzyEquals(initialAcceleration + (duration * jerk), finalAcceleration, Math.max(Math.abs(initialAcceleration), Math.abs(finalAcceleration)) * ROUNDING_ERROR_FRACTION)) {
            throw new TraversalCalculationException("Cannot have a ConstantJerkTraversalSection with initialAcceleration + jerk * time != finalAcceleration");
        }

        if (!DoubleMath.fuzzyEquals(initialSpeed + (initialAcceleration * duration) + (0.5 * jerk * Math.pow(duration, 2)), finalSpeed, getTolerance(Math.max(initialSpeed, finalSpeed)))) {
            throw new TraversalCalculationException("Cannot have a ConstantJerkTraversalSection with initialSpeed + initialAcceleration * time + 0.5 * jerk * time^2 != finalSpeed");
        }

        if (!DoubleMath.fuzzyEquals(initialSpeed * duration + (0.5 * initialAcceleration * Math.pow(duration, 2)) + (1/6d * jerk * Math.pow(duration, 3)), distance, getTolerance(distance))) {
            throw new TraversalCalculationException("Cannot have a ConstantJerkTraversalSection with initialSpeed * duration + 0.5 * initialAcceleration * duration^2 + 1/6 * jerk * duration^3 != distance");
        }

        this.duration = duration;
        this.distance = distance;
        this.initialSpeed = initialSpeed;
        this.finalSpeed = finalSpeed;
        this.initialAcceleration = initialAcceleration;
        this.finalAcceleration = finalAcceleration;
        this.jerk = jerk;
    }

    private static double getTolerance(double magnitude) {
        return Math.max(ABS_EPSILON, Math.abs(magnitude) * ROUNDING_ERROR_FRACTION);
    }

    @Override
    public double getDuration() {
        return duration;
    }

    @Override
    public double getTotalDistance() {
        return distance;
    }

    /**
     * @throws TraversalCalculationException in the following cases: distance greater than traversal section distance,
     *          distance is negative.
     */
    @Override
    public double getTimeAtDistance(@Nonnegative double distance) {
        if (DoubleMath.fuzzyCompare(distance, this.distance, this.distance * ROUNDING_ERROR_FRACTION) == 1) {
            throw new TraversalCalculationException("Distance " + distance + " must not be greater than traversal section distance " + this.distance);
        }

        if (DoubleMath.fuzzyCompare(distance, 0, this.distance * ROUNDING_ERROR_FRACTION) == -1) {
            throw new TraversalCalculationException("Distance must be non-negative");
        }

        if (DoubleMath.fuzzyEquals(distance, this.distance, this.distance * ROUNDING_ERROR_FRACTION)) {
            return duration;
        }

        if (DoubleMath.fuzzyEquals(distance, 0, this.distance * ROUNDING_ERROR_FRACTION))  {
            return 0;
        }

        //s = u*t + 1/2*a.*t^2 + 1/6*j*t^3
        ImmutableList<Complex> roots = CubicRootFinder.find((1/6d)*jerk, (1/2d)*initialAcceleration, initialSpeed, -distance);
        double minimumPositiveTime = PolynomialRootUtils.getMinimumPositiveRealRoot(roots);
        if (DoubleMath.fuzzyCompare(minimumPositiveTime, duration, duration * ROUNDING_ERROR_FRACTION) <= 0) {
            return minimumPositiveTime;
        }
        //rounding error detection
        boolean rootsContainZero = roots.stream()
                .anyMatch(root -> DoubleMath.fuzzyEquals(root.abs(), 0, duration * ROUNDING_ERROR_FRACTION));
        if (rootsContainZero) {
            return 0;
        }
        throw new TraversalCalculationException("No solution found for time to reach distance " + distance + " in " + this);
    }

    /**
     * @throws TraversalCalculationException in the following cases: time after traversal section duration, time is
     *          negative.
     */
    @Override
    public double getDistanceAtTime(@Nonnegative double time) {
        if (DoubleMath.fuzzyCompare(time, this.duration, this.duration * ROUNDING_ERROR_FRACTION) == 1) {
            throw new TraversalCalculationException("Time " + time + " must not be greater than traversal section Time " + this.duration);
        }

        if (DoubleMath.fuzzyCompare(time, 0, this.duration * ROUNDING_ERROR_FRACTION) == -1) {
            throw new TraversalCalculationException("Time must be non-negative");
        }

        if (DoubleMath.fuzzyEquals(time, this.duration, this.duration * ROUNDING_ERROR_FRACTION)) {
            return distance;
        }

        if (DoubleMath.fuzzyEquals(time, 0, this.duration * ROUNDING_ERROR_FRACTION))  {
            return 0;
        }

        //s = u*t + 1/2*a.*t^2 + 1/6*j*t^3
        return initialSpeed * time + 1/2d * initialAcceleration * Math.pow(time, 2) + 1/6d * jerk * Math.pow(time, 3);
    }

    /**
     * @throws TraversalCalculationException in the following cases: time after traversal section duration, time is
     *          negative.
     */
    @Override
    public double getSpeedAtTime(@Nonnegative double time) {
        if (DoubleMath.fuzzyCompare(time, this.duration, this.duration * ROUNDING_ERROR_FRACTION) == 1) {
            throw new TraversalCalculationException("Time " + time + " must not be greater than traversal section duration " + this.duration);
        }

        if (DoubleMath.fuzzyCompare(time, 0, this.duration * ROUNDING_ERROR_FRACTION) == -1) {
            throw new TraversalCalculationException("Time must be non-negative");
        }

        if (DoubleMath.fuzzyEquals(time, this.duration, this.duration * ROUNDING_ERROR_FRACTION)) {
            return finalSpeed;
        }

        if (DoubleMath.fuzzyEquals(time, 0, this.duration * ROUNDING_ERROR_FRACTION))  {
            return initialSpeed;
        }

        return initialSpeed + initialAcceleration * time + 1/2d * jerk * Math.pow(time, 2);
    }

    /**
     * @throws TraversalCalculationException in the following cases: time after traversal section duration, time is
     *          negative.
     */
    @Override
    public double getAccelerationAtTime(@Nonnegative double time) {
        if (DoubleMath.fuzzyCompare(time, this.duration, this.duration * ROUNDING_ERROR_FRACTION) == 1) {
            throw new TraversalCalculationException("Time " + time + " must not be greater than traversal section duration " + this.duration);
        }

        if (DoubleMath.fuzzyCompare(time, 0, this.duration * ROUNDING_ERROR_FRACTION) == -1) {
            throw new TraversalCalculationException("Time must be non-negative");
        }

        if (DoubleMath.fuzzyEquals(time, this.duration, this.duration * ROUNDING_ERROR_FRACTION)) {
            return finalAcceleration;
        }

        if (DoubleMath.fuzzyEquals(time, 0, this.duration * ROUNDING_ERROR_FRACTION))  {
            return initialAcceleration;
        }

        // a = a. + jt
        return initialAcceleration + jerk * time;
    }

    @Override
    public boolean isConstantAcceleration() {
        return false;
    }

    @Override
    public boolean isAccelerating() {
        return initialAcceleration > 0 || finalAcceleration > 0;
    }

    @Override
    public boolean isDecelerating() {
        return initialAcceleration < 0 || finalAcceleration < 0;
    }

    @Override
    public String toString() {
        return String.format("ConstantJerkTraversalSection(t=%.3g, s=%.3g, u=%.3g, v=%.3g, a.=%.3g, a=%.3g, j=%.3g)",
                duration, distance, initialSpeed, finalSpeed, initialAcceleration, finalAcceleration, jerk);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstantJerkTraversalSection that = (ConstantJerkTraversalSection) o;
        return isEquals(distance, that.distance) &&
                isEquals(jerk, that.jerk) &&
                isEquals(initialAcceleration, that.initialAcceleration) &&
                isEquals(finalAcceleration, that.finalAcceleration) &&
                isEquals(initialSpeed, that.initialSpeed) &&
                isEquals(finalSpeed, that.finalSpeed) &&
                isEquals(duration, that.duration);
    }

    private static boolean isEquals(double thisValue, double thatValue) {
        double thisAbs = Math.abs(thisValue);
        double thatAbs = Math.abs(thatValue);
        return DoubleMath.fuzzyEquals(thatValue, thisValue, Math.min(thisAbs, thatAbs) * ROUNDING_ERROR_FRACTION);
    }

    @Override
    public int hashCode() {
        return Objects.hash(duration, distance, initialSpeed, finalSpeed, initialAcceleration, finalAcceleration, jerk);
    }
}
