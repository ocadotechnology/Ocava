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
package com.ocadotechnology.physics.units;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.RoundingMode;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.math.DoubleMath;
import com.ocadotechnology.testing.UtilityClassTest;

public class TimeUnitConverterTest implements UtilityClassTest {

    @Override
    public Class<?> getTestSubject() {
        return TimeUnitConverter.class;
    }

    @ParameterizedTest
    @MethodSource("getAllUnitCombinations")
    void toTimeUnitLong_fromLong_withUnits(TimeUnit currentTimeUnit, TimeUnit targetTimeUnit) {
        long eventTime = 1L;
        Assertions.assertEquals(
                targetTimeUnit.convert(eventTime, currentTimeUnit),
                TimeUnitConverter.toTimeUnitLong(eventTime, currentTimeUnit, targetTimeUnit)
        );
    }

    @ParameterizedTest
    @MethodSource("getAllUnitCombinations")
    void toTimeUnitLong_fromDouble_withUnits(TimeUnit currentTimeUnit, TimeUnit targetTimeUnit) {
        long eventTime = 1L;
        double ratio = getRatioOfUnits(currentTimeUnit, targetTimeUnit);
        Assertions.assertEquals(
                DoubleMath.roundToLong(eventTime * ratio, RoundingMode.FLOOR),
                TimeUnitConverter.toTimeUnitLong(eventTime, currentTimeUnit, targetTimeUnit)
        );
    }

    @ParameterizedTest
    @MethodSource("getAllUnitCombinations")
    void toTimeUnitDouble_fromLong_withUnits(TimeUnit currentTimeUnit, TimeUnit targetTimeUnit) {
        long eventTime = 1L;
        double ratio = getRatioOfUnits(currentTimeUnit, targetTimeUnit);
        Assertions.assertEquals(
                eventTime * ratio,
                TimeUnitConverter.toTimeUnitDouble(eventTime, currentTimeUnit, targetTimeUnit)
        );
    }

    @ParameterizedTest
    @MethodSource("getAllUnitCombinations")
    void toTimeUnitDouble_fromDouble_withUnits(TimeUnit currentTimeUnit, TimeUnit targetTimeUnit) {
        double eventTime = 1.5d;
        double ratio = getRatioOfUnits(currentTimeUnit, targetTimeUnit);
        Assertions.assertEquals(
                eventTime * ratio,
                TimeUnitConverter.toTimeUnitDouble(eventTime, currentTimeUnit, targetTimeUnit)
        );
    }

    @ParameterizedTest
    @MethodSource("getAllUnitCombinations")
    void fromTimeUnit_fromLong_withUnits(TimeUnit currentTimeUnit, TimeUnit targetTimeUnit) {
        long eventTime = 1L;
        double ratio = getRatioOfUnits(currentTimeUnit, targetTimeUnit);
        Assertions.assertEquals(
                eventTime * ratio,
                TimeUnitConverter.fromTimeUnit(eventTime, currentTimeUnit, targetTimeUnit)
        );
    }

    @ParameterizedTest
    @MethodSource("getAllUnitCombinations")
    void fromTimeUnit_fromDouble_withUnits(TimeUnit currentTimeUnit, TimeUnit targetTimeUnit) {
        double eventTime = 1.5d;
        double ratio = getRatioOfUnits(currentTimeUnit, targetTimeUnit);
        Assertions.assertEquals(
                eventTime * ratio,
                TimeUnitConverter.fromTimeUnit(eventTime, currentTimeUnit, targetTimeUnit)
        );
    }

    @Test
    void toTimeUnitDouble_Double_MinValue_withNoConversion() {
        assertEquals(-Double.MAX_VALUE, TimeUnitConverter.toTimeUnitDouble(-Double.MAX_VALUE, TimeUnit.MILLISECONDS, TimeUnit.MILLISECONDS));
    }

    @Test
    void toTimeUnitDouble_Double_MinValue_withSmallerConversion() {
        assertEquals(Double.NEGATIVE_INFINITY, TimeUnitConverter.toTimeUnitDouble(-Double.MAX_VALUE, TimeUnit.MILLISECONDS, TimeUnit.MICROSECONDS));
    }

    @Test
    void toTimeUnitDouble_Double_MinValue_withLargerConversion() {
        double ratio = getRatioOfUnits(TimeUnit.MILLISECONDS, TimeUnit.DAYS);
        assertEquals(-Double.MAX_VALUE * ratio, TimeUnitConverter.toTimeUnitDouble(-Double.MAX_VALUE, TimeUnit.MILLISECONDS, TimeUnit.DAYS));
    }

    @Test
    void toTimeUnitDouble_Double_MaxValue_withNoConversion() {
        assertEquals(Double.MAX_VALUE, TimeUnitConverter.toTimeUnitDouble(Double.MAX_VALUE, TimeUnit.MILLISECONDS, TimeUnit.MILLISECONDS));
    }

    @Test
    void toTimeUnitDouble_Double_MaxValue_withSmallerConversion() {
        assertEquals(Double.POSITIVE_INFINITY, TimeUnitConverter.toTimeUnitDouble(Double.MAX_VALUE, TimeUnit.MILLISECONDS, TimeUnit.MICROSECONDS));
    }

    @Test
    void toTimeUnitDouble_Double_MaxValue_withLargerConversion() {
        double ratio = getRatioOfUnits(TimeUnit.MILLISECONDS, TimeUnit.DAYS);
        assertEquals(Double.MAX_VALUE * ratio, TimeUnitConverter.toTimeUnitDouble(Double.MAX_VALUE, TimeUnit.MILLISECONDS, TimeUnit.DAYS));
    }

    private double getRatioOfUnits(TimeUnit unitA, TimeUnit unitB) {
        return (double) unitA.toNanos(1) / unitB.toNanos(1);
    }

    private static Stream<Arguments> getAllUnitCombinations() {
        return Arrays.stream(TimeUnit.values())
                .flatMap(unitA -> Arrays.stream(TimeUnit.values()).map(unitB -> Arguments.of(unitA, unitB))
        );
    }
}
