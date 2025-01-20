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
import java.util.concurrent.TimeUnit;

/**
 * Provides a time based on realtime but offset and optionally scaled based on user specification
 *
 * Given a multiplier of 2, this will mean that for every 1 second that passes in realtime, 2 seconds will pass on
 * this time provider.
 *
 * This multiplier can be used for modelling more or less computing (network/cpu) resources within a realtime simulation
 * as execution events can be given longer or shorter amounts of times to be computed in.
 * In realtime systems that are highly unconstrained by system resources, this time provider allows for the simulation
 * to be run faster.
 */
public class ScalableOffsetUtcTimeProvider extends UtcTimeProvider implements Serializable {
    private final double offset;

    /**
     * Creates a new realtime time provider with a default time unit of MILLISECONDS,
     * starting at the time specified by the user.
     * @param startTime The start time from epoch (0.0 is 00:00 1st January 1970)
     *
     * @deprecated since 6.00
     * Use {@link #ScalableOffsetUtcTimeProvider(double startTime, TimeUnit timeUnit)} instead.
     */
    @Deprecated
    public ScalableOffsetUtcTimeProvider(double startTime) {
        this(startTime, TimeUnit.MILLISECONDS, 1);
    }

    /**
     * Creates a new realtime time provider starting at the time specified by the user.
     * @param startTime The start time from epoch (0.0 is 00:00 1st January 1970)
     * @param timeUnit The {@link TimeUnit} to return results in
     */
    public ScalableOffsetUtcTimeProvider(double startTime, TimeUnit timeUnit) {
        this(startTime, timeUnit, 1);
    }

    /**
     * Creates a new scaled realtime time provider with a default time unit of MILLISECONDS,
     * starting at the time specified by the user and advancing that the multiplier rate against realtime
     *
     * @param startTime The start time from epoch (0.0 is 00:00 1st January 1970)
     * @param multiplier The multiplier to apply to realtime. A multiplier of 1.0 will act in the same way as
     *                   the underlying `OffsetUtcTimeProvider`

     * @deprecated since 6.00
     * Use {@link #ScalableOffsetUtcTimeProvider(double startTime, TimeUnit timeUnit, double multiplier)} instead.
     */
    public ScalableOffsetUtcTimeProvider(double startTime, double multiplier) {
        this(startTime, TimeUnit.MILLISECONDS, multiplier);
    }

    /**
     * Creates a new scaled realtime time provider starting at the time specified by the user
     * and advancing that the multiplier rate against realtime
     *
     * @param startTime The start time from epoch (0.0 is 00:00 1st January 1970)
     * @param timeUnit The {@link TimeUnit} to return results in
     * @param multiplier The multiplier to apply to realtime. A multiplier of 1.0 will act in the same way as
     *                   the underlying `OffsetUtcTimeProvider`
     */
    public ScalableOffsetUtcTimeProvider(double startTime, TimeUnit timeUnit, double multiplier) {
        super(timeUnit, multiplier);
        this.offset = super.getTime() - startTime;
    }

    @Override
    public double getTime() {
        return super.getTime() - offset;
    }
}
