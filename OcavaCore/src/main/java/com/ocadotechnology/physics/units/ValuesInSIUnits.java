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
package com.ocadotechnology.physics.units;

import java.util.concurrent.TimeUnit;

/**
 * Utility class for converting values into SI units.
 */
public final class ValuesInSIUnits {
    private static final long NANOS_IN_SECOND = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);

    private ValuesInSIUnits() {
        throw new UnsupportedOperationException("Should never instantiate ValuesInSIUnits");
    }

    public static double convertDuration(double duration, TimeUnit sourceUnit) {
        return duration * getTimeUnitAsSeconds(sourceUnit);
    }

    public static double convertSpeed(double speed, LengthUnit sourceLengthUnit, TimeUnit sourceTimeUnit) {
        return speed * LengthUnit.METERS.getUnitsIn(sourceLengthUnit) / getTimeUnitAsSeconds(sourceTimeUnit);
    }

    public static double convertAcceleration(double acceleration, LengthUnit sourceLengthUnit, TimeUnit sourceTimeUnit) {
        return acceleration * LengthUnit.METERS.getUnitsIn(sourceLengthUnit) / Math.pow(getTimeUnitAsSeconds(sourceTimeUnit), 2);
    }

    public static double convertJerk(double jerk, LengthUnit sourceLengthUnit, TimeUnit sourceTimeUnit) {
        return jerk * LengthUnit.METERS.getUnitsIn(sourceLengthUnit) / Math.pow(getTimeUnitAsSeconds(sourceTimeUnit), 3);
    }

    private static double getTimeUnitAsSeconds(TimeUnit unit) {
        if (unit == TimeUnit.SECONDS) {
            return 1;
        }
        return TimeUnit.NANOSECONDS.convert(1, unit) * 1.0 / NANOS_IN_SECOND;
    }
}
