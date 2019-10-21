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
package com.ocadotechnology.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
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
import com.ocadotechnology.config.TestConfig.Colours;
import com.ocadotechnology.config.TestConfig.FirstSubConfig;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.StringId;
import com.ocadotechnology.physics.units.LengthUnit;
import com.ocadotechnology.validation.Failer;

@DisplayName("A Config object")
@SuppressWarnings("InnerClassMayBeStatic") // @Nested classes cannot be static
class ConfigTest {

    @Nested
    @DisplayName("that is empty")
    class EmptyConfigTests {
        private final Config<TestConfig> config = Config.empty(TestConfig.class);

        @Test
        @DisplayName("does not contain any keys")
        void doesNotContainAnyTopLevelKey() {
            assertThat(config.containsKey(TestConfig.FOO)).isFalse();
            assertThat(config.containsKey(TestConfig.FOO)).isFalse();
            assertThat(config.containsKey(TestConfig.FOO)).isFalse();
            assertThat(config.containsKey(TestConfig.FOO)).isFalse();
            assertThat(config.containsKey(TestConfig.FOO)).isFalse();
            assertThat(config.containsKey(FirstSubConfig.WOO)).isFalse();
        }

        @Test
        @DisplayName("does not have any sub-config")
        void doesNotHaveSubConfig() {
            assertThat(config.getSubConfig(FirstSubConfig.class)).isNull();
        }

        @Test
        @DisplayName("returns Optional.empty() for optional getters")
        void returnsEmptyOptionals() {
            assertThat(config.getStringIfPresent(TestConfig.FOO)).isEmpty();
            assertThat(config.getIntIfPresent(TestConfig.FOO)).isEmpty();
            assertThat(config.getLongIfPresent(TestConfig.FOO)).isEmpty();
            assertThat(config.getDoubleIfPresent(TestConfig.FOO)).isEmpty();
            assertThat(config.getEnumIfPresent(TestConfig.FOO, TestConfig.Colours.class)).isEmpty();
        }

        @Test
        @DisplayName("throws exception for getters")
        void returnsNull() {
            assertThatThrownBy(() -> config.getString(TestConfig.FOO)).isInstanceOf(ConfigKeyNotFoundException.class);
            assertThatThrownBy(() -> config.getInt(TestConfig.FOO)).isInstanceOf(ConfigKeyNotFoundException.class);
            assertThatThrownBy(() -> config.getInt(TestConfig.FOO)).isInstanceOf(ConfigKeyNotFoundException.class);
            assertThatThrownBy(() -> config.getDouble(TestConfig.FOO)).isInstanceOf(ConfigKeyNotFoundException.class);
            assertThatThrownBy(() -> config.getTime(TestConfig.FOO)).isInstanceOf(ConfigKeyNotFoundException.class);
            assertThatThrownBy(() -> config.getEnum(TestConfig.FOO, TestConfig.Colours.class)).isInstanceOf(ConfigKeyNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("for Boolean values")
    class BooleanConfigTests {

        @Test
        @DisplayName("returns the boolean value")
        void returnBooleanValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "true");
            assertThat(config.getBoolean(TestConfig.FOO)).isTrue();
        }

        @Test
        @DisplayName("allows case insensitive values")
        void allowsCaseInsensitiveValues() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "trUE");
            assertThat(config.getBoolean(TestConfig.FOO)).isTrue();
        }

