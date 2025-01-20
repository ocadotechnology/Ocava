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
package com.ocadotechnology.config;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.ocadotechnology.maths.stats.Probability;
import com.ocadotechnology.physics.units.LengthUnit;

/**
 * Parser class to convert a config value into a typed optional result. All parsing methods will return {@link
 * Optional#empty()} if the value is am empty String.
 */
public class OptionalValueParser {
    private final Optional<StrictValueParser> parser;

    @VisibleForTesting
    OptionalValueParser(Enum<?> key, String value) {
        this(key, value, null, null);
    }

    OptionalValueParser(Enum<?> key, String value, @Nullable TimeUnit timeUnit, @Nullable LengthUnit lengthUnit) {
        if (value.isEmpty()) {
            parser = Optional.empty();
        } else {
            parser = Optional.of(new StrictValueParser(key, value, timeUnit, lengthUnit));
        }
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing the the config value.
     */
    public Optional<String> asString() {
        return parser.map(StrictValueParser::asString);
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing the string config value parsed to a boolean.
     * @throws IllegalStateException if the config value does not strictly equal "true" or "false", case insensitive.
     */
    public Optional<Boolean> asBoolean() {
        return parser.map(StrictValueParser::asBoolean);
    }

    /**
     * @return {@link OptionalInt#empty()} if the config value is an empty String, otherwise returns an {@link
     *          OptionalInt} containing the string config value parsed to an integer. If the value is the String "max"
     *          or "min" (case insensitive) parses the value to {@link Integer#MAX_VALUE} or {@link Integer#MIN_VALUE}
     *          respectively, otherwise defers to {@link Integer#parseInt(String)}.
     * @throws IllegalStateException if the config value cannot be parsed to an integer.
     */
    public OptionalInt asInt() {
        return parser.map(p -> OptionalInt.of(p.asInt())).orElse(OptionalInt.empty());
    }

    /**
     * @return {@link OptionalLong#empty()} if the config value is an empty String, otherwise returns an {@link
     *          OptionalLong} containing the string config value parsed to a long. If the value is the String "max" or
     *          "min" (case insensitive) parses the value to {@link Long#MAX_VALUE} or {@link Long#MIN_VALUE}
     *          respectively, otherwise defers to {@link Long#parseLong(String)}.
     * @throws IllegalStateException if the config value cannot be parsed to a long.
     */
    public OptionalLong asLong() {
        return parser.map(p -> OptionalLong.of(p.asLong())).orElse(OptionalLong.empty());
    }

    /**
     * @return {@link OptionalDouble#empty()}  if the config value is an empty string, otherwise the {@link OptionalDouble}
     *          resulting from parsing the string config value parsed to a fraction via {@link ConfigParsers#parseFraction(String)}.
     *          In particular, that will parse it as a Double first, and then validate that it lies between 0 and 1.
     * @throws IllegalStateException if
     *                                  - the config value cannot be parsed to a double.
     *                                  - the config value does not lie between 0 and 1.
     */
    public OptionalDouble asFraction() {
        return parser.map(StrictValueParser::asFraction).map(OptionalDouble::of).orElse(OptionalDouble.empty());
    }

    /**
     * @return {@link Optional#empty()}  if the config value is an empty string, otherwise the {@link Optional}
     *          resulting from parsing the string config value parsed to a {@link Probability} via {@link ConfigParsers#parseProbability(String)}.
     * @throws IllegalStateException if
     *                                  - the config value cannot be parsed to a {@link Probability}.
     *                                  - the input is not between 0.0 and 1.0, or 0.0% and 100.0%.
     */
    public Optional<Probability> asProbability() {
        return parser.map(StrictValueParser::asProbability);
    }

    /**
     * @return {@link OptionalDouble#empty()} if the config value is an empty String, otherwise returns an {@link
     *          OptionalDouble} containing the string config value parsed to a double via {@link
     *          Double#parseDouble(String)}.
     * @throws IllegalStateException if the config value cannot be parsed to a double.
     */
    public OptionalDouble asDouble() {
        return parser.map(p -> OptionalDouble.of(p.asDouble())).orElse(OptionalDouble.empty());
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing the string config value parsed to an enum value via {@link Enum#valueOf(Class, String)}.
     * @throws IllegalStateException if the string config value does not match a defined enum value.
     */
    public <T extends Enum<T>> Optional<T> asEnum(Class<T> enumClass) {
        return parser.map(p -> p.asEnum(enumClass));
    }

    /**
     * @return {@link OptionalDouble#empty()} if the config value is an empty String, otherwise returns an {@link OptionalDouble}
     * containing a double representing a time in the unit returned by {@link Config#getTimeUnit()}.
     * <p>
     * The double is created by parsing the string config value and converting between the time unit declared in the
     * string config value and the time unit of the enclosing Config class instance. The value returned will also include
     * the non-integer fraction of the time, if any, that results from converting between time units.
     * See {@link OptionalValueParser#asTime()} for a variant that only returns a whole number of time units.
     * <p>
     * String config values representing times can be given either:
     * <ul>
     *     <li>in the form "{@code <value>,<time unit>}" or "{@code <value>:<time unit>}". E.g. "5.65,SECONDS" or "1:HOURS"
     *     <li>as a double, in which case it will be assumed that the value is being specified <b>with a time unit of seconds</b>. I.e. this is equivalent to doing "{@code <value>,SECONDS}"
     * </ul>
     * Valid time units are those defined by {@link TimeUnit}, though they can be specified in any case and without the final "s".
     *
     * @throws IllegalStateException       if
     *                                      - {@link Config#getTimeUnit()} has not been set.
     *                                      - the config value does not satisfy one of the formats given above.
     *                                      - the time unit in the config value does not match one of the values specified by {@link TimeUnit}.
     *                                      - the numerical part of the config value given cannot be parsed by {@link Double#parseDouble(String)}.
     */
    public OptionalDouble asFractionalTime() {
        return parser.map(p -> OptionalDouble.of(p.asFractionalTime())).orElse(OptionalDouble.empty());
    }

    /**
     * @return {@link OptionalLong#empty()} if the config value is an empty String, otherwise returns an {@link OptionalLong}
     * containing a long representing a time in the unit returned by {@link Config#getTimeUnit()}.
     * <p>
     * The long is created by parsing the string config value and converting between the time unit declared in the
     * string config value and the time unit of the enclosing Config class instance. The value returned will be rounded to the nearest
     * whole number of units.
     * <p>
     * String config values representing times can be given either:
     * <ul>
     *     <li>in the form "{@code <value>,<time unit>}" or "{@code <value>:<time unit>}". E.g. "5.65,SECONDS" or "1:HOURS"
     *     <li>as a double, in which case it will be assumed that the value is being specified <b>with a time unit of seconds</b>. I.e. this is equivalent to doing "{@code <value>,SECONDS}"
     * </ul>
     * Valid time units are those defined by {@link TimeUnit}, though they can be specified in any case and without the final "s".
     *
     * @throws IllegalStateException       if
     *                                      - {@link Config#getTimeUnit()} has not been set.
     *                                      - the config value does not satisfy one of the formats given above.
     *                                      - the time unit in the config value does not match one of the values specified by {@link TimeUnit}.
     *                                      - the numerical part of the config value given cannot be parsed by {@link Double#parseDouble(String)}.
     */
    public OptionalLong asTime() {
        return parser.map(p -> OptionalLong.of(p.asTime())).orElse(OptionalLong.empty());
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing the string config value parsed as a {@link Duration} rounded to the nearest nanosecond.
     * <p>
     * Duration config values can be given either:
     * - As a double on its own, in which case it will be assumed that the value is being specified in seconds
     * - In the form {@code <value>,<time unit>} or {@code <value>:<time unit>}.
     * <br>
     * Valid time units are those defined by {@link TimeUnit} or {@link java.time.temporal.ChronoUnit} (either are
     * acceptable), though they can be specified in any case and without the final "s".
     *
     * @throws IllegalStateException      if
     *                                      - the config value does not satisfy one of the formats given above.
     *                                      - the time unit in the config value does not match an enum value.
     *                                      - the value given cannot be parsed as a double.
     */
    public Optional<Duration> asDuration() {
        return parser.map(StrictValueParser::asDuration);
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing the string config value parsed as a length using the declared application length unit.
     * <p>
     * Length config values can be given either
     * - as a double, in which case Config will assume that the value is being specified in meters.
     * - in the form {@code <value>,<length unit>} or {@code <value>:<length unit>}.
     * <br>
     * Valid length units are those defined by {@link LengthUnit}, though they can be specified in any case and without the final "s".
     *
     * @throws IllegalStateException       if
     *                                      - the application length unit has not been set.
     *                                      - the config value does not satisfy one of the formats given above.
     *                                      - the length unit in the config value does not match an enum value.
     *                                      - the value given cannot be parsed as a double.
     */
    public OptionalDouble asLength() {
        return parser.map(p -> OptionalDouble.of(p.asLength())).orElse(OptionalDouble.empty());
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing the string config value parsed as a speed using the declared application time and length
     *          units.
     * <p>
     * Speed config values can be given either
     * - as a double, in which case Config will assume that the value is being specified in meters per second
     * - in the form {@code <value>,<length unit>,<time unit>} or {@code <value>:<length unit>:<time unit>}
     * <br>
     * Valid time units are those defined by {@link TimeUnit}, and valid length units are those defined by {@link LengthUnit}
     * though they can be specified in any case and without the final "s".
     *
     * @throws IllegalStateException       if
     *                                      - the application time or length units have not been set
     *                                      - the config value does not satisfy one of the formats given above
     *                                      - the time or length units in the config value do not match an enum value
     *                                      - the value given cannot be parsed as a double
     */
    public OptionalDouble asSpeed() {
        return parser.map(p -> OptionalDouble.of(p.asSpeed())).orElse(OptionalDouble.empty());
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing the string config value parsed as an acceleration using the declared application time and
     *          length units.
     * <p>
     * Acceleration config values can be given either
     * - as a double, in which case Config will assume that the value is being specified in meters per second squared
     * - in the form {@code <value>,<length unit>,<time unit>} or {@code <value>:<length unit>:<time unit>}
     * <br>
     * Valid time units are those defined by {@link TimeUnit}, and valid length units are those defined by {@link LengthUnit}
     * though they can be specified in any case and without the final "s".
     *
     * @throws IllegalStateException       if
     *                                      - the application time or length units have not been set
     *                                      - the config value does not satisfy one of the formats given above
     *                                      - the time or length units in the config value do not match an enum value
     *                                      - the value given cannot be parsed as a double
     */
    public OptionalDouble asAcceleration() {
        return parser.map(p -> OptionalDouble.of(p.asAcceleration())).orElse(OptionalDouble.empty());
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing the string config value parsed as a jerk using the declared application time and length
     *          units.
     * <p>
     * Jerk config values can be given either
     * - as a double, in which case Config will assume that the value is being specified in meters per second cubed
     * - in the form {@code <value>,<length unit>,<time unit>} or {@code <value>:<length unit>:<time unit>}
     * <br>
     * Valid time units are those defined by {@link TimeUnit}, and valid length units are those defined by {@link LengthUnit}
     * though they can be specified in any case and without the final "s".
     *
     * @throws IllegalStateException       if
     *                                      - the application time or length units have not been set
     *                                      - the config value does not satisfy one of the formats given above
     *                                      - the time or length units in the config value do not match an enum value
     *                                      - the value given cannot be parsed as a double
     */
    public OptionalDouble asJerk() {
        return parser.map(p -> OptionalDouble.of(p.asJerk())).orElse(OptionalDouble.empty());
    }

    /**
     * @return a {@link OptionalListValueParser} operating on the String config value.
     */
    public OptionalListValueParser asList() {
        return new OptionalListValueParser(parser.map(StrictValueParser::asList));
    }

    /**
     * @return a {@link OptionalSetValueParser} operating on the String config value.
     */
    public OptionalSetValueParser asSet() {
        return new OptionalSetValueParser(parser.map(StrictValueParser::asSet));
    }

    /**
     * @return a {@link OptionalMapValueParser} operating on the String config value.
     */
    public OptionalMapValueParser asMap() {
        return new OptionalMapValueParser(parser.map(StrictValueParser::asMap));
    }

    /**
     * @return a {@link OptionalMapValueParser} operating on the String config value.
     */
    public OptionalSetMultimapValueParser asSetMultimap() {
        return new OptionalSetMultimapValueParser(parser.map(StrictValueParser::asSetMultimap));
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing the the result of the the provided custom parser applied to the config value.
     */
    public <T> Optional<T> withCustomParser(Function<String, T> parser) {
        return this.parser.map(p -> p.withCustomParser(parser));
    }

    /**
     * @deprecated to help avoid calling this when {@link OptionalValueParser#asString()} is desired
     */
    @Deprecated
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("value", asString().orElse(""))
                .toString();
    }
}
