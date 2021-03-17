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
package com.ocadotechnology.time;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Provides a time based on the system clock time.
 */
public class UtcTimeProvider implements TimeProvider, Serializable {
    public static UtcTimeProvider NULL = new UtcTimeProvider(TimeUnit.MILLISECONDS, 0);

    private final TimeUnit timeUnit;
    private final double multiplier;

    /**
     * Creates a new realtime time provider with a default time unit of MILLISECONDS.
     *
     * @deprecated since 6.00
     * Use {@link #UtcTimeProvider(TimeUnit timeUnit)} instead.
     */
    @Deprecated
    public UtcTimeProvider() {
        this(TimeUnit.MILLISECONDS, 1);
    }

    /**
     * Creates a new realtime time provider
     * @param timeUnit The {@link TimeUnit} to return results in
     */
    public UtcTimeProvider(TimeUnit timeUnit) {
        this(timeUnit, 1);
    }

    /**
     * Creates a new realtime time provider where time progresses at a specified multiple of realtime.  Protected
     * because it probably doesn't make sense to apply a multiplier without an offset.
     * @param timeUnit The {@link TimeUnit} to return results in
     * @param multiplier The realtime multiplier to use
     */
    UtcTimeProvider(TimeUnit timeUnit, double multiplier) {
        this.timeUnit = timeUnit;
        this.multiplier = multiplier / TimeUnit.MILLISECONDS.convert(1, timeUnit);
    }

    @Override
    public double getTime() {
        return System.currentTimeMillis() * multiplier;
    }

    public double getMillisecondMultiplier() {
        return multiplier;
    }
}
