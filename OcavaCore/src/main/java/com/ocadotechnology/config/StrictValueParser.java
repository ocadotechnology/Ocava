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

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.ocadotechnology.physics.units.LengthUnit;

/**
 * Parser class to convert a config value into a typed result.
 */
public class StrictValueParser {
    private final String value;
    @CheckForNull
    private final TimeUnit timeUnit;
    @CheckForNull
    private final LengthUnit lengthUnit;

    @VisibleForTesting
    StrictValueParser(String value) {
        this(value, null, null);
    }

    StrictValueParser(String value, @Nullable TimeUnit timeUnit, @Nullable LengthUnit lengthUnit) {
        this.value = value;
        this.timeUnit = timeUnit;
        this.lengthUnit = lengthUnit;
    }

    /**
     * @return the raw string config value.
     */
    public String asString() {
        return value;
    }

    /**
     * @return the string config value parsed to a boolean.
     * @throws IllegalStateException if the config value does not strictly equal "true" or "false", case insensitive.
     */
    public boolean asBoolean() {
        return ConfigParsers.parseBoolean(value);
    }

    /**
     * @return the string config value parsed to an integer. If the value is the String "max" or "min" (case
     *          insensitive) returns {@link Integer#MAX_VALUE} or {@link Integer#MIN_VALUE} respectively, otherwise
     *          defers to {@link Integer#parseInt(String)}.
     * @throws NumberFormatException if the config value cannot be parsed to an integer.
     */
    public int asInt() {
        return ConfigParsers.parseInt(value);
    }

    /**
     * @return the string config value parsed to a long. If the value is the String "max" or "min" (case insensitive)
     *          returns {@link Long#MAX_VALUE} or {@link Long#MIN_VALUE} respectively, otherwise defers to {@link
     *          Long#parseLong(String)}.
     * @throws NumberFormatException if the config value cannot be parsed to a long.
     */
    public long asLong() {
        return ConfigParsers.parseLong(value);
    }

    /**
     * @return the string config value parsed to a double via {@link Double#parseDouble(String)}.
     * @throws NumberFormatException if the config value cannot be parsed to a double.
     */
    public double asDouble() {
        return ConfigParsers.parseDouble(value);
    }

    /**
     * @return the string config value parsed to an enum value via {@link Enum#valueOf(Class, String)}.
     * @throws IllegalArgumentException if the string config value does not match a defined enum value.
     */
    public <T extends Enum<T>> T asEnum(Class<T> enumClass) {
        return Enum.valueOf(enumClass, value);
    }

    /**
     * @return the string config value parsed as a time using the declared application time unit.
     * <p>
     * Time config values can be given either
     * - as a double, in which case Config will assume that the value is being specified in seconds
     * - in the form {@code <value>,<time unit>} or {@code <value>:<time unit>}.
     *
     * @throws NullPointerException       if the application time unit has not been set.
     * @throws IllegalStateException      if the config value does not satisfy one of the formats given above.
     * @throws IllegalArgumentException   if the time unit in the config value does not match an enum value.
     * @throws NumberFormatException      if the value given cannot be parsed as a double.
     */
    public double asFractionalTime() {
        return ConfigParsers.parseFractionalTime(value, getTimeUnit());
    }

    /**
     * @return the string config value parsed as a time using the declared application time unit, rounded to the nearest
     *          whole number of units.
     * <p>
     * Time config values can be given either
     * - as a double, in which case Config will assume that the value is being specified in seconds
     * - in the form {@code <value>,<time unit>} or {@code <value>:<time unit>}.
     *
     * @throws NullPointerException       if the application time unit has not been set.
     * @throws IllegalStateException      if the config value does not satisfy one of the formats given above.
     * @throws IllegalArgumentException   if the time unit in the config value does not match an enum value.
     * @throws NumberFormatException      if the value given cannot be parsed as a double.
     */
    public long asTime() {
        return Math.round(asFractionalTime());
    }

    /**
     * @return the string config value parsed as a {@link Duration} rounded to the nearest nanosecond.
     * <p>
     * Duration config values can be given either:
     * - As a double on its own, in which case it will be assumed that the value is being specified in seconds
     * - In the form {@code <value>,<time unit>} or {@code <value>:<time unit>}.
     *
     * @throws IllegalStateException      if the config value does not satisfy one of the formats given above.
     * @throws IllegalArgumentException   if the time unit in the config value does not match an enum value.
     * @throws NumberFormatException      if the value given cannot be parsed as a double.
     */
    public Duration asDuration() {
        return ConfigParsers.parseDuration(value);
    }

    /**
     * @return the string config value parsed as a length using the declared application length unit.
     * <p>
     * Length config values can be given either
     * - as a double, in which case Config will assume that the value is being specified in meters.
     * - in the form {@code <value>,<length unit>} or {@code <value>:<length unit>}.
     *
     * @throws NullPointerException       if the application length unit has not been set.
     * @throws IllegalStateException      if the config value does not satisfy one of the formats given above.
     * @throws IllegalArgumentException   if the length unit in the config value does not match an enum value.
     * @throws NumberFormatException      if the value given cannot be parsed as a double.
     */
    public double asLength() {
        return ConfigParsers.parseLength(value, getLengthUnit());
    }

    /**
     * @return the string config value parsed as a speed using the declared application time and length units.
     * <p>
     * Speed config values can be given either
     * - as a double, in which case Config will assume that the value is being specified in meters per second
     * - in the form {@code <value>,<length unit>,<time unit>} or {@code <value>:<length unit>:<time unit>}
     *
     * @throws NullPointerException       if the application time or length units have not been set
     * @throws IllegalStateException      if the config value does not satisfy one of the formats given above
     * @throws IllegalArgumentException   if the time or length units in the config value do not match an enum value
     * @throws NumberFormatException      if the value given cannot be parsed as a double
     */
    public double asSpeed() {
        return ConfigParsers.parseSpeed(value, getLengthUnit(), getTimeUnit());
    }

    /**
     * @return the string config value parsed as an acceleration using the declared application time and length units.
     * <p>
     * Acceleration config values can be given either
     * - as a double, in which case Config will assume that the value is being specified in meters per second squared
     * - in the form {@code <value>,<length unit>,<time unit>} or {@code <value>:<length unit>:<time unit>}
     *
     * @throws NullPointerException       if the application time or length units have not been set
     * @throws IllegalStateException      if the config value does not satisfy one of the formats given above
     * @throws IllegalArgumentException   if the time or length units in the config value do not match an enum value
     * @throws NumberFormatException      if the value given cannot be parsed as a double
     */
    public double asAcceleration() {
        return ConfigParsers.parseAcceleration(value, getLengthUnit(), getTimeUnit());
    }

    /**
     * @return a {@link ListValueParser} operating on the String config value.
     */
    public ListValueParser asList() {
        return new ListValueParser(value);
    }

    /**
     * @return a {@link SetValueParser} operating on the String config value.
     */
    public SetValueParser asSet() {
        return new SetValueParser(value);
    }

    /**
     * @return a {@link MapValueParser} operating on the String config value.
     */
    public MapValueParser asMap() {
        return new MapValueParser(value);
    }

    /**
     * @return the String config value parsed using the provided custom parser.
     */
    public <T> T withCustomParser(Function<String, T> parser) {
        return parser.apply(value);
    }

    /**
     * @deprecated to help avoid calling this when {@link StrictValueParser#asString()} is desired
     */
    @Deprecated
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("value", value)
                .toString();
    }

    private TimeUnit getTimeUnit() {
        return Preconditions.checkNotNull(timeUnit, "timeUnit not set. See ConfigManager.Builder.setTimeUnit.");
    }

    private LengthUnit getLengthUnit() {
        return Preconditions.checkNotNull(lengthUnit, "lengthUnit not set. See ConfigManager.Builder.setLengthUnit.");
    }
}
