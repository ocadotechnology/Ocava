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
package com.ocadotechnology.time;

/**
 * Provides a time based on realtime but offset and scaled based on user specification
 *
 * Given a multiplier of 2, this will mean that for every 1 second that passes in realtime, 2 seconds will pass on
 * this time provider.
 *
 * This time provider can be used for modelling more or less computing (network/cpu) resources within a realtime simulation
 * as execution events can be given longer or shorter amounts of times to be computed in.
 * In realtime systems that are highly unconstrained by system resources, this time provider allows for the simulation to
 * be run faster.
 */
public class ScalableOffsetUtcTimeProvider extends OffsetUtcTimeProvider {

    private final double multiplier;

    /**
     * Creates a new scaled realtime time provider starting at the time specified by the user
     * and advancing that the multiplier rate against realtime
     *
     * @param simulationStartTime The start time from epoch (0.0 is 00:00 1st January 1970)
     * @param multiplier The multiplier to apply to realtime. A multiplier of 1.0 will act in the same way as
     *                   the underlying `OffsetUtcTimeProvider`
     */
    public ScalableOffsetUtcTimeProvider(double simulationStartTime, double multiplier) {
        //Divide the given simulation start time by delta as it will be multiplied by delta when `getTime()` is called
        super(simulationStartTime / multiplier);

        this.multiplier = multiplier;
    }

    @Override
    public double getTime() {
        return super.getTime() * multiplier;
    }
}
