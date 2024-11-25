/*
 * Copyright © 2017-2024 Ocado (Ocava)
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
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collector;

import javax.annotation.CheckForNull;

import com.google.common.base.Enums;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.StringId;
import com.ocadotechnology.maths.stats.Probability;
import com.ocadotechnology.physics.units.LengthUnit;
import com.ocadotechnology.validation.Failer;

/**
 * Collection of parser functions used in the parsing of config values
 */
public class ConfigParsers {

    private ConfigParsers() {
        // Utility class
    }

    /**
     * Parses the input string as a Double, using {@link ConfigParsers#parseDouble(String)}, and validates that it lies between 0 and 1.
     * See {@link #parseProbability(String)} for parsing for Probabilities.
     * @throws IllegalStateException if the input is not between 0 and 1.
     * @throws NumberFormatException if the config value cannot be parsed to a double.
     */
    public static double parseFraction(String s) {
        var proportion = parseDouble(s);
        Preconditions.checkState(proportion >= 0 && proportion <= 1, "Value must be between 0 and 1");
        return proportion;
    }

    /**
     * Parses the input string as a {@link Probability}.
     * It may be in the form of a percentage (eg 5.5%) or as a value between 0.0 and 1.0 (eg 0.055).
     * @throws IllegalStateException if the input is not between 0.0 and 1.0, or 0.0% and 100.0%.
     * @throws NumberFormatException if the config value cannot be interpreted and parsed to a double.
     */
    public static Probability parseProbability(String s) {
        double probability;
        if (s.endsWith("%")) {
            String percent = s.substring(0, s.length() - 1);
            probability = parseDouble(percent) / 100.0;
            Preconditions.checkState(probability >= 0 && probability <= 1, "Value must be between 0.0% and 100.0%");
        } else {
            probability = parseFraction(s);
        }
        return new Probability(probability);
    }

    /**
     * If the String is a number defers to {@link Integer#parseInt(String)}, if it is the String "max" or "min"
     * (case-insensitive) returns {@link Integer#MAX_VALUE} or {@link Integer#MIN_VALUE} respectively
     */
    public static int parseInt(String configValue) {
        if (configValue.equalsIgnoreCase("max")) {
            return Integer.MAX_VALUE;
        } else if (configValue.equalsIgnoreCase("min")) {
            return Integer.MIN_VALUE;
        }
        return Integer.parseInt(configValue);
    }

    /**
     * If the String is a number defers to {@link Long#parseLong(String)}, if it is the String "max" or "min"
     * (case-insensitive) returns {@link Long#MAX_VALUE} or {@link Long#MIN_VALUE} respectively
     */
    public static long parseLong(String configValue) {
        if (configValue.equalsIgnoreCase("max")) {
            return Long.MAX_VALUE;
        } else if (configValue.equalsIgnoreCase("min")) {
            return Long.MIN_VALUE;
        }
        return Long.parseLong(configValue);
    }

    /**
     * defers to {@link Double#parseDouble(String)}
     */
    public static double parseDouble(String configValue) {
        return Double.parseDouble(configValue);
    }

