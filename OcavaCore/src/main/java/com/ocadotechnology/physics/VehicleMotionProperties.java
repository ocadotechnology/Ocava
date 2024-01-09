/*
 * Copyright © 2017-2024 Ocado (Ocava)
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

import java.io.Serializable;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

/**
 * Wrapper class describing an objects physical motion properties.
 */
public class VehicleMotionProperties implements Serializable {

    /** maximum (inclusive) speed.  Must be strictly positive (and greater than any tolerance). */
    public final double maxSpeed;
    /** maximum (inclusive) acceleration.  Must be strictly positive (and greater than any tolerance). */
    public final double acceleration;
    /** NEGATIVE maximum (inclusive) deceleration.  Must be strictly negative (and less than any tolerance). */
    public final double deceleration;

    /**
     * If using a constant jerk model, jerkAccelerationUp is the initial positive jerk to accelerate the vehicle.<br>
     * Must be strictly positive.
     */
    public final double jerkAccelerationUp;
    /**
     * If using a constant jerk model, jerkAccelerationDown is the negative jerk to reduce
     * acceleration to zero for the constant (maximum) velocity phase of the motion.<br>
     * Must be strictly negative.
     */
    public final double jerkAccelerationDown;
    /**
     * If using a constant jerk model, jerkDecelerationUp is the negative jerk to increase
     * deceleration to maximum for the final deceleration phase of the motion.<br>
     * Must be strictly negative.
     */
    public final double jerkDecelerationUp;
    /**
     * If using a constant jerk model, jerkDecelerationDown is the positive jerk to decrease
     * deceleration to zero to bring the vehicle to a stop.<br>
     * Must be strictly positive.
     */
    public final double jerkDecelerationDown;

    /**
     * tolerance (same units as maxSpeed).  The bounds for maxSpeed is: maxSpeed +/- maxSpeedAbsoluteTolerance.
     * Value from zero (inclusive) to maxSpeed (exclusive) are valid.
     */
    public final double maxSpeedAbsoluteTolerance;
    /**
     * tolerance (same units as acceleration).  The bounds for acceleration is: acceleration +/- accelerationAbsoluteTolerance.
     * Value from zero (inclusive) to acceleration (exclusive) are valid.
     */
    public final double accelerationAbsoluteTolerance;
    /**
     * tolerance (same units as deceleration).  The bounds for deceleration is: deceleration -/+ decelerationAbsoluteTolerance.
     * Value from zero (inclusive) to deceleration (exclusive) are valid.<br>
     * ie, this value is NEGATIVE.
     */
    public final double decelerationAbsoluteTolerance;

    /**
     *
     * @param acceleration must be greater than zero and greater than the tolerance
     * @param accelerationAbsoluteTolerance must be non-negative and less than acceleration.<br>
     *        It is the same units as acceleration.<br>
     *        The acceleration bounds is, therefore, [acceleration - accelerationAbsoluteTolerance, acceleration + accelerationAbsoluteTolerance].
     * @param deceleration must be less than zero
     * @param decelerationAbsoluteTolerance must be smaller than the absolute value of deceleration (we always use the absolute value).<br>
     *        It is the same units as deceleration.<br>
     *        The deceleration bounds is, therefore, [deceleration - abs(decelerationAbsoluteTolerance), deceleration + abs(decelerationAbsoluteTolerance)].
     * @param maxSpeed must be greater than zero
     * @param maxSpeedAbsoluteTolerance must be non-negative and less than maxSpeed<br>
     *        It is the same units as maxSpeed<br>
     *        The maxSpeed bounds is, therefore, [maxSpeed - maxSpeedAbsoluteTolerance, maxSpeed + maxSpeedAbsoluteTolerance].
     * @param jerkAccelerationUp must be greater than zero
     * @param jerkAccelerationDown must be less than zero
     * @param jerkDecelerationUp must be less than zero
     * @param jerkDecelerationDown must be greater than zero
     */
    public VehicleMotionProperties(
            double acceleration,
            double accelerationAbsoluteTolerance,
            double deceleration,
            double decelerationAbsoluteTolerance,
            double maxSpeed,
            double maxSpeedAbsoluteTolerance,
            double jerkAccelerationUp,
            double jerkAccelerationDown,
            double jerkDecelerationUp,
            double jerkDecelerationDown) {

        Preconditions.checkArgument(acceleration > 0, "Acceleration should be greater than 0. Value: %s", acceleration);
        Preconditions.checkArgument(accelerationAbsoluteTolerance >= 0, "AccelerationAbsoluteTolerance should be greater or equal to 0. Value: %s", accelerationAbsoluteTolerance);
        Preconditions.checkArgument(acceleration > accelerationAbsoluteTolerance, "Acceleration %s should be greater than the acceleration tolerance %s", acceleration, accelerationAbsoluteTolerance);

        Preconditions.checkArgument(deceleration < 0, "Deceleration should be less than 0. Value: %s", deceleration);
        Preconditions.checkArgument(-deceleration > Math.abs(decelerationAbsoluteTolerance), "Deceleration %s should be a larger negative value than the deceleration tolerance %s", deceleration, decelerationAbsoluteTolerance);

        Preconditions.checkArgument(maxSpeed > 0, "Max Speed should be positive. Value: %s", maxSpeed);
        Preconditions.checkArgument(maxSpeedAbsoluteTolerance >= 0, "MaxSpeedAbsoluteTolerance should be greater or equal to 0. Value: %s", maxSpeedAbsoluteTolerance);
        Preconditions.checkArgument(maxSpeed > maxSpeedAbsoluteTolerance, "MaxSpeed %s should be greater than the maxSpeedAbsoluteTolerance %s", maxSpeed, maxSpeedAbsoluteTolerance);

        Preconditions.checkArgument(jerkAccelerationUp > 0, "Jerk Acceleration Up should be positive. Value: %s", jerkAccelerationUp);
        Preconditions.checkArgument(jerkAccelerationDown < 0, "Jerk Acceleration Down should be negative. Value: %s", jerkAccelerationDown);
        Preconditions.checkArgument(jerkDecelerationUp < 0, "Jerk Deceleration Up should be negative. Value: %s", jerkDecelerationUp);
        Preconditions.checkArgument(jerkDecelerationDown > 0, "Jerk Deceleration Down should be positive. Value: %s", jerkDecelerationDown);

        this.acceleration = acceleration;
        this.accelerationAbsoluteTolerance = accelerationAbsoluteTolerance;
        this.deceleration = deceleration;
        this.decelerationAbsoluteTolerance = -Math.abs(decelerationAbsoluteTolerance);
        this.maxSpeed = maxSpeed;
        this.maxSpeedAbsoluteTolerance = maxSpeedAbsoluteTolerance;
        this.jerkAccelerationUp = jerkAccelerationUp;
        this.jerkAccelerationDown = jerkAccelerationDown;
        this.jerkDecelerationUp = jerkDecelerationUp;
        this.jerkDecelerationDown = jerkDecelerationDown;
    }