        @DisplayName("returns false")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"false", "False", "FALSE", "fAlSe"})
        void allowsFalseValues(String value) {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, value);
            assertThat(config.getBoolean(TestConfig.FOO)).isFalse();
        }

        @DisplayName("throws an exception for typo")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"fa lse", "ture", "yes"})
        void throwsExceptionForMisspelledValue(String value) {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, value);
            assertThatThrownBy(() -> config.getBoolean(TestConfig.FOO)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("for Integer values")
    class IntegerConfigTests {

        @Test
        @DisplayName("returns the integer value")
        void returnIntegerValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "42");
            assertThat(config.getInt(TestConfig.FOO)).isEqualTo(42);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "-2");
            assertThat(config.getInt(TestConfig.FOO)).isEqualTo(-2);
        }

        @Test
        @DisplayName("returns an Optional containing the value")
        void returnsOptionalWithIntegerValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "42");
            assertThat(config.getIntIfPresent(TestConfig.FOO)).hasValue(42);
        }

        @DisplayName("returns Integer.MAX_VALUE")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"max", "MAX", "mAx", " max ", "maximum", "maximumwithadditionalletters", "MAX1234"})
        void testMaxValues(String value) {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, value);
            assertThat(config.getInt(TestConfig.FOO)).isEqualTo(Integer.MAX_VALUE);
        }

        @DisplayName("returns Integer.MIN_VALUE")
        @ParameterizedTest(name = "for config value \"{0}\"")
        @ValueSource(strings = {"min", "MIN", "MiN", " min ", "minimum", "minumumwithadditionalletters", "MIN1234"})
        void testMinValues(String value) {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, value);
            assertThat(config.getInt(TestConfig.FOO)).isEqualTo(Integer.MIN_VALUE);
        }

        @Test
        @DisplayName("throws an exception for none-number")
        void throwsExceptionForNonNumber() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "FAIL");
            assertThatThrownBy(() -> config.getInt(TestConfig.FOO)).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception for a non-integer number")
        void throwsExceptionForDecimalNumber() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.7182");
            assertThatThrownBy(() -> config.getInt(TestConfig.FOO)).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception for an overly-large number")
        void throwsExceptionForLargeNumber() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, String.valueOf(Long.MAX_VALUE));
            assertThatThrownBy(() -> config.getInt(TestConfig.FOO)).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception for none-number when getting as an optional")
        void throwsExceptionForNonNumberOptional() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "FAIL");
            assertThatThrownBy(() -> config.getIntIfPresent(TestConfig.FOO)).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception for a non-integer number when getting as an optional")
        void throwsExceptionForDecimalNumberOptional() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.7182");
            assertThatThrownBy(() -> config.getIntIfPresent(TestConfig.FOO)).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception for an overly-large number when getting as an optional")
        void throwsExceptionForLargeNumberOptional() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, String.valueOf(Long.MAX_VALUE));
            assertThatThrownBy(() -> config.getIntIfPresent(TestConfig.FOO)).isInstanceOf(NumberFormatException.class);
        }
    }

    @Nested
    @DisplayName("for Long values")
    class LongConfigTests {

        @Test
        @DisplayName("returns the long value")
        void returnIntegerValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "42");
            assertThat(config.getLong(TestConfig.FOO)).isEqualTo(42);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "-2");
            assertThat(config.getLong(TestConfig.FOO)).isEqualTo(-2);
        }

        @Test
        @DisplayName("returns an Optional containing the value")
        void returnsOptionalWithIntegerValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "42");
            assertThat(config.getLongIfPresent(TestConfig.FOO)).hasValue(42);
        }

        @Test
        @DisplayName("throws an exception for none-number")
        void throwsExceptionForNonNumber() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "FAIL");
            assertThatThrownBy(() -> config.getLong(TestConfig.FOO)).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception for a non-integer number")
        void throwsExceptionForDecimalNumber() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.7182");
            assertThatThrownBy(() -> config.getLong(TestConfig.FOO)).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception for none-number when getting an optional")
        void throwsExceptionForNonNumberOptional() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "FAIL");
            assertThatThrownBy(() -> config.getLongIfPresent(TestConfig.FOO)).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception for a non-integer number when getting an optional")
        void throwsExceptionForDecimalNumberOptional() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.7182");
            assertThatThrownBy(() -> config.getLongIfPresent(TestConfig.FOO)).isInstanceOf(NumberFormatException.class);
        }
    }

    @Nested
    @DisplayName("for Double values")
    class DoubleConfigTests {

        @Test
        @DisplayName("returns the double value")
        void returnDoubleValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.7182");
            assertThat(config.getDouble(TestConfig.FOO)).isEqualTo(2.7182);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "-2.7182");
            assertThat(config.getDouble(TestConfig.FOO)).isEqualTo(-2.7182);
        }

        @Test
        @DisplayName("returns an Optional containing the value")
        void returnsOptionalWithIntegerValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.7182");
            assertThat(config.getDoubleIfPresent(TestConfig.FOO)).hasValue(2.7182);
        }

        @Test
        @DisplayName("throws an exception for none-number")
        void throwsExceptionForNonNumber() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "FAIL");
            assertThatThrownBy(() -> config.getDouble(TestConfig.FOO)).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception for none-number when getting an optional")
        void throwsExceptionForNonNumberOptional() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "FAIL");
            assertThatThrownBy(() -> config.getDoubleIfPresent(TestConfig.FOO)).isInstanceOf(NumberFormatException.class);
        }
    }

    @Nested
    @DisplayName("for Length values")
    class LengthConfigTests {

        @Test
        @DisplayName("returns the double value")
        void returnValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.3, METERS", LengthUnit.METERS);
            assertThat(config.getLength(TestConfig.FOO)).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the double value when units are not specified")
        void returnValueWithNoSpecifiedUnits() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.3", LengthUnit.METERS);
            assertThat(config.getLength(TestConfig.FOO)).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the scaled value")
        void returnScaledValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.3, KILOMETERS", LengthUnit.METERS);
            assertThat(config.getLength(TestConfig.FOO)).isEqualTo(2300);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "-2.3, METERS", LengthUnit.METERS);
            assertThat(config.getLength(TestConfig.FOO)).isEqualTo(-2.3);
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "FAIL, METERS", LengthUnit.METERS);
            assertThatThrownBy(() -> config.getLength(TestConfig.FOO)).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2, METERS, METERS", LengthUnit.METERS);
            assertThatThrownBy(() -> config.getLength(TestConfig.FOO)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2, ORANGES", LengthUnit.METERS);
            assertThatThrownBy(() -> config.getLength(TestConfig.FOO)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Throws an exception if length unit not set")
        void throwsExceptionWhenUnitNotSet() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2, METERS");
            assertThatThrownBy(() -> config.getLength(TestConfig.FOO)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("for Time values")
    class TimeConfigTests {

        @Test
        @DisplayName("returns the integer value")
        void returnIntegerTimeValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.3, SECONDS", TimeUnit.SECONDS);
            assertThat(config.getTime(TestConfig.FOO)).isEqualTo(2);
        }

        @Test
        @DisplayName("returns the integer value when units are not specified")
        void returnIntegerTimeValueWithNoSpecifiedUnits() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.3", TimeUnit.SECONDS);
            assertThat(config.getTime(TestConfig.FOO)).isEqualTo(2);
        }

        @Test
        @DisplayName("returns the double value")
        void returnFloatTimeValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.3, SECONDS", TimeUnit.SECONDS);
            assertThat(config.getFractionalTime(TestConfig.FOO)).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the scaled value")
        void returnScaledTimeValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2, MINUTES", TimeUnit.SECONDS);
            assertThat(config.getTime(TestConfig.FOO)).isEqualTo(120);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "-2, SECONDS", TimeUnit.SECONDS);
            assertThat(config.getTime(TestConfig.FOO)).isEqualTo(-2);
        }

        @Test
        @DisplayName("returns an Optional containing the integer value")
        void returnsOptionalWithIntegerValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2, SECONDS", TimeUnit.SECONDS);
            assertThat(config.getTimeIfPresent(TestConfig.FOO)).hasValue(2L);
        }

        @Test
        @DisplayName("returns an Optional containing the fractional value")
        void returnsOptionalWithFractionalValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.3, SECONDS", TimeUnit.SECONDS);
            assertThat(config.getFractionalTimeIfPresent(TestConfig.FOO)).hasValue(2.3);
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "FAIL, SECONDS", TimeUnit.SECONDS);
            assertThatThrownBy(() -> config.getTime(TestConfig.FOO)).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2, SECONDS, SECONDS", TimeUnit.SECONDS);
            assertThatThrownBy(() -> config.getTime(TestConfig.FOO)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2, ORANGES", TimeUnit.SECONDS);
            assertThatThrownBy(() -> config.getTime(TestConfig.FOO)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Throws an exception if time unit not set")
        void throwsExceptionWhenUnitNotSet() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2, SECONDS");
            assertThatThrownBy(() -> config.getTime(TestConfig.FOO)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("for Duration values")
    class DurationConfigTests {

        @Test
        @DisplayName("returns a Duration for an integer value")
        void returnDurationForInt() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2, MILLISECONDS");
            assertThat(config.getDuration(TestConfig.FOO)).isEqualTo(Duration.ofMillis(2));
        }

        @Test
        @DisplayName("returns a Duration for a fractional value")
        void returnDurationForFraction() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.3, MILLISECONDS");
            assertThat(config.getDuration(TestConfig.FOO)).isEqualTo(Duration.ofNanos(2_300_000));
        }

        @Test
        @DisplayName("returns a Duration when the parts have multiple spaces")
        void returnDurationWithSpaces() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "     2.3     ,     MILLISECONDS     ");
            assertThat(config.getDuration(TestConfig.FOO)).isEqualTo(Duration.ofNanos(2_300_000));
        }

        @Test
        @DisplayName("returns a Duration with units of seconds when units are not specified")
        void returnDurationInSecondsWhenNoSpecifiedUnits() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.3");
            assertThat(config.getDuration(TestConfig.FOO)).isEqualTo(Duration.ofMillis(2_300));
        }

        @Test
        @DisplayName("handles a 0 value")
        void zeroValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "0");
            assertThat(config.getDuration(TestConfig.FOO)).isEqualTo(Duration.ofSeconds(0));
        }

        @Test
        @DisplayName("rounds to the nearest nanosecond - below")
        void roundToNanoBelow() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "0.1,NANOSECONDS");
            assertThat(config.getDuration(TestConfig.FOO)).isEqualTo(Duration.ofNanos(0));
        }

        @Test
        @DisplayName("rounds to the nearest nanosecond - above")
        void roundToNanoAbove() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "0.6,NANOSECONDS");
            assertThat(config.getDuration(TestConfig.FOO)).isEqualTo(Duration.ofNanos(1));
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "-2, SECONDS");
            assertThat(config.getDuration(TestConfig.FOO)).isEqualTo(Duration.ofSeconds(-2));
        }

        @Test
        @DisplayName("returns an Optional containing the value")
        void returnsOptionalWithDuration() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2, SECONDS");
            assertThat(config.getDurationIfPresent(TestConfig.FOO)).hasValue(Duration.ofSeconds(2));
        }

        @Test
        @DisplayName("returns an empty Optional when not present")
        void returnsEmptyOptionalWhenNotPresent() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2, SECONDS");
            assertThat(config.getDurationIfPresent(TestConfig.BAR)).isEmpty();
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "FAIL, SECONDS");
            assertThatThrownBy(() -> config.getDuration(TestConfig.FOO)).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2, SECONDS, SECONDS");
            assertThatThrownBy(() -> config.getDuration(TestConfig.FOO)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2, ORANGES");
            assertThatThrownBy(() -> config.getDuration(TestConfig.FOO)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("for Speed values")
    class SpeedConfigTests {

        @Test
        @DisplayName("returns the double value when units are specified")
        void returnValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(config.getSpeed(TestConfig.FOO)).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the double value when units are not specified")
        void returnValueWithDefaultUnits() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(config.getSpeed(TestConfig.FOO)).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the scaled value")
        void returnTimeScaledTimeValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.3, METERS, MILLISECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(config.getSpeed(TestConfig.FOO)).isEqualTo(2300);
        }

        @Test
        @DisplayName("returns the scaled value")
        void returnLengthScaledTimeValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.KILOMETERS);
            assertThat(config.getSpeed(TestConfig.FOO)).isEqualTo(0.0023);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "-2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(config.getSpeed(TestConfig.FOO)).isEqualTo(-2.3);
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "FAIL, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThatThrownBy(() -> config.getSpeed(TestConfig.FOO)).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2, METERS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThatThrownBy(() -> config.getSpeed(TestConfig.FOO)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2, METERS, ORANGES", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThatThrownBy(() -> config.getSpeed(TestConfig.FOO)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Throws an exception if length unit not set")
        void throwsExceptionWhenLengthUnitNotSet() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.3, METERS, SECONDS", TimeUnit.SECONDS);
            assertThatThrownBy(() -> config.getSpeed(TestConfig.FOO)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Throws an exception if time unit not set")
        void throwsExceptionWhenTimeUnitNotSet() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.3, METERS, SECONDS", LengthUnit.METERS);
            assertThatThrownBy(() -> config.getSpeed(TestConfig.FOO)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("for Acceleration values")
    class AccelerationConfigTests {

        @Test
        @DisplayName("returns the double value with units specified")
        void returnValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(config.getAcceleration(TestConfig.FOO)).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the double value with no units specified")
        void returnValueWithDefaultUnits() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.3", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(config.getAcceleration(TestConfig.FOO)).isEqualTo(2.3);
        }

        @Test
        @DisplayName("returns the scaled value")
        void returnTimeScaledTimeValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.3, METERS, MILLISECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(config.getAcceleration(TestConfig.FOO)).isEqualTo(2300_000);
        }

        @Test
        @DisplayName("returns the scaled value")
        void returnLengthScaledTimeValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.KILOMETERS);
            assertThat(config.getAcceleration(TestConfig.FOO)).isEqualTo(0.0023);
        }

        @Test
        @DisplayName("allows negative values")
        void allowsNegativeValues() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "-2.3, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThat(config.getAcceleration(TestConfig.FOO)).isEqualTo(-2.3);
        }

        @Test
        @DisplayName("throws an exception for non-number")
        void throwsExceptionForNonNumber() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "FAIL, METERS, SECONDS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThatThrownBy(() -> config.getAcceleration(TestConfig.FOO)).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("throws an exception invalid structure")
        void throwsExceptionForInvalidStructure() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2, METERS", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThatThrownBy(() -> config.getAcceleration(TestConfig.FOO)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws an exception for invalid enum value")
        void throwsExceptionForInvalidEnumValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2, METERS, ORANGES", TimeUnit.SECONDS, LengthUnit.METERS);
            assertThatThrownBy(() -> config.getAcceleration(TestConfig.FOO)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Throws an exception if length unit not set")
        void throwsExceptionWhenLengthUnitNotSet() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.3, METERS, SECONDS", TimeUnit.SECONDS);
            assertThatThrownBy(() -> config.getAcceleration(TestConfig.FOO)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Throws an exception if time unit not set")
        void throwsExceptionWhenTimeUnitNotSet() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "2.3, METERS, SECONDS", LengthUnit.METERS);
            assertThatThrownBy(() -> config.getAcceleration(TestConfig.FOO)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("for Enum values")
    class EnumConfigTests {

        @Test
        @DisplayName("parses correct config")
        void parseCorrectConfig() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED");
            assertThat(config.getEnum(TestConfig.FOO, TestConfig.Colours.class)).isEqualTo(TestConfig.Colours.RED);
        }

        @Test
        @DisplayName("returns optional with value if present")
        void returnsOptionalWithValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED");
            assertThat(config.getEnumIfPresent(TestConfig.FOO, TestConfig.Colours.class)).hasValue(TestConfig.Colours.RED);
        }

        @Test
        @DisplayName("returns empty optional if config key does not exist")
        void returnsEmptyOptional() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED");
            assertThat(config.getEnumIfPresent(TestConfig.BAR, TestConfig.Colours.class)).isEmpty();
        }

        @Test
        @DisplayName("throws IllegalArgumentException for incorrect value")
        void throwsException() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "BLACK");
            assertThatThrownBy(() -> config.getEnum(TestConfig.FOO, TestConfig.Colours.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No enum constant");
        }

        @Test
        @DisplayName("throws IllegalArgumentException for incorrect value when getting optionally")
        void throwsExceptionWithOptional() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "BLACK");
            assertThatThrownBy(() -> config.getEnumIfPresent(TestConfig.FOO, TestConfig.Colours.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No enum constant");
        }
    }

    @Nested
    @DisplayName("test get Set Of Enums")
    class SetOfEnumsTests {

        @Test
        @DisplayName("Set of 3 colon-separated strings case")
        void testColonSeparated() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED:BLUE:GREEN");
            assertThat(config.getSetOfEnums(TestConfig.FOO, TestConfig.Colours.class)).isEqualTo(ImmutableSet.of(Colours.BLUE, Colours.GREEN, Colours.RED));
        }

        @Test
        @DisplayName("Set of 3 comma-separated strings case")
        void testCommaSeparated() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED,BLUE,GREEN");
            assertThat(config.getSetOfEnums(TestConfig.FOO, TestConfig.Colours.class)).isEqualTo(ImmutableSet.of(Colours.BLUE, Colours.GREEN, Colours.RED));
        }

        @Test
        @DisplayName("Set of 3 strings with spaces after commas")
        void testSpaceAfterCommas() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED, BLUE,  GREEN");
            assertThat(config.getSetOfEnums(TestConfig.FOO, TestConfig.Colours.class)).isEqualTo(ImmutableSet.of(Colours.BLUE, Colours.GREEN, Colours.RED));
        }

        @Test
        @DisplayName("Set of 3 strings with spaces before commas")
        void testSpaceBeforeCommas() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED ,BLUE  ,GREEN");
            assertThat(config.getSetOfEnums(TestConfig.FOO, TestConfig.Colours.class)).isEqualTo(ImmutableSet.of(Colours.BLUE, Colours.GREEN, Colours.RED));
        }

        @Test
        @DisplayName("Duplicate item is removed")
        void testDuplicateIsRemoved() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED,RED");
            assertThat(config.getSetOfEnums(TestConfig.FOO, TestConfig.Colours.class)).isEqualTo(ImmutableSet.of(Colours.RED));
        }

        @Test
        @DisplayName("Empty string case returns empty set")
        void testEmptyString() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "");
            ImmutableSet<TestConfig.Colours> setOfEnums = config.getSetOfEnums(TestConfig.FOO, TestConfig.Colours.class);
            assertThat(setOfEnums).isEqualTo(ImmutableSet.of());
        }

        @Test
        @DisplayName("Config doesn't exist case")
        void testNonExistentConfigKey() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.BAR, "RED, CAR");
            assertThatThrownBy(() -> config.getSetOfEnums(TestConfig.FOO, TestConfig.Colours.class))
                    .isInstanceOf(ConfigKeyNotFoundException.class);
        }

        @Test
        @DisplayName("Wrong enum value")
        void testWrongEnumValue() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED, CAR");
            assertThatThrownBy(() -> config.getSetOfEnums(TestConfig.FOO, TestConfig.Colours.class))
                    .isInstanceOf(IllegalArgumentException.class);
        }

    }

    @Nested
    @DisplayName("test get List Of Enums")
    class ListOfEnumsTests {

        @Test
        @DisplayName("List of 3 colon-separated strings case")
        void testColonSeparated() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED:BLUE:GREEN");
            assertThat(config.getListOfEnums(TestConfig.FOO, TestConfig.Colours.class)).isEqualTo(ImmutableList.of(Colours.RED, Colours.BLUE, Colours.GREEN));
        }

        @Test
        @DisplayName("List of 3 comma-separated strings case")
        void testCommaSeparated() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED,BLUE,GREEN");
            assertThat(config.getListOfEnums(TestConfig.FOO, TestConfig.Colours.class)).isEqualTo(ImmutableList.of(Colours.RED, Colours.BLUE, Colours.GREEN));
        }

        @Test
        @DisplayName("List of 3 strings with spaces after commas")
        void testSpaceAfterCommas() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED, BLUE,  GREEN");
            assertThat(config.getListOfEnums(TestConfig.FOO, TestConfig.Colours.class)).isEqualTo(ImmutableList.of(Colours.RED, Colours.BLUE, Colours.GREEN));
        }

        @Test
        @DisplayName("List of 3 strings with spaces before commas")
        void testSpaceBeforeCommas() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED ,BLUE  ,GREEN");
            assertThat(config.getListOfEnums(TestConfig.FOO, TestConfig.Colours.class)).isEqualTo(ImmutableList.of(Colours.RED, Colours.BLUE, Colours.GREEN));
        }

        @Test
        @DisplayName("Empty string case returns empty list")
        void testEmptyString() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "");
            ImmutableList<TestConfig.Colours> listOfEnums = config.getListOfEnums(TestConfig.FOO, TestConfig.Colours.class);
            assertThat(listOfEnums).isEqualTo(ImmutableList.of());
        }

        @Test
        @DisplayName("Config doesn't exist case")
        void testNonExistentConfigKey() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.BAR, "RED, CAR");
            assertThatThrownBy(() -> config.getListOfEnums(TestConfig.FOO, TestConfig.Colours.class))
                    .isInstanceOf(ConfigKeyNotFoundException.class);
        }

        @Test
        @DisplayName("Wrong enum value")
        void testWrongEnum() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED, CAR");
            assertThatThrownBy(() -> config.getListOfEnums(TestConfig.FOO, TestConfig.Colours.class))
                    .isInstanceOf(IllegalArgumentException.class);
        }

    }

    @Nested
    @DisplayName("test get List Of Strings")
    class ListOfStringsTests {

        @Test
        @DisplayName("Can be colon-separated")
        void testColonSeparated() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED:YELLOW:APPLE");
            assertThat(config.getListOfStrings(TestConfig.FOO)).containsExactly("RED", "YELLOW", "APPLE");
        }

        @Test
        @DisplayName("List of 3 comma-separated strings case")
        void testCommaSeparated() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED,YELLOW,APPLE");
            assertThat(config.getListOfStrings(TestConfig.FOO)).isEqualTo(ImmutableList.of("RED", "YELLOW", "APPLE"));
        }

        @Test
        @DisplayName("Single case returns singleton list")
        void testSingleElement() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED");
            ImmutableList<String> listOfStrings = config.getListOfStrings(TestConfig.FOO);
            assertThat(listOfStrings).isEqualTo(ImmutableList.of("RED"));
        }

        @Test
        @DisplayName("Empty string case returns empty list")
        void testEmptyString() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "");
            ImmutableList<String> listOfStrings = config.getListOfStrings(TestConfig.FOO);
            assertThat(listOfStrings).isEqualTo(ImmutableList.of());
        }

        @Test
        @DisplayName("Config doesn't exist case")
        void testNonExistentConfigKey() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.BAR, "RED,CAR");
            assertThatThrownBy(() -> config.getListOfStrings(TestConfig.FOO))
                    .isInstanceOf(ConfigKeyNotFoundException.class);
        }

        @Test
        @DisplayName("List of strings with space is trimmed")
        void testSpaceIsTrimmed() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED,YELLOW, APPLE");
            assertThat(config.getListOfStrings(TestConfig.FOO)).isEqualTo(ImmutableList.of("RED", "YELLOW", "APPLE"));
        }

        @Test
        @DisplayName("Single string with space is trimmed")
        void testSingletonListHasSpaceTrimmed() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, " RED ");
            assertThat(config.getListOfStrings(TestConfig.FOO)).isEqualTo(ImmutableList.of("RED"));
        }

        @Test
        @DisplayName("Comma-separated string can contain colons")
        void testCommaSeparatedStringsWithColons() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "key1:value1,key2:value2");
            assertThat(config.getListOfStrings(TestConfig.FOO)).containsExactly("key1:value1", "key2:value2");
        }

    }

    @Nested
    @DisplayName("test get List Of Integers")
    class ListOfIntegersTests {

        @Test
        @DisplayName("List of 3 comma-separated integers case")
        void testThreeInts() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "1234,321,266");
            assertThat(config.getListOfIntegers(TestConfig.FOO)).isEqualTo(ImmutableList.of(1234, 321, 266));
        }

        @Test
        @DisplayName("List of 3 colon-separated integers case")
        void testColonSeparatedThreeInts() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "1234:321:266");
            assertThat(config.getListOfIntegers(TestConfig.FOO)).isEqualTo(ImmutableList.of(1234, 321, 266));
        }

        @Test
        @DisplayName("Single case returns singleton list")
        void testSingleNumber() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "1234");
            assertThat(config.getListOfIntegers(TestConfig.FOO)).isEqualTo(ImmutableList.of(1234));
        }

        @Test
        @DisplayName("Empty case returns empty list")
        void testEmptyList() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "");
            assertThat(config.getListOfIntegers(TestConfig.FOO)).isEqualTo(ImmutableList.of());
        }

        @Test
        @DisplayName("Partly empty list returns non empty values")
        void testPartlyEmptyList() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "123,,456,");
            assertThat(config.getListOfIntegers(TestConfig.FOO)).isEqualTo(ImmutableList.of(123, 456));
        }

        @Test
        @DisplayName("Config doesn't exist case")
        void testUnknownConfig() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.BAR, "1234,321");
            assertThatThrownBy(() -> config.getListOfIntegers(TestConfig.FOO))
                    .isInstanceOf(ConfigKeyNotFoundException.class);
        }

        @Test
        @DisplayName("List of ints with space is trimmed")
        void testTrimSpacesInList() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "1234,321, 266");
            assertThat(config.getListOfIntegers(TestConfig.FOO)).isEqualTo(ImmutableList.of(1234, 321, 266));
        }

        @Test
        @DisplayName("Single int with space is trimmed")
        void testTrimSpaces() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, " 1234 ");
            assertThat(config.getListOfIntegers(TestConfig.FOO)).isEqualTo(ImmutableList.of(1234));
        }

    }

    @Nested
    @DisplayName("test get List Of StringIds")
    class ListOfStringIdsTests {

        @Test
        @DisplayName("List of 3 colon-separated strings case")
        void testColonSeparated() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED:YELLOW:APPLE");
            ImmutableList<StringId<Object>> expected = ImmutableList.of(StringId.create("RED"), StringId.create("YELLOW"), StringId.create("APPLE"));
            assertThat(config.getListOfStringIds(TestConfig.FOO)).isEqualTo(expected);
        }

        @Test
        @DisplayName("List of 3 comma-separated strings case")
        void testCommaSeparated() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED,YELLOW,APPLE");
            ImmutableList<StringId<Object>> expected = ImmutableList.of(StringId.create("RED"), StringId.create("YELLOW"), StringId.create("APPLE"));
            assertThat(config.getListOfStringIds(TestConfig.FOO)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Single case returns singleton list")
        void testSingletonList() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED");
            ImmutableList<StringId<Object>> listOfStrings = config.getListOfStringIds(TestConfig.FOO);
            assertThat(listOfStrings).isEqualTo(ImmutableList.of(StringId.create("RED")));
        }

        @Test
        @DisplayName("Empty string case returns empty list")
        void testEmptyString() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "");
            ImmutableList<StringId<Object>> listOfStrings = config.getListOfStringIds(TestConfig.FOO);
            assertThat(listOfStrings).isEqualTo(ImmutableList.of());
        }

        @Test
        @DisplayName("Config doesn't exist case")
        void testNonExistentConfig() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.BAR, "RED,CAR");
            assertThatThrownBy(() -> config.getListOfStringIds(TestConfig.FOO))
                    .isInstanceOf(ConfigKeyNotFoundException.class);
        }

        @Test
        @DisplayName("List of strings with space is trimmed")
        void testSpaceIsTrimmed() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED,YELLOW, APPLE");
            assertThat(config.getListOfStringIds(TestConfig.FOO)).isEqualTo(ImmutableList.of(StringId.create("RED"), StringId.create("YELLOW"), StringId.create("APPLE")));
        }

        @Test
        @DisplayName("Single string with space is trimmed")
        void testSpaceTrimmedFromSingleton() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, " RED ");
            assertThat(config.getListOfStringIds(TestConfig.FOO)).isEqualTo(ImmutableList.of(StringId.create("RED")));
        }

        @Test
        @DisplayName("Empty commas returns empty list")
        void testEmptyCommas() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, ",,,,");
            ImmutableList<StringId<Object>> listOfStrings = config.getListOfStringIds(TestConfig.FOO);
            assertThat(listOfStrings).isEqualTo(ImmutableList.of());
        }

        @Test
        @DisplayName("Empty colons returns empty list")
        void testEmptyColons() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, ":::::");
            ImmutableList<StringId<Object>> listOfStrings = config.getListOfStringIds(TestConfig.FOO);
            assertThat(listOfStrings).isEqualTo(ImmutableList.of());
        }

        @Test
        @DisplayName("IfPresent method returns empty list if not present")
        void testIfPresentWhenNotPresent() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.BAR, "RED,YELLOW,APPLE");
            assertThat(config.getListOfStringIdsIfPresent(TestConfig.FOO)).isEqualTo(ImmutableList.of());
        }

        @Test
        @DisplayName("IfPresent method returns list of ids if present")
        void testIfPresentWhenPresent() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED,YELLOW,APPLE");
            ImmutableList<StringId<Object>> expected = ImmutableList.of(StringId.create("RED"), StringId.create("YELLOW"), StringId.create("APPLE"));
            assertThat(config.getListOfStringIdsIfPresent(TestConfig.FOO)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Comma-separated lists can contain colons")
        void testCommaSeparatedListOfColons() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "host1:port1,host1:port2");
            ImmutableList<StringId<Object>> expected = ImmutableList.of(StringId.create("host1:port1"), StringId.create("host1:port2"));
            assertThat(config.getListOfStringIds(TestConfig.FOO)).isEqualTo(expected);
        }

    }

    @Nested
    @DisplayName("test get Set Of StringIds")
    class SetOfStringIdsTests {

        @Test
        @DisplayName("Set of 3 of the same string case")
        void testIdenticalStrings() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED,RED,RED");
            ImmutableSet<StringId<Object>> expected = ImmutableSet.of(StringId.create("RED"));
            assertThat(config.getSetOfStringIds(TestConfig.FOO)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Set of 3 comma-separated strings case")
        void testCommaSeparated() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED,YELLOW,APPLE");
            ImmutableSet<StringId<Object>> expected = ImmutableSet.of(StringId.create("RED"), StringId.create("YELLOW"), StringId.create("APPLE"));
            assertThat(config.getSetOfStringIds(TestConfig.FOO)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Single case returns singleton set")
        void testSingletonSet() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED");
            ImmutableSet<StringId<Object>> setOfStrings = config.getSetOfStringIds(TestConfig.FOO);
            assertThat(setOfStrings).isEqualTo(ImmutableSet.of(StringId.create("RED")));
        }

        @Test
        @DisplayName("Empty string case returns empty set")
        void testEmptyString() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "");
            ImmutableSet<StringId<Object>> setOfStrings = config.getSetOfStringIds(TestConfig.FOO);
            assertThat(setOfStrings).isEqualTo(ImmutableSet.of());
        }

        @Test
        @DisplayName("Config doesn't exist case")
        void testNonExistentConfigKey() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.BAR, "RED,CAR");
            assertThatThrownBy(() -> config.getSetOfStringIds(TestConfig.FOO))
                    .isInstanceOf(ConfigKeyNotFoundException.class);
        }

        @Test
        @DisplayName("Set of strings with space is trimmed")
        void testSetWithSpaces() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED,YELLOW, APPLE");
            assertThat(config.getSetOfStringIds(TestConfig.FOO)).isEqualTo(ImmutableSet.of(StringId.create("RED"), StringId.create("YELLOW"), StringId.create("APPLE")));
        }

        @Test
        @DisplayName("Single string with space is trimmed")
        void testSingleStringWithSpace() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, " RED ");
            assertThat(config.getSetOfStringIds(TestConfig.FOO)).isEqualTo(ImmutableSet.of(StringId.create("RED")));
        }

        @Test
        @DisplayName("Empty commas returns empty set")
        void testEmptyCommas() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, ",,,,");
            ImmutableSet<StringId<Object>> setOfStrings = config.getSetOfStringIds(TestConfig.FOO);
            assertThat(setOfStrings).isEqualTo(ImmutableSet.of());
        }

        @Test
        @DisplayName("Empty colons returns empty set")
        void testEmptyColons() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "::::");
            ImmutableSet<StringId<Object>> setOfStrings = config.getSetOfStringIds(TestConfig.FOO);
            assertThat(setOfStrings).isEqualTo(ImmutableSet.of());
        }

        @Test
        @DisplayName("IfPresent method returns empty set if not present")
        void testIfPresentWhenNotPresent() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.BAR, "RED,YELLOW,APPLE");
            assertThat(config.getSetOfStringIdsIfPresent(TestConfig.FOO)).isEqualTo(ImmutableSet.of());
        }

        @Test
        @DisplayName("IfPresent method returns set of ids if present")
        void testIfPresentWhenPresent() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED,YELLOW,APPLE");
            ImmutableSet<StringId<Object>> expected = ImmutableSet.of(StringId.create("RED"), StringId.create("YELLOW"), StringId.create("APPLE"));
            assertThat(config.getSetOfStringIdsIfPresent(TestConfig.FOO)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Set of 3 colon-separated strings case")
        void testColonSeparated() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED:YELLOW:APPLE");
            ImmutableSet<StringId<Object>> expected = ImmutableSet.of(StringId.create("RED"), StringId.create("YELLOW"), StringId.create("APPLE"));
            assertThat(config.getSetOfStringIds(TestConfig.FOO)).isEqualTo(expected);
        }

    }

    @Nested
    @DisplayName("test get List Of Ids")
    class ListOfIdsTests {

        @Test
        @DisplayName("List of 3 colon-separated ids case")
        void testColonSeparated() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "1:2:3");
            ImmutableList<Id<Object>> expected = ImmutableList.of(Id.create(1), Id.create(2), Id.create(3));
            assertThat(config.getListOfIds(TestConfig.FOO)).isEqualTo(expected);
        }

        @Test
        @DisplayName("List of 3 comma-separated ids case")
        void testCommaSeparated() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "1,2,3");
            ImmutableList<Id<Object>> expected = ImmutableList.of(Id.create(1), Id.create(2), Id.create(3));
            assertThat(config.getListOfIds(TestConfig.FOO)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Single case returns singleton list")
        void testSingletonList() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "1");
            ImmutableList<Id<Object>> listOfStrings = config.getListOfIds(TestConfig.FOO);
            assertThat(listOfStrings).isEqualTo(ImmutableList.of(Id.create(1)));
        }

        @Test
        @DisplayName("Empty string case returns empty list")
        void testEmptyString() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "");
            ImmutableList<Id<Object>> listOfStrings = config.getListOfIds(TestConfig.FOO);
            assertThat(listOfStrings).isEqualTo(ImmutableList.of());
        }

        @Test
        @DisplayName("Config doesn't exist case")
        void testNonExistentConfig() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.BAR, "1,2");
            assertThatThrownBy(() -> config.getListOfIds(TestConfig.FOO))
                    .isInstanceOf(ConfigKeyNotFoundException.class);
        }

        @Test
        @DisplayName("List of ids with space is trimmed")
        void testSpacesTrimmed() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "1,2, 3");
            assertThat(config.getListOfIds(TestConfig.FOO)).isEqualTo(ImmutableList.of(Id.create(1), Id.create(2), Id.create(3)));
        }

        @Test
        @DisplayName("Single id with space is trimmed")
        void testSpacesTrimmedWithSingleton() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, " 1 ");
            assertThat(config.getListOfIds(TestConfig.FOO)).isEqualTo(ImmutableList.of(Id.create(1)));
        }

        @Test
        @DisplayName("Empty separators returns empty list")
        void testEmptyCommas() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, ",,,,");
            ImmutableList<Id<Object>> listOfStrings = config.getListOfIds(TestConfig.FOO);
            assertThat(listOfStrings).isEqualTo(ImmutableList.of());
        }

        @Test
        @DisplayName("Empty separators returns empty list")
        void testEmptyColons() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "::::");
            ImmutableList<Id<Object>> listOfStrings = config.getListOfIds(TestConfig.FOO);
            assertThat(listOfStrings).isEqualTo(ImmutableList.of());
        }

        @Test
        @DisplayName("List of strings throws exception")
        void testNonNumericStringsThroughsException() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED,YELLOW");
            assertThatThrownBy(() -> config.getListOfIds(TestConfig.FOO))
                    .isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("IfPresent method returns empty list if not present")
        void testIfPresentWhenNotPresent() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.BAR, "1,2,3");
            assertThat(config.getListOfIdsIfPresent(TestConfig.FOO)).isEqualTo(ImmutableList.of());
        }

        @Test
        @DisplayName("IfPresent method returns list of ids if present")
        void testIfPresentWhenPresent() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "1,2,3");
            ImmutableList<Id<Object>> expected = ImmutableList.of(Id.create(1), Id.create(2), Id.create(3));
            assertThat(config.getListOfIdsIfPresent(TestConfig.FOO)).isEqualTo(expected);
        }

    }

    @Nested
    @DisplayName("test get Set Of Ids")
    class SetOfIdsTests {

        @Test
        @DisplayName("Set of 3 of the same id case")
        void testIdenticalValues() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "1,1,1");
            ImmutableSet<Id<Object>> expected = ImmutableSet.of(Id.create(1));
            assertThat(config.getSetOfIds(TestConfig.FOO)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Set of 3 comma-separated ids case")
        void testCommaSeparatedValues() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "1,2,3");
            ImmutableSet<Id<Object>> expected = ImmutableSet.of(Id.create(1), Id.create(2), Id.create(3));
            assertThat(config.getSetOfIds(TestConfig.FOO)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Set of 3 colon-separated ids case")
        void testColonSeparatedValues() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "1:2:3");
            ImmutableSet<Id<Object>> expected = ImmutableSet.of(Id.create(1), Id.create(2), Id.create(3));
            assertThat(config.getSetOfIds(TestConfig.FOO)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Single case returns singleton set")
        void testSingleton() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "1");
            ImmutableSet<Id<Object>> setOfStrings = config.getSetOfIds(TestConfig.FOO);
            assertThat(setOfStrings).isEqualTo(ImmutableSet.of(Id.create(1)));
        }

        @Test
        @DisplayName("Empty string case returns empty set")
        void testEmptyString() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "");
            ImmutableSet<Id<Object>> setOfStrings = config.getSetOfIds(TestConfig.FOO);
            assertThat(setOfStrings).isEqualTo(ImmutableSet.of());
        }

        @Test
        @DisplayName("Config doesn't exist case")
        void testNonExistentConfigKey() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.BAR, "1,2");
            assertThatThrownBy(() -> config.getSetOfIds(TestConfig.FOO))
                    .isInstanceOf(ConfigKeyNotFoundException.class);
        }

        @Test
        @DisplayName("Set of ids with space is trimmed")
        void testSpacesAreTrimmed() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "1,2, 3");
            assertThat(config.getSetOfIds(TestConfig.FOO)).isEqualTo(ImmutableSet.of(Id.create(1), Id.create(2), Id.create(3)));
        }

        @Test
        @DisplayName("Single id with space is trimmed")
        void testSpacesAreTrimmedWithSingleton() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, " 1 ");
            assertThat(config.getSetOfIds(TestConfig.FOO)).isEqualTo(ImmutableSet.of(Id.create(1)));
        }

        @Test
        @DisplayName("Empty separators returns empty set")
        void testEmptyCommasReturnsEmptySet() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, ",,,,");
            ImmutableSet<Id<Object>> setOfStrings = config.getSetOfIds(TestConfig.FOO);
            assertThat(setOfStrings).isEqualTo(ImmutableSet.of());
        }

        @Test
        @DisplayName("Empty separators returns empty set")
        void testEmptyColonsReturnsEmptySet() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "::::");
            ImmutableSet<Id<Object>> setOfStrings = config.getSetOfIds(TestConfig.FOO);
            assertThat(setOfStrings).isEqualTo(ImmutableSet.of());
        }

        @Test
        @DisplayName("Set of strings throws exception")
        void testStringsThrowsException() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "RED,YELLOW");
            assertThatThrownBy(() -> config.getSetOfIds(TestConfig.FOO))
                    .isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("IfPresent method returns empty set if not present")
        void testIfPresentWhenNotPresent() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.BAR, "1,2,3");
            assertThat(config.getSetOfIdsIfPresent(TestConfig.FOO)).isEqualTo(ImmutableSet.of());
        }

        @Test
        @DisplayName("IfPresent method returns set of ids if present")
        void testIfPresentWhenPresent() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "1,2,3");
            ImmutableSet<Id<Object>> expected = ImmutableSet.of(Id.create(1), Id.create(2), Id.create(3));
            assertThat(config.getSetOfIdsIfPresent(TestConfig.FOO)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("for Map values")
    class MapConfigTests {

        abstract class CommonMapTests<K, V> {

            static final String SIMPLE_CONFIG_VALUE = "1=True;2=False;3=False;4=False";

            abstract ImmutableMap<K, V> readMap(String configValue, Enum<?> keyToWrite, Enum<?> keyToRead);

            abstract void verifyMapIsComplete(ImmutableMap<K, V> map);

            /** Convenience overload that reads and writes the config value to TestConfig.FOO */
            ImmutableMap<K, V> readMap(String configValue) {
                return readMap(configValue, TestConfig.FOO, TestConfig.FOO);
            }

            @Test
            @DisplayName("works with valid config")
            void withValidConfig() {
                ImmutableMap<K, V> map = readMap(SIMPLE_CONFIG_VALUE);
                verifyMapIsComplete(map);
            }

            @Test
            @DisplayName("returns an empty map if config key does not exist")
            void configKeyNotPresent() {
                ImmutableMap<K, V> map = readMap(SIMPLE_CONFIG_VALUE, TestConfig.FOO, TestConfig.BAR);
                assertThat(map).isEmpty();
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
                verifyMapIsComplete(map);
            }

            @Test
            @DisplayName("ignores empty entries")
            void emptyEntries() {
                ImmutableMap<K, V> map = readMap("1=True;;2=False;3=False;4=False");
                verifyMapIsComplete(map);
            }

            @Test
            @DisplayName("trims whitespace")
            void trimsWhitespace() {
                ImmutableMap<K, V> map = readMap(" 1 = True ; 2 = False ; 3 = False ; 4 = False ");
                verifyMapIsComplete(map);
            }

            @Test
            @DisplayName("entries with missing keys are ignored")
            void ignoresMissingKeys() {
                ImmutableMap<K, V> map = readMap("=False;1=True;2=False;3=False;4=False");
                verifyMapIsComplete(map);
            }
        }

        @Nested
        @DisplayName("read as a String map")
        class StringMapTests extends CommonMapTests<String, String> {

            @Override
            ImmutableMap<String, String> readMap(String configValue, Enum<?> keyToWrite, Enum<?> keyToRead) {
                Config<TestConfig> config = generateConfigWithEntry(keyToWrite, configValue);
                return config.getStringMap(keyToRead);
            }

            @Override
            void verifyMapIsComplete(ImmutableMap<String, String> map) {
                assertThat(map).containsOnlyKeys("1", "2", "3", "4");
                assertThat(map).containsEntry("1", "True");
                assertThat(map).containsEntry("2", "False");
                assertThat(map).containsEntry("3", "False");
                assertThat(map).containsEntry("4", "False");
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
        @DisplayName("read as a typed map")
        class TypedMapTests extends CommonMapTests<Integer, Boolean> {

            @Override
            ImmutableMap<Integer, Boolean> readMap(String configValue, Enum<?> keyToWrite, Enum<?> keyToRead) {
                Config<TestConfig> config = generateConfigWithEntry(keyToWrite, configValue);
                return config.getMap(keyToRead, Integer::valueOf, Boolean::parseBoolean);
            }

            @Override
            void verifyMapIsComplete(ImmutableMap<Integer, Boolean> map) {
                assertThat(map).containsOnlyKeys(1, 2, 3, 4);
                assertThat(map).containsEntry(1, true);
                assertThat(map).containsEntry(2, false);
                assertThat(map).containsEntry(3, false);
                assertThat(map).containsEntry(4, false);
            }

            @Test
            @DisplayName("applies valueParser to empty string for missing values")
            void handlesMissingValues() {

                Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "1;2=");
                Function<String, String> identityFunctionWithAssertionStringIsEmpty = v -> {
                    assertThat(v).isEmpty();
                    return v;
                };
                config.getMap(TestConfig.FOO, Integer::valueOf, identityFunctionWithAssertionStringIsEmpty);
            }

            @Test
            @DisplayName("throws exceptions generated by key parser function")
            void throwsKeyParserExceptions() {
                Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, SIMPLE_CONFIG_VALUE);

                assertThatThrownBy(() -> config.getMap(TestConfig.FOO, s -> Failer.fail("Boom"), Boolean::parseBoolean))
                        .hasMessageContaining("Boom");
            }

            @Test
            @DisplayName("throws exceptions generated by key parser function")
            void throwsValueParserExceptions() {
                Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, SIMPLE_CONFIG_VALUE);

                assertThatThrownBy(() -> config.getMap(TestConfig.FOO, Integer::valueOf, s -> Failer.fail("Boom")))
                        .hasMessageContaining("Boom");
            }
        }
    }

    @Nested
    @DisplayName("toString() method")
    class ToStringMethodTest {

        @Test
        @DisplayName("contains type and empty list of properties for empty config")
        void emptyConfig() {
            assertThat(Config.empty(TestConfig.class).toString()).isEqualToIgnoringWhitespace("TestConfig{}");
        }

        @Test
        @DisplayName("includes the specified config entries")
        void containsConfigEntries() {
            Config<TestConfig> config = generateConfigWithEntry(TestConfig.FOO, "foo");
            assertThat(config.toString()).isEqualToIgnoringWhitespace("TestConfig{TestConfig.FOO=foo}");
        }

        @Test
        @DisplayName("provides the full key name of nested enums")
        void nestedEnums() {
            Config<TestConfig> config = SimpleConfigBuilder.create(TestConfig.class).put("TestConfig.FirstSubConfig.SubSubConfig.X", "x").buildWrapped();
            assertThat(config.toString()).contains("TestConfig.FirstSubConfig.SubSubConfig.X=x");
        }

        @Test
        @DisplayName("lists one config entry per-line")
        void listsConfigOnIndividualLines() {
            Config<TestConfig> config = SimpleConfigBuilder.create(TestConfig.class)
                    .put("TestConfig.FOO", "foo")
                    .put("TestConfig.BAR", "bar")
                    .buildWrapped();

            assertThat(config.toString())
                    .contains("\nTestConfig.FOO=foo\n")
                    .contains("\nTestConfig.BAR=bar\n")
                    .hasLineCount(4);
        }

        @Test
        @DisplayName("does not contain value of SecretConfig")
        void secretConfig() {
            Config<TestConfig> config = SimpleConfigBuilder.create(TestConfig.class)
                    .put("TestConfig.SECRET_1", "secret1")
                    .put("TestConfig.FirstSubConfig.SECRET_2", "s2")
                    .buildWrapped();

            assertThat(config.toString())
                    .doesNotContain("TestConfig.SECRET_1")
                    .doesNotContain("TestConfig.FirstSubConfig.SECRET_2");
        }

        @Test
        @DisplayName("does not contain value of SecretConfig but does contain value of non-secret config")
        void secretAndClearConfig() {
            Config<TestConfig> config = SimpleConfigBuilder.create(TestConfig.class)
                    .put("TestConfig.SECRET_1", "secret1")
                    .put("TestConfig.FOO", "foo")
                    .buildWrapped();

            assertThat(config.toString())
                    .doesNotContain("TestConfig.SECRET_1")
                    .contains("TestConfig.FOO=foo");
        }
    }

    private static Config<TestConfig> generateConfigWithEntry(Enum<?> key, String value) {
        return generateConfigWithEntry(key, value, null, null);
    }

    private static Config<TestConfig> generateConfigWithEntry(Enum<?> key, String value, TimeUnit timeUnit) {
        return generateConfigWithEntry(key, value, timeUnit, null);
    }

    private static Config<TestConfig> generateConfigWithEntry(Enum<?> key, String value, LengthUnit lengthUnit) {
        return generateConfigWithEntry(key, value, null, lengthUnit);
    }

    private static Config<TestConfig> generateConfigWithEntry(Enum<?> key, String value, TimeUnit timeUnit, LengthUnit lengthUnit) {
        return SimpleConfigBuilder.create(TestConfig.class)
                .put(key.getClass().getSimpleName() + "." + key.name(), value)
                .setTimeUnit(timeUnit)
                .setLengthUnit(lengthUnit)
                .buildWrapped();
    }
}
