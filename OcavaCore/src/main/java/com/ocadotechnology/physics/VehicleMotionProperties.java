/*
 * Copyright © 2017-2021 Ocado (Ocava)
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
import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

/**
 * Wrapper class describing an objects physical motion properties.
 */
public class VehicleMotionProperties implements Serializable {

    public final double maxSpeed;
    public final double acceleration;
    public final double deceleration;
    public final double jerkAccelerationUp;
    public final double jerkAccelerationDown;
    public final double jerkDecelerationUp;
    public final double jerkDecelerationDown;

    /**
     *
     * @param acceleration must be greater than zero
     * @param deceleration must be less than zero
     * @param maxSpeed must be greater than zero
     * @param jerkAccelerationUp must be greater than zero
     * @param jerkAccelerationDown must be less than zero
     * @param jerkDecelerationUp must be less than zero
     * @param jerkDecelerationDown must be greater than zero
     */
    public VehicleMotionProperties(
            double acceleration,
            double deceleration,
            double maxSpeed,
            double jerkAccelerationUp,
            double jerkAccelerationDown,
            double jerkDecelerationUp,
            double jerkDecelerationDown) {

        Preconditions.checkArgument(acceleration > 0, "Acceleration should be greater than 0. Value: %s", acceleration);
        Preconditions.checkArgument(deceleration < 0, "Deceleration should be less than 0. Value: %s", deceleration);
        Preconditions.checkArgument(maxSpeed > 0, "Max Speed should be positive. Value: %s", maxSpeed);
        Preconditions.checkArgument(jerkAccelerationUp > 0, "Jerk Acceleration Up should be positive. Value: %s", jerkAccelerationUp);
        Preconditions.checkArgument(jerkAccelerationDown < 0, "Jerk Acceleration Down should be negative. Value: %s", jerkAccelerationDown);
        Preconditions.checkArgument(jerkDecelerationUp < 0, "Jerk Deceleration Up should be negative. Value: %s", jerkDecelerationUp);
        Preconditions.checkArgument(jerkDecelerationDown > 0, "Jerk Deceleration Down should be positive. Value: %s", jerkDecelerationDown);

        this.acceleration = acceleration;
        this.deceleration = deceleration;
        this.maxSpeed = maxSpeed;
        this.jerkAccelerationUp = jerkAccelerationUp;
        this.jerkAccelerationDown = jerkAccelerationDown;
        this.jerkDecelerationUp = jerkDecelerationUp;
        this.jerkDecelerationDown = jerkDecelerationDown;
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
                && Double.compare(that.acceleration, acceleration) == 0
                && Double.compare(that.deceleration, deceleration) == 0
                && Double.compare(that.jerkAccelerationUp, jerkAccelerationUp) == 0
                && Double.compare(that.jerkAccelerationDown, jerkAccelerationDown) == 0
                && Double.compare(that.jerkDecelerationUp, jerkDecelerationUp) == 0
                && Double.compare(that.jerkDecelerationDown, jerkDecelerationDown) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxSpeed, acceleration);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("maxSpeed", maxSpeed)
                .add("acceleration", acceleration)
                .add("deceleration", deceleration)
                .add("jerkAccelerationUp", jerkAccelerationUp)
                .add("jerkAccelerationDown", jerkAccelerationDown)
                .add("jerkDecelerationUp", jerkDecelerationUp)
                .add("jerkDecelerationDown", jerkDecelerationDown)
                .toString();
    }
}