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

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Class for converting between primitive and java.time types.
 */
@ParametersAreNonnullByDefault
public class TimeConverter implements Serializable {
    private final TimeUnit timeUnit;
    //Having the ratios both ways around seems to allow for more accurate conversions
    private final double unitsPerSecond;
    private final double secondsPerUnit;
    private final double unitsPerNanoSecond;
    private final double nanoSecondsPerUnit;

    protected TimeConverter(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        this.unitsPerSecond = getRatio(timeUnit, TimeUnit.SECONDS);
        this.secondsPerUnit = getRatio(TimeUnit.SECONDS, timeUnit);
        this.unitsPerNanoSecond = getRatio(timeUnit, TimeUnit.NANOSECONDS);
        this.nanoSecondsPerUnit = getRatio(TimeUnit.NANOSECONDS, timeUnit);
    }

    /**
     * It took me too long to figure which way up this should be:
     * <br>
     * TimeUnit.toNanos(1) returns the number of nanos in 1 unit of the TimeUnit. (nanos / unit)
     * <br>
     * To get the ratio top / bottom, we need to perform the calculation:
     * <br>
     * top / bottom = top/nanos * nanos/bottom
     * <br>
     * where top/nanos = 1 / (nanos/top), so:
     * <br>
     * top / bottom = (nanos/bottom) / (nanos/top)
     * <br>
     * A multiplication by 1.0 is added to convert the result into a floating point number, as the division
     * may produce a value less than 1.
     */
    private static double getRatio(TimeUnit top, TimeUnit bottom) {
        return bottom.toNanos(1) * 1.0 / top.toNanos(1);
    }

    /**
     * Returns the TimeUnit that defines the meaning of 1 unit of time in this TimeProvider.
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    /**
     * Helper method to convert a Duration object into a primitive double in this TimeProvider's time unit
     */
    public double convertFromDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long nanos = duration.getNano();
        return seconds * unitsPerSecond + nanos * unitsPerNanoSecond;
    }

    /**
     * Helper method to convert a primitive double in this TimeProvider's time unit into a Duration object
     */
    public Duration convertToDuration(double duration) {
        long seconds = (long) (duration * secondsPerUnit);
        long nanos = (long) ((duration % unitsPerSecond) * nanoSecondsPerUnit);
        return Duration.ofSeconds(seconds, nanos);
    }

    /**
     * Helper method to convert an Instant object into a primitive double in this TimeProvider's time unit, assuming
     * that t = 0 is epoch
     */
    public double convertFromInstant(Instant instant) {
        long seconds = instant.getEpochSecond();
        long nanos = instant.getNano();
        return seconds * unitsPerSecond + nanos * unitsPerNanoSecond;
    }

    /**
     * Helper method to convert a primitive double in this TimeProvider's time unit into an Instant object, assuming
     * that t = 0 is epoch
     */
    public Instant convertToInstant(double instant) {
        long seconds = (long) (instant * secondsPerUnit);
        long nanos = (long) ((instant % unitsPerSecond) * nanoSecondsPerUnit);
        return Instant.ofEpochSecond(seconds, nanos);
    }
}