    public VehicleMotionProperties(double maxSpeed, double acceleration, double jerk, double toleranceFraction) {
        this(acceleration,
                acceleration * toleranceFraction,
                -acceleration,
                -acceleration * toleranceFraction,
                maxSpeed,
                maxSpeed * toleranceFraction,
                jerk,
                -jerk,
                -jerk,
                jerk);
    }

    public VehicleMotionProperties(double maxSpeed, double acceleration, double deceleration, double speedAndAccelerationToleranceFraction,
            double jerkAccelUp, double jerkAccelDown, double jerkDecelUp, double jerkDecelDown) {
        this(acceleration, acceleration * speedAndAccelerationToleranceFraction,
                deceleration, deceleration * speedAndAccelerationToleranceFraction,
                maxSpeed, maxSpeed * speedAndAccelerationToleranceFraction,
                jerkAccelUp, jerkAccelDown, jerkDecelUp, jerkDecelDown);
    }

    public double getFastestMaxSpeed() {
        return maxSpeed + maxSpeedAbsoluteTolerance;
    }

    public double getFastestMaxSpeed(double minSpeed) {
        return Math.max(minSpeed, getFastestMaxSpeed());
    }

    public double getSlowestMaxSpeed() {
        return maxSpeed - maxSpeedAbsoluteTolerance;
    }

    public double getSlowestMaxSpeed(double minSpeed) {
        return Math.max(minSpeed, getSlowestMaxSpeed());
    }

    public double getFastestMaxAcceleration() {
        return acceleration + accelerationAbsoluteTolerance;
    }

    public double getFastestMaxAcceleration(double minAcceleration) {
        return Math.max(minAcceleration, getFastestMaxAcceleration());
    }

    public double getSlowestMaxAcceleration() {
        return acceleration - accelerationAbsoluteTolerance;
    }

    public double getSlowestMaxAcceleration(double minAcceleration) {
        return Math.max(minAcceleration, getSlowestMaxAcceleration());
    }

    /** Largest NEGATIVE value for maximum deceleration. */
    public double getFastestMaxDeceleration() {
        return deceleration + decelerationAbsoluteTolerance;
    }

    public double getFastestMaxDeceleration(double minDeceleration) {
        return Math.min(minDeceleration, getFastestMaxDeceleration());  // min as we want most negative
    }

    /** Smallest NEGATIVE value for maximum deceleration. */
    public double getSlowestMaxDeceleration() {
        return deceleration - decelerationAbsoluteTolerance;
    }

    public double getSlowestMaxDeceleration(double minDeceleration) {
        return Math.min(minDeceleration, getSlowestMaxDeceleration());  // min as we want most negative
    }

    public double getMaxSpeedToleranceFraction() {
        return maxSpeedAbsoluteTolerance / maxSpeed;
    }

