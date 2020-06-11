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
            OptionalValueParser parser = new OptionalValueParser("2.3, SECONDS", TimeUnit.SECONDS);
            assertThat(parser.asTime()).isEqualTo(OptionalLong.of(2));
            assertThat(parser.asFractionalTime()).isEqualTo(OptionalDouble.of(2.3));
        }

        @Test
        @DisplayName("returns the long value when units are not specified")
        void returnTimeValueWithNoSpecifiedUnits() {
            OptionalValueParser parser = new OptionalValueParser("2.3", TimeUnit.SECONDS);
            assertThat(parser.asTime()).isEqualTo(OptionalLong.of(2));
            assertThat(parser.asFractionalTime()).isEqualTo(OptionalDouble.of(2.3));
        }

        @Test
        @DisplayName("returns the scaled value")
        void returnScaledTimeValue() {
            OptionalValueParser parser = new OptionalValueParser("2.11, MINUTES", TimeUnit.SECONDS);
            assertThat(parser.asTime()).isEqualTo(OptionalLong.of(127));
            assertThat(parser.asFractionalTime()).isEqualTo(OptionalDouble.of(126.6));
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            OptionalValueParser parser = new OptionalValueParser("-2.3, SECONDS", TimeUnit.SECONDS);
            assertThat(parser.asTime()).isEqualTo(OptionalLong.of(-2));
            assertThat(parser.asFractionalTime()).isEqualTo(OptionalDouble.of(-2.3));
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            OptionalValueParser parser = new OptionalValueParser("FAIL, SECONDS", TimeUnit.SECONDS);
            assertThatThrownBy(parser::asTime).isInstanceOf(NumberFormatException.class);
            assertThatThrownBy(parser::asFractionalTime).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            OptionalValueParser parser = new OptionalValueParser("2, SECONDS, SECONDS", TimeUnit.SECONDS);
            assertThatThrownBy(parser::asTime).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(parser::asFractionalTime).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            OptionalValueParser parser = new OptionalValueParser("2, ORANGES", TimeUnit.SECONDS);
            assertThatThrownBy(parser::asTime).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(parser::asFractionalTime).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Throws an exception if time unit not set")
        void throwsExceptionWhenUnitNotSet() {
            OptionalValueParser parser = new OptionalValueParser("2, SECONDS", null);
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

    private static final class TestClass{}
}
