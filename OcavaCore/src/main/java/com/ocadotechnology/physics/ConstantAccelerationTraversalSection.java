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

import java.io.Serializable;
import java.util.Objects;

import javax.annotation.Nonnegative;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.google.common.math.DoubleMath;

/**
 * Implementation of TraversalSection for a section with constant, non-zero acceleration.
 */
public class ConstantAccelerationTraversalSection implements TraversalSection, Serializable {
    private static final long serialVersionUID = 1L;
    private static final double ROUNDING_ERROR_FRACTION = 1E-9;

    private final double distance;
    private final double acceleration;
    private final double initialSpeed;
    private final double finalSpeed;
    private final double time;

    /**
     * @throws TraversalCalculationException in the following cases: negative distance, negative time, negative initialSpeed, negative finalSpeed, zero acceleration, incompatible inputs.
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "This object does not contain data that constitutes a security risk")
    ConstantAccelerationTraversalSection(double distance, double acceleration, double initialSpeed, double finalSpeed, double time) {
        if (distance < 0) {
            throw new TraversalCalculationException("Cannot have a ConstantAccelerationTraversalSection with a negative distance (May mean it is not possible to decelerate to rest in the distance available) distance: " + distance);
        }
        if (time < 0) {
            throw new TraversalCalculationException("Cannot have a ConstantAccelerationTraversalSection with a negative time duration");
        }
        if (initialSpeed < 0) {
            throw new TraversalCalculationException("Cannot have a ConstantAccelerationTraversalSection with a negative initialSpeed");
        }
        if (finalSpeed < 0) {
            throw new TraversalCalculationException("Cannot have a ConstantAccelerationTraversalSection with a negative finalSpeed");
        }
        if (acceleration == 0) {
            throw new TraversalCalculationException("Cannot have a ConstantAccelerationTraversalSection with a zero acceleration");
        }

        if (!DoubleMath.fuzzyEquals(initialSpeed + (time * acceleration), finalSpeed, Math.max(initialSpeed, finalSpeed) * ROUNDING_ERROR_FRACTION)) {
            throw new TraversalCalculationException("Cannot have a ConstantAccelerationTraversalSection with initialSpeed + acceleration * time != finalSpeed");
        }

        if (!DoubleMath.fuzzyEquals((initialSpeed * time) + (0.5 * acceleration * Math.pow(time, 2)), distance, distance * ROUNDING_ERROR_FRACTION)) {
            throw new TraversalCalculationException("Cannot have a ConstantAccelerationTraversalSection with initialSpeed * time + 0.5 * acceleration * time^2 != distance");
        }

        this.distance = distance;
        this.acceleration = acceleration;
        this.initialSpeed = initialSpeed;
        this.finalSpeed = finalSpeed;
        this.time = time;
    }

    @Override
    public double getDuration() {
        return time;
    }

    @Override
    public double getTotalDistance() {
        return distance;
    }

    /**
     * @throws TraversalCalculationException in the following cases: negative time, time beyond traversal section time.
     */
    @Override
    public double getDistanceAtTime(@Nonnegative double time) {
        if (time < 0) {
            throw new TraversalCalculationException("Time must be positive " + time);
        }
        if (time > this.time) {
            throw new TraversalCalculationException("Time " + time + " must not be greater than traversal section duration " + this.time);
        }
        return initialSpeed * time + 0.5 * acceleration * Math.pow(time, 2);
    }

    /**
     * @throws TraversalCalculationException in the following cases: negative distance, distance beyond traversal section distance.
     */
    @Override
    public double getTimeAtDistance(@Nonnegative double distance) {
        if (distance < 0) {
            throw new TraversalCalculationException("Distance must be positive " + distance);
        }
        if (distance > this.distance) {
            throw new TraversalCalculationException("Distance " + distance + " must not be greater than traversal section distance " + this.distance);
        }
        // d = u * t + (1/2) * a * t^2

        if (distance == 0) {
            return 0;
        }

        return (Math.sqrt(2 * acceleration * distance + Math.pow(initialSpeed, 2)) - initialSpeed) / acceleration;
    }

    /**
     * @throws TraversalCalculationException in the following cases: negative time, time beyond traversal section time.
     */
    @Override
    public double getSpeedAtTime(@Nonnegative double time) {
        if (time < 0) {
            throw new TraversalCalculationException("Time must be positive " + time);
        }
        if (time > this.time) {
            throw new TraversalCalculationException("Time " + time + " must not be greater than traversal section duration " + this.time);
        }

        // v = u + a * t
        return initialSpeed + acceleration * time;
    }

    @Override
    public double getAccelerationAtDistance(@Nonnegative double distance) {
        if (distance < 0) {
            throw new TraversalCalculationException("Distance must be positive " + distance);
        }
        if (distance > this.distance) {
            throw new TraversalCalculationException("Distance " + distance + " must not be greater than traversal section distance " + this.distance);
        }

        return acceleration;
    }

    @Override
    public double getAccelerationAtTime(@Nonnegative double time) {
        if (time < 0) {
            throw new TraversalCalculationException("Time must be positive " + time);
        }
        if (time > this.time) {
            throw new TraversalCalculationException("Time " + time + " must not be greater than traversal section duration " + this.time);
        }

        return acceleration;
    }

    @Override
    public boolean isAccelerating() {
        return acceleration > 0;
    }

    @Override
    public boolean isDecelerating() {
        return acceleration < 0;
    }

    @Override
    public String toString() {
        return "ConstantAccelerationTraversalSection(distance=" + distance + ", acceleration=" + acceleration + ", initialSpeed="
                + initialSpeed + ", finalSpeed=" + finalSpeed + ", time=" + time + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstantAccelerationTraversalSection that = (ConstantAccelerationTraversalSection) o;
        return isEquals(distance, that.distance)
                && isEquals(acceleration, that.acceleration)
                && isEquals(initialSpeed, that.initialSpeed)
                && isEquals(finalSpeed, that.finalSpeed)
                && isEquals(time, that.time);
    }

    private static boolean isEquals(double thisValue, double thatValue) {
        double thisAbs = Math.abs(thisValue);
        double thatAbs = Math.abs(thatValue);
        return DoubleMath.fuzzyEquals(thatValue, thisValue, Math.min(thisAbs, thatAbs) * ROUNDING_ERROR_FRACTION);
    }

    @Override
    public int hashCode() {
        return Objects.hash(distance, acceleration, initialSpeed, finalSpeed, time);
    }
}
