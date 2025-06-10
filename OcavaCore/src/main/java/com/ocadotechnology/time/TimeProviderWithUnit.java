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

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * An abstract implementation of {@link TimeProvider} for TimeProviders which store the real units they return time in.
 */
@ParametersAreNonnullByDefault
public interface TimeProviderWithUnit extends TimeProvider {
    /**
     * Static instance of TimeProviderWithUnit which always return 0 for current time, and uses the millisecond time unit
     */
    TimeProviderWithUnit NULL_WITH_MILLIS = new UnmodifiableTimeProviderWithUnit(TimeUnit.MILLISECONDS);

    /**
     * Static instance of TimeProviderWithUnit which always return 0 for current time, and uses the second time unit
     */
    TimeProviderWithUnit NULL_WITH_SECONDS = new UnmodifiableTimeProviderWithUnit(TimeUnit.SECONDS);

    /**
     * Static instance of TimeProviderWithUnit which always return 0 for current time, and uses the millisecond time unit.
     * The same instance as {@link #NULL_WITH_MILLIS}, but used to hide TimeProvider.NULL. Without this constant, it is
     * possible to invoke TimeProviderWithUnit.NULL and get a TimeProvider which does not satisfy the contract of this
     * interface.
     */
    TimeProviderWithUnit NULL = NULL_WITH_MILLIS;

    /**
     * Returns the TimeConverter utility class.
     */
    TimeConverter getConverter();

    /**
     * Returns the time in the unit specified for this time provider
     */
    default TimeUnit getTimeUnit() {
        return getConverter().getTimeUnit();
    }

    /**
     * Returns the time as an Instant object.
     */
    default Instant getInstant() {
        return getConverter().convertToInstant(getTime());
    }

    /**
     * Implementation of {@link TimeProviderWithUnit} that always returns a fixed value for the current time. Intended
     * for use in test-cases where the progression of time is not desired, but the code requires a time unit to be
     * specified. This class still has a fully realised {@link TimeConverter}, which can be used to convert between
     * primitive and object times.
     */
    class UnmodifiableTimeProviderWithUnit implements TimeProviderWithUnit {
        private final TimeConverter converter;
        private final double now;

        UnmodifiableTimeProviderWithUnit(double now, TimeUnit unit) {
            this.converter = new TimeConverter(unit);
            this.now = now;
        }

        UnmodifiableTimeProviderWithUnit(TimeUnit unit) {
            this(0d, unit);
        }

        @Override
        public double getTime() {
            return now;
        }

        @Override
        public TimeConverter getConverter() {
            return converter;
        }
    }
}
