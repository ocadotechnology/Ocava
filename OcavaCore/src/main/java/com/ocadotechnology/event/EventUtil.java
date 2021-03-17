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
package com.ocadotechnology.event;

import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.math.DoubleMath;

/**
 * Utilities for converting time values to readable strings. Supports timestamps corresponding to dates no earlier than
 * 0001-01-01 00:00:00.000.
 *
 * Currently asserts that the time unit for all schedulers is 1ms.
 */
public class EventUtil {
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter isoDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final DateTimeFormatter dateTimeWithoutMsFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final long MIN_TIMESTAMP = -62135596800000L;  // 0001-01-01 00:00:00.000

    private static long millisecondsInSimulationTimeUnitLong = 1000; // Default to assuming the simulation runs in seconds
    private static double millisecondsInSimulationTimeUnitDouble = Double.NaN; // Only required for sub ms time units

    /**
     * Sets the simulation time unit to allow for conversion to a standard display format
     *
     * @param simulationTimeUnit the timeUnit that inputs to this class will be using
     */
    public static void setSimulationTimeUnit(TimeUnit simulationTimeUnit) {
        millisecondsInSimulationTimeUnitLong = TimeUnit.MILLISECONDS.convert(1, simulationTimeUnit);
        if (millisecondsInSimulationTimeUnitLong == 0) { // simulationTimeUnit < MILLISECONDS
            millisecondsInSimulationTimeUnitDouble = 1.0 / simulationTimeUnit.convert(1, TimeUnit.MILLISECONDS);
        }
    }

    private static String eventTimeToFormat(double eventTime, DateTimeFormatter dateTimeFormatter) {
        return Double.isFinite(eventTime)
                ? milliEventTimeToFormat(convertToMillis(eventTime), dateTimeFormatter)
                : Double.toString(eventTime);
    }

    private static String eventTimeToFormat(long eventTime, DateTimeFormatter dateTimeFormatter) {
        return milliEventTimeToFormat(convertToMillis(eventTime), dateTimeFormatter);
    }

    private static long convertToMillis(double eventTime) {
        try {
            if (millisecondsInSimulationTimeUnitLong != 0) {
                return DoubleMath.roundToLong(eventTime * millisecondsInSimulationTimeUnitLong, RoundingMode.FLOOR);
            } else {
                return DoubleMath.roundToLong(eventTime * millisecondsInSimulationTimeUnitDouble, RoundingMode.FLOOR);
            }
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Invalid timestamp: " + eventTime, e);
        }
    }

    private static long convertToMillis(long eventTime) {
        try {
            if (millisecondsInSimulationTimeUnitLong != 0) {
                long milliTime = eventTime * millisecondsInSimulationTimeUnitLong;
                if (milliTime / millisecondsInSimulationTimeUnitLong != eventTime) {
                    throw new IllegalArgumentException("Invalid timestamp - overflows Long: " + eventTime);
                }
                return milliTime;
            } else {
                return DoubleMath.roundToLong(eventTime * millisecondsInSimulationTimeUnitDouble, RoundingMode.FLOOR);
            }
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Invalid timestamp: " + eventTime, e);
        }
    }

