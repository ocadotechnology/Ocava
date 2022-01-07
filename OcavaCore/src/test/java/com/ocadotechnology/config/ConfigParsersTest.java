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
package com.ocadotechnology.config;

import static com.ocadotechnology.config.ConfigParsers.getListOf;
import static com.ocadotechnology.config.ConfigParsers.getListOfDoubles;
import static com.ocadotechnology.config.ConfigParsers.getListOfEnums;
import static com.ocadotechnology.config.ConfigParsers.getListOfIds;
import static com.ocadotechnology.config.ConfigParsers.getListOfIntegers;
import static com.ocadotechnology.config.ConfigParsers.getListOfLongs;
import static com.ocadotechnology.config.ConfigParsers.getListOfStringIds;
import static com.ocadotechnology.config.ConfigParsers.getListOfStrings;
import static com.ocadotechnology.config.ConfigParsers.getSetOf;
import static com.ocadotechnology.config.ConfigParsers.getSetOfDoubles;
import static com.ocadotechnology.config.ConfigParsers.getSetOfEnums;
import static com.ocadotechnology.config.ConfigParsers.getSetOfIds;
import static com.ocadotechnology.config.ConfigParsers.getSetOfIntegers;
import static com.ocadotechnology.config.ConfigParsers.getSetOfLongs;
import static com.ocadotechnology.config.ConfigParsers.getSetOfStringIds;
import static com.ocadotechnology.config.ConfigParsers.getSetOfStrings;
import static com.ocadotechnology.config.ConfigParsers.parseAcceleration;
import static com.ocadotechnology.config.ConfigParsers.parseBoolean;
import static com.ocadotechnology.config.ConfigParsers.parseDouble;
import static com.ocadotechnology.config.ConfigParsers.parseDuration;
import static com.ocadotechnology.config.ConfigParsers.parseEnum;
import static com.ocadotechnology.config.ConfigParsers.parseFractionalTime;
import static com.ocadotechnology.config.ConfigParsers.parseInt;
import static com.ocadotechnology.config.ConfigParsers.parseLength;
import static com.ocadotechnology.config.ConfigParsers.parseLong;
import static com.ocadotechnology.config.ConfigParsers.parseMap;
import static com.ocadotechnology.config.ConfigParsers.parseSpeed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.config.TestConfig.Colours;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.StringId;
import com.ocadotechnology.physics.units.LengthUnit;

class ConfigParsersTest {

    private static final String LIST_INT_VALUES = "1,1,2,3,5,8,12,2147483647";
    private static final String LIST_STRING_VALUES = "1,A,A,Hello my name is, Bob";
    private static final String LIST_LONG_VALUES = "1,1,2,3,5,8,12,9223372036854775807";
    private static final String LIST_DOUBLE_VALUES = "1.0,1.0,2.0,3.0,5.0,8.0,12.0,9223372036854775808.5";
    private static final String LIST_ENUM_VALUES = "RED, RED, BLUE ";

    @Nested
    class ParseInt {

        @ParameterizedTest
        @ValueSource(strings = {"MAX", "max", "MaX"})
        void testMax(String value) {
            assertThat(parseInt(value)).isEqualTo(Integer.MAX_VALUE);
        }

        @ParameterizedTest
        @ValueSource(strings = {"MIN", "min", "Min"})
        void testMin(String value) {
            assertThat(parseInt(value)).isEqualTo(Integer.MIN_VALUE);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE})
        void testParses(Integer value) {
            assertThat(parseInt(value.toString())).isEqualTo(value);
        }

        @Test
        void testNotAnNumber() {
            assertThatThrownBy(() -> parseInt("a"))
                    .isInstanceOf(NumberFormatException.class);
        }

        @Test
        void testTooBig() {
            assertThatThrownBy(() -> parseInt(Long.toString((long) Integer.MAX_VALUE + 1)))
                    .isInstanceOf(NumberFormatException.class);
        }

