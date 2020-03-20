/*
 * Copyright © 2017-2020 Ocado (Ocava)
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

    private static String eventTimeToFormat(double eventTime, DateTimeFormatter dateTimeFormatter) {
        return Double.isFinite(eventTime)
                ? eventTimeToFormat(roundToLong(eventTime), dateTimeFormatter)
                : Double.toString(eventTime);
    }

    private static long roundToLong(double eventTime) {
        try {
            return DoubleMath.roundToLong(eventTime, RoundingMode.FLOOR);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Invalid timestamp: " + eventTime, e);
        }
    }

    private static String eventTimeToFormat(long eventTime, DateTimeFormatter dateTimeFormatter) {
        Preconditions.checkArgument(eventTime >= MIN_TIMESTAMP, "Timestamp is before 0001-01-01 00:00:00.000: %s", eventTime);
        return dateTimeFormatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(eventTime), UTC));
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
     * @throws IllegalArgumentException If the timestamp corresponds to a time prior to 0001-01-01 00:00:00.000.
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
     * @throws IllegalArgumentException If the timestamp corresponds to a time prior to 0001-01-01 00:00:00.000.
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
