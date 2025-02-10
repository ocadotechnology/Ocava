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
package com.ocadotechnology.physics.units;

import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

import com.google.common.math.DoubleMath;

/**
 * Utility class for converting values of time between different units.
 */
public final class TimeUnitConverter {

    private TimeUnitConverter() {
        throw new UnsupportedOperationException("Should never instantiate TimeUnitConverter");
    }

    /**
     * Converts the given event time from one time unit to another as a {@code long}.
     *
     * @param eventTime The time in {@code currentUnit} units to convert.
     * @param currentUnit The unit of time {@code eventTime} is currently in.
     * @param targetUnit The unit of time {@code eventTime} is to be converted into.
     */
    public static long toTimeUnitLong(double eventTime, TimeUnit currentUnit, TimeUnit targetUnit) {
        long time;
        try {
            time = DoubleMath.roundToLong(eventTime * getTimeUnitsInSourceTimeUnit(currentUnit, targetUnit), RoundingMode.FLOOR);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Invalid timestamp: " + eventTime, e);
        }
        return time;
    }

    /**
     * Converts the given event time from one time unit to another as a {@code long}.
     *
     * @param eventTime The time in {@code currentUnit} units to convert.
     * @param currentUnit The unit of time {@code eventTime} is currently in.
     * @param targetUnit The unit of time {@code eventTime} is to be converted into.
     */
    public static long toTimeUnitLong(long eventTime, TimeUnit currentUnit, TimeUnit targetUnit) {
        long time;
        try {
            double ratio = getTimeUnitsInSourceTimeUnit(currentUnit, targetUnit);
            long longRatio = DoubleMath.roundToLong(ratio, RoundingMode.FLOOR);
            if (longRatio == ratio) {
                long convertedTime = eventTime * longRatio;
                if (convertedTime / longRatio != eventTime) {
                    throw new IllegalArgumentException("Invalid timestamp - overflows Long: " + eventTime);
                }
                time = convertedTime;
            } else {
                time = DoubleMath.roundToLong(eventTime * ratio, RoundingMode.FLOOR);
            }
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Invalid timestamp: " + eventTime, e);
        }
        return time;
    }

    /**
     * Converts the given event time from one time unit to another as a {@code double}.
     *
     * @param eventTime The time in {@code currentUnit} units to convert.
     * @param currentUnit The unit of time {@code eventTime} is currently in.
     * @param targetUnit The unit of time {@code eventTime} is to be converted into.
     */
    public static double toTimeUnitDouble(double eventTime, TimeUnit currentUnit, TimeUnit targetUnit) {
        return eventTime * getTimeUnitsInSourceTimeUnit(currentUnit, targetUnit);
    }

    /**
     * Converts the given event time from the given time units to simulation time units.
     *
     * @param eventTime The time value to be converted into simulation time units.
     * @param currentUnit The unit of time the eventTime is to be converted from.
     * @deprecated duplicate method - use {@link #toTimeUnitDouble} instead.
     */
    @Deprecated
    public static double fromTimeUnit(double eventTime, TimeUnit currentUnit, TimeUnit targetUnit) {
        return toTimeUnitDouble(eventTime, currentUnit, targetUnit);
    }

    /**
     * Create a ratio from the source time unit to the wanted time unit
     */
    private static double getTimeUnitsInSourceTimeUnit(TimeUnit sourceUnit, TimeUnit targetUnit) {
        return (double) sourceUnit.toNanos(1) / targetUnit.toNanos(1);
    }
}