        @Test
        void testTooSmall() {
            assertThatThrownBy(() -> parseInt(Long.toString((long) Integer.MIN_VALUE - 1)))
                    .isInstanceOf(NumberFormatException.class);
        }
    }

    @Nested
    class ParseLong {

        @ParameterizedTest
        @ValueSource(strings = {"MAX", "max", "MaX"})
        void testMax(String value) {
            assertThat(parseLong(value)).isEqualTo(Long.MAX_VALUE);
        }

        @ParameterizedTest
        @ValueSource(strings = {"MIN", "min", "Min"})
        void testMin(String value) {
            assertThat(parseLong(value)).isEqualTo(Long.MIN_VALUE);
        }

        @ParameterizedTest
        @ValueSource(longs = {0, 1, -1, Long.MAX_VALUE, Long.MIN_VALUE})
        void testParses(Long value) {
            assertThat(parseLong(value.toString())).isEqualTo(value);
        }

        @Test
        void testNotAnNumber() {
            assertThatThrownBy(() -> parseLong("a"))
                    .isInstanceOf(NumberFormatException.class);
        }

        @Test
        void testTooBig() {
            assertThatThrownBy(() -> parseLong(Double.toString((double) Long.MAX_VALUE + 1)))
                    .isInstanceOf(NumberFormatException.class);
        }

        @Test
        void testTooSmall() {
            assertThatThrownBy(() -> parseLong(Double.toString((double) Long.MIN_VALUE - 1)))
                    .isInstanceOf(NumberFormatException.class);
        }
    }

    @Nested
    class ParseDouble {

        @ParameterizedTest
        @ValueSource(doubles = {0.5, 1, -1E23, Double.MAX_VALUE, -Double.MAX_VALUE})
        void testParses(Double value) {
            assertThat(parseDouble(value.toString())).isEqualTo(value);
        }

        @Test
        void testNotAnNumber() {
            assertThatThrownBy(() -> parseDouble("a"))
                    .isInstanceOf(NumberFormatException.class);
        }
    }

    @Nested
    class ParseBoolean {

        @Test
        void testTrue() {
            assertThat(parseBoolean("true")).isTrue();
            assertThat(parseBoolean("TRUE")).isTrue();
            assertThat(parseBoolean("TrUe")).isTrue();
        }

        @Test
        void testFalse() {
            assertThat(parseBoolean("false")).isFalse();
            assertThat(parseBoolean("FALSE")).isFalse();
            assertThat(parseBoolean("FaLsE")).isFalse();
        }

        @Test
        void testInvalid() {
            assertThatThrownBy(() -> parseBoolean("not a boolean"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid boolean value");
            assertThatThrownBy(() -> parseBoolean("0"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid boolean value");
        }
    }

    @Nested
    class ParseLength {

        @Test
        void testParseWithNoUnit() {
            assertThat(parseLength("1", LengthUnit.METERS)).isEqualTo(1D);
        }

        @Test
        void testParseWithUnits() {
            assertThat(parseLength("1, CENTIMETERS", LengthUnit.METERS)).isEqualTo(0.01D);
        }

        @Test
        void testInvalid() {
            assertThatThrownBy(() -> parseLength("not a number", LengthUnit.METERS))
                    .isInstanceOf(NumberFormatException.class);
            assertThatThrownBy(() -> parseLength("0, a, b, c", LengthUnit.METERS))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("need to be specified without units");
        }
    }

    @Nested
    class ParseFractionalTime {

        @Test
        void testParseWithNoUnit() {
            assertThat(parseFractionalTime("1", TimeUnit.SECONDS)).isEqualTo(1D);
        }

        @Test
        void testParseWithUnits() {
            assertThat(parseFractionalTime("1, MINUTES", TimeUnit.SECONDS)).isEqualTo(60D);
            assertThat(parseFractionalTime("1.5, MINUTES", TimeUnit.SECONDS)).isEqualTo(90D);
        }

        @Test
        void testInvalid() {
            assertThatThrownBy(() -> parseFractionalTime("not a number", TimeUnit.MINUTES))
                    .isInstanceOf(NumberFormatException.class);
            assertThatThrownBy(() -> parseFractionalTime("0, a, b, c", TimeUnit.MINUTES))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("need to be specified without units");
        }
    }

    @Nested
    class ParseDuration {

        @Test
        void testParseWithNoUnit() {
            assertThat(parseDuration("1")).isEqualTo(Duration.ofSeconds(1));
        }

        @Test
        void testParseWithUnits() {
            assertThat(parseDuration("1, MINUTES")).isEqualTo(Duration.ofMinutes(1));
        }

        @Test
        void testInvalid() {
            assertThatThrownBy(() -> parseDuration("not a number"))
                    .isInstanceOf(NumberFormatException.class);
            assertThatThrownBy(() -> parseDuration("0, a, b, c"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("need to be specified without units");
        }
    }

    @Nested
    class ParseSpeed {

        @Test
        void testParseWithNoUnit() {
            assertThat(parseSpeed("1", LengthUnit.METERS, TimeUnit.SECONDS)).isEqualTo(1D);
        }

        @Test
        void testParseWithUnits() {
            assertThat(parseSpeed("1, METERS, MINUTES", LengthUnit.METERS, TimeUnit.SECONDS)).isCloseTo(0.0166, Offset.offset(0.001));
        }

        @Test
        void testInvalid() {
            assertThatThrownBy(() -> parseSpeed("not a number", LengthUnit.METERS, TimeUnit.MINUTES))
                    .isInstanceOf(NumberFormatException.class);
            assertThatThrownBy(() -> parseSpeed("0, a, b, c", LengthUnit.METERS, TimeUnit.MINUTES))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("need to be specified without units");
        }
    }

    @Nested
    class ParseAcceleration {

        @Test
        void testParseWithNoUnit() {
            assertThat(parseAcceleration("1", LengthUnit.METERS, TimeUnit.SECONDS)).isEqualTo(1D);
        }

        @Test
        void testParseWithUnits() {
            assertThat(parseAcceleration("1, METERS, MINUTES", LengthUnit.METERS, TimeUnit.SECONDS)).isCloseTo(2.777E-4, Offset.offset(0.001E-4));
        }

        @Test
        void testInvalid() {
            assertThatThrownBy(() -> parseAcceleration("not a number", LengthUnit.METERS, TimeUnit.MINUTES))
                    .isInstanceOf(NumberFormatException.class);
            assertThatThrownBy(() -> parseAcceleration("0, a, b, c", LengthUnit.METERS, TimeUnit.MINUTES))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("need to be specified without units");
        }
    }

    @Nested
    class ParseEnum {

        @Test
        void testParses() {
            assertThat(parseEnum("BLUE", TestConfig.Colours.class)).isEqualTo(Colours.BLUE);
        }

        @Test
        void testInvalid() {
            assertThatThrownBy(() -> parseEnum("not a colour", Colours.class))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class GetListOf {

        @Test
        void testEmptyString() {
            assertThat(getListOf(Function.identity()).apply("")).isEmpty();
        }

        @Test
        void testEmptyCommas() {
            assertThat(getListOf(Function.identity()).apply(",,,")).isEmpty();
        }

        @Test
        void testEmptyColons() {
            assertThat(getListOf(Function.identity()).apply(":::")).isEmpty();
        }

        @Test
        void testOfStrings() {
            assertThat(getListOfStrings().apply(LIST_STRING_VALUES)).containsExactly("1", "A", "A", "Hello my name is", "Bob");
        }

        @Test
        void testOfIntegers() {
            assertThat(getListOfIntegers().apply(LIST_INT_VALUES)).containsExactly(1, 1, 2, 3, 5, 8, 12, Integer.MAX_VALUE);
        }

        @Test
        void testOfLongs() {
            assertThat(getListOfLongs().apply(LIST_LONG_VALUES)).containsExactly(1L, 1L, 2L, 3L, 5L, 8L, 12L, Long.MAX_VALUE);
        }

        @Test
        void testOfDoubles() {
            assertThat(getListOfDoubles().apply(LIST_DOUBLE_VALUES)).containsExactly(1D, 1D, 2D, 3D, 5D, 8D, 12D, (double) Long.MAX_VALUE + 1.5D);
        }

        @Test
        void testOfIds() {
            assertThat(getListOfIds().apply(LIST_LONG_VALUES)).containsAll(Stream.of(1L, 1L, 2L, 3L, 5L, 8L, 12L, Long.MAX_VALUE).map(Id::create).collect(Collectors.toList()));
        }

        @Test
        void testOfStringIds() {
            assertThat(getListOfStringIds().apply(LIST_STRING_VALUES)).containsAll(Stream.of("1", "A", "A", "Hello my name is", "Bob").map(StringId::create).collect(Collectors.toList()));
        }

        @Test
        void testOfEnums() {
            assertThat(getListOfEnums(Colours.class).apply(LIST_ENUM_VALUES)).contains(Colours.RED, Colours.RED, Colours.BLUE);
        }
    }

    @Nested
    class GetSetOf {

        @Test
        void testEmptyString() {
            assertThat(getSetOf(Function.identity()).apply("")).isEmpty();
        }

        @Test
        void testEmptyCommas() {
            assertThat(getSetOf(Function.identity()).apply(",,,")).isEmpty();
        }

        @Test
        void testEmptyColons() {
            assertThat(getSetOf(Function.identity()).apply(":::")).isEmpty();
        }

        @Test
        void testOfStrings() {
            assertThat(getSetOfStrings().apply(LIST_STRING_VALUES)).containsExactly("1", "A", "Hello my name is", "Bob");
        }

        @Test
        void testOfIntegers() {
            assertThat(getSetOfIntegers().apply(LIST_INT_VALUES)).containsExactly(1, 2, 3, 5, 8, 12, Integer.MAX_VALUE);
        }

        @Test
        void testOfLongs() {
            assertThat(getSetOfLongs().apply(LIST_LONG_VALUES)).containsExactly(1L, 2L, 3L, 5L, 8L, 12L, Long.MAX_VALUE);
        }

        @Test
        void testOfDoubles() {
            assertThat(getSetOfDoubles().apply(LIST_DOUBLE_VALUES)).containsExactly(1D, 2D, 3D, 5D, 8D, 12D, (double) Long.MAX_VALUE + 1.5D);
        }

        @Test
        void testOfIds() {
            assertThat(getSetOfIds().apply(LIST_LONG_VALUES)).containsAll(Stream.of(1L, 2L, 3L, 5L, 8L, 12L, Long.MAX_VALUE).map(Id::create).collect(Collectors.toList()));
        }

        @Test
        void testOfStringIds() {
            assertThat(getSetOfStringIds().apply(LIST_STRING_VALUES)).containsAll(Stream.of("1", "A", "Hello my name is", "Bob").map(StringId::create).collect(Collectors.toList()));
        }

        @Test
        void testOfEnums() {
            assertThat(getSetOfEnums(Colours.class).apply(LIST_ENUM_VALUES)).contains(Colours.RED, Colours.BLUE);
        }
    }

    @Nested
    class ParseMap {

        @Test
        void testParses() {
            ImmutableMap<String, String> parsed = parseMap("key1=value1;key2=value2", Function.identity(), Function.identity());

            assertThat(parsed).containsOnlyKeys("key1", "key2");
            assertThat(parsed).containsEntry("key1", "value1");
            assertThat(parsed).containsEntry("key2", "value2");
        }

        @Test
        void testList() {
            ImmutableMap<String, ImmutableList<String>> parsed = parseMap("1=a,b;2=c,d", Function.identity(), getListOfStrings());
            assertThat(parsed).containsOnlyKeys("1", "2");
            assertThat(parsed).containsEntry("1", ImmutableList.of("a", "b"));
            assertThat(parsed).containsEntry("2", ImmutableList.of("c", "d"));
        }

        @Test
        void testMissingEquals() {
            ImmutableMap<String, String> parsed = parseMap("key1=value1;key2 value2", Function.identity(), Function.identity());

            assertThat(parsed).containsOnlyKeys("key1");
            assertThat(parsed).containsEntry("key1", "value1");
        }
    }
}

