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
package com.ocadotechnology.physics.units;

/**
 * Representation of a unit of length for interconversion.  Based on {@code TimeUnit}
 */
public enum LengthUnit {
    NANOMETERS(1L),
    MICROMETERS(1000L),
    MILLIMETERS(1000_000L),
    CENTIMETERS(10_000_000L),
    METERS(1000_000_000L),
    KILOMETERS(1000_000_000_000L);

    private final long nanometers;

    LengthUnit(long nanometers) {
        this.nanometers = nanometers;
    }

    /**
     * Converts the given distance in the given unit to this unit.
     * Conversions from finer to coarser granularities truncate, so
     * lose precision. For example, converting {@code 999} millimeters
     * to meters results in {@code 0}. Conversions from coarser to
     * finer granularities with arguments that would numerically
     * overflow saturate to {@code Long.MIN_VALUE} if negative or
     * {@code Long.MAX_VALUE} if positive.
     *
     * <p>For example, to convert 10 meters to millimeters, use:
     * {@code LengthUnit.MILLIMETERS.convert(10L, LengthUnit.METERS)}
     *
     * @return the converted duration in this unit,
     * or {@code Long.MIN_VALUE} if conversion would negatively
     * overflow, or {@code Long.MAX_VALUE} if it would positively overflow.
     */
    public long convert(long sourceDistance, LengthUnit sourceUnit) {
        if (this == sourceUnit) {
            return sourceDistance;
        }
        if (this.nanometers < sourceUnit.nanometers) {
            long multiplier = sourceUnit.nanometers / this.nanometers;
            return scaleUp(sourceDistance, multiplier, Long.MAX_VALUE / multiplier);
        }
        return sourceDistance / (this.nanometers / sourceUnit.nanometers);
    }

    /**
     * Scale distance by multiplier, checking for overflow.
     */
    private static long scaleUp(long distance, long multiplier, long limit) {
        if (distance >  limit) return Long.MAX_VALUE;
        if (distance < -limit) return Long.MIN_VALUE;
        return distance * multiplier;
    }

    /**
     * @return the number of this unit in the supplied unit.  Eg MILLIMETERS.getUnitsIn(METERS)
     *         returns 1000.0
     */
    public double getUnitsIn(LengthUnit unit) {
        return unit.nanometers * 1.0 / this.nanometers;
    }
}