    private static String milliEventTimeToFormat(long eventTimeInMillis, DateTimeFormatter dateTimeFormatter) {
        Preconditions.checkArgument(eventTimeInMillis >= MIN_TIMESTAMP, "Timestamp is before 0001-01-01 00:00:00.000: %s", eventTimeInMillis);
        return dateTimeFormatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(eventTimeInMillis), UTC));
    }

    /**
     * Returns a string representation of the given timestamp, if finite. If the corresponding year has fewer than 4
     * digits, it will be padded by zeros (e.g., 0034), and if it has 5 or more digits it will be prefixed with '+'
     * (e.g., +10000). For Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY or Double.NAN, the string representation
     * of those values will be returned.
     *
     * @param eventTime The timestamp to convert.
     *
     * @throws IllegalArgumentException If the timestamp corresponds to a time prior to 0001-01-01 00:00:00.000, or
     * later than 292278994-08-17 07:12:54.784. Note that, due to rounding calculations, the maximum supported timestamp
     * is slightly smaller than for the equivalent methods which accept a long or Long value.
     */
    public static String eventTimeToString(double eventTime) {
        return eventTimeToFormat(eventTime, dateTimeFormatter);
    }

    /**
     * Returns a string representation of the given timestamp, if finite. If the corresponding year has fewer than 4
     * digits, it will be padded by zeros (e.g., 0034), and if it has 5 or more digits it will be prefixed with '+'
     * (e.g., +10000). For values corresponding to Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NAN, or
     * null, the string representation of those values will be returned.
     *
     * @param eventTime The timestamp to convert.
     *
     * @throws IllegalArgumentException If the timestamp corresponds to a time prior to 0001-01-01 00:00:00.000, or
     * later than 292278994-08-17 07:12:54.784. Note that, due to rounding calculations, the maximum supported timestamp
     * is slightly smaller than for the equivalent methods which accept a long or Long value.
     */
    public static String eventTimeToString(@Nullable Double eventTime) {
        return eventTime == null
                ? "null"
                : eventTimeToString(eventTime.doubleValue());
    }

    /**
     * Returns a string representation of the given timestamp. If the corresponding year has fewer than 4 digits, it
     * will be padded by zeros (e.g., 0034), and if it has 5 or more digits it will be prefixed with '+' (e.g., +10000).
     *
     * @param eventTime The timestamp to convert.
     *
     * @throws IllegalArgumentException If the timestamp corresponds to a time prior to 0001-01-01 00:00:00.000 or
     *          greater than Long.MAX_VALUE milliseconds.
     */
    public static String eventTimeToString(long eventTime) {
        return eventTimeToFormat(eventTime, dateTimeFormatter);
    }

    /**
     * Returns a string representation of the given timestamp, or "null" if the timestamp is null. If the corresponding
     * year has fewer than 4 digits, it will be padded by zeros (e.g., 0034), and if it has 5 or more digits it will be
     * prefixed with '+' (e.g., +10000).
     *
     * @param eventTime The timestamp to convert.
     *
     * @throws IllegalArgumentException If the timestamp corresponds to a time prior to 0001-01-01 00:00:00.000 or
     *          greater than Long.MAX_VALUE milliseconds.
     */
    public static String eventTimeToString(@Nullable Long eventTime) {
        return eventTime == null
                ? "null"
                : eventTimeToString(eventTime.longValue());
    }

    public static String eventTimeToIsoString(double eventTime) {
        return eventTimeToFormat(eventTime, isoDateTimeFormatter);
    }

    public static String eventTimeToStringWithoutMs(double eventTime) {
        return eventTimeToFormat(eventTime, dateTimeWithoutMsFormatter);
    }

    /**
     * Generates a computation for formatting a timestamp.
     * <br><br>
     * Many loggers defer the actual log-line formatting.  For example:<br>
     * <code>
     *    logger.info("Scheduling missiles for {}", EventUtil.logTime(now + durationToSafeDistance));
     * </code><br>
     * Here, <code>EventUtil.logTime</code> is always called regardless of the actual log level.
     * <br>
     * But, if the log level is higher than info, the log line will never be formatted and <code>toString</code>
     * will never be called, which depending on how many millions of log lines are generated may save significant computation.
     * <br>
     * Further, many logging libraries off-load the log line formatting to separate threads, so the
     * original thread may never incur the <code>toString</code> penalty.
     * In thise case, using <code>logTime</code> instead of <code>eventTimeToString</code> is
     * at least two orders of magnitude faster (for the calling thread).
     *
     * @param eventTime The timestamp to convert
     * @return an Object where <code>toString</code> returns the same as <code>EventUtil.eventTimeToString</code>
     */
    public static Object logTime(double eventTime) {
        return new Object() {
            public String toString() {
                return eventTimeToString(eventTime);
            }
        };
    }

    /**
     * Generates a computation for formatting a (possibly null) timestamp.
     * <br><br>
     * This may provide faster logging; see {@link #logTime(double)}
     *
     * @param eventTime The timestamp to convert
     * @return an Object where <code>toString</code> returns the same as <code>EventUtil.eventTimeToString</code>
     */
    public static Object logTime(@Nullable Double eventTime) {
        return new Object() {
            public String toString() {
                return eventTimeToString(eventTime);
            }
        };
    }

    /**
     * Generates a computation for formatting a timestamp.
     * <br><br>
     * This may provide faster logging; see {@link #logTime(double)}
     *
     * @param eventTime The timestamp to convert
     * @return an Object where <code>toString</code> returns the same as <code>EventUtil.eventTimeToString</code>
     */
    public static Object logTime(long eventTime) {
        return new Object() {
            public String toString() {
                return eventTimeToString(eventTime);
            }
        };
    }

    /**
     * Generates a computation for formatting a (possibly null) timestamp.
     * <br><br>
     * This may provide faster logging; see {@link #logTime(double)}
     *
     * @param eventTime The timestamp to convert
     * @return an Object where <code>toString</code> returns the same as <code>EventUtil.eventTimeToString</code>
     */
    public static Object logTime(@Nullable Long eventTime) {
        return new Object() {
            public String toString() {
                return eventTimeToString(eventTime);
            }
        };
    }

    public static String durationToString(double duration) {
        Duration d = Duration.ofMillis((long) duration);
        long seconds = d.getSeconds();
        long absSeconds = Math.abs(seconds);
        String positive = String.format(
                "%02d:%02d:%02d.%03d",
                absSeconds / 3600,
                (absSeconds % 3600) / 60,
                absSeconds % 60,
                Math.abs(d.toMillis()) % 1000);
        return seconds < 0 ? "-" + positive : positive;
    }
}