    /** @return true if all tolerances are zero */
    public boolean isIntolerant() {
        return accelerationAbsoluteTolerance == 0 && decelerationAbsoluteTolerance == 0 && maxSpeedAbsoluteTolerance == 0;
    }

    /**
     * @param original required to ensure minimum tolerance.  It is only used as a bounds.
     * @return upper bound of this motion properties.
     * The new properties are a limit, therefore, have tolerances set to zero.<br>
     * Therefore, <code>this.fastest().fastest() == this.fastest()</code>.
     */
    public VehicleMotionProperties fastest(VehicleMotionProperties original, double absoluteMinimumTolerableFractionOfOriginal) {
        if (isIntolerant()) {
            return this;
        }
        return new VehicleMotionProperties(
                getFastestMaxAcceleration(original.acceleration * absoluteMinimumTolerableFractionOfOriginal), 0.0,
                getFastestMaxDeceleration(original.deceleration * absoluteMinimumTolerableFractionOfOriginal), 0.0,
                getFastestMaxSpeed(original.maxSpeed * absoluteMinimumTolerableFractionOfOriginal), 0.0,
                jerkAccelerationUp,
                jerkAccelerationDown,
                jerkDecelerationUp,
                jerkDecelerationDown);
    }

    /**
     * @return lower bound of this motion properties.
     * The new properties are a limit, therefore, have tolerances set to zero.<br>
     * Therefore, <code>this.slowest().slowest() == this.slowest()</code>.
     */
    public VehicleMotionProperties slowest(VehicleMotionProperties original, double absoluteMinimumTolerableFractionOfOriginal) {
        if (isIntolerant()) {
            return this;
        }
        return new VehicleMotionProperties(
                getSlowestMaxAcceleration(original.acceleration * absoluteMinimumTolerableFractionOfOriginal), 0.0,
                getSlowestMaxDeceleration(original.deceleration * absoluteMinimumTolerableFractionOfOriginal), 0.0,
                getSlowestMaxSpeed(original.maxSpeed * absoluteMinimumTolerableFractionOfOriginal), 0.0,
                jerkAccelerationUp,
                jerkAccelerationDown,
                jerkDecelerationUp,
                jerkDecelerationDown);
    }

    /**
     * @return scaled speed and accelerations of this motion properties.<br>
     * The new tolerances are ALSO scaled.
     */
    public VehicleMotionProperties scale(double speedMultiplier, double accelerationMultiplier) {
        Preconditions.checkArgument(speedMultiplier > 0, "speedMultipler must be positive: %s in %s", speedMultiplier, this);
        Preconditions.checkArgument(accelerationMultiplier > 0, "accelerationMultiplier must be positive: %s in %s", accelerationMultiplier, this);
        return new VehicleMotionProperties(
                acceleration * accelerationMultiplier,
                accelerationAbsoluteTolerance * accelerationMultiplier,
                deceleration * accelerationMultiplier,
                decelerationAbsoluteTolerance * accelerationMultiplier,
                maxSpeed * speedMultiplier,
                maxSpeedAbsoluteTolerance * speedMultiplier,
                jerkAccelerationUp,
                jerkAccelerationDown,
                jerkDecelerationUp,
                jerkDecelerationDown);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        VehicleMotionProperties that = (VehicleMotionProperties) o;

        return Double.compare(that.maxSpeed, maxSpeed) == 0
                && Double.compare(that.maxSpeedAbsoluteTolerance, maxSpeedAbsoluteTolerance) == 0
                && Double.compare(that.acceleration, acceleration) == 0
                && Double.compare(that.accelerationAbsoluteTolerance, accelerationAbsoluteTolerance) == 0
                && Double.compare(that.deceleration, deceleration) == 0
                && Double.compare(that.decelerationAbsoluteTolerance, decelerationAbsoluteTolerance) == 0
                && Double.compare(that.jerkAccelerationUp, jerkAccelerationUp) == 0
                && Double.compare(that.jerkAccelerationDown, jerkAccelerationDown) == 0
                && Double.compare(that.jerkDecelerationUp, jerkDecelerationUp) == 0
                && Double.compare(that.jerkDecelerationDown, jerkDecelerationDown) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(maxSpeed) + Double.hashCode(acceleration);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("maxSpeed", maxSpeed)
                .add("maxSpeedAbsoluteTolerance", maxSpeedAbsoluteTolerance)
                .add("acceleration", acceleration)
                .add("accelerationAbsoluteTolerance", accelerationAbsoluteTolerance)
                .add("deceleration", deceleration)
                .add("decelerationAbsoluteTolerance", decelerationAbsoluteTolerance)
                .add("jerkAccelerationUp", jerkAccelerationUp)
                .add("jerkAccelerationDown", jerkAccelerationDown)
                .add("jerkDecelerationUp", jerkDecelerationUp)
                .add("jerkDecelerationDown", jerkDecelerationDown)
                .toString();
    }
}