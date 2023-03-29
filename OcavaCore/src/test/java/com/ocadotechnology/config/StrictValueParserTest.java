/*
 * Copyright © 2017-2023 Ocado (Ocava)
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
package com.ocadotechnology.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.math.DoubleMath;
import com.ocadotechnology.config.TestConfig.Colours;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.StringId;
import com.ocadotechnology.physics.units.LengthUnit;
import com.ocadotechnology.validation.Failer;

public class StrictValueParserTest {

    @Nested
    @DisplayName("for String values")
    class StringParserTests {
        @DisplayName("returns non-empty value")
        @Test
        void returnsValue() {
            String testValue = "A TEST VALUE";
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, testValue);
            assertThat(parser.asString()).isEqualTo(testValue);
        }

        @DisplayName("returns empty value")
        @Test
        void returnsEmptyValue() {
            String testValue = "";
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, testValue);
            assertThat(parser.asString()).isEqualTo(testValue);
        }
    }

    @Nested
    @DisplayName("For fraction values")
    class FractionParserTests {
        @Test
        @DisplayName("returns the expected value when within bounds")
        void returnsValueWhenValid() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "0.1");
            assertThat(parser.asFraction()).isEqualTo(0.1);
        }

        @Test
        @DisplayName("throws IllegalStateException when out of bounds")
        void throwsIllegalStateException() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.7182");
            assertThatThrownBy(parser::asFraction).hasCauseInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws NumberFormatException when not a number")
        void throwsNumberFormatException() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "abcde");
            assertThatThrownBy(parser::asFraction).hasCauseInstanceOf(NumberFormatException.class);
        }
    }

    @Nested
    @DisplayName("for Boolean values")
    class BooleanParserTests {

        @DisplayName("returns true")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"true", "True", "TRUE", "tRUe"})
        void allowsTrueValues(String value) {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, value);
            assertThat(parser.asBoolean()).isTrue();
        }

        @DisplayName("returns false")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"false", "False", "FALSE", "fAlSe"})
        void allowsFalseValues(String value) {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, value);
            assertThat(parser.asBoolean()).isFalse();
        }

        @DisplayName("throws an exception for typo")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"fa lse", "ture", "yes", "0", "1"})
        void throwsExceptionForMisspelledValue(String value) {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, value);
            assertThrowsWithKey(parser::asBoolean, IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("for Integer values")
    class IntegerParserTests {

        @Test
        @DisplayName("returns the integer value")
        void returnIntegerValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "42");
            assertThat(parser.asInt()).isEqualTo(42);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "-2");
            assertThat(parser.asInt()).isEqualTo(-2);
        }

        @DisplayName("returns Integer.MAX_VALUE")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"max", "MAX", "mAx"})
        void testMaxValues(String value) {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, value);
            assertThat(parser.asInt()).isEqualTo(Integer.MAX_VALUE);
        }

        @DisplayName("returns Integer.MIN_VALUE")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"min", "MIN", "MiN"})
        void testMinValues(String value) {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, value);
            assertThat(parser.asInt()).isEqualTo(Integer.MIN_VALUE);
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "FAIL");
            assertThrowsWithKey(parser::asInt, NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception for a non-integer number")
        void throwsExceptionForDecimalNumber() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.7182");
            assertThrowsWithKey(parser::asInt, NumberFormatException.class);
        }

        @DisplayName("throws an exception for an invalid number format")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"1e3", "1x10^3"})
        void throwsExceptionForInvalidNumberFormat(String value) {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, value);
            assertThrowsWithKey(parser::asInt, NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception for an overly-large number")
        void throwsExceptionForLargeNumber() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, String.valueOf(Long.MAX_VALUE));
            assertThrowsWithKey(parser::asInt, NumberFormatException.class);
        }
    }

    @Nested
    @DisplayName("for Long values")
    class LongParserTests {

        @Test
        @DisplayName("returns the long value")
        void returnLongValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "42");
            assertThat(parser.asLong()).isEqualTo(42);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "-2");
            assertThat(parser.asLong()).isEqualTo(-2);
        }

        @DisplayName("returns Long.MAX_VALUE")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"max", "MAX", "mAx"})
        void testMaxValues(String value) {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, value);
            assertThat(parser.asLong()).isEqualTo(Long.MAX_VALUE);
        }

        @DisplayName("returns Long.MIN_VALUE")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"min", "MIN", "MiN"})
        void testMinValues(String value) {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, value);
            assertThat(parser.asLong()).isEqualTo(Long.MIN_VALUE);
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "FAIL");
            assertThrowsWithKey(parser::asLong, NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception for a non-integer number")
        void throwsExceptionForDecimalNumber() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.7182");
            assertThrowsWithKey(parser::asLong, NumberFormatException.class);
        }

        @DisplayName("throws an exception for an invalid number format")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"1e3", "1x10^3"})
        void throwsExceptionForInvalidNumberFormat(String value) {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, value);
            assertThrowsWithKey(parser::asLong, NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception for an overly-large number")
        void throwsExceptionForLargeNumber() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, Long.MAX_VALUE + "00");
            assertThrowsWithKey(parser::asLong, NumberFormatException.class);
        }
    }

    @Nested
    @DisplayName("for Double values")
    class DoubleParserTests {

        @Test
        @DisplayName("returns the double value")
        void returnDoubleValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.7182");
            assertThat(parser.asDouble()).isEqualTo(2.7182);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "-2.7182");
            assertThat(parser.asDouble()).isEqualTo(-2.7182);
        }

        @DisplayName("allows all valid number formats")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"1.3e3"})
        void allowsValidNumberFormat(String value) {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, value);
            assertThat(parser.asDouble()).isEqualTo(1300);
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "FAIL");
            assertThrowsWithKey(parser::asDouble, NumberFormatException.class);
        }

        @DisplayName("throws an exception for an invalid number format")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"1x10^3"})
        void throwsExceptionForInvalidNumberFormat(String value) {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, value);
            assertThrowsWithKey(parser::asDouble, NumberFormatException.class);
        }
    }

    @Nested
    @DisplayName("for Enum values")
    class EnumConfigTests {

        @Test
        @DisplayName("parses correct config")
        void parseCorrectConfig() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "RED");
            assertThat(parser.asEnum(TestConfig.Colours.class)).isEqualTo(TestConfig.Colours.RED);
        }

        @Test
        @DisplayName("throws IllegalArgumentException for incorrect value")
        void throwsException() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "REDDER");
            assertThrowsWithKey(() -> parser.asEnum(TestConfig.Colours.class), IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("for Time values")
    class TimeConfigTests {

        @Test
        @DisplayName("returns the long value")
        void returnTimeValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, SECONDS", TimeUnit.SECONDS, null);
            assertThat(parser.asTime()).isEqualTo(2);
            assertThat(parser.asFractionalTime()).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the long value when units are not specified")
        void returnTimeValueWithNoSpecifiedUnits() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3", TimeUnit.SECONDS, null);
            assertThat(parser.asTime()).isEqualTo(2);
            assertThat(parser.asFractionalTime()).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the scaled value")
        void returnScaledTimeValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.11, MINUTES", TimeUnit.SECONDS, null);
            assertThat(parser.asTime()).isEqualTo(127);
            assertThat(parser.asFractionalTime()).isEqualTo(126.6);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "-2.3, SECONDS", TimeUnit.SECONDS, null);
            assertThat(parser.asTime()).isEqualTo(-2);
            assertThat(parser.asFractionalTime()).isEqualTo(-2.3);
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "FAIL, SECONDS", TimeUnit.SECONDS, null);
            assertThrowsWithKey(parser::asTime, NumberFormatException.class);
            assertThrowsWithKey(parser::asFractionalTime, NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2, SECONDS, SECONDS", TimeUnit.SECONDS, null);
            assertThrowsWithKey(parser::asTime, IllegalStateException.class);
            assertThrowsWithKey(parser::asFractionalTime, IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2, ORANGES", TimeUnit.SECONDS, null);
            assertThrowsWithKey(parser::asTime, IllegalArgumentException.class);
            assertThrowsWithKey(parser::asFractionalTime, IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Throws an exception if time unit not set")
        void throwsExceptionWhenUnitNotSet() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2, SECONDS", null, null);
            assertThrowsWithKey(parser::asTime, NullPointerException.class);
            assertThrowsWithKey(parser::asFractionalTime, NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("for Duration values")
    class DurationConfigTests {

        @Test
        @DisplayName("returns a Duration for an integer value")
        void returnDurationForInt() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2, MILLISECONDS");
            assertThat(parser.asDuration()).isEqualTo(Duration.ofMillis(2));
        }

        @Test
        @DisplayName("returns a Duration for a fractional value")
        void returnDurationForFraction() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, MILLISECONDS");
            assertThat(parser.asDuration()).isEqualTo(Duration.ofNanos(2_300_000));
        }

        @Test
        @DisplayName("returns a Duration with units of seconds when units are not specified")
        void returnDurationInSecondsWhenNoSpecifiedUnits() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3");
            assertThat(parser.asDuration()).isEqualTo(Duration.ofMillis(2_300));
        }

        @Test
        @DisplayName("handles a 0 value")
        void zeroValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "0");
            assertThat(parser.asDuration()).isEqualTo(Duration.ofSeconds(0));
        }

        @Test
        @DisplayName("rounds to the nearest nanosecond - below")
        void roundToNanoBelow() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "0.1,NANOSECONDS");
            assertThat(parser.asDuration()).isEqualTo(Duration.ofNanos(0));
        }

        @Test
        @DisplayName("rounds to the nearest nanosecond - above")
        void roundToNanoAbove() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "0.6,NANOSECONDS");
            assertThat(parser.asDuration()).isEqualTo(Duration.ofNanos(1));
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "-2, SECONDS");
            assertThat(parser.asDuration()).isEqualTo(Duration.ofSeconds(-2));
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "FAIL, SECONDS");
            assertThrowsWithKey(parser::asDuration, NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2, SECONDS, SECONDS");
            assertThrowsWithKey(parser::asDuration, IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2, ORANGES");
            assertThrowsWithKey(parser::asDuration, IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("for Length values")
    class LengthConfigTests {

        @Test
        @DisplayName("returns the double value")
        void returnValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, METERS", null, LengthUnit.METERS);
            assertThat(parser.asLength()).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the double value when units are not specified")
        void returnValueWithNoSpecifiedUnits() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3", null, LengthUnit.METERS);
            assertThat(parser.asLength()).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the scaled value km -> m")
        void returnInputScaledValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, KILOMETERS", null, LengthUnit.METERS);
            assertThat(parser.asLength()).isEqualTo(2300);
        }

        @Test
        @DisplayName("returns the scaled value m -> km")
        void returnOutputScaledValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, METERS", null, LengthUnit.KILOMETERS);
            assertThat(parser.asLength()).isEqualTo(0.0023);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "-2.3, METERS", null, LengthUnit.METERS);
            assertThat(parser.asLength()).isEqualTo(-2.3);
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "FAIL, METERS", null, LengthUnit.METERS);
            assertThrowsWithKey(parser::asLength, NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2, METERS, METERS", null, LengthUnit.METERS);
            assertThrowsWithKey(parser::asLength, IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2, ORANGES", null, LengthUnit.METERS);
            assertThrowsWithKey(parser::asLength, IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Throws an exception if length unit not set")
        void throwsExceptionWhenUnitNotSet() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "FAIL, METERS", null, null);
            assertThrowsWithKey(parser::asLength, NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("for Speed values")
    class SpeedConfigTests {

        @Test
        @DisplayName("returns the double value when units are specified")
        void returnValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asSpeed()).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the double value when units are not specified")
        void returnValueWithDefaultUnits() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asSpeed()).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the scaled value ms -> s")
        void returnTimeInputScaledTimeValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, METERS, MILLISECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asSpeed()).isEqualTo(2300);
        }

        @Test
        @DisplayName("returns the scaled value s -> ms")
        void returnTimeOutputScaledTimeValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, METERS, SECONDS", TimeUnit.MILLISECONDS, LengthUnit.METERS);
            assertThat(parser.asSpeed()).isEqualTo(0.0023);
        }

        @Test
        @DisplayName("returns the scaled value km -> m")
        void returnLengthInputScaledTimeValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, KILOMETERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asSpeed()).isEqualTo(2300);
        }

        @Test
        @DisplayName("returns the scaled value m -> km")
        void returnLengthOutputScaledTimeValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.KILOMETERS);
            assertThat(parser.asSpeed()).isEqualTo(0.0023);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "-2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asSpeed()).isEqualTo(-2.3);
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "FAIL, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThrowsWithKey(parser::asSpeed, NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2, METERS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThrowsWithKey(parser::asSpeed, IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2, METERS, ORANGES", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThrowsWithKey(parser::asSpeed, IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Throws an exception if length unit not set")
        void throwsExceptionWhenLengthUnitNotSet() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, METERS, SECONDS", TimeUnit.SECONDS, null);
            assertThrowsWithKey(parser::asSpeed, NullPointerException.class);
        }

        @Test
        @DisplayName("Throws an exception if time unit not set")
        void throwsExceptionWhenTimeUnitNotSet() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, METERS, SECONDS", null, LengthUnit.METERS);
            assertThrowsWithKey(parser::asSpeed, NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("for Acceleration values")
    class AccelerationConfigTests {

        @Test
        @DisplayName("returns the double value with units specified")
        void returnValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asAcceleration()).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the double value with no units specified")
        void returnValueWithDefaultUnits() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asAcceleration()).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the scaled value ms -> s")
        void returnTimeInputScaledValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, METERS, MILLISECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asAcceleration()).isEqualTo(2300_000);
        }

        @Test
        @DisplayName("returns the scaled value s -> ms")
        void returnTimeOutputScaledValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, METERS, SECONDS", TimeUnit.MILLISECONDS, LengthUnit.METERS);
            assertThat(parser.asAcceleration()).isEqualTo(0.0000023);
        }

        @Test
        @DisplayName("returns the scaled value km -> m")
        void returnLengthInputScaledValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, KILOMETERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asAcceleration()).isEqualTo(2300);
        }

        @Test
        @DisplayName("returns the scaled value m -> km")
        void returnLengthOutputScaledValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.KILOMETERS);
            assertThat(parser.asAcceleration()).isEqualTo(0.0023);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "-2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asAcceleration()).isEqualTo(-2.3);
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "FAIL, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThrowsWithKey(parser::asAcceleration, NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2, METERS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThrowsWithKey(parser::asAcceleration, IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2, METERS, ORANGES", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThrowsWithKey(parser::asAcceleration, IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Throws an exception if length unit not set")
        void throwsExceptionWhenLengthUnitNotSet() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, METERS, SECONDS", TimeUnit.SECONDS, null);
            assertThrowsWithKey(parser::asAcceleration, NullPointerException.class);
        }

        @Test
        @DisplayName("Throws an exception if time unit not set")
        void throwsExceptionWhenTimeUnitNotSet() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, METERS, SECONDS", null, LengthUnit.METERS);
            assertThrowsWithKey(parser::asAcceleration, NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("for Jerk values")
    class JerkConfigTests {

        @Test
        @DisplayName("returns the double value with units specified")
        void returnValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asJerk()).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the double value with no units specified")
        void returnValueWithDefaultUnits() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asJerk()).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the scaled value ms -> s")
        void returnTimeInputScaledValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, METERS, MILLISECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(DoubleMath.fuzzyEquals(parser.asJerk(), 2.3e9, 1e-3)).isTrue();
        }

        @Test
        @DisplayName("returns the scaled value s -> ms")
        void returnTimeOutputScaledValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, METERS, SECONDS", TimeUnit.MILLISECONDS, LengthUnit.METERS);
            assertThat(parser.asJerk()).isEqualTo(2.3e-9);
        }

        @Test
        @DisplayName("returns the scaled value km -> m")
        void returnLengthInputScaledValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, KILOMETERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asJerk()).isEqualTo(2300);
        }

        @Test
        @DisplayName("returns the scaled value m -> km")
        void returnLengthOutputScaledValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.KILOMETERS);
            assertThat(parser.asJerk()).isEqualTo(0.0023);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "-2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asJerk()).isEqualTo(-2.3);
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "FAIL, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThrowsWithKey(parser::asJerk, NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2, METERS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThrowsWithKey(parser::asJerk, IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2, METERS, ORANGES", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThrowsWithKey(parser::asJerk, IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Throws an exception if length unit not set")
        void throwsExceptionWhenLengthUnitNotSet() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, METERS, SECONDS", TimeUnit.SECONDS, null);
            assertThrowsWithKey(parser::asJerk, NullPointerException.class);
        }

        @Test
        @DisplayName("Throws an exception if time unit not set")
        void throwsExceptionWhenTimeUnitNotSet() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "2.3, METERS, SECONDS", null, LengthUnit.METERS);
            assertThrowsWithKey(parser::asJerk, NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("test Lists")
    class ListTests {

        @Test
        @DisplayName("Empty string case returns empty list")
        void testEmptyString() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "");
            assertThat(parser.asList().ofStrings()).isEqualTo(ImmutableList.of());
        }

        @Test
        @DisplayName("Single case returns singleton list")
        void testSingleElement() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "RED");
            assertThat(parser.asList().ofStrings()).isEqualTo(ImmutableList.of("RED"));
        }

        @DisplayName("separates valid strings")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"RED:YELLOW:APPLE", "RED,YELLOW,APPLE"})
        void testColonSeparated(String value) {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, value);
            assertThat(parser.asList().ofStrings()).isEqualTo(ImmutableList.of("RED", "YELLOW", "APPLE"));
        }

        @Test
        @DisplayName("List of strings with space is trimmed")
        void testSpaceIsTrimmed() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "RED,YELLOW, APPLE");
            assertThat(parser.asList().ofStrings()).isEqualTo(ImmutableList.of("RED", "YELLOW", "APPLE"));
        }

        @Test
        @DisplayName("Comma-separated string can contain colons")
        void testCommaSeparatedStringsWithColons() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "key1:value1,key2:value2");
            assertThat(parser.asList().ofStrings()).isEqualTo(ImmutableList.of("key1:value1", "key2:value2"));
        }

        @Test
        @DisplayName("numerical methods with numbers")
        void testNumericalLists() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "1,5,10,3");
            assertThat(parser.asList().ofIntegers()).isEqualTo(ImmutableList.of(1, 5, 10, 3));
            assertThat(parser.asList().ofLongs()).isEqualTo(ImmutableList.of(1L, 5L, 10L, 3L));
            assertThat(parser.asList().ofDoubles()).isEqualTo(ImmutableList.of(1D, 5D, 10D, 3D));
            assertThat(parser.asList().ofIds()).isEqualTo(ImmutableList.of(Id.create(1), Id.create(5), Id.create(10), Id.create(3)));
        }

        @Test
        @DisplayName("numerical methods with non-numbers throw exceptions")
        void testNumericalListsThrow() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "RED,BLUE,APPLE,PEAR");
            assertThrowsWithKey(() -> parser.asList().ofIntegers(), NumberFormatException.class);
            assertThrowsWithKey(() -> parser.asList().ofLongs(), NumberFormatException.class);
            assertThrowsWithKey(() -> parser.asList().ofDoubles(), NumberFormatException.class);
            assertThrowsWithKey(() -> parser.asList().ofIds(), NumberFormatException.class);
        }

        @Test
        @DisplayName("enum values are parsed")
        void testEnumLists() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "RED,BLUE,BLUE,RED");
            ImmutableList<Colours> expected = ImmutableList.of(Colours.RED, Colours.BLUE, Colours.BLUE, Colours.RED);
            assertThat(parser.asList().ofEnums(TestConfig.Colours.class)).isEqualTo(expected);
        }

        @Test
        @DisplayName("incorrect enum values throw exception")
        void testEnumListsThrow() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "RED,BLUE,APPLE,PEAR");
            assertThrowsWithKey(() -> parser.asList().ofEnums(TestConfig.Colours.class), IllegalArgumentException.class);
        }

        @Test
        @DisplayName("string Ids are conveted")
        void testStringIdLists() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "RED,BLUE,APPLE,PEAR");
            ImmutableList<StringId<Object>> expected = ImmutableList.of(
                    StringId.create("RED"),
                    StringId.create("BLUE"),
                    StringId.create("APPLE"),
                    StringId.create("PEAR")
            );
            assertThat(parser.asList().ofStringIds()).isEqualTo(expected);
        }

        @Test
        @DisplayName("custom parser is used as expected")
        void callsParser() {
            String testValue = "ANOTHER TEST VALUE";
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, testValue);

            List<String> arguments = new ArrayList<>();
            TestClass testClass = new TestClass();
            Function<String, TestClass> parserFunction = value -> {
                arguments.add(value);
                return testClass;
            };
            assertThat(parser.asList().withElementParser(parserFunction)).isEqualTo(ImmutableList.of(testClass));
            assertThat(arguments.size()).isEqualTo(1);
            assertThat(arguments.get(0)).isEqualTo(testValue);
        }
    }

    @Nested
    @DisplayName("test Sets")
    class SetTests {

        @Test
        @DisplayName("Empty string case returns empty set")
        void testEmptyString() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "");
            assertThat(parser.asSet().ofStrings()).isEqualTo(ImmutableSet.of());
        }

        @Test
        @DisplayName("Single case returns singleton set")
        void testSingleElement() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "RED");
            assertThat(parser.asSet().ofStrings()).isEqualTo(ImmutableSet.of("RED"));
        }

        @DisplayName("separates valid strings")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"RED:YELLOW:APPLE", "RED,YELLOW,APPLE"})
        void testColonSeparated(String value) {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, value);
            assertThat(parser.asSet().ofStrings()).isEqualTo(ImmutableSet.of("RED", "YELLOW", "APPLE"));
        }

        @Test
        @DisplayName("Set of strings with space is trimmed")
        void testSpaceIsTrimmed() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "RED,YELLOW, APPLE");
            assertThat(parser.asSet().ofStrings()).isEqualTo(ImmutableSet.of("RED", "YELLOW", "APPLE"));
        }

        @Test
        @DisplayName("Comma-separated string can contain colons")
        void testCommaSeparatedStringsWithColons() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "key1:value1,key2:value2");
            assertThat(parser.asSet().ofStrings()).isEqualTo(ImmutableSet.of("key1:value1", "key2:value2"));
        }

        @Test
        @DisplayName("numerical methods with numbers")
        void testNumericalSets() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "1,5,10,3");
            assertThat(parser.asSet().ofIntegers()).isEqualTo(ImmutableSet.of(1, 5, 10, 3));
            assertThat(parser.asSet().ofLongs()).isEqualTo(ImmutableSet.of(1L, 5L, 10L, 3L));
            assertThat(parser.asSet().ofDoubles()).isEqualTo(ImmutableSet.of(1D, 5D, 10D, 3D));
            assertThat(parser.asSet().ofIds()).isEqualTo(ImmutableSet.of(Id.create(1), Id.create(5), Id.create(10), Id.create(3)));
        }

        @Test
        @DisplayName("numerical methods with non-numbers throw exceptions")
        void testNumericalSetsThrow() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "RED,BLUE,APPLE,PEAR");
            assertThrowsWithKey(() -> parser.asSet().ofIntegers(), NumberFormatException.class);
            assertThrowsWithKey(() -> parser.asSet().ofLongs(), NumberFormatException.class);
            assertThrowsWithKey(() -> parser.asSet().ofDoubles(), NumberFormatException.class);
            assertThrowsWithKey(() -> parser.asSet().ofIds(), NumberFormatException.class);
        }

        @Test
        @DisplayName("enum values are parsed")
        void testEnumSets() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "RED,BLUE,BLUE,RED");
            ImmutableSet<Colours> expected = ImmutableSet.of(Colours.RED, Colours.BLUE, Colours.BLUE, Colours.RED);
            assertThat(parser.asSet().ofEnums(TestConfig.Colours.class)).isEqualTo(expected);
        }

        @Test
        @DisplayName("incorrect enum values throw exception")
        void testEnumSetsThrow() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "RED,BLUE,APPLE,PEAR");
            assertThrowsWithKey(() -> parser.asSet().ofEnums(TestConfig.Colours.class), IllegalArgumentException.class);
        }

        @Test
        @DisplayName("string Ids are conveted")
        void testStringIdSets() {
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "RED,BLUE,APPLE,PEAR");
            ImmutableSet<StringId<Object>> expected = ImmutableSet.of(
                    StringId.create("RED"),
                    StringId.create("BLUE"),
                    StringId.create("APPLE"),
                    StringId.create("PEAR")
            );
            assertThat(parser.asSet().ofStringIds()).isEqualTo(expected);
        }

        @Test
        @DisplayName("custom parser is used as expected")
        void callsParser() {
            String testValue = "ANOTHER TEST VALUE";
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, testValue);

            List<String> arguments = new ArrayList<>();
            TestClass testClass = new TestClass();
            Function<String, TestClass> parserFunction = value -> {
                arguments.add(value);
                return testClass;
            };
            assertThat(parser.asSet().withElementParser(parserFunction)).isEqualTo(ImmutableSet.of(testClass));
            assertThat(arguments.size()).isEqualTo(1);
            assertThat(arguments.get(0)).isEqualTo(testValue);
        }
    }

    @Nested
    @DisplayName("for Map values")
    class MapConfigTests {
        abstract class CommonMapTests<K, V> {

            static final String SIMPLE_CONFIG_VALUE = "1=True;2=False;3=False;4=False";

            abstract ImmutableMap<K, V> readMap(String configValue);

            abstract ImmutableMap<K, V> getDefaultMap();

            @Test
            @DisplayName("works with valid config")
            void withValidConfig() {
                ImmutableMap<K, V> map = readMap(SIMPLE_CONFIG_VALUE);
                assertThat(map).isEqualTo(getDefaultMap());
            }

            @Test
            @DisplayName("returns an empty map for an empty value")
            void configReturnsEmptyMap() {
                ImmutableMap<K, V> map = readMap((""));
                assertThat(map).isEmpty();
            }

            @Test
            @DisplayName("works with a trailing semicolon")
            void trailingSemiColonIsAllowed() {
                ImmutableMap<K, V> map = readMap("1=True;2=False;3=False;4=False;");
                assertThat(map).isEqualTo(getDefaultMap());
            }

            @Test
            @DisplayName("ignores empty entries")
            void emptyEntries() {
                ImmutableMap<K, V> map = readMap("1=True;;2=False;3=False;4=False");
                assertThat(map).isEqualTo(getDefaultMap());
            }

            @Test
            @DisplayName("trims whitespace")
            void trimsWhitespace() {
                ImmutableMap<K, V> map = readMap(" 1 = True ; 2 = False ; 3 = False ; 4 = False ");
                assertThat(map).isEqualTo(getDefaultMap());
            }

            @Test
            @DisplayName("entries with missing keys are ignored")
            void ignoresMissingKeys() {
                ImmutableMap<K, V> map = readMap("=False;1=True;2=False;3=False;4=False");
                assertThat(map).isEqualTo(getDefaultMap());
            }

            @Test
            @DisplayName("throws exception for duplicate keys")
            void duplicateKeysThrows() {
                assertThrowsWithKey(() -> readMap("1=False;1=True"), IllegalArgumentException.class);
            }
        }

        @Nested
        @DisplayName("read as a String map")
        class StringMapTests extends CommonMapTests<String, String> {
            @Override
            ImmutableMap<String, String> readMap(String configValue) {
                StrictValueParser parser = new StrictValueParser(TestConfig.FOO, configValue);
                return parser.asMap().ofStrings();
            }

            @Override
            ImmutableMap<String, String> getDefaultMap() {
                return ImmutableMap.of(
                        "1", "True",
                        "2", "False",
                        "3", "False",
                        "4", "False"
                );
            }

            @Test
            @DisplayName("ignores missing values")
            void handlesMissingValues() {
                ImmutableMap<String, String> map = readMap("1;2=");
                assertThat(map.get("1")).isNull();
                assertThat(map.get("2")).isEmpty();
            }
        }

        @Nested
        @DisplayName("read as a custom typed map")
        class TypedMapTests extends CommonMapTests<Integer, Boolean> {

            @Override
            ImmutableMap<Integer, Boolean> readMap(String configValue) {
                StrictValueParser parser = new StrictValueParser(TestConfig.FOO, configValue);
                return parser.asMap().withKeyAndValueParsers(ConfigParsers::parseInt, ConfigParsers::parseBoolean);
            }

            @Override
            ImmutableMap<Integer, Boolean> getDefaultMap() {
                return ImmutableMap.of(
                        1, true,
                        2, false,
                        3, false,
                        4, false
                );
            }

            @Test
            @DisplayName("uses listParser as valueParser")
            void mapOfLists() {
                StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "1=4,5;2=6,7");
                ImmutableMap<Integer, ImmutableList<Integer>> map = parser.asMap().withKeyAndValueParsers(Integer::parseInt, ConfigParsers.getListOfIntegers());
                assertThat(map).containsOnlyKeys(1, 2);
                assertThat(map).containsEntry(1, ImmutableList.of(4, 5));
                assertThat(map).containsEntry(2, ImmutableList.of(6, 7));
            }

            @Test
            @DisplayName("applies valueParser to empty string for missing values")
            void handlesMissingValues() {
                StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "1;2=");

                Function<String, String> identityFunctionWithAssertionStringIsEmpty = v -> {
                    assertThat(v).isEmpty();
                    return v;
                };
                ImmutableMap<Integer, String> map = parser.asMap().withKeyAndValueParsers(Integer::valueOf, identityFunctionWithAssertionStringIsEmpty);
                assertThat(map).containsOnlyKeys(2);
                assertThat(map).containsEntry(2, "");
            }

            @Test
            @DisplayName("throws exceptions generated by key parser function")
            void throwsKeyParserExceptions() {
                StrictValueParser parser = new StrictValueParser(TestConfig.FOO, SIMPLE_CONFIG_VALUE);

                assertThatThrownBy(() -> parser.asMap().withKeyAndValueParsers(s -> Failer.fail("Boom"), Boolean::parseBoolean))
                        .hasRootCauseMessage("Boom")
                        .hasMessageContaining("TestConfig.FOO");
            }

            @Test
            @DisplayName("throws exceptions generated by value parser function")
            void throwsValueParserExceptions() {
                StrictValueParser parser = new StrictValueParser(TestConfig.FOO, SIMPLE_CONFIG_VALUE);

                assertThatThrownBy(() -> parser.asMap().withKeyAndValueParsers(Integer::valueOf, s -> Failer.fail("Boom")))
                        .hasRootCauseMessage("Boom")
                        .hasMessageContaining("TestConfig.FOO");
            }
        }
    }

    @Nested
    @DisplayName("for Multimap values")
    class MultimapConfigTests {
        abstract class CommonMultimapTests<K, V> {

            static final String SIMPLE_CONFIG_VALUE = "1=True;1=False;2=False;3=False;4=False";

            abstract ImmutableSetMultimap<K, V> readMultimap(String configValue);

            abstract ImmutableSetMultimap<K, V> getDefaultMultimap();

            @Test
            @DisplayName("works with valid config")
            void withValidConfig() {
                ImmutableMultimap<K, V> multimap = readMultimap(SIMPLE_CONFIG_VALUE);
                assertThat(multimap).isEqualTo(getDefaultMultimap());
            }

            @Test
            @DisplayName("returns an empty map for an empty value")
            void configReturnsEmptyMap() {
                ImmutableMultimap<K, V> multimap = readMultimap((""));
                assertThat(multimap.asMap()).isEmpty();
            }

            @Test
            @DisplayName("works with a trailing semicolon")
            void trailingSemiColonIsAllowed() {
                ImmutableMultimap<K, V> multimap = readMultimap("1=True;1=False;2=False;3=False;4=False;");
                assertThat(multimap).isEqualTo(getDefaultMultimap());
            }

            @Test
            @DisplayName("works with permutations")
            void permutationIsAllowed() {
                ImmutableMultimap<K, V> multimap = readMultimap("1=True;2=False;3=False;1=False;4=False");
                assertThat(multimap).isEqualTo(getDefaultMultimap());
            }

            @Test
            @DisplayName("ignores empty entries")
            void emptyEntries() {
                ImmutableMultimap<K, V> multimap = readMultimap("1=True;1=False;;2=False;3=False;4=False");
                assertThat(multimap).isEqualTo(getDefaultMultimap());
            }

            @Test
            @DisplayName("trims whitespace")
            void trimsWhitespace() {
                ImmutableMultimap<K, V> multimap = readMultimap(" 1 = True ; 1 = False ; 2 = False ; 3 = False ; 4 = False ");
                assertThat(multimap).isEqualTo(getDefaultMultimap());
            }

            @Test
            @DisplayName("entries with missing keys are ignored")
            void ignoresMissingKeys() {
                ImmutableMultimap<K, V> multimap = readMultimap("=False;1=True;1=False;2=False;3=False;4=False");
                assertThat(multimap).isEqualTo(getDefaultMultimap());
            }
        }

        @Nested
        @DisplayName("read as a String map")
        class StringMultimapTests extends CommonMultimapTests<String, String> {

            @Override
            ImmutableSetMultimap<String, String> readMultimap(String configValue) {
                return new StrictValueParser(TestConfig.FOO, configValue).asSetMultimap().ofStrings();
            }

            @Override
            ImmutableSetMultimap<String, String> getDefaultMultimap() {
                return ImmutableSetMultimap.of(
                        "1", "True",
                        "1", "False",
                        "2", "False",
                        "3", "False",
                        "4", "False"
                );
            }

            @Test
            @DisplayName("ignores missing values")
            void handlesMissingValues() {
                ImmutableMultimap<String, String> multimap = readMultimap("1;2=");
                assertThat(multimap.asMap().get("1")).isNull();
                assertThat(new ArrayList<>(multimap.asMap().get("2")).get(0)).isEmpty();
            }
        }

        @Nested
        @DisplayName("read as a typed multimap")
        class TypedMapTests extends CommonMultimapTests<Integer, Boolean> {

            @Override
            ImmutableSetMultimap<Integer, Boolean> readMultimap(String configValue) {
                return new StrictValueParser(TestConfig.FOO, configValue).asSetMultimap().withKeyAndValueParsers(ConfigParsers::parseInt, ConfigParsers::parseBoolean);
            }

            @Override
            ImmutableSetMultimap<Integer, Boolean> getDefaultMultimap() {
                return ImmutableSetMultimap.of(
                        1, true,
                        1, false,
                        2, false,
                        3, false,
                        4, false
                );
            }

            @Test
            @DisplayName("can use listParser as valueParser")
            void multimapOfLists() {
                StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "1=4,5;1=8,9;2=6,7");
                ImmutableSetMultimap<Integer, ImmutableSet<Integer>> multimap = parser.asSetMultimap().withKeyAndValueParsers(Integer::parseInt, ConfigParsers.getSetOfIntegers());
                assertThat(multimap.asMap()).containsOnlyKeys(1, 2);
                assertThat(multimap.asMap()).containsEntry(1, ImmutableSet.of(ImmutableSet.of(4, 5), ImmutableSet.of(8, 9)));
                assertThat(multimap.asMap()).containsEntry(2, ImmutableSet.of(ImmutableSet.of(6, 7)));
            }

            @Test
            @DisplayName("applies valueParser to empty string for missing values")
            void handlesMissingValues() {
                StrictValueParser parser = new StrictValueParser(TestConfig.FOO, "1;2=");
                Function<String, String> identityFunctionWithAssertionStringIsEmpty = v -> {
                    assertThat(v).isEmpty();
                    return v;
                };
                ImmutableSetMultimap<Integer, String> map = parser.asSetMultimap().withKeyAndValueParsers(Integer::valueOf, identityFunctionWithAssertionStringIsEmpty);
                assertThat(map.asMap()).containsOnlyKeys(2);
                assertThat(map.containsEntry(2, "")).isTrue();
            }

            @Test
            @DisplayName("throws exceptions generated by key parser function")
            void throwsKeyParserExceptions() {
                StrictValueParser parser = new StrictValueParser(TestConfig.FOO, SIMPLE_CONFIG_VALUE);

                assertThatThrownBy(() -> parser.asSetMultimap().withKeyAndValueParsers(s -> Failer.fail("Boom"), Boolean::parseBoolean))
                        .hasRootCauseMessage("Boom")
                        .hasMessageContaining("TestConfig.FOO");
            }

            @Test
            @DisplayName("throws exceptions generated by value parser function")
            void throwsValueParserExceptions() {
                StrictValueParser parser = new StrictValueParser(TestConfig.FOO, SIMPLE_CONFIG_VALUE);

                assertThatThrownBy(() -> parser.asSetMultimap().withKeyAndValueParsers(Integer::valueOf, s -> Failer.fail("Boom")))
                        .hasRootCauseMessage("Boom")
                        .hasMessageContaining("TestConfig.FOO");
            }
        }
    }

    @Nested
    @DisplayName("for custom Object values")
    class CustomParserTests {
        @Test
        @DisplayName("passes value into parser and returns exact object")
        void callsParser() {
            String testValue = "ANOTHER TEST VALUE";
            StrictValueParser parser = new StrictValueParser(TestConfig.FOO, testValue);

            List<String> arguments = new ArrayList<>();
            TestClass testClass = new TestClass();
            Function<String, TestClass> parserFunction = value -> {
                arguments.add(value);
                return testClass;
            };
            assertThat(parser.withCustomParser(parserFunction)).isSameAs(testClass);
            assertThat(arguments.size()).isEqualTo(1);
            assertThat(arguments.get(0)).isSameAs(testValue);
        }
    }

    private static void assertThrowsWithKey(ThrowingCallable throwingRunnable, Class<? extends Exception> causingClass) {
        assertThatThrownBy(throwingRunnable)
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(causingClass)
                .hasMessageContaining("TestConfig.FOO");
    }

    private static final class TestClass {
    }
}
