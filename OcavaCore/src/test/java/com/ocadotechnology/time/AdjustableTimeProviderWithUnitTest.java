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

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AdjustableTimeProviderWithUnitTest {
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
    void testGetTimeAndInstant(double time, Instant instant, TimeUnit timeUnit) {
        AdjustableTimeProviderWithUnit timeProvider = createTimeProvider(timeUnit);
        timeProvider.setTime(time);
        Instant convertedInstant = timeProvider.getInstant();
        double convertedTime = timeProvider.getTime();
        assertEquals(time, convertedTime, time * 1e-15);
        assertEquals(instant, convertedInstant);
    }

    private static AdjustableTimeProviderWithUnit createTimeProvider(TimeUnit timeUnit) {
        return new AdjustableTimeProviderWithUnit(0, timeUnit);
    }
}