/*
 * Copyright © 2017-2019 Ocado (Ocava)
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

import com.google.common.math.DoubleMath;

/**
 * Utilities for converting double time values to readable strings.
 *
 * Currently asserts that the time unit for all schedulers is 1ms.
 */
public class EventUtil {
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter isoDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final DateTimeFormatter dateTimeWithoutMsFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ZoneId UTC = ZoneId.of("UTC");

    private static String eventTimeToFormat(double eventTime, DateTimeFormatter dateTimeFormatter) {
        if (!Double.isFinite(eventTime)) {
            return Double.toString(eventTime);
        }
        if (eventTime > 0) {
            return dateTimeFormatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(DoubleMath.roundToLong(eventTime, RoundingMode.FLOOR)), UTC));
        }
        return dateTimeFormatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(0), UTC));
    }

    public static String eventTimeToString(double eventTime) {
        return eventTimeToFormat(eventTime, dateTimeFormatter);
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
