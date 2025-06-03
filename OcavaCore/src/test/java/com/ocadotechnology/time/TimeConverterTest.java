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
package com.ocadotechnology.time;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TimeConverterTest {
    public static Stream<Arguments> getDurationConversions() {
        return Stream.of(
                Arguments.of(1.5341e13, Duration.ofMillis(15_341_000), TimeUnit.NANOSECONDS),
                Arguments.of(1.5341e7, Duration.ofMillis(15_341_000), TimeUnit.MILLISECONDS),
                Arguments.of(1.5341e4, Duration.ofMillis(15_341_000), TimeUnit.SECONDS),
                Arguments.of(8.345, Duration.ofSeconds(500, 700_000_000), TimeUnit.MINUTES),
                Arguments.of(0.543, Duration.ofSeconds(46_915, 200_000_000), TimeUnit.DAYS)
        );
    }

    @ParameterizedTest
    @MethodSource("getDurationConversions")
    void testConvertToAndFromDuration(double time, Duration duration, TimeUnit timeUnit) {
        TimeConverter timeConverter = new TimeConverter(timeUnit);
        double convertedTime = timeConverter.convertFromDuration(duration);
        Duration convertedDuration = timeConverter.convertToDuration(time);
        assertEquals(time, convertedTime, time * 1e-15);
        assertEquals(duration, convertedDuration);
    }

    public static Stream<Arguments> getInstantConversions() {
        return Stream.of(
                Arguments.of(1.5341e13, Instant.ofEpochMilli(15_341_000), TimeUnit.NANOSECONDS),
                Arguments.of(1.5341e7, Instant.ofEpochMilli(15_341_000), TimeUnit.MILLISECONDS),
                Arguments.of(1.5341e4, Instant.ofEpochMilli(15_341_000), TimeUnit.SECONDS),
                Arguments.of(8.345, Instant.ofEpochSecond(500, 700_000_000), TimeUnit.MINUTES),
                Arguments.of(0.543, Instant.ofEpochSecond(46_915, 200_000_000), TimeUnit.DAYS)
        );
    }

    @ParameterizedTest
    @MethodSource("getInstantConversions")
    void testConvertToAndFromInstant(double time, Instant instant, TimeUnit timeUnit) {
        TimeConverter timeConverter = new TimeConverter(timeUnit);
        double convertedTime = timeConverter.convertFromInstant(instant);
        Instant convertedInstant = timeConverter.convertToInstant(time);
        assertEquals(time, convertedTime, time * 1e-15);
        assertEquals(instant, convertedInstant);
    }
}