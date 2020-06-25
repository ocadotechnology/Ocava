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
package com.ocadotechnology.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.common.collect.ImmutableList;
import com.ocadotechnology.config.TestConfig.Colours;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.StringId;
import com.ocadotechnology.physics.units.LengthUnit;

public class StrictValueParserTest {

    @Nested
    @DisplayName("for String values")
    class StringParserTests {
        @DisplayName("returns non-empty value")
        @Test
        void returnsValue() {
            String testValue = "A TEST VALUE";
            StrictValueParser parser = new StrictValueParser(testValue);
            assertThat(parser.asString()).isEqualTo(testValue);
        }

        @DisplayName("returns empty value")
        @Test
        void returnsEmptyValue() {
            String testValue = "";
            StrictValueParser parser = new StrictValueParser(testValue);
            assertThat(parser.asString()).isEqualTo(testValue);
        }
    }
    
    @Nested
    @DisplayName("for Boolean values")
    class BooleanParserTests {

        @DisplayName("returns true")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"true", "True", "TRUE", "tRUe"})
        void allowsTrueValues(String value) {
            StrictValueParser parser = new StrictValueParser(value);
            assertThat(parser.asBoolean()).isTrue();
        }

        @DisplayName("returns false")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"false", "False", "FALSE", "fAlSe"})
        void allowsFalseValues(String value) {
            StrictValueParser parser = new StrictValueParser(value);
            assertThat(parser.asBoolean()).isFalse();
        }

        @DisplayName("throws an exception for typo")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"fa lse", "ture", "yes", "0", "1"})
        void throwsExceptionForMisspelledValue(String value) {
            StrictValueParser parser = new StrictValueParser(value);
            assertThatThrownBy(parser::asBoolean).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("for Integer values")
    class IntegerParserTests {

        @DisplayName("returns the integer value")
        void returnIntegerValue() {
            StrictValueParser parser = new StrictValueParser("42");
            assertThat(parser.asInt()).isEqualTo(42);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            StrictValueParser parser = new StrictValueParser("-2");
            assertThat(parser.asInt()).isEqualTo(-2);
        }

        @DisplayName("returns Integer.MAX_VALUE")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"max", "MAX", "mAx"})
        void testMaxValues(String value) {
            StrictValueParser parser = new StrictValueParser(value);
            assertThat(parser.asInt()).isEqualTo(Integer.MAX_VALUE);
        }

        @DisplayName("returns Integer.MIN_VALUE")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"min", "MIN", "MiN"})
        void testMinValues(String value) {
            StrictValueParser parser = new StrictValueParser(value);
            assertThat(parser.asInt()).isEqualTo(Integer.MIN_VALUE);
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            StrictValueParser parser = new StrictValueParser("FAIL");
            assertThatThrownBy(parser::asInt).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception for a non-integer number")
        void throwsExceptionForDecimalNumber() {
            StrictValueParser parser = new StrictValueParser("2.7182");
            assertThatThrownBy(parser::asInt).isInstanceOf(NumberFormatException.class);
        }

        @DisplayName("throws an exception for an invalid number format")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"1e3", "1x10^3"})
        void throwsExceptionForInvalidNumberFormat(String value) {
            StrictValueParser parser = new StrictValueParser(value);
            assertThatThrownBy(parser::asInt).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception for an overly-large number")
        void throwsExceptionForLargeNumber() {
            StrictValueParser parser = new StrictValueParser(String.valueOf(Long.MAX_VALUE));
            assertThatThrownBy(parser::asInt).isInstanceOf(NumberFormatException.class);
        }
    }

    @Nested
    @DisplayName("for Long values")
    class LongParserTests {

        @Test
        @DisplayName("returns the long value")
        void returnLongValue() {
            StrictValueParser parser = new StrictValueParser("42");
            assertThat(parser.asLong()).isEqualTo(42);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            StrictValueParser parser = new StrictValueParser("-2");
            assertThat(parser.asLong()).isEqualTo(-2);
        }

        @DisplayName("returns Long.MAX_VALUE")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"max", "MAX", "mAx"})
        void testMaxValues(String value) {
            StrictValueParser parser = new StrictValueParser(value);
            assertThat(parser.asLong()).isEqualTo(Long.MAX_VALUE);
        }

        @DisplayName("returns Long.MIN_VALUE")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"min", "MIN", "MiN"})
        void testMinValues(String value) {
            StrictValueParser parser = new StrictValueParser(value);
            assertThat(parser.asLong()).isEqualTo(Long.MIN_VALUE);
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            StrictValueParser parser = new StrictValueParser("FAIL");
            assertThatThrownBy(parser::asLong).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception for a non-integer number")
        void throwsExceptionForDecimalNumber() {
            StrictValueParser parser = new StrictValueParser("2.7182");
            assertThatThrownBy(parser::asLong).isInstanceOf(NumberFormatException.class);
        }

        @DisplayName("throws an exception for an invalid number format")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"1e3", "1x10^3"})
        void throwsExceptionForInvalidNumberFormat(String value) {
            StrictValueParser parser = new StrictValueParser(value);
            assertThatThrownBy(parser::asLong).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception for an overly-large number")
        void throwsExceptionForLargeNumber() {
            StrictValueParser parser = new StrictValueParser(Long.MAX_VALUE + "00");
            assertThatThrownBy(parser::asLong).isInstanceOf(NumberFormatException.class);
        }
    }

    @Nested
    @DisplayName("for Double values")
    class DoubleParserTests {

        @Test
        @DisplayName("returns the double value")
        void returnDoubleValue() {
            StrictValueParser parser = new StrictValueParser("2.7182");
            assertThat(parser.asDouble()).isEqualTo(2.7182);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            StrictValueParser parser = new StrictValueParser("-2.7182");
            assertThat(parser.asDouble()).isEqualTo(-2.7182);
        }

        @DisplayName("allows all valid number formats")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"1.3e3"})
        void allowsValidNumberFormat(String value) {
            StrictValueParser parser = new StrictValueParser(value);
            assertThat(parser.asDouble()).isEqualTo(1300);
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            StrictValueParser parser = new StrictValueParser("FAIL");
            assertThatThrownBy(parser::asDouble).isInstanceOf(NumberFormatException.class);
        }

        @DisplayName("throws an exception for an invalid number format")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"1x10^3"})
        void throwsExceptionForInvalidNumberFormat(String value) {
            StrictValueParser parser = new StrictValueParser(value);
            assertThatThrownBy(parser::asDouble).isInstanceOf(NumberFormatException.class);
        }
    }

    @Nested
    @DisplayName("for Enum values")
    class EnumConfigTests {

        @Test
        @DisplayName("parses correct config")
        void parseCorrectConfig() {
            StrictValueParser parser = new StrictValueParser("RED");
            assertThat(parser.asEnum(TestConfig.Colours.class)).isEqualTo(TestConfig.Colours.RED);
        }

        @Test
        @DisplayName("throws IllegalArgumentException for incorrect value")
        void throwsException() {
            StrictValueParser parser = new StrictValueParser("REDDER");
            assertThatThrownBy(() -> parser.asEnum(TestConfig.Colours.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No enum constant");
        }
    }

    @Nested
    @DisplayName("for Time values")
    class TimeConfigTests {

        @Test
        @DisplayName("returns the long value")
        void returnTimeValue() {
            StrictValueParser parser = new StrictValueParser("2.3, SECONDS", TimeUnit.SECONDS, null);
            assertThat(parser.asTime()).isEqualTo(2);
            assertThat(parser.asFractionalTime()).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the long value when units are not specified")
        void returnTimeValueWithNoSpecifiedUnits() {
            StrictValueParser parser = new StrictValueParser("2.3", TimeUnit.SECONDS, null);
            assertThat(parser.asTime()).isEqualTo(2);
            assertThat(parser.asFractionalTime()).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the scaled value")
        void returnScaledTimeValue() {
            StrictValueParser parser = new StrictValueParser("2.11, MINUTES", TimeUnit.SECONDS, null);
            assertThat(parser.asTime()).isEqualTo(127);
            assertThat(parser.asFractionalTime()).isEqualTo(126.6);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            StrictValueParser parser = new StrictValueParser("-2.3, SECONDS", TimeUnit.SECONDS, null);
            assertThat(parser.asTime()).isEqualTo(-2);
            assertThat(parser.asFractionalTime()).isEqualTo(-2.3);
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            StrictValueParser parser = new StrictValueParser("FAIL, SECONDS", TimeUnit.SECONDS, null);
            assertThatThrownBy(parser::asTime).isInstanceOf(NumberFormatException.class);
            assertThatThrownBy(parser::asFractionalTime).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            StrictValueParser parser = new StrictValueParser("2, SECONDS, SECONDS", TimeUnit.SECONDS, null);
            assertThatThrownBy(parser::asTime).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(parser::asFractionalTime).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            StrictValueParser parser = new StrictValueParser("2, ORANGES", TimeUnit.SECONDS, null);
            assertThatThrownBy(parser::asTime).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(parser::asFractionalTime).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Throws an exception if time unit not set")
        void throwsExceptionWhenUnitNotSet() {
            StrictValueParser parser = new StrictValueParser("2, SECONDS", null, null);
            assertThatThrownBy(parser::asTime).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(parser::asFractionalTime).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("for Duration values")
    class DurationConfigTests {

        @Test
        @DisplayName("returns a Duration for an integer value")
        void returnDurationForInt() {
            StrictValueParser parser = new StrictValueParser("2, MILLISECONDS");
            assertThat(parser.asDuration()).isEqualTo(Duration.ofMillis(2));
        }

        @Test
        @DisplayName("returns a Duration for a fractional value")
        void returnDurationForFraction() {
            StrictValueParser parser = new StrictValueParser("2.3, MILLISECONDS");
            assertThat(parser.asDuration()).isEqualTo(Duration.ofNanos(2_300_000));
        }

        @Test
        @DisplayName("returns a Duration with units of seconds when units are not specified")
        void returnDurationInSecondsWhenNoSpecifiedUnits() {
            StrictValueParser parser = new StrictValueParser("2.3");
            assertThat(parser.asDuration()).isEqualTo(Duration.ofMillis(2_300));
        }

        @Test
        @DisplayName("handles a 0 value")
        void zeroValue() {
            StrictValueParser parser = new StrictValueParser("0");
            assertThat(parser.asDuration()).isEqualTo(Duration.ofSeconds(0));
        }

        @Test
        @DisplayName("rounds to the nearest nanosecond - below")
        void roundToNanoBelow() {
            StrictValueParser parser = new StrictValueParser("0.1,NANOSECONDS");
            assertThat(parser.asDuration()).isEqualTo(Duration.ofNanos(0));
        }

        @Test
        @DisplayName("rounds to the nearest nanosecond - above")
        void roundToNanoAbove() {
            StrictValueParser parser = new StrictValueParser("0.6,NANOSECONDS");
            assertThat(parser.asDuration()).isEqualTo(Duration.ofNanos(1));
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            StrictValueParser parser = new StrictValueParser("-2, SECONDS");
            assertThat(parser.asDuration()).isEqualTo(Duration.ofSeconds(-2));
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            StrictValueParser parser = new StrictValueParser("FAIL, SECONDS");
            assertThatThrownBy(parser::asDuration).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            StrictValueParser parser = new StrictValueParser("2, SECONDS, SECONDS");
            assertThatThrownBy(parser::asDuration).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            StrictValueParser parser = new StrictValueParser("2, ORANGES");
            assertThatThrownBy(parser::asDuration).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("for Length values")
    class LengthConfigTests {

        @Test
        @DisplayName("returns the double value")
        void returnValue() {
            StrictValueParser parser = new StrictValueParser("2.3, METERS", null, LengthUnit.METERS);
            assertThat(parser.asLength()).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the double value when units are not specified")
        void returnValueWithNoSpecifiedUnits() {
            StrictValueParser parser = new StrictValueParser("2.3", null, LengthUnit.METERS);
            assertThat(parser.asLength()).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the scaled value km -> m")
        void returnInputScaledValue() {
            StrictValueParser parser = new StrictValueParser("2.3, KILOMETERS", null, LengthUnit.METERS);
            assertThat(parser.asLength()).isEqualTo(2300);
        }

        @Test
        @DisplayName("returns the scaled value m -> km")
        void returnOutputScaledValue() {
            StrictValueParser parser = new StrictValueParser("2.3, METERS", null, LengthUnit.KILOMETERS);
            assertThat(parser.asLength()).isEqualTo(0.0023);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            StrictValueParser parser = new StrictValueParser("-2.3, METERS", null, LengthUnit.METERS);
            assertThat(parser.asLength()).isEqualTo(-2.3);
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            StrictValueParser parser = new StrictValueParser("FAIL, METERS", null, LengthUnit.METERS);
            assertThatThrownBy(parser::asLength).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            StrictValueParser parser = new StrictValueParser("2, METERS, METERS", null, LengthUnit.METERS);
            assertThatThrownBy(parser::asLength).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            StrictValueParser parser = new StrictValueParser("2, ORANGES", null, LengthUnit.METERS);
            assertThatThrownBy(parser::asLength).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Throws an exception if length unit not set")
        void throwsExceptionWhenUnitNotSet() {
            StrictValueParser parser = new StrictValueParser("FAIL, METERS", null, null);
            assertThatThrownBy(parser::asLength).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("for Speed values")
    class SpeedConfigTests {

        @Test
        @DisplayName("returns the double value when units are specified")
        void returnValue() {
            StrictValueParser parser = new StrictValueParser("2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asSpeed()).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the double value when units are not specified")
        void returnValueWithDefaultUnits() {
            StrictValueParser parser = new StrictValueParser("2.3", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asSpeed()).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the scaled value ms -> s")
        void returnTimeInputScaledTimeValue() {
            StrictValueParser parser = new StrictValueParser("2.3, METERS, MILLISECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asSpeed()).isEqualTo(2300);
        }

        @Test
        @DisplayName("returns the scaled value s -> ms")
        void returnTimeOutputScaledTimeValue() {
            StrictValueParser parser = new StrictValueParser("2.3, METERS, SECONDS", TimeUnit.MILLISECONDS, LengthUnit.METERS);
            assertThat(parser.asSpeed()).isEqualTo(0.0023);
        }

        @Test
        @DisplayName("returns the scaled value km -> m")
        void returnLengthInputScaledTimeValue() {
            StrictValueParser parser = new StrictValueParser("2.3, KILOMETERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asSpeed()).isEqualTo(2300);
        }

        @Test
        @DisplayName("returns the scaled value m -> km")
        void returnLengthOutputScaledTimeValue() {
            StrictValueParser parser = new StrictValueParser("2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.KILOMETERS);
            assertThat(parser.asSpeed()).isEqualTo(0.0023);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            StrictValueParser parser = new StrictValueParser("-2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asSpeed()).isEqualTo(-2.3);
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            StrictValueParser parser = new StrictValueParser("FAIL, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThatThrownBy(parser::asSpeed).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            StrictValueParser parser = new StrictValueParser("2, METERS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThatThrownBy(parser::asSpeed).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            StrictValueParser parser = new StrictValueParser("2, METERS, ORANGES", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThatThrownBy(parser::asSpeed).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Throws an exception if length unit not set")
        void throwsExceptionWhenLengthUnitNotSet() {
            StrictValueParser parser = new StrictValueParser("2.3, METERS, SECONDS", TimeUnit.SECONDS, null);
            assertThatThrownBy(parser::asSpeed).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Throws an exception if time unit not set")
        void throwsExceptionWhenTimeUnitNotSet() {
            StrictValueParser parser = new StrictValueParser("2.3, METERS, SECONDS", null, LengthUnit.METERS);
            assertThatThrownBy(parser::asSpeed).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("for Acceleration values")
    class AccelerationConfigTests {

        @Test
        @DisplayName("returns the double value with units specified")
        void returnValue() {
            StrictValueParser parser = new StrictValueParser("2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asAcceleration()).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the double value with no units specified")
        void returnValueWithDefaultUnits() {
            StrictValueParser parser = new StrictValueParser("2.3", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asAcceleration()).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the scaled value ms -> s")
        void returnTimeInputScaledValue() {
            StrictValueParser parser = new StrictValueParser("2.3, METERS, MILLISECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asAcceleration()).isEqualTo(2300_000);
        }

        @Test
        @DisplayName("returns the scaled value s -> ms")
        void returnTimeOutputScaledValue() {
            StrictValueParser parser = new StrictValueParser("2.3, METERS, SECONDS", TimeUnit.MILLISECONDS, LengthUnit.METERS);
            assertThat(parser.asAcceleration()).isEqualTo(0.0000023);
        }

        @Test
        @DisplayName("returns the scaled value km -> m")
        void returnLengthInputScaledValue() {
            StrictValueParser parser = new StrictValueParser("2.3, KILOMETERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asAcceleration()).isEqualTo(2300);
        }

        @Test
        @DisplayName("returns the scaled value m -> km")
        void returnLengthOutputScaledValue() {
            StrictValueParser parser = new StrictValueParser("2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.KILOMETERS);
            assertThat(parser.asAcceleration()).isEqualTo(0.0023);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            StrictValueParser parser = new StrictValueParser("-2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asAcceleration()).isEqualTo(-2.3);
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            StrictValueParser parser = new StrictValueParser("FAIL, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThatThrownBy(parser::asAcceleration).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            StrictValueParser parser = new StrictValueParser("2, METERS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThatThrownBy(parser::asAcceleration).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            StrictValueParser parser = new StrictValueParser("2, METERS, ORANGES", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThatThrownBy(parser::asAcceleration).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Throws an exception if length unit not set")
        void throwsExceptionWhenLengthUnitNotSet() {
            StrictValueParser parser = new StrictValueParser("2.3, METERS, SECONDS", TimeUnit.SECONDS, null);
            assertThatThrownBy(parser::asAcceleration).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Throws an exception if time unit not set")
        void throwsExceptionWhenTimeUnitNotSet() {
            StrictValueParser parser = new StrictValueParser("2.3, METERS, SECONDS", null, LengthUnit.METERS);
            assertThatThrownBy(parser::asAcceleration).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("test Lists")
    class ListOfStringsTests {

        @Test
        @DisplayName("Empty string case returns empty list")
        void testEmptyString() {
            StrictValueParser parser = new StrictValueParser("");
            assertThat(parser.asList().ofStrings()).isEqualTo(ImmutableList.of());
        }

        @Test
        @DisplayName("Single case returns singleton list")
        void testSingleElement() {
            StrictValueParser parser = new StrictValueParser("RED");
            assertThat(parser.asList().ofStrings()).isEqualTo(ImmutableList.of("RED"));
        }

        @DisplayName("separates valid strings")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"RED:YELLOW:APPLE", "RED,YELLOW,APPLE"})
        void testColonSeparated(String value) {
            StrictValueParser parser = new StrictValueParser(value);
            assertThat(parser.asList().ofStrings()).isEqualTo(ImmutableList.of("RED", "YELLOW", "APPLE"));
        }

        @Test
        @DisplayName("List of strings with space is trimmed")
        void testSpaceIsTrimmed() {
            StrictValueParser parser = new StrictValueParser("RED,YELLOW, APPLE");
            assertThat(parser.asList().ofStrings()).isEqualTo(ImmutableList.of("RED", "YELLOW", "APPLE"));
        }

        @Test
        @DisplayName("Comma-separated string can contain colons")
        void testCommaSeparatedStringsWithColons() {
            StrictValueParser parser = new StrictValueParser("key1:value1,key2:value2");
            assertThat(parser.asList().ofStrings()).isEqualTo(ImmutableList.of("key1:value1", "key2:value2"));
        }

        @Test
        @DisplayName("numerical methods with numbers")
        void testNumericalLists() {
            StrictValueParser parser = new StrictValueParser("1,5,10,3");
            assertThat(parser.asList().ofIntegers()).isEqualTo(ImmutableList.of(1, 5, 10, 3));
            assertThat(parser.asList().ofLongs()).isEqualTo(ImmutableList.of(1L, 5L, 10L, 3L));
            assertThat(parser.asList().ofDoubles()).isEqualTo(ImmutableList.of(1D, 5D, 10D, 3D));
            assertThat(parser.asList().ofIds()).isEqualTo(ImmutableList.of(Id.create(1), Id.create(5), Id.create(10), Id.create(3)));
        }

        @Test
        @DisplayName("numerical methods with non-numbers throw exceptions")
        void testNumericalListsThrow() {
            StrictValueParser parser = new StrictValueParser("RED,BLUE,APPLE,PEAR");
            assertThatThrownBy(() -> parser.asList().ofIntegers()).isInstanceOf(NumberFormatException.class);
            assertThatThrownBy(() -> parser.asList().ofLongs()).isInstanceOf(NumberFormatException.class);
            assertThatThrownBy(() -> parser.asList().ofDoubles()).isInstanceOf(NumberFormatException.class);
            assertThatThrownBy(() -> parser.asList().ofIds()).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("enum values are parsed")
        void testEnumLists() {
            StrictValueParser parser = new StrictValueParser("RED,BLUE,BLUE,RED");
            ImmutableList<Colours> expected = ImmutableList.of(Colours.RED, Colours.BLUE, Colours.BLUE, Colours.RED);
            assertThat(parser.asList().ofEnums(TestConfig.Colours.class)).isEqualTo(expected);
        }

        @Test
        @DisplayName("incorrect enum values throw exception")
        void testEnumListsThrow() {
            StrictValueParser parser = new StrictValueParser("RED,BLUE,APPLE,PEAR");
            assertThatThrownBy(() -> parser.asList().ofEnums(TestConfig.Colours.class)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("string Ids are conveted")
        void testStringIdLists() {
            StrictValueParser parser = new StrictValueParser("RED,BLUE,APPLE,PEAR");
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
            StrictValueParser parser = new StrictValueParser(testValue);

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
    @DisplayName("for custom Object values")
    class CustomParserTests {
        @Test
        @DisplayName("passes value into parser and returns exact object")
        void callsParser() {
            String testValue = "ANOTHER TEST VALUE";
            StrictValueParser parser = new StrictValueParser(testValue);

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

    private static final class TestClass{}
}