    /**
     * Unlike {@link Boolean#parseBoolean}, this parser only accepts TRUE or FALSE (case-insensitive). Other values will
     * cause a failure.
     *
     * @param value String value to parse
     */
    public static boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        } else if ("false".equalsIgnoreCase(value)) {
            return false;
        } else {
            throw Failer.fail("Invalid boolean value (%s).  Must be equal to true or false, case insensitive", value);
        }
    }

    /**
     * Parse the given String into a length value in the given {@link LengthUnit}. The String can specify input
     * length unit. If none are specified the SI Units are used. The parser can cope with case insensitivity, as
     * well as adding an "S" to pluralise units.
     * <br>
     * <br>
     * Example:
     * <pre>
     *     parseAcceleration("1, CENTIMETERS", METERS) &#61;&#62; 0.01 m
     *     parseAcceleration("1, CentiMEter", METERS) &#61;&#62; 0.01 m
     *     parseAcceleration("1", METERS) &#61;&#62; 1 m
     * </pre>
     * <br>
     * SI Unit is {@link LengthUnit#METERS}
     *
     * @param value A String to parse. Must be either a number or a number with a {@link LengthUnit} separated by a
     *              comma (",") or a colon (":").
     */
    public static double parseLength(String value, LengthUnit returnLengthUnit) {
        String[] parts = parseParts(value);
        double length = Double.parseDouble(parts[0].trim());
        LengthUnit sourceUnit;
        if (parts.length == 1) {
            sourceUnit = LengthUnit.METERS;
        } else if (parts.length == 2) {
            sourceUnit = parseLengthUnit(parts[1]);
        } else {
            throw Failer.fail("Length values (%s) need to be specified without units (for SI) or in the following format: '<value>,<length unit>' or '<value>:<length unit>'", Arrays.toString(parts));
        }
        return length * returnLengthUnit.getUnitsIn(sourceUnit);
    }

    /**
     * Parse the given String into a fractional time value in the given {@link TimeUnit}. The String can specify input
     * time unit. If none are specified the SI Units are used. The parser can cope with case insensitivity, as well as
     * adding an "S" to pluralise units.
     * <br>
     * <br>
     * Example:
     * <pre>
     *     parseAcceleration("1, MINUTES", SECONDS) &#61;&#62; 60 s
     *     parseAcceleration("1, miNUte", SECONDS) &#61;&#62; 60 s
     *     parseAcceleration("1", SECONDS) &#61;&#62; 1 s
     * </pre>
     * <br>
     * SI Unit is {@link TimeUnit#SECONDS}
     *
     * @param value A String to parse. Must be either a number or a number with a {@link TimeUnit} separated by a
     *              comma (",") or a colon (":").
     */
    public static double parseFractionalTime(String value, TimeUnit returnTimeUnit) {
        String[] parts = parseParts(value);
        double time = Double.parseDouble(parts[0].trim());
        TimeUnit sourceUnit;
        if (parts.length == 1) {
            sourceUnit = TimeUnit.SECONDS;
        } else if (parts.length == 2) {
            sourceUnit = parseTimeUnit(parts[1]);
        } else {
            throw Failer.fail("Time values (%s) need to be specified without units (for SI) or in the following format: '<value>,<time unit>' or '<value>:<time unit>'", Arrays.toString(parts));
        }
        return time * getTimeUnitsInSourceTimeUnit(sourceUnit, returnTimeUnit);
    }

    /**
     * Parse the given String into a {@link Duration}. The String can specify input {@link TimeUnit} or {@link ChronoUnit}.
     * If none are specified the SI Units are used. The parser can cope with case insensitivity, as well as adding an
     * "S" to pluralise units.
     * <br>
     * SI Unit is {@link TimeUnit#SECONDS}
     */
    public static Duration parseDuration(String value) {
        String[] parts = parseParts(value);
        if (parts.length > 0 && parts.length <= 2) {
            ChronoUnit unit = parts.length == 1 ? ChronoUnit.SECONDS : parseChronoUnit(parts[1]);
            double nanoTime = Double.parseDouble(parts[0].trim()) * unit.getDuration().toNanos();
            return Duration.ofNanos(Math.round(nanoTime));
        } else {
            throw Failer.fail("Duration values (%s) need to be specified without units (for SI) or in the following format: '<value>,<time unit>' or '<value>:<time unit>'", Arrays.toString(parts));
        }
    }

    /**
     * Parse the given String into a speed value in the given {@link LengthUnit} over {@link TimeUnit}. The String can
     * specify input length and time units. If none are specified the SI Units are used. The parser can cope with case
     * insensitivity, as well as adding an "S" to pluralise units.
     * <br>
     * <br>
     * Example:
     * <pre>
     *     parseSpeed("10, METERS, HOURS", METERS, SECONDS) &#61;&#62; 0.00277778 m/s
     *     parseSpeed("10, METER, hoUrs", METERS, SECONDS) &#61;&#62; 0.00277778 m/s
     *     parseSpeed("10", METERS, SECONDS) &#61;&#62; 10 m/s
     * </pre>
     * <br>
     * SI Unit is {@link LengthUnit#METERS} / {@link TimeUnit#SECONDS}
     *
     * @param value A String to parse. Must be either a number or a number with a {@link LengthUnit}
     *              and a {@link TimeUnit} separated by a comma (",") or a colon (":").
     */
    public static double parseSpeed(String value, LengthUnit returnLengthUnit, TimeUnit returnTimeUnit) {
        String[] parts = parseParts(value);
        double speed = Double.parseDouble(parts[0].trim());
        LengthUnit sourceLengthUnit;
        TimeUnit sourceTimeUnit;
        if (parts.length == 1) {
            sourceLengthUnit = LengthUnit.METERS;
            sourceTimeUnit = TimeUnit.SECONDS;
        } else if (parts.length == 3) {
            sourceLengthUnit = parseLengthUnit(parts[1]);
            sourceTimeUnit = parseTimeUnit(parts[2]);
        } else {
            throw Failer.fail("Speed values (%s) need to be specified without units (for SI) or in the following format: '<value>,<length unit>,<time unit>' or '<value>:<length unit>:<time unit>'", Arrays.toString(parts));
        }
        return speed * returnLengthUnit.getUnitsIn(sourceLengthUnit) / getTimeUnitsInSourceTimeUnit(sourceTimeUnit, returnTimeUnit);
    }

    /**
     * Parse the given String into an acceleration value in the given {@link LengthUnit} over {@link TimeUnit} squared.
     * The String can specify input length and time units. If none are specified the SI Units are used. The parser can
     * cope with case insensitivity, as well as adding an "S" to pluralise units.
     * <br>
     * <br>
     * Example:
     * <pre>
     *     parseAcceleration("10, METERS, HOURS", METERS, SECONDS) &#61;&#62; 7.716E-7 m/s2
     *     parseAcceleration("10, METER, hoUrs", METERS, SECONDS) &#61;&#62; 7.716E-7 m/s2
     *     parseAcceleration("10", METERS, SECONDS) &#61;&#62; 10 m/s2
     * </pre>
     * <br>
     * SI Unit is {@link LengthUnit#METERS} / {@link TimeUnit#SECONDS} ^ 2
     *
     * @param value A String to parse. Must be either a number or a number with a {@link LengthUnit}
     *              and a {@link TimeUnit} separated by a comma (",") or a colon (":").
     */
    public static double parseAcceleration(String value, LengthUnit returnLengthUnit, TimeUnit returnTimeUnit) {
        String[] parts = parseParts(value);
        double acceleration = Double.parseDouble(parts[0].trim());
        LengthUnit sourceLengthUnit;
        TimeUnit sourceTimeUnit;
        if (parts.length == 1) {
            sourceLengthUnit = LengthUnit.METERS;
            sourceTimeUnit = TimeUnit.SECONDS;
        } else if (parts.length == 3) {
            sourceLengthUnit = parseLengthUnit(parts[1]);
            sourceTimeUnit = parseTimeUnit(parts[2]);
        } else {
            throw Failer.fail("Acceleration values (%s) need to be specified without units (for SI) or in the following format: '<value>,<length unit>,<time unit>' or '<value>:<length unit>:<time unit>'", Arrays.toString(parts));
        }
        return acceleration * returnLengthUnit.getUnitsIn(sourceLengthUnit) / (Math.pow(getTimeUnitsInSourceTimeUnit(sourceTimeUnit, returnTimeUnit), 2));
    }

    /**
     * Parse the given String into a jerk value in the given {@link LengthUnit} over {@link TimeUnit} cubed. The String
     * can specify input length and time units. If none are specified the SI Units are used. The parser can cope with
     * case insensitivity, as well as adding an "S" to pluralise units.
     * <br>
     * <br>
     * Example:
     * <pre>
     *     parseJerk("10, METERS, HOURS", METERS, SECONDS) &#61;&#62; 2.143-10 m/s3
     *     parseJerk("10, METER, hoUrs", METERS, SECONDS) &#61;&#62; 2.143-10 m/s3
     *     parseJerk("10", METERS, SECONDS) &#61;&#62; 10 m/s3
     * </pre>
     * <br>
     * SI Unit is {@link LengthUnit#METERS} / {@link TimeUnit#SECONDS} ^ 3
     *
     * @param value A String to parse. Must be either a number or a number with a {@link LengthUnit}
     *              and a {@link TimeUnit} separated by a comma (",") or a colon (":").
     */
    public static double parseJerk(String value, LengthUnit returnLengthUnit, TimeUnit returnTimeUnit) {
        String[] parts = parseParts(value);
        double jerk = Double.parseDouble(parts[0].trim());
        LengthUnit sourceLengthUnit;
        TimeUnit sourceTimeUnit;
        if (parts.length == 1) {
            sourceLengthUnit = LengthUnit.METERS;
            sourceTimeUnit = TimeUnit.SECONDS;
        } else if (parts.length == 3) {
            sourceLengthUnit = parseLengthUnit(parts[1]);
            sourceTimeUnit = parseTimeUnit(parts[2]);
        } else {
            throw Failer.fail("Jerk values (%s) need to be specified without units (for SI) or in the following format: '<value>,<length unit>,<time unit>' or '<value>:<length unit>:<time unit>'", Arrays.toString(parts));
        }
        return jerk * returnLengthUnit.getUnitsIn(sourceLengthUnit) / (Math.pow(getTimeUnitsInSourceTimeUnit(sourceTimeUnit, returnTimeUnit), 3));
    }

    /**
     * Defers to {@link Enum#valueOf(Class, String)}
     */
    public static <T extends Enum<T>> T parseEnum(String value, Class<T> enumClass) {
        return Enum.valueOf(enumClass, value.trim());
    }

    /**
     * Utility to convert a comma (,) or colon (:) separated string into an {@link ImmutableCollection} of T.
     *
     * @param conversion Function that can convert a single entry of the list from String to the desired type T
     * @param collector  ImmutableCollection collector
     * @param <T>        Type of value to be stored in the list
     * @param <C>        Type of collection
     */
    public static <T, C extends ImmutableCollection<T>> Function<String, C> getCollectionOf(Function<String, T> conversion, Collector<T, ?, C> collector) {
        return value -> Arrays.stream(parseParts(value)).filter(s -> !s.isEmpty()).map(conversion).collect(collector);
    }

    /**
     * Utility to convert a comma (,) or colon (:) separated string into an {@link ImmutableList} of T.
     *
     * @param conversion Function that can convert a single entry of the String to the desired type T
     * @param <T>        Type of value to be stored in the list
     */
    public static <T> Function<String, ImmutableList<T>> getListOf(Function<String, T> conversion) {
        return value -> getCollectionOf(conversion, ImmutableList.toImmutableList()).apply(value);
    }

    /**
     * Utility to convert a comma (,) or colon (:) separated string into an {@link ImmutableList} of T.
     *
     * @param conversion Function that can convert a single entry of the String to the desired type T
     * @param <T>        Type of value to be stored in the list
     */
    public static <T> Function<String, ImmutableSet<T>> getSetOf(Function<String, T> conversion) {
        return value -> getCollectionOf(conversion, ImmutableSet.toImmutableSet()).apply(value);
    }

    /**
     * Utility function to convert a comma (,) or colon (:) separated string into an {@link ImmutableList} of Strings.
     */
    public static Function<String, ImmutableList<String>> getListOfStrings() {
        return getListOf(Function.identity());
    }

    /**
     * Utility function to convert a comma (,) or colon (:) separated string into an {@link ImmutableList} of Strings.
     */
    public static Function<String, ImmutableSet<String>> getSetOfStrings() {
        return getSetOf(Function.identity());
    }

    /**
     * Utility function to convert a comma (,) or colon (:) separated string into an {@link ImmutableList} of Integers.
     * The values are parsed to Integers using {@link #parseInt(String)}.
     */
    public static Function<String, ImmutableList<Integer>> getListOfIntegers() {
        return getListOf(ConfigParsers::parseInt);
    }

    /**
     * Utility function to convert a comma (,) or colon (:) separated string into an {@link ImmutableSet} of Integers.
     * The values are parsed to Integers using {@link #parseInt(String)}.
     */
    public static Function<String, ImmutableSet<Integer>> getSetOfIntegers() {
        return getSetOf(ConfigParsers::parseInt);
    }

    /**
     * Utility function to convert a comma (,) or colon (:) separated string into an {@link ImmutableList} of Longs.
     * The values are parsed to Longs using {@link #parseLong(String)}.
     */
    public static Function<String, ImmutableList<Long>> getListOfLongs() {
        return getListOf(ConfigParsers::parseLong);
    }

    /**
     * Utility function to convert a comma (,) or colon (:) separated string into an {@link ImmutableSet} of Longs.
     * The values are parsed to Longs using {@link #parseLong(String)}.
     */
    public static Function<String, ImmutableSet<Long>> getSetOfLongs() {
        return getSetOf(ConfigParsers::parseLong);
    }

    /**
     * Utility function to convert a comma (,) or colon (:) separated string into an {@link ImmutableList} of Doubles.
     * The values are parsed to Longs using {@link #parseDouble(String)}.
     */
    public static Function<String, ImmutableList<Double>> getListOfDoubles() {
        return getListOf(ConfigParsers::parseDouble);
    }

    /**
     * Utility function to convert a comma (,) or colon (:) separated string into an {@link ImmutableSet} of Doubles.
     * The values are parsed to Doubles using {@link #parseDouble(String)}.
     */
    public static Function<String, ImmutableSet<Double>> getSetOfDoubles() {
        return getSetOf(ConfigParsers::parseDouble);
    }

    /**
     * Utility function to convert a comma (,) or colon (:)  separated string into an {@link ImmutableList} of Ids.
     * The values are parsed to Longs using {@link #parseLong(String)}.
     *
     * @param <T> The Generic type of the Id
     */
    public static <T> Function<String, ImmutableList<Id<T>>> getListOfIds() {
        return getListOf(v -> Id.create(ConfigParsers.parseLong(v)));
    }

    /**
     * Utility function to convert a comma (,) or colon (:) separated string into an {@link ImmutableSet} of Ids.
     * The values are parsed to Longs using {@link #parseLong(String)}.
     *
     * @param <T> The Generic type of the Id
     */
    public static <T> Function<String, ImmutableSet<Id<T>>> getSetOfIds() {
        return getSetOf(v -> Id.create(ConfigParsers.parseLong(v)));
    }

    /**
     * Utility function to convert a comma (,) or colon (:) separated string into an {@link ImmutableList} of StringIds.
     *
     * @param <T> The Generic type of the StringId
     */
    public static <T> Function<String, ImmutableList<StringId<T>>> getListOfStringIds() {
        return getListOf(StringId::create);
    }

    /**
     * Utility function to convert a comma (,) or colon (:) separated string into an {@link ImmutableSet} of StringIds.
     *
     * @param <T> The Generic type of the StringId
     */
    public static <T> Function<String, ImmutableSet<StringId<T>>> getSetOfStringIds() {
        return getSetOf(StringId::create);
    }

    /**
     * Utility function to convert a comma (,) or colon (:) separated string into an {@link ImmutableList} of Enums.
     *
     * @param enumClass The Enumeration to convert the Strings into.
     * @param <T>       The Enum class
     */
    public static <T extends Enum<T>> Function<String, ImmutableList<T>> getListOfEnums(Class<T> enumClass) {
        return getListOf(v -> ConfigParsers.parseEnum(v, enumClass));
    }

    /**
     * Utility function to convert a comma (,) or colon (:) separated string into an {@link ImmutableSet} of Enums.
     *
     * @param enumClass The Enumeration to convert the Strings into.
     * @param <T>       The Enum class
     */
    public static <T extends Enum<T>> Function<String, ImmutableSet<T>> getSetOfEnums(Class<T> enumClass) {
        return getSetOf(v -> ConfigParsers.parseEnum(v, enumClass));
    }

    /**
     * Returns a typed-Map for a String specified as a collection of key-value pairs.
     * <p>
     * Given a string value that is a (semicolon-separated) list of (equals-separated) key-value pairs:
     * <pre>"key1=value1;key2=value2"</pre>
     * Keys and values will be trimmed, before being supplied to the functions that translate them to the
     * correct types.
     * Any pair which does not contain the character '=' will be ignored.
     */
    public static <K, V> ImmutableMap<K, V> parseMap(String value, Function<String, K> keyParser, Function<String, V> valueParser) {
        ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
        for (String pair : value.split(";")) {
            int x = pair.indexOf('=');
            if (x <= 0) {
                continue;
            }
            K propertyKey = keyParser.apply(pair.substring(0, x).trim());
            V propertyValue = valueParser.apply(pair.substring(x + 1).trim());
            builder.put(propertyKey, propertyValue);
        }
        return builder.build();
    }

    /**
     * Splits the String value by either comma (",") or colon (":")
     */
    private static String[] parseParts(String value) {
        String[] splitArray = value.contains(",")
                ? value.split(",")
                : value.split(":");

        for (int i = 0; i < splitArray.length; i++) {
            splitArray[i] = splitArray[i].trim();
        }

        return splitArray;
    }

    /**
     * Create a ratio from the source time unit to the wanted time unit
     */
    private static double getTimeUnitsInSourceTimeUnit(TimeUnit sourceUnit, TimeUnit wantedTimeUnit) {
        return (double) sourceUnit.toNanos(1) / wantedTimeUnit.toNanos(1);
    }

    public static <K, V> ImmutableSetMultimap<K, V> parseSetMultimap(
            String value,
            Function<String, K> keyParser,
            Function<String, V> valueParser) {
        ImmutableSetMultimap.Builder<K, V> builder = ImmutableSetMultimap.builder();
        for (String pair : value.split(";")) {
            String[] keyValues = pair.split("=", -1);
            if (keyValues.length < 2 || keyValues[0].isEmpty()) {
                continue;
            }

            K propertyKey = keyParser.apply(keyValues[0].trim());
            V propertyValue = valueParser.apply(keyValues[1].trim());
            builder.put(propertyKey, propertyValue);
        }
        return builder.build();
    }

    private static LengthUnit parseLengthUnit(String value) {
        String normalizedValue = value.trim().toUpperCase();
        LengthUnit result = Enums.getIfPresent(LengthUnit.class, normalizedValue)
                .or(Enums.getIfPresent(LengthUnit.class, normalizedValue + "S"))
                .orNull();
        if (result == null) {
            throw new IllegalArgumentException("Unrecognized LengthUnit value: " + value);
        }
        return result;
    }

    private static TimeUnit parseTimeUnit(String value) {
        String normalizedValue = value.trim().toUpperCase();
        TimeUnit result = Enums.getIfPresent(TimeUnit.class, normalizedValue)
                .or(Enums.getIfPresent(TimeUnit.class, normalizedValue + "S"))
                .orNull();
        if (result == null) {
            throw new IllegalArgumentException("Unrecognized TimeUnit value: " + value);
        }
        return result;
    }

    private static ChronoUnit parseChronoUnit(String value) {
        String normalizedValue = value.trim().toUpperCase();
        ChronoUnit result = parseChronoUnitStrict(normalizedValue);
        if (result != null) {
            return result;
        }
        result = parseChronoUnitStrict(normalizedValue + "S");
        if (result != null) {
            return result;
        }
        throw new IllegalArgumentException("Unrecognized ChronoUnit or TimeUnit value: " + value);
    }

    private static @CheckForNull ChronoUnit parseChronoUnitStrict(String normalizedValue) {
        return Enums.getIfPresent(TimeUnit.class, normalizedValue).transform(TimeUnit::toChronoUnit)
                .or(Enums.getIfPresent(ChronoUnit.class, normalizedValue))
                .orNull();
    }
}
