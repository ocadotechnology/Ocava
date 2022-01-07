/*
 * Copyright © 2017-2022 Ocado (Ocava)
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.ocadotechnology.testing.UtilityClassTest;

class ValuesInSIUnitsTest implements UtilityClassTest {
    private static final double TOLERANCE = 1e-12;

    @Override
    public Class<?> getTestSubject() {
        return ValuesInSIUnits.class;
    }

    @Test
    void convertDuration_whenValueGetsLarger_thenPrecisionIsMaintained() {
        double minuteDuration = 1.31211;
        double siDuration = minuteDuration * 60;
        assertThat(ValuesInSIUnits.convertDuration(minuteDuration, TimeUnit.MINUTES)).isCloseTo(siDuration, withinPercentage(TOLERANCE));
    }

    @Test
    void convertDuration_whenValueGetsSmaller_thenPrecisionIsMaintained() {
        double millisecondDuration = 1.31211;
        double siDuration = millisecondDuration / 1000;
        assertThat(ValuesInSIUnits.convertDuration(millisecondDuration, TimeUnit.MILLISECONDS)).isCloseTo(siDuration, withinPercentage(TOLERANCE));
    }

    @Test
    void convertSpeed_whenValueGetsLarger_thenPrecisionIsMaintained() {
        double kmPerMsSpeed = 1.31211;
        double siSpeed = kmPerMsSpeed * 1000 * 1000;
        assertThat(ValuesInSIUnits.convertSpeed(kmPerMsSpeed, LengthUnit.KILOMETERS, TimeUnit.MILLISECONDS)).isCloseTo(siSpeed, withinPercentage(TOLERANCE));
    }

    @Test
    void convertSpeed_whenValueGetsSmaller_thenPrecisionIsMaintained() {
        double mmPerMinuteSpeed = 1.31211;
        double siSpeed = mmPerMinuteSpeed / (1000 * 60);
        assertThat(ValuesInSIUnits.convertSpeed(mmPerMinuteSpeed, LengthUnit.MILLIMETERS, TimeUnit.MINUTES)).isCloseTo(siSpeed, withinPercentage(TOLERANCE));
    }

    @Test
    void convertAcceleration_whenValueGetsLarger_thenPrecisionIsMaintained() {
        double mmPerMsAcceleration = 1.31211;
        double siSpeed = mmPerMsAcceleration * 1000 * 1000 / 1000;
        assertThat(ValuesInSIUnits.convertAcceleration(mmPerMsAcceleration, LengthUnit.MILLIMETERS, TimeUnit.MILLISECONDS)).isCloseTo(siSpeed, withinPercentage(TOLERANCE));
    }

    @Test
    void convertAcceleration_whenValueGetsSmaller_thenPrecisionIsMaintained() {
        double kmPerHourAcceleration = 1.31211;
        double siSpeed = kmPerHourAcceleration * 1000 / (3600 * 3600);
        assertThat(ValuesInSIUnits.convertAcceleration(kmPerHourAcceleration, LengthUnit.KILOMETERS, TimeUnit.HOURS)).isCloseTo(siSpeed, withinPercentage(TOLERANCE));
    }

    @Test
    void convertJerk_whenValueGetsLarger_thenPrecisionIsMaintained() {
        double mmPerMsJerk = 1.31211;
        double siSpeed = mmPerMsJerk * 1000 * 1000 * 1000 / 1000;
        assertThat(ValuesInSIUnits.convertJerk(mmPerMsJerk, LengthUnit.MILLIMETERS, TimeUnit.MILLISECONDS)).isCloseTo(siSpeed, withinPercentage(TOLERANCE));
    }

    @Test
    void convertJerk_whenValueGetsSmaller_thenPrecisionIsMaintained() {
        double mmPerMinuteJerk = 1.31211;
        double siSpeed = mmPerMinuteJerk / (1000L * 60L * 60L * 60L);
        assertThat(ValuesInSIUnits.convertJerk(mmPerMinuteJerk, LengthUnit.MILLIMETERS, TimeUnit.MINUTES)).isCloseTo(siSpeed, withinPercentage(TOLERANCE));
    }
}
