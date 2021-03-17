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

import javax.annotation.Nonnegative;

import com.google.common.math.DoubleMath;

/**
 * Implementation of TraversalSection for a section with constant, positive speed.
 */
public class ConstantSpeedTraversalSection implements TraversalSection, Serializable {
    private static final double ROUNDING_ERROR_FRACTION = 1E-9;
    private static final long serialVersionUID = 1L;

    private final double distance;
    private final double speed;
    private final double time;

    /**
     * @throws TraversalCalculationException in the following cases: non-positive distance, non-positive time, non-positive speed, incompatible inputs.
     */
    ConstantSpeedTraversalSection(double distance, double speed, double time) {
        if (distance < 0) {
            throw new TraversalCalculationException("Cannot have a ConstantSpeedTraversalSection with a non-positive distance (May mean it is not possible to decelerate to rest in the distance available) distance: " + distance);
        }
        if (time < 0) {
            throw new TraversalCalculationException("Cannot have a ConstantSpeedTraversalSection with a non-positive time duration");
        }
        if (speed <= 0) {
            throw new TraversalCalculationException("Cannot have a ConstantSpeedTraversalSection with a non-positive speed");
        }

        if (!DoubleMath.fuzzyEquals(distance / time, speed, speed * ROUNDING_ERROR_FRACTION)) {
            throw new TraversalCalculationException("Cannot have a ConstantSpeedTraversalSection with distance / time != speed");
        }

        this.distance = distance;
        this.time = time;
        this.speed = speed;
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
     * @throws TraversalCalculationException in the following cases: negative time, time after traversal section time.
     */
    @Override
    public double getDistanceAtTime(@Nonnegative double time) {
        if (time < 0) {
            throw new TraversalCalculationException("Time must be positive " + time);
        }
        if (time > this.time) {
            throw new TraversalCalculationException("Time " + time + " must not be greater than traversal section duration " + this.time);
        }
        return speed * time;
    }

    /**
     * @throws TraversalCalculationException in the following cases: negative distance, distance greater than traversal section distance.
     */
    @Override
    public double getTimeAtDistance(@Nonnegative double distance) {
        if (distance < 0) {
            throw new TraversalCalculationException("Distance must be positive " + distance);
        }
        if (distance > this.distance) {
            throw new TraversalCalculationException("Distance " + distance + " must not be greater than traversal section distance " + this.distance);
        }
        return distance / speed;
    }

    /**
     * @throws TraversalCalculationException in the following cases: negative time, time after traversal section time.
     */
    @Override
    public double getSpeedAtTime(@Nonnegative double time) {
        if (time < 0) {
            throw new TraversalCalculationException("Time must be positive " + time);
        }
        if (time > this.time) {
            throw new TraversalCalculationException("Time " + time + " must not be greater than traversal section duration " + this.time);
        }
        return speed;
    }

    @Override
    public double getAccelerationAtDistance(@Nonnegative double distance) {
        if (distance < 0) {
            throw new TraversalCalculationException("Distance must be positive " + distance);
        }
        if (distance > this.distance) {
            throw new TraversalCalculationException("Distance " + distance + " must not be greater than traversal section distance " + this.distance);
        }

        return 0;
    }

    @Override
    public double getAccelerationAtTime(@Nonnegative double time) {
        if (time < 0) {
            throw new TraversalCalculationException("Time must be positive " + time);
        }
        if (time > this.time) {
            throw new TraversalCalculationException("Time " + time + " must not be greater than traversal section duration " + this.time);
        }

        return 0;
    }

    @Override
    public boolean isConstantSpeed() {
        return true;
    }

    @Override
    public String toString() {
        return "ConstantSpeedTraversalSection(distance=" + distance + ", speed=" + speed + ", time=" + time + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstantSpeedTraversalSection that = (ConstantSpeedTraversalSection) o;
        return Double.compare(that.distance, distance) == 0 &&
                Double.compare(that.speed, speed) == 0 &&
                Double.compare(that.time, time) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(distance, speed, time);
    }
}
