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
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.math.DoubleMath;
import com.ocadotechnology.config.TestConfig.Colours;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.StringId;
import com.ocadotechnology.physics.units.LengthUnit;
import com.ocadotechnology.validation.Failer;

public class OptionalValueParserTest {

    @Nested
    @DisplayName("for String values")
    class StringParserTests {
        @Test
        @DisplayName("returns non-empty value")
        void returnsValue() {
            String testValue = "A TEST VALUE";
            OptionalValueParser parser = new OptionalValueParser(testValue);
            assertThat(parser.asString()).isEqualTo(Optional.of(testValue));
        }

        @Test
        @DisplayName("returns empty for empty value")
        void returnsEmptyValue() {
            String testValue = "";
            OptionalValueParser parser = new OptionalValueParser(testValue);
            assertThat(parser.asString().isPresent()).isFalse();
        }
    }

    @Nested
    @DisplayName("for Boolean values")
    class BooleanParserTests {
        @Test
        @DisplayName("returns empty for empty value")
        void returnsEmptyValue() {
            String testValue = "";
            OptionalValueParser parser = new OptionalValueParser(testValue);
            assertThat(parser.asBoolean().isPresent()).isFalse();
        }

        @DisplayName("returns true")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"true", "True", "TRUE", "tRUe"})
        void allowsTrueValues(String value) {
            OptionalValueParser parser = new OptionalValueParser(value);
            assertThat(parser.asBoolean()).isEqualTo(Optional.of(true));
        }

        @DisplayName("returns false")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"false", "False", "FALSE", "fAlSe"})
        void allowsFalseValues(String value) {
            OptionalValueParser parser = new OptionalValueParser(value);
            assertThat(parser.asBoolean()).isEqualTo(Optional.of(false));
        }

        @DisplayName("throws an exception for typo")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"fa lse", "ture", "yes", "0", "1"})
        void throwsExceptionForMisspelledValue(String value) {
            OptionalValueParser parser = new OptionalValueParser(value);
            assertThatThrownBy(parser::asBoolean).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("for Integer values")
    class IntegerParserTests {
        @Test
        @DisplayName("returns empty for empty value")
        void returnsEmptyValue() {
            String testValue = "";
            OptionalValueParser parser = new OptionalValueParser(testValue);
            assertThat(parser.asInt().isPresent()).isFalse();
        }

        @Test
        @DisplayName("returns the integer value")
        void returnIntegerValue() {
            OptionalValueParser parser = new OptionalValueParser("42");
            assertThat(parser.asInt()).isEqualTo(OptionalInt.of(42));
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            OptionalValueParser parser = new OptionalValueParser("-2");
            assertThat(parser.asInt()).isEqualTo(OptionalInt.of(-2));
        }

        @DisplayName("returns Integer.MAX_VALUE")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"max", "MAX", "mAx"})
        void testMaxValues(String value) {
            OptionalValueParser parser = new OptionalValueParser(value);
            assertThat(parser.asInt()).isEqualTo(OptionalInt.of(Integer.MAX_VALUE));
        }

        @DisplayName("returns Integer.MIN_VALUE")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"min", "MIN", "MiN"})
        void testMinValues(String value) {
            OptionalValueParser parser = new OptionalValueParser(value);
            assertThat(parser.asInt()).isEqualTo(OptionalInt.of(Integer.MIN_VALUE));
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            OptionalValueParser parser = new OptionalValueParser("FAIL");
            assertThatThrownBy(parser::asInt).isInstanceOf(NumberFormatException.class);
        }

        @DisplayName("throws an exception for an invalid number format")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"1e3", "1x10^3"})
        void throwsExceptionForInvalidNumberFormat(String value) {
            OptionalValueParser parser = new OptionalValueParser(value);
            assertThatThrownBy(parser::asInt).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception for a non-integer number")
        void throwsExceptionForDecimalNumber() {
            OptionalValueParser parser = new OptionalValueParser("2.7182");
            assertThatThrownBy(parser::asInt).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception for an overly-large number")
        void throwsExceptionForLargeNumber() {
            OptionalValueParser parser = new OptionalValueParser(String.valueOf(Long.MAX_VALUE));
            assertThatThrownBy(parser::asInt).isInstanceOf(NumberFormatException.class);
        }
    }

    @Nested
    @DisplayName("for Long values")
    class LongParserTests {
        @Test
        @DisplayName("returns empty for empty value")
        void returnsEmptyValue() {
            String testValue = "";
            OptionalValueParser parser = new OptionalValueParser(testValue);
            assertThat(parser.asLong().isPresent()).isFalse();
        }

        @Test
        @DisplayName("returns the long value")
        void returnLongValue() {
            OptionalValueParser parser = new OptionalValueParser("42");
            assertThat(parser.asLong()).isEqualTo(OptionalLong.of(42));
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            OptionalValueParser parser = new OptionalValueParser("-2");
            assertThat(parser.asLong()).isEqualTo(OptionalLong.of(-2));
        }

        @DisplayName("returns Long.MAX_VALUE")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"max", "MAX", "mAx"})
        void testMaxValues(String value) {
            OptionalValueParser parser = new OptionalValueParser(value);
            assertThat(parser.asLong()).isEqualTo(OptionalLong.of(Long.MAX_VALUE));
        }

        @DisplayName("returns Long.MIN_VALUE")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"min", "MIN", "MiN"})
        void testMinValues(String value) {
            OptionalValueParser parser = new OptionalValueParser(value);
            assertThat(parser.asLong()).isEqualTo(OptionalLong.of(Long.MIN_VALUE));
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            OptionalValueParser parser = new OptionalValueParser("FAIL");
            assertThatThrownBy(parser::asLong).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception for a non-integer number")
        void throwsExceptionForDecimalNumber() {
            OptionalValueParser parser = new OptionalValueParser("2.7182");
            assertThatThrownBy(parser::asLong).isInstanceOf(NumberFormatException.class);
        }

        @DisplayName("throws an exception for an invalid number format")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"1e3", "1x10^3"})
        void throwsExceptionForInvalidNumberFormat(String value) {
            OptionalValueParser parser = new OptionalValueParser(value);
            assertThatThrownBy(parser::asLong).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception for an overly-large number")
        void throwsExceptionForLargeNumber() {
            OptionalValueParser parser = new OptionalValueParser(Long.MAX_VALUE + "00");
            assertThatThrownBy(parser::asLong).isInstanceOf(NumberFormatException.class);
        }
    }

    @Nested
    @DisplayName("for Double values")
    class DoubleParserTests {
        @Test
        @DisplayName("returns empty for empty value")
        void returnsEmptyValue() {
            String testValue = "";
            OptionalValueParser parser = new OptionalValueParser(testValue);
            assertThat(parser.asDouble().isPresent()).isFalse();
        }

        @Test
        @DisplayName("returns the double value")
        void returnDoubleValue() {
            OptionalValueParser parser = new OptionalValueParser("2.7182");
            assertThat(parser.asDouble()).isEqualTo(OptionalDouble.of(2.7182));
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            OptionalValueParser parser = new OptionalValueParser("-2.7182");
            assertThat(parser.asDouble()).isEqualTo(OptionalDouble.of(-2.7182));
        }

        @DisplayName("allows all valid number formats")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"1.3e3"})
        void allowsValidNumberFormat(String value) {
            OptionalValueParser parser = new OptionalValueParser(value);
            assertThat(parser.asDouble()).isEqualTo(OptionalDouble.of(1300));
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            OptionalValueParser parser = new OptionalValueParser("FAIL");
            assertThatThrownBy(parser::asDouble).isInstanceOf(NumberFormatException.class);
        }

        @DisplayName("throws an exception for an invalid number format")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"1x10^3"})
        void throwsExceptionForInvalidNumberFormat(String value) {
            OptionalValueParser parser = new OptionalValueParser(value);
            assertThatThrownBy(parser::asDouble).isInstanceOf(NumberFormatException.class);
        }
    }

    @Nested
    @DisplayName("for Enum values")
    class EnumConfigTests {
        @Test
        @DisplayName("returns empty for empty value")
        void returnsEmptyValue() {
            String testValue = "";
            OptionalValueParser parser = new OptionalValueParser(testValue);
            assertThat(parser.asEnum(TestConfig.Colours.class).isPresent()).isFalse();
        }

        @Test
        @DisplayName("parses correct config")
        void parseCorrectConfig() {
            OptionalValueParser parser = new OptionalValueParser("RED");
            assertThat(parser.asEnum(TestConfig.Colours.class)).isEqualTo(Optional.of(TestConfig.Colours.RED));
        }

        @Test
        @DisplayName("throws IllegalArgumentException for incorrect value")
        void throwsException() {
            OptionalValueParser parser = new OptionalValueParser("REDDER");
            assertThatThrownBy(() -> parser.asEnum(TestConfig.Colours.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No enum constant");
        }
    }

    @Nested
    @DisplayName("for Time values")
    class TimeConfigTests {
        @Test
        @DisplayName("returns empty for empty value")
        void returnsEmptyValue() {
            String testValue = "";
            OptionalValueParser parser = new OptionalValueParser(testValue);
            assertThat(parser.asTime().isPresent()).isFalse();
            assertThat(parser.asFractionalTime().isPresent()).isFalse();
        }

        @Test
        @DisplayName("returns the long value")
        void returnTimeValue() {
            OptionalValueParser parser = new OptionalValueParser("2.3, SECONDS", TimeUnit.SECONDS, null);
            assertThat(parser.asTime()).isEqualTo(OptionalLong.of(2));
            assertThat(parser.asFractionalTime()).isEqualTo(OptionalDouble.of(2.3));
        }

        @Test
        @DisplayName("returns the long value when units are not specified")
        void returnTimeValueWithNoSpecifiedUnits() {
            OptionalValueParser parser = new OptionalValueParser("2.3", TimeUnit.SECONDS, null);
            assertThat(parser.asTime()).isEqualTo(OptionalLong.of(2));
            assertThat(parser.asFractionalTime()).isEqualTo(OptionalDouble.of(2.3));
        }

        @Test
        @DisplayName("returns the scaled value")
        void returnScaledTimeValue() {
            OptionalValueParser parser = new OptionalValueParser("2.11, MINUTES", TimeUnit.SECONDS, null);
            assertThat(parser.asTime()).isEqualTo(OptionalLong.of(127));
            assertThat(parser.asFractionalTime()).isEqualTo(OptionalDouble.of(126.6));
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            OptionalValueParser parser = new OptionalValueParser("-2.3, SECONDS", TimeUnit.SECONDS, null);
            assertThat(parser.asTime()).isEqualTo(OptionalLong.of(-2));
            assertThat(parser.asFractionalTime()).isEqualTo(OptionalDouble.of(-2.3));
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            OptionalValueParser parser = new OptionalValueParser("FAIL, SECONDS", TimeUnit.SECONDS, null);
            assertThatThrownBy(parser::asTime).isInstanceOf(NumberFormatException.class);
            assertThatThrownBy(parser::asFractionalTime).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            OptionalValueParser parser = new OptionalValueParser("2, SECONDS, SECONDS", TimeUnit.SECONDS, null);
            assertThatThrownBy(parser::asTime).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(parser::asFractionalTime).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            OptionalValueParser parser = new OptionalValueParser("2, ORANGES", TimeUnit.SECONDS, null);
            assertThatThrownBy(parser::asTime).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(parser::asFractionalTime).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Throws an exception if time unit not set")
        void throwsExceptionWhenUnitNotSet() {
            OptionalValueParser parser = new OptionalValueParser("2, SECONDS", null, null);
            assertThatThrownBy(parser::asTime).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(parser::asFractionalTime).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("for Duration values")
    class DurationConfigTests {
        @Test
        @DisplayName("returns empty for empty value")
        void returnsEmptyValue() {
            String testValue = "";
            OptionalValueParser parser = new OptionalValueParser(testValue);
            assertThat(parser.asDuration().isPresent()).isFalse();
        }

        @Test
        @DisplayName("returns a Duration for an integer value")
        void returnDurationForInt() {
            OptionalValueParser parser = new OptionalValueParser("2, MILLISECONDS");
            assertThat(parser.asDuration()).isEqualTo(Optional.of(Duration.ofMillis(2)));
        }

        @Test
        @DisplayName("returns a Duration for a fractional value")
        void returnDurationForFraction() {
            OptionalValueParser parser = new OptionalValueParser("2.3, MILLISECONDS");
            assertThat(parser.asDuration()).isEqualTo(Optional.of(Duration.ofNanos(2_300_000)));
        }

        @Test
        @DisplayName("returns a Duration with units of seconds when units are not specified")
        void returnDurationInSecondsWhenNoSpecifiedUnits() {
            OptionalValueParser parser = new OptionalValueParser("2.3");
            assertThat(parser.asDuration()).isEqualTo(Optional.of(Duration.ofMillis(2_300)));
        }

        @Test
        @DisplayName("handles a 0 value")
        void zeroValue() {
            OptionalValueParser parser = new OptionalValueParser("0");
            assertThat(parser.asDuration()).isEqualTo(Optional.of(Duration.ofSeconds(0)));
        }

        @Test
        @DisplayName("rounds to the nearest nanosecond - below")
        void roundToNanoBelow() {
            OptionalValueParser parser = new OptionalValueParser("0.1,NANOSECONDS");
            assertThat(parser.asDuration()).isEqualTo(Optional.of(Duration.ofNanos(0)));
        }

        @Test
        @DisplayName("rounds to the nearest nanosecond - above")
        void roundToNanoAbove() {
            OptionalValueParser parser = new OptionalValueParser("0.6,NANOSECONDS");
            assertThat(parser.asDuration()).isEqualTo(Optional.of(Duration.ofNanos(1)));
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            OptionalValueParser parser = new OptionalValueParser("-2, SECONDS");
            assertThat(parser.asDuration()).isEqualTo(Optional.of(Duration.ofSeconds(-2)));
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            OptionalValueParser parser = new OptionalValueParser("FAIL, SECONDS");
            assertThatThrownBy(parser::asDuration).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            OptionalValueParser parser = new OptionalValueParser("2, SECONDS, SECONDS");
            assertThatThrownBy(parser::asDuration).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            OptionalValueParser parser = new OptionalValueParser("2, ORANGES");
            assertThatThrownBy(parser::asDuration).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("for Length values")
    class LengthConfigTests {
        @Test
        @DisplayName("returns empty for empty value")
        void returnsEmptyValue() {
            OptionalValueParser parser = new OptionalValueParser("");
            assertThat(parser.asLength().isPresent()).isFalse();
        }

        @Test
        @DisplayName("returns the double value")
        void returnValue() {
            OptionalValueParser parser = new OptionalValueParser("2.3, METERS", null, LengthUnit.METERS);
            assertThat(parser.asLength()).isEqualTo(OptionalDouble.of(2.3));
        }

        @Test
        @DisplayName("returns the double value when units are not specified")
        void returnValueWithNoSpecifiedUnits() {
            OptionalValueParser parser = new OptionalValueParser("2.3", null, LengthUnit.METERS);
            assertThat(parser.asLength()).isEqualTo(OptionalDouble.of(2.3));
        }

        @Test
        @DisplayName("returns the scaled value km -> m")
        void returnInputScaledValue() {
            OptionalValueParser parser = new OptionalValueParser("2.3, KILOMETERS", null, LengthUnit.METERS);
            assertThat(parser.asLength()).isEqualTo(OptionalDouble.of(2300));
        }

        @Test
        @DisplayName("returns the scaled value m -> km")
        void returnOutputScaledValue() {
            OptionalValueParser parser = new OptionalValueParser("2.3, METERS", null, LengthUnit.KILOMETERS);
            assertThat(parser.asLength()).isEqualTo(OptionalDouble.of(0.0023));
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            OptionalValueParser parser = new OptionalValueParser("-2.3, METERS", null, LengthUnit.METERS);
            assertThat(parser.asLength()).isEqualTo(OptionalDouble.of(-2.3));
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            OptionalValueParser parser = new OptionalValueParser("FAIL, METERS", null, LengthUnit.METERS);
            assertThatThrownBy(parser::asLength).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            OptionalValueParser parser = new OptionalValueParser("2, METERS, METERS", null, LengthUnit.METERS);
            assertThatThrownBy(parser::asLength).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            OptionalValueParser parser = new OptionalValueParser("2, ORANGES", null, LengthUnit.METERS);
            assertThatThrownBy(parser::asLength).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Throws an exception if length unit not set")
        void throwsExceptionWhenUnitNotSet() {
            OptionalValueParser parser = new OptionalValueParser("FAIL, METERS", null, null);
            assertThatThrownBy(parser::asLength).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("for Speed values")
    class SpeedConfigTests {
        @Test
        @DisplayName("returns empty for empty value")
        void returnsEmptyValue() {
            OptionalValueParser parser = new OptionalValueParser("");
            assertThat(parser.asSpeed().isPresent()).isFalse();
        }

        @Test
        @DisplayName("returns the double value when units are specified")
        void returnValue() {
            OptionalValueParser parser = new OptionalValueParser("2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asSpeed()).isEqualTo(OptionalDouble.of(2.3));
        }

        @Test
        @DisplayName("returns the double value when units are not specified")
        void returnValueWithDefaultUnits() {
            OptionalValueParser parser = new OptionalValueParser("2.3", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asSpeed()).isEqualTo(OptionalDouble.of(2.3));
        }

        @Test
        @DisplayName("returns the scaled value ms -> s")
        void returnTimeInputScaledTimeValue() {
            OptionalValueParser parser = new OptionalValueParser("2.3, METERS, MILLISECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asSpeed()).isEqualTo(OptionalDouble.of(2300));
        }

        @Test
        @DisplayName("returns the scaled value s -> ms")
        void returnTimeOutputScaledTimeValue() {
            OptionalValueParser parser = new OptionalValueParser("2.3, METERS, SECONDS", TimeUnit.MILLISECONDS, LengthUnit.METERS);
            assertThat(parser.asSpeed()).isEqualTo(OptionalDouble.of(0.0023));
        }

        @Test
        @DisplayName("returns the scaled value km -> m")
        void returnLengthInputScaledTimeValue() {
            OptionalValueParser parser = new OptionalValueParser("2.3, KILOMETERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asSpeed()).isEqualTo(OptionalDouble.of(2300));
        }

        @Test
        @DisplayName("returns the scaled value m -> km")
        void returnLengthOutputScaledTimeValue() {
            OptionalValueParser parser = new OptionalValueParser("2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.KILOMETERS);
            assertThat(parser.asSpeed()).isEqualTo(OptionalDouble.of(0.0023));
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            OptionalValueParser parser = new OptionalValueParser("-2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asSpeed()).isEqualTo(OptionalDouble.of(-2.3));
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            OptionalValueParser parser = new OptionalValueParser("FAIL, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThatThrownBy(parser::asSpeed).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            OptionalValueParser parser = new OptionalValueParser("2, METERS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThatThrownBy(parser::asSpeed).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            OptionalValueParser parser = new OptionalValueParser("2, METERS, ORANGES", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThatThrownBy(parser::asSpeed).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Throws an exception if length unit not set")
        void throwsExceptionWhenLengthUnitNotSet() {
            OptionalValueParser parser = new OptionalValueParser("2.3, METERS, SECONDS", TimeUnit.SECONDS, null);
            assertThatThrownBy(parser::asSpeed).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Throws an exception if time unit not set")
        void throwsExceptionWhenTimeUnitNotSet() {
            OptionalValueParser parser = new OptionalValueParser("2.3, METERS, SECONDS", null, LengthUnit.METERS);
            assertThatThrownBy(parser::asSpeed).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("for Acceleration values")
    class AccelerationConfigTests {
        @Test
        @DisplayName("returns empty for empty value")
        void returnsEmptyValue() {
            OptionalValueParser parser = new OptionalValueParser("");
            assertThat(parser.asAcceleration().isPresent()).isFalse();
        }

        @Test
        @DisplayName("returns the double value with units specified")
        void returnValue() {
            OptionalValueParser parser = new OptionalValueParser("2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asAcceleration()).isEqualTo(OptionalDouble.of(2.3));
        }

        @Test
        @DisplayName("returns the double value with no units specified")
        void returnValueWithDefaultUnits() {
            OptionalValueParser parser = new OptionalValueParser("2.3", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asAcceleration()).isEqualTo(OptionalDouble.of(2.3));
        }

        @Test
        @DisplayName("returns the scaled value ms -> s")
        void returnTimeInputScaledValue() {
            OptionalValueParser parser = new OptionalValueParser("2.3, METERS, MILLISECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asAcceleration()).isEqualTo(OptionalDouble.of(2300_000));
        }

        @Test
        @DisplayName("returns the scaled value s -> ms")
        void returnTimeOutputScaledValue() {
            OptionalValueParser parser = new OptionalValueParser("2.3, METERS, SECONDS", TimeUnit.MILLISECONDS, LengthUnit.METERS);
            assertThat(parser.asAcceleration()).isEqualTo(OptionalDouble.of(0.0000023));
        }

        @Test
        @DisplayName("returns the scaled value km -> m")
        void returnLengthInputScaledValue() {
            OptionalValueParser parser = new OptionalValueParser("2.3, KILOMETERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asAcceleration()).isEqualTo(OptionalDouble.of(2300));
        }

        @Test
        @DisplayName("returns the scaled value m -> km")
        void returnLengthOutputScaledValue() {
            OptionalValueParser parser = new OptionalValueParser("2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.KILOMETERS);
            assertThat(parser.asAcceleration()).isEqualTo(OptionalDouble.of(0.0023));
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            OptionalValueParser parser = new OptionalValueParser("-2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asAcceleration()).isEqualTo(OptionalDouble.of(-2.3));
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            OptionalValueParser parser = new OptionalValueParser("FAIL, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThatThrownBy(parser::asAcceleration).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            OptionalValueParser parser = new OptionalValueParser("2, METERS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThatThrownBy(parser::asAcceleration).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            OptionalValueParser parser = new OptionalValueParser("2, METERS, ORANGES", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThatThrownBy(parser::asAcceleration).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Throws an exception if length unit not set")
        void throwsExceptionWhenLengthUnitNotSet() {
            OptionalValueParser parser = new OptionalValueParser("2.3, METERS, SECONDS", TimeUnit.SECONDS, null);
            assertThatThrownBy(parser::asAcceleration).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Throws an exception if time unit not set")
        void throwsExceptionWhenTimeUnitNotSet() {
            OptionalValueParser parser = new OptionalValueParser("2.3, METERS, SECONDS", null, LengthUnit.METERS);
            assertThatThrownBy(parser::asAcceleration).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("for Jerk values")
    class JerkConfigTests {
        @Test
        @DisplayName("returns empty for empty value")
        void returnsEmptyValue() {
            OptionalValueParser parser = new OptionalValueParser("");
            assertThat(parser.asJerk().isPresent()).isFalse();
        }

        @Test
        @DisplayName("returns the double value with units specified")
        void returnValue() {
            OptionalValueParser parser = new OptionalValueParser("2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asJerk()).isEqualTo(OptionalDouble.of(2.3));
        }

        @Test
        @DisplayName("returns the double value with no units specified")
        void returnValueWithDefaultUnits() {
            OptionalValueParser parser = new OptionalValueParser("2.3", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asJerk()).isEqualTo(OptionalDouble.of(2.3));
        }

        @Test
        @DisplayName("returns the scaled value ms -> s")
        void returnTimeInputScaledValue() {
            OptionalValueParser parser = new OptionalValueParser("2.3, METERS, MILLISECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            OptionalDouble result = parser.asJerk();
            assertThat(result).isPresent();
            assertThat(DoubleMath.fuzzyEquals(result.getAsDouble(), 2.3e9, 1e-3));
        }

        @Test
        @DisplayName("returns the scaled value s -> ms")
        void returnTimeOutputScaledValue() {
            OptionalValueParser parser = new OptionalValueParser("2.3, METERS, SECONDS", TimeUnit.MILLISECONDS, LengthUnit.METERS);
            assertThat(parser.asJerk()).isEqualTo(OptionalDouble.of(2.3e-9));
        }

        @Test
        @DisplayName("returns the scaled value km -> m")
        void returnLengthInputScaledValue() {
            OptionalValueParser parser = new OptionalValueParser("2.3, KILOMETERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asJerk()).isEqualTo(OptionalDouble.of(2300));
        }

        @Test
        @DisplayName("returns the scaled value m -> km")
        void returnLengthOutputScaledValue() {
            OptionalValueParser parser = new OptionalValueParser("2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.KILOMETERS);
            assertThat(parser.asJerk()).isEqualTo(OptionalDouble.of(0.0023));
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            OptionalValueParser parser = new OptionalValueParser("-2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(parser.asJerk()).isEqualTo(OptionalDouble.of(-2.3));
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            OptionalValueParser parser = new OptionalValueParser("FAIL, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThatThrownBy(parser::asJerk).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            OptionalValueParser parser = new OptionalValueParser("2, METERS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThatThrownBy(parser::asJerk).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            OptionalValueParser parser = new OptionalValueParser("2, METERS, ORANGES", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThatThrownBy(parser::asJerk).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Throws an exception if length unit not set")
        void throwsExceptionWhenLengthUnitNotSet() {
            OptionalValueParser parser = new OptionalValueParser("2.3, METERS, SECONDS", TimeUnit.SECONDS, null);
            assertThatThrownBy(parser::asJerk).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Throws an exception if time unit not set")
        void throwsExceptionWhenTimeUnitNotSet() {
            OptionalValueParser parser = new OptionalValueParser("2.3, METERS, SECONDS", null, LengthUnit.METERS);
            assertThatThrownBy(parser::asJerk).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("test Lists")
    class ListTests {

        @Test
        @DisplayName("Empty string case returns optional empty")
        void testEmptyString() {
            OptionalValueParser parser = new OptionalValueParser("");
            assertThat(parser.asList().ofStrings().isPresent()).isFalse();
        }

        @Test
        @DisplayName("Single case returns singleton list")
        void testSingleElement() {
            OptionalValueParser parser = new OptionalValueParser("RED");
            assertThat(parser.asList().ofStrings()).isEqualTo(Optional.of(ImmutableList.of("RED")));
        }

        @DisplayName("separates valid strings")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"RED:YELLOW:APPLE", "RED,YELLOW,APPLE"})
        void testColonSeparated(String value) {
            OptionalValueParser parser = new OptionalValueParser(value);
            assertThat(parser.asList().ofStrings()).isEqualTo(Optional.of(ImmutableList.of("RED", "YELLOW", "APPLE")));
        }

        @Test
        @DisplayName("List of strings with space is trimmed")
        void testSpaceIsTrimmed() {
            OptionalValueParser parser = new OptionalValueParser("RED,YELLOW, APPLE");
            assertThat(parser.asList().ofStrings()).isEqualTo(Optional.of(ImmutableList.of("RED", "YELLOW", "APPLE")));
        }

        @Test
        @DisplayName("Comma-separated string can contain colons")
        void testCommaSeparatedStringsWithColons() {
            OptionalValueParser parser = new OptionalValueParser("key1:value1,key2:value2");
            assertThat(parser.asList().ofStrings()).isEqualTo(Optional.of(ImmutableList.of("key1:value1", "key2:value2")));
        }

        @Test
        @DisplayName("numerical methods with numbers")
        void testNumericalLists() {
            OptionalValueParser parser = new OptionalValueParser("1,5,10,3");
            assertThat(parser.asList().ofIntegers()).isEqualTo(Optional.of(ImmutableList.of(1, 5, 10, 3)));
            assertThat(parser.asList().ofLongs()).isEqualTo(Optional.of(ImmutableList.of(1L, 5L, 10L, 3L)));
            assertThat(parser.asList().ofDoubles()).isEqualTo(Optional.of(ImmutableList.of(1D, 5D, 10D, 3D)));
            assertThat(parser.asList().ofIds()).isEqualTo(Optional.of(ImmutableList.of(Id.create(1), Id.create(5), Id.create(10), Id.create(3))));
        }

        @Test
        @DisplayName("numerical methods with non-numbers throw exceptions")
        void testNumericalListsThrow() {
            OptionalValueParser parser = new OptionalValueParser("RED,BLUE,APPLE,PEAR");
            assertThatThrownBy(() -> parser.asList().ofIntegers()).isInstanceOf(NumberFormatException.class);
            assertThatThrownBy(() -> parser.asList().ofLongs()).isInstanceOf(NumberFormatException.class);
            assertThatThrownBy(() -> parser.asList().ofDoubles()).isInstanceOf(NumberFormatException.class);
            assertThatThrownBy(() -> parser.asList().ofIds()).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("enum values are parsed")
        void testEnumLists() {
            OptionalValueParser parser = new OptionalValueParser("RED,BLUE,BLUE,RED");
            ImmutableList<Colours> expected = ImmutableList.of(Colours.RED, Colours.BLUE, Colours.BLUE, Colours.RED);
            assertThat(parser.asList().ofEnums(TestConfig.Colours.class)).isEqualTo(Optional.of(expected));
        }

        @Test
        @DisplayName("incorrect enum values throw exception")
        void testEnumListsThrow() {
            OptionalValueParser parser = new OptionalValueParser("RED,BLUE,APPLE,PEAR");
            assertThatThrownBy(() -> parser.asList().ofEnums(TestConfig.Colours.class)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("string Ids are conveted")
        void testStringIdLists() {
            OptionalValueParser parser = new OptionalValueParser("RED,BLUE,APPLE,PEAR");
            ImmutableList<StringId<Object>> expected = ImmutableList.of(
                    StringId.create("RED"),
                    StringId.create("BLUE"),
                    StringId.create("APPLE"),
                    StringId.create("PEAR")
            );
            assertThat(parser.asList().ofStringIds()).isEqualTo(Optional.of(expected));
        }

        @Test
        @DisplayName("custom parser is used as expected")
        void callsParser() {
            String testValue = "ANOTHER TEST VALUE";
            OptionalValueParser parser = new OptionalValueParser(testValue);

            List<String> arguments = new ArrayList<>();
            TestClass testClass = new TestClass();
            Function<String, TestClass> parserFunction = value -> {
                arguments.add(value);
                return testClass;
            };
            assertThat(parser.asList().withElementParser(parserFunction)).isEqualTo(Optional.of(ImmutableList.of(testClass)));
            assertThat(arguments.size()).isEqualTo(1);
            assertThat(arguments.get(0)).isEqualTo(testValue);
        }
    }

    @Nested
    @DisplayName("test Sets")
    class SetTests {

        @Test
        @DisplayName("Empty string case returns optional empty")
        void testEmptyString() {
            OptionalValueParser parser = new OptionalValueParser("");
            assertThat(parser.asSet().ofStrings().isPresent()).isFalse();
        }

        @Test
        @DisplayName("Single case returns singleton set")
        void testSingleElement() {
            OptionalValueParser parser = new OptionalValueParser("RED");
            assertThat(parser.asSet().ofStrings()).isEqualTo(Optional.of(ImmutableSet.of("RED")));
        }

        @DisplayName("separates valid strings")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"RED:YELLOW:APPLE", "RED,YELLOW,APPLE"})
        void testColonSeparated(String value) {
            OptionalValueParser parser = new OptionalValueParser(value);
            assertThat(parser.asSet().ofStrings()).isEqualTo(Optional.of(ImmutableSet.of("RED", "YELLOW", "APPLE")));
        }

        @Test
        @DisplayName("Set of strings with space is trimmed")
        void testSpaceIsTrimmed() {
            OptionalValueParser parser = new OptionalValueParser("RED,YELLOW, APPLE");
            assertThat(parser.asSet().ofStrings()).isEqualTo(Optional.of(ImmutableSet.of("RED", "YELLOW", "APPLE")));
        }

        @Test
        @DisplayName("Comma-separated string can contain colons")
        void testCommaSeparatedStringsWithColons() {
            OptionalValueParser parser = new OptionalValueParser("key1:value1,key2:value2");
            assertThat(parser.asSet().ofStrings()).isEqualTo(Optional.of(ImmutableSet.of("key1:value1", "key2:value2")));
        }

        @Test
        @DisplayName("numerical methods with numbers")
        void testNumericalSets() {
            OptionalValueParser parser = new OptionalValueParser("1,5,10,3");
            assertThat(parser.asSet().ofIntegers()).isEqualTo(Optional.of(ImmutableSet.of(1, 5, 10, 3)));
            assertThat(parser.asSet().ofLongs()).isEqualTo(Optional.of(ImmutableSet.of(1L, 5L, 10L, 3L)));
            assertThat(parser.asSet().ofDoubles()).isEqualTo(Optional.of(ImmutableSet.of(1D, 5D, 10D, 3D)));
            assertThat(parser.asSet().ofIds()).isEqualTo(Optional.of(ImmutableSet.of(Id.create(1), Id.create(5), Id.create(10), Id.create(3))));
        }

        @Test
        @DisplayName("numerical methods with non-numbers throw exceptions")
        void testNumericalSetsThrow() {
            OptionalValueParser parser = new OptionalValueParser("RED,BLUE,APPLE,PEAR");
            assertThatThrownBy(() -> parser.asSet().ofIntegers()).isInstanceOf(NumberFormatException.class);
            assertThatThrownBy(() -> parser.asSet().ofLongs()).isInstanceOf(NumberFormatException.class);
            assertThatThrownBy(() -> parser.asSet().ofDoubles()).isInstanceOf(NumberFormatException.class);
            assertThatThrownBy(() -> parser.asSet().ofIds()).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("enum values are parsed")
        void testEnumSets() {
            OptionalValueParser parser = new OptionalValueParser("RED,BLUE,BLUE,RED");
            ImmutableSet<Colours> expected = ImmutableSet.of(Colours.RED, Colours.BLUE, Colours.BLUE, Colours.RED);
            assertThat(parser.asSet().ofEnums(TestConfig.Colours.class)).isEqualTo(Optional.of(expected));
        }

        @Test
        @DisplayName("incorrect enum values throw exception")
        void testEnumSetsThrow() {
            OptionalValueParser parser = new OptionalValueParser("RED,BLUE,APPLE,PEAR");
            assertThatThrownBy(() -> parser.asSet().ofEnums(TestConfig.Colours.class)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("string Ids are conveted")
        void testStringIdSets() {
            OptionalValueParser parser = new OptionalValueParser("RED,BLUE,APPLE,PEAR");
            ImmutableSet<StringId<Object>> expected = ImmutableSet.of(
                    StringId.create("RED"),
                    StringId.create("BLUE"),
                    StringId.create("APPLE"),
                    StringId.create("PEAR")
            );
            assertThat(parser.asSet().ofStringIds()).isEqualTo(Optional.of(expected));
        }

        @Test
        @DisplayName("custom parser is used as expected")
        void callsParser() {
            String testValue = "ANOTHER TEST VALUE";
            OptionalValueParser parser = new OptionalValueParser(testValue);

            List<String> arguments = new ArrayList<>();
            TestClass testClass = new TestClass();
            Function<String, TestClass> parserFunction = value -> {
                arguments.add(value);
                return testClass;
            };
            assertThat(parser.asSet().withElementParser(parserFunction)).isEqualTo(Optional.of(ImmutableSet.of(testClass)));
            assertThat(arguments.size()).isEqualTo(1);
            assertThat(arguments.get(0)).isEqualTo(testValue);
        }
    }

    @Nested
    @DisplayName("for Map values")
    class MapConfigTests {
        abstract class CommonMapTests<K, V> {

            static final String SIMPLE_CONFIG_VALUE = "1=True;2=False;3=False;4=False";

            abstract Optional<ImmutableMap<K, V>> readMap(String configValue);

            abstract ImmutableMap<K, V> getDefaultMap();

            @Test
            @DisplayName("works with valid config")
            void withValidConfig() {
                Optional<ImmutableMap<K, V>> map = readMap(SIMPLE_CONFIG_VALUE);
                assertThat(map).isEqualTo(Optional.of(getDefaultMap()));
            }

            @Test
            @DisplayName("returns an Optional empty for an empty value")
            void configReturnsEmptyMap() {
                Optional<ImmutableMap<K, V>> map = readMap((""));
                assertThat(map.isPresent()).isFalse();
            }

            @Test
            @DisplayName("works with a trailing semicolon")
            void trailingSemiColonIsAllowed() {
                Optional<ImmutableMap<K, V>> map = readMap("1=True;2=False;3=False;4=False;");
                assertThat(map).isEqualTo(Optional.of(getDefaultMap()));
            }

            @Test
            @DisplayName("ignores empty entries")
            void emptyEntries() {
                Optional<ImmutableMap<K, V>> map = readMap("1=True;;2=False;3=False;4=False");
                assertThat(map).isEqualTo(Optional.of(getDefaultMap()));
            }

            @Test
            @DisplayName("trims whitespace")
            void trimsWhitespace() {
                Optional<ImmutableMap<K, V>> map = readMap(" 1 = True ; 2 = False ; 3 = False ; 4 = False ");
                assertThat(map).isEqualTo(Optional.of(getDefaultMap()));
            }

            @Test
            @DisplayName("entries with missing keys are ignored")
            void ignoresMissingKeys() {
                Optional<ImmutableMap<K, V>> map = readMap("=False;1=True;2=False;3=False;4=False");
                assertThat(map).isEqualTo(Optional.of(getDefaultMap()));
            }

            @Test
            @DisplayName("throws exception for duplicate keys")
            void duplicateKeysThrows() {
                assertThatThrownBy(() -> readMap("1=False;1=True")).isInstanceOf(IllegalArgumentException.class);
            }
        }

        @Nested
        @DisplayName("read as a String map")
        class StringMapTests extends CommonMapTests<String, String> {
            @Override
            Optional<ImmutableMap<String, String>> readMap(String configValue) {
                OptionalValueParser parser = new OptionalValueParser(configValue);
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
                Optional<ImmutableMap<String, String>> map = readMap("1;2=");
                assertThat(map.get()).isEqualTo(ImmutableMap.of("2", ""));
            }
        }

        @Nested
        @DisplayName("read as a custom typed map")
        class TypedMapTests extends CommonMapTests<Integer, Boolean> {

            @Override
            Optional<ImmutableMap<Integer, Boolean>> readMap(String configValue) {
                OptionalValueParser parser = new OptionalValueParser(configValue);
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
                OptionalValueParser parser = new OptionalValueParser("1=4,5;2=6,7");
                Optional<ImmutableMap<Object, ImmutableList<Integer>>> mapOptional = parser.asMap().withKeyAndValueParsers(Integer::parseInt, ConfigParsers.getListOfIntegers());
                assertThat(mapOptional).isPresent();
                ImmutableMap<Object, ImmutableList<Integer>> map = mapOptional.get();
                assertThat(map).containsOnlyKeys(1, 2);
                assertThat(map).containsEntry(1, ImmutableList.of(4, 5));
                assertThat(map).containsEntry(2, ImmutableList.of(6, 7));
            }

            @Test
            @DisplayName("applies valueParser to empty string for missing values")
            void handlesMissingValues() {
                OptionalValueParser parser = new OptionalValueParser("1;2=");

                Function<String, String> identityFunctionWithAssertionStringIsEmpty = v -> {
                    assertThat(v).isEmpty();
                    return v;
                };
                Optional<ImmutableMap<Object, String>> map = parser.asMap().withKeyAndValueParsers(Integer::valueOf, identityFunctionWithAssertionStringIsEmpty);
                assertThat(map.get()).isEqualTo(ImmutableMap.of(2, ""));
            }

            @Test
            @DisplayName("throws exceptions generated by key parser function")
            void throwsKeyParserExceptions() {
                OptionalValueParser parser = new OptionalValueParser(SIMPLE_CONFIG_VALUE);

                assertThatThrownBy(() -> parser.asMap().withKeyAndValueParsers(s -> Failer.fail("Boom"), Boolean::parseBoolean))
                        .hasMessageContaining("Boom");
            }

            @Test
            @DisplayName("throws exceptions generated by value parser function")
            void throwsValueParserExceptions() {
                OptionalValueParser parser = new OptionalValueParser(SIMPLE_CONFIG_VALUE);

                assertThatThrownBy(() -> parser.asMap().withKeyAndValueParsers(Integer::valueOf, s -> Failer.fail("Boom")))
                        .hasMessageContaining("Boom");
            }
        }
    }

    @Nested
    @DisplayName("for Multimap values")
    class MultimapConfigTests {
        abstract class CommonMultimapTests<K, V> {

            static final String SIMPLE_CONFIG_VALUE = "1=True;1=False;2=False;3=False;4=False";

            abstract Optional<ImmutableSetMultimap<K, V>> readMultimap(String configValue);

            abstract ImmutableSetMultimap<K, V> getDefaultMultimap();

            @Test
            @DisplayName("works with valid config")
            void withValidConfig() {
                Optional<ImmutableSetMultimap<K, V>> multimap = readMultimap(SIMPLE_CONFIG_VALUE);
                assertThat(multimap).isEqualTo(Optional.of(getDefaultMultimap()));
            }

            @Test
            @DisplayName("returns an empty optional for an empty value")
            void configReturnsEmptyMap() {
                Optional<ImmutableSetMultimap<K, V>> multimap = readMultimap((""));
                assertThat(multimap).isNotPresent();
            }

            @Test
            @DisplayName("works with a trailing semicolon")
            void trailingSemiColonIsAllowed() {
                Optional<ImmutableSetMultimap<K, V>> multimap = readMultimap("1=True;1=False;2=False;3=False;4=False;");
                assertThat(multimap).isEqualTo(Optional.of(getDefaultMultimap()));
            }

            @Test
            @DisplayName("works with permutations")
            void permutationIsAllowed() {
                Optional<ImmutableSetMultimap<K, V>> multimap = readMultimap("1=True;2=False;3=False;1=False;4=False");
                assertThat(multimap).isEqualTo(Optional.of(getDefaultMultimap()));
            }

            @Test
            @DisplayName("ignores empty entries")
            void emptyEntries() {
                Optional<ImmutableSetMultimap<K, V>> multimap = readMultimap("1=True;1=False;;2=False;3=False;4=False");
                assertThat(multimap).isEqualTo(Optional.of(getDefaultMultimap()));
            }

            @Test
            @DisplayName("trims whitespace")
            void trimsWhitespace() {
                Optional<ImmutableSetMultimap<K, V>> multimap = readMultimap(" 1 = True ; 1 = False ; 2 = False ; 3 = False ; 4 = False ");
                assertThat(multimap).isEqualTo(Optional.of(getDefaultMultimap()));
            }

            @Test
            @DisplayName("entries with missing keys are ignored")
            void ignoresMissingKeys() {
                Optional<ImmutableSetMultimap<K, V>> multimap = readMultimap("=False;1=True;1=False;2=False;3=False;4=False");
                assertThat(multimap).isEqualTo(Optional.of(getDefaultMultimap()));
            }
        }

        @Nested
        @DisplayName("read as a String map")
        class StringMultimapTests extends CommonMultimapTests<String, String> {

            @Override
            Optional<ImmutableSetMultimap<String, String>> readMultimap(String configValue) {
                return new OptionalValueParser(configValue).asSetMultimap().ofStrings();
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
                Optional<ImmutableSetMultimap<String, String>> optionalMultimap = readMultimap("1;2=");
                assertThat(optionalMultimap).isPresent();
                ImmutableSetMultimap<String, String> multimap = optionalMultimap.get();
                assertThat(multimap.asMap().get("1")).isNull();
                assertThat(new ArrayList<>(multimap.asMap().get("2")).get(0)).isEmpty();
            }
        }

        @Nested
        @DisplayName("read as a typed multimap")
        class TypedMapTests extends CommonMultimapTests<Integer, Boolean> {

            @Override
            Optional<ImmutableSetMultimap<Integer, Boolean>> readMultimap(String configValue) {
                return new OptionalValueParser(configValue).asSetMultimap().withKeyAndValueParsers(ConfigParsers::parseInt, ConfigParsers::parseBoolean);
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
                OptionalValueParser parser = new OptionalValueParser("1=4,5;1=8,9;2=6,7");
                Optional<ImmutableSetMultimap<Object, ImmutableSet<Integer>>> optionalMultimap = parser.asSetMultimap()
                        .withKeyAndValueParsers(Integer::parseInt, ConfigParsers.getSetOfIntegers());
                assertThat(optionalMultimap.isPresent());
                ImmutableSetMultimap<Object, ImmutableSet<Integer>> multimap = optionalMultimap.get();
                assertThat(multimap.asMap()).containsOnlyKeys(1, 2);
                assertThat(multimap.asMap()).containsEntry(1, ImmutableSet.of(ImmutableSet.of(4, 5), ImmutableSet.of(8, 9)));
                assertThat(multimap.asMap()).containsEntry(2, ImmutableSet.of(ImmutableSet.of(6, 7)));
            }

            @Test
            @DisplayName("applies valueParser to empty string for missing values")
            void handlesMissingValues() {
                OptionalValueParser parser = new OptionalValueParser("1;2=");
                Function<String, String> identityFunctionWithAssertionStringIsEmpty = v -> {
                    assertThat(v).isEmpty();
                    return v;
                };
                Optional<ImmutableSetMultimap<Integer, String>> optionalMultimap = parser.asSetMultimap()
                        .withKeyAndValueParsers(Integer::valueOf, identityFunctionWithAssertionStringIsEmpty);

                assertThat(optionalMultimap.isPresent());
                ImmutableSetMultimap<Integer, String> multimap = optionalMultimap.get();

                assertThat(multimap.asMap()).containsOnlyKeys(2);
                assertThat(multimap.containsEntry(2, "")).isTrue();
            }

            @Test
            @DisplayName("throws exceptions generated by key parser function")
            void throwsKeyParserExceptions() {
                OptionalValueParser parser = new OptionalValueParser(SIMPLE_CONFIG_VALUE);

                assertThatThrownBy(() -> parser.asSetMultimap().withKeyAndValueParsers(s -> Failer.fail("Boom"), Boolean::parseBoolean))
                        .hasMessageContaining("Boom");
            }

            @Test
            @DisplayName("throws exceptions generated by key parser function")
            void throwsValueParserExceptions() {
                OptionalValueParser parser = new OptionalValueParser(SIMPLE_CONFIG_VALUE);

                assertThatThrownBy(() -> parser.asSetMultimap().withKeyAndValueParsers(Integer::valueOf, s -> Failer.fail("Boom")))
                        .hasMessageContaining("Boom");
            }
        }
    }

    @Nested
    @DisplayName("for custom Object values")
    class CustomParserTests {
        @Test
        @DisplayName("returns empty for empty value")
        void returnsEmptyValue() {
            String testValue = "";
            OptionalValueParser parser = new OptionalValueParser(testValue);
            assertThat(parser.withCustomParser(Function.identity()).isPresent()).isFalse();
        }

        @Test
        @DisplayName("passes value into parser and returns exact object")
        void callsParser() {
            String testValue = "ANOTHER TEST VALUE";
            OptionalValueParser parser = new OptionalValueParser(testValue);

            List<String> arguments = new ArrayList<>();
            TestClass testClass = new TestClass();
            Function<String, TestClass> parserFunction = value -> {
                arguments.add(value);
                return testClass;
            };

            Optional<TestClass> result = parser.withCustomParser(parserFunction);
            assertThat(result.isPresent()).isTrue();
            assertThat(result.get()).isSameAs(testClass);

            assertThat(arguments.size()).isEqualTo(1);
            assertThat(arguments.get(0)).isSameAs(testValue);
        }
    }

    private static final class TestClass {
    }
}
