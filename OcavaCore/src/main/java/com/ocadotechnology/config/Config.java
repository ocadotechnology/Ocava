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

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.StringId;
import com.ocadotechnology.physics.units.LengthUnit;
import com.ocadotechnology.validation.Failer;

public class Config<E extends Enum<E>> implements Serializable, Comparable<Config<?>> {
    private static final long serialVersionUID = 1L;

    public final Class<E> cls;

    private final ImmutableMap<E, ConfigValue> values;
    private final ImmutableMap<?, Config<?>> subConfig;
    private final String qualifier;

    private final TimeUnit timeUnit;
    private final LengthUnit lengthUnit;

    private Config(
            Class<E> cls,
            ImmutableMap<E, ConfigValue> values,
            ImmutableMap<?, Config<?>> subConfig,
            String qualifier,
            TimeUnit timeUnit,
            LengthUnit lengthUnit) {

        this.cls = cls;
        this.values = values;
        this.subConfig = subConfig;
        this.qualifier = qualifier;
        this.timeUnit = timeUnit;
        this.lengthUnit = lengthUnit;
    }

    Config(
            Class<E> cls,
            ImmutableMap<E, ConfigValue> values,
            ImmutableMap<?, Config<?>> subConfig,
            String qualifier) {
        this(cls, values, subConfig, qualifier, null, null);
    }

    /**
     * Returns a new Config that has entered one layer into the prefix config values.
     * This means that when multi-prefix values are being used,
     * the sub-prefixes can now be accessed below the given prefix.
     * For each config item, it will set the value to be that of the prefix value if present,
     * otherwise it will keep its original value.
     * @param prefix The prefix to use for the current value / the prefix tree to use for multi-prefix configs
     * @return A new Config object with config values and prefixes from the given prefix
     */
    public Config<E> getPrefixedConfigItems(String prefix) {
        return map(configValue -> configValue.getPrefix(prefix));
    }

    /**
     * Returns a new Config where for each config item, it will set the value to be that of the prefix value if present,
     * otherwise it will keep its original value.
     * The Config returned will have the same prefix data as the original, so multi-prefixed values are not accessible
     * and prefixed values with a different root are still present.
     * @param prefix The prefix to use for the current value / the prefix tree to use for multi-prefix configs
     * @return A new Config object with config values and prefixes from the given prefix
     */

    public Config<E> getPrefixBiasedConfigItems(String prefix) {
        return map(configValue -> configValue.getWithPrefixBias(prefix));
    }

    /**
     * Perform a function on all config values in the config tree
     * @param f function to apply
     * @return A new config with the function applied
     */
    private Config<E> map(Function<ConfigValue, ConfigValue> f) {
        ImmutableMap<E, ConfigValue> values =  this.values.entrySet().stream()
                .collect(Maps.toImmutableEnumMap(Entry::getKey, e -> f.apply(e.getValue())));
        ImmutableMap<?, Config<?>> subConfig = this.subConfig.entrySet().stream()
                .collect(ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue().map(f)));
        return new Config<E>(this.cls, values, subConfig, this.qualifier, this.timeUnit, this.lengthUnit);
    }

    public ImmutableSet<String> getPrefixes() {
        Stream<String> subConfigStream = subConfig.values().stream()
                .flatMap(subConfig -> subConfig.getPrefixes().stream());

        Stream<String> valuesStream = values.values().stream()
                .flatMap(value -> value.getPrefixes().stream());

        return Stream.concat(subConfigStream, valuesStream)
                .collect(ImmutableSet.toImmutableSet());
    }

    public ImmutableMap<E, ConfigValue> getValues() {
        return this.values;
    }

    public static <T extends Enum<T>> Config<T> empty(Class<T> c) {
        return new Config<>(c, ImmutableMap.of(), ImmutableMap.of(), c.getSimpleName(), null, null);
    }

    public TimeUnit getTimeUnit() {
        return Preconditions.checkNotNull(timeUnit, "timeUnit not set.  See ConfigManager.Builder.setTimeUnit.");
    }

    public LengthUnit getLengthUnit() {
        return Preconditions.checkNotNull(lengthUnit, "lengthUnit not set.  See ConfigManager.Builder.setLengthUnit.");
    }

    public boolean containsKey(Enum<?> key) {
        return getOrNull(key) != null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> Config<T> getSubConfig(Class<T> key) {
        return (Config<T>)subConfig.get(key);
    }

    public int getInt(Enum<?> key) {
        return parseInt(getString(key));
    }

    public OptionalInt getIntIfPresent(Enum<?> key) {
        if (!containsKey(key)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(getInt(key));
    }

    public static int parseInt(String configValue) {
        if (configValue.length() > 0 && !Character.isDigit(configValue.charAt(0))) {
            configValue = configValue.toLowerCase();
            if (configValue.startsWith("max")) {
                return Integer.MAX_VALUE;
            } else if (configValue.startsWith("min")) {
                return Integer.MIN_VALUE;
            }
        }
        return Integer.parseInt(configValue);
    }

    /**
     * Using Optional in place of OptionalDouble as OptionalDouble is missing some features.
     */
    public Optional<Double> getDoubleIfPresent(Enum<?> key) {
        if (!containsKey(key)) {
            return Optional.empty();
        }

        return Optional.of(getDouble(key));
    }

    public double getDouble(Enum<?> key) {
        return parseDouble(getString(key));
    }

    public static double parseDouble(String configValue) {
        return Double.parseDouble(configValue);
    }

    /**
     * Interprets a config value as a boolean.
     *
     * @throws IllegalStateException if the value is not equal to either the string "true" or "false", ignoring case.
     */
    public boolean getBoolean(Enum<?> key) {
        String value = getString(key);
        if ("true".equalsIgnoreCase(value)) {
            return true;
        } else if ("false".equalsIgnoreCase(value)) {
            return false;
        } else {
            throw Failer.fail("Invalid boolean value %s.  Must be equal to true or false, case insensitive", value);
        }
    }

    public long getLong(Enum<?> key) {
        return parseLong(getString(key));
    }

    public OptionalLong getLongIfPresent(Enum<?> key) {
        if (!containsKey(key)) {
            return OptionalLong.empty();
        }

        return OptionalLong.of(getLong(key));
    }

    public static long parseLong(String configValue) {
        if (configValue.length() > 0 && !Character.isDigit(configValue.charAt(0))) {
            configValue = configValue.toLowerCase();
            if (configValue.startsWith("max")) {
                return Long.MAX_VALUE;
            } else if (configValue.startsWith("min")) {
                return Long.MIN_VALUE;
            }
        }
        return Long.parseLong(configValue);
    }

    /**
     * Interprets a config value as a length using the declared application length unit.
     *
     * Length config values can be given either
     * - as a double, in which case Config will assume that the value is being specified in m
     * - in the form {@code <value>,<length unit>} or {@code <value>:<length unit>}
     *
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @throws NullPointerException if the application length unit has not been set
     * @throws IllegalStateException if the config value does not satisfy one of the formats given above
     * @throws IllegalArgumentException if the length unit in the config value does not match an enum value
     * @throws NumberFormatException if the value given cannot be parsed as a double
     */
    public double getLength(Enum<?> key) {
        String[] parts = getParts(key);
        double length = Double.parseDouble(parts[0].trim());
        LengthUnit sourceUnit;
        if (parts.length == 1) {
            sourceUnit = LengthUnit.METERS;
        } else if (parts.length == 2) {
            sourceUnit = LengthUnit.valueOf(parts[1].trim());
        } else {
            throw Failer.fail("Length values (%s) need to be specified without units (for SI) or in the following format: '<value>,<length unit>' or '<value>:<length unit>'", Arrays.toString(parts));
        }
        return length * getLengthUnit().getUnitsIn(sourceUnit);
    }

    /**
     * Interprets a config value as a time using the declared application time unit.
     *
     * Time config values can be given either
     * - as a double, in which case Config will assume that the value is being specified in s
     * - in the form {@code <value>,<time unit>} or {@code <value>:<time unit>}
     *
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @throws NullPointerException if the application time unit has not been set
     * @throws IllegalStateException if the config value does not satisfy one of the formats given above
     * @throws IllegalArgumentException if the time unit in the config value does not match an enum value
     * @throws NumberFormatException if the value given cannot be parsed as a double
     */
    public double getFractionalTime(Enum<?> key) {
        String[] parts = getParts(key);
        double time = Double.parseDouble(parts[0].trim());
        TimeUnit sourceUnit;
        if (parts.length == 1) {
            sourceUnit = TimeUnit.SECONDS;
        } else if (parts.length == 2) {
            sourceUnit = TimeUnit.valueOf(parts[1].trim());
        } else {
            throw Failer.fail("Time values (%s) need to be specified without units (for SI) or in the following format: '<value>,<time unit>' or '<value>:<time unit>'", Arrays.toString(parts));
        }
        return time * getTimeUnitsInSourceTimeUnit(sourceUnit);
    }

    /**
     * @return the result of {@link Config#getFractionalTime} if the config key has a defined value, else {@link Optional#empty()}
     * Optional is used in place of OptionalDouble as OptionalDouble is missing some features.
     */
    public Optional<Double> getFractionalTimeIfPresent(Enum<?> key) {
        if (!containsKey(key)) {
            return Optional.empty();
        }
        return Optional.of(getFractionalTime(key));
    }

    /**
     * Interprets a config value as a time using the declared application time unit.
     *
     * Time config values can be given either
     * - as a double, in which case Config will assume that the value is being specified in s
     * - in the form {@code <value>,<time unit>} or {@code <value>:<time unit>}
     *
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @throws NullPointerException if the application time unit has not been set
     * @throws IllegalStateException if the config value does not satisfy one of the formats given above
     * @throws IllegalArgumentException if the time unit in the config value does not match an enum value
     * @throws NumberFormatException if the value given cannot be parsed as a double
     */
    public long getTime(Enum<?> key) {
        return Math.round(getFractionalTime(key));
    }

    /**
     * @return the result of {@link Config#getTime} if the config key has a defined value, else {@link Optional#empty()}
     * Optional is used in place of OptionalLong as OptionalLong is missing some features.
     */
    public Optional<Long> getTimeIfPresent(Enum<?> key) {
        if (!containsKey(key)) {
            return Optional.empty();
        }
        return Optional.of(getTime(key));
    }

    /**
     * Interprets a config value as a {@link Duration} rounded to the nearest nanosecond
     *
     * Duration config values can be given either:
     * - As a double on its own, in which case it will be assumed that the value is being specified in seconds
     * - In the form {@code <value>,<time unit>} or {@code <value>:<time unit>}
     *
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @throws IllegalStateException if the config value does not satisfy one of the formats given above
     * @throws IllegalArgumentException if the time unit in the config value does not match an enum value
     * @throws NumberFormatException if the value given cannot be parsed as a double
     */
    public Duration getDuration(Enum<?> key) {
        String[] parts = getParts(key);
        Preconditions.checkState(parts.length > 0 && parts.length <= 2, "Duration values (%s) need to be specified without units (for SI) or in the following format: '<value>,<time unit>' or '<value>:<time unit>'", Arrays.toString(parts));
        TimeUnit unit = parts.length == 1 ? TimeUnit.SECONDS : TimeUnit.valueOf(parts[1].trim());
        double nanoTime = Double.parseDouble(parts[0].trim()) * unit.toNanos(1);
        return Duration.ofNanos(Math.round(nanoTime));
    }

    /**
     * @return the result of {@link Config#getDuration} if the config key has a defined value, else {@link Optional#empty()}
     */
    public Optional<Duration> getDurationIfPresent(Enum<?> key) {
        if (!containsKey(key)) {
            return Optional.empty();
        }
        return Optional.of(getDuration(key));
    }

    /**
     * Interprets a config value as a speed using the declared application time and length units.
     *
     * Speed config values can be given either
     * - as a double, in which case Config will assume that the value is being specified in m/s
     * - in the form {@code <value>,<length unit>,<time unit>} or {@code <value>:<length unit>:<time unit>}
     *
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @throws NullPointerException if the application time or length units have not been set
     * @throws IllegalStateException if the config value does not satisfy one of the formats given above
     * @throws IllegalArgumentException if the time or length units in the config value do not match an enum value
     * @throws NumberFormatException if the value given cannot be parsed as a double
     */
    public double getSpeed(Enum<?> key) {
        String[] parts = getParts(key);
        double speed = Double.parseDouble(parts[0].trim());
        LengthUnit sourceLengthUnit;
        TimeUnit sourceTimeUnit;
        if (parts.length == 1) {
            sourceLengthUnit = LengthUnit.METERS;
            sourceTimeUnit = TimeUnit.SECONDS;
        } else if (parts.length == 3) {
            sourceLengthUnit = LengthUnit.valueOf(parts[1].trim());
            sourceTimeUnit = TimeUnit.valueOf(parts[2].trim());
        } else {
            throw Failer.fail("Speed values (%s) need to be specified without units (for SI) or in the following format: '<value>,<length unit>,<time unit>' or '<value>:<length unit>:<time unit>'", Arrays.toString(parts));
        }
        return speed * getLengthUnit().getUnitsIn(sourceLengthUnit) / getTimeUnitsInSourceTimeUnit(sourceTimeUnit);
    }

    /**
     * Interprets a config value as an acceleration using the declared application time and length units.
     *
     * Acceleration config values can be given either
     * - as a double, in which case Config will assume that the value is being specified in m/s^2
     * - in the form {@code <value>,<length unit>,<time unit>} or {@code <value>:<length unit>:<time unit>}
     *
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @throws NullPointerException if the application time or length units have not been set
     * @throws IllegalStateException if the config value does not satisfy one of the formats given above
     * @throws IllegalArgumentException if the time or length units in the config value do not match an enum value
     * @throws NumberFormatException if the value given cannot be parsed as a double
     */
    public double getAcceleration(Enum<?> key) {
        String[] parts = getParts(key);
        double acceleration = Double.parseDouble(parts[0].trim());
        LengthUnit sourceLengthUnit;
        TimeUnit sourceTimeUnit;
        if (parts.length == 1) {
            sourceLengthUnit = LengthUnit.METERS;
            sourceTimeUnit = TimeUnit.SECONDS;
        } else if (parts.length == 3) {
            sourceLengthUnit = LengthUnit.valueOf(parts[1].trim());
            sourceTimeUnit = TimeUnit.valueOf(parts[2].trim());
        } else {
            throw Failer.fail("Acceleration values (%s) need to be specified without units (for SI) or in the following format: '<value>,<length unit>,<time unit>' or '<value>:<length unit>:<time unit>'", Arrays.toString(parts));
        }
        return acceleration * getLengthUnit().getUnitsIn(sourceLengthUnit) / (Math.pow(getTimeUnitsInSourceTimeUnit(sourceTimeUnit), 2));
    }

    private double getTimeUnitsInSourceTimeUnit(TimeUnit sourceUnit) {
        return sourceUnit.toNanos(1) * 1.0 / getTimeUnit().toNanos(1);
    }

    public ImmutableList<Integer> getListOfIntegers(Enum<?> key) {
        String[] parts = getParts(key);
        ImmutableList.Builder<Integer> builder = ImmutableList.builder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                builder.add(Integer.parseInt(part));
            }
        }
        return builder.build();
    }

    /**
     * @return The same as {@link #getListOfIds} or an empty list if the config key isn't found
     * @throws NumberFormatException if the values given cannot be parsed as longs
     */
    public <T> ImmutableList<Id<T>> getListOfIdsIfPresent(Enum<?> key) {
        if (!containsKey(key)) {
            return ImmutableList.of();
        }
        return getListOfIds(key);
    }

    /**
     * @return The same as {@link #parseListOfIds} with the value linked to the given config key
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @throws NumberFormatException if the values given cannot be parsed as longs
     */
    public <T> ImmutableList<Id<T>> getListOfIds(Enum<?> key) {
        return parseListOfIds(getString(key));
    }

    /**
     * Converts a string of ',' or ':' separated values to an {@link ImmutableList} of {@link Id}s
     * @throws NumberFormatException if the values given cannot be parsed as longs
     */
    public static <T> ImmutableList<Id<T>> parseListOfIds(String configValue) {
        if (configValue.length() == 0) {
            return ImmutableList.of();
        }
        String[] parts = parseParts(configValue);
        ImmutableList.Builder<Id<T>> builder = ImmutableList.builder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                builder.add(Id.create(Long.parseLong(part)));
            }
        }
        return builder.build();
    }

    /**
     * @return The same as {@link #getSetOfIds} or an empty set if the config key isn't found
     * @throws NumberFormatException if the values given cannot be parsed as longs
     */
    public <T> ImmutableSet<Id<T>> getSetOfIdsIfPresent(Enum<?> key) {
        if (!containsKey(key)) {
            return ImmutableSet.of();
        }
        return getSetOfIds(key);
    }

    /**
     * @return The set of ids in the list returned by {@link #parseListOfIds} for the given config key
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @throws NumberFormatException if the values given cannot be parsed as longs
     */
    public <T> ImmutableSet<Id<T>> getSetOfIds(Enum<?> key) {
        return ImmutableSet.copyOf(parseListOfIds(getString(key)));
    }

    public <T extends Enum<T>> ImmutableSet<T> getSetOfEnums(Enum<?> key, Class<T> enumClass) {
        return getStreamOfEnums(key, enumClass)
                .collect(ImmutableSet.toImmutableSet());
    }

    public <T extends Enum<T>> ImmutableList<T> getListOfEnums(Enum<?> key, Class<T> enumClass) {
        return getStreamOfEnums(key, enumClass)
                .collect(ImmutableList.toImmutableList());
    }

    private <T extends Enum<T>> Stream<T> getStreamOfEnums(Enum<?> key, Class<T> enumClass) {
        return Arrays.stream(getParts(key))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> Enum.valueOf(enumClass, s));
    }

    public ImmutableList<String> getListOfStrings(Enum<?> key) {
        return Arrays.stream(getParts(key))
                .filter(s -> !s.equals(""))
                .collect(ImmutableList.toImmutableList());
    }

    /**
     * @return The same as {@link #getListOfStringIds} or an empty list if the config key isn't found
     */
    public <T> ImmutableList<StringId<T>> getListOfStringIdsIfPresent(Enum<?> key) {
        if (!containsKey(key)) {
            return ImmutableList.of();
        }
        return getListOfStringIds(key);
    }

    /**
     * @return The same as {@link #parseListOfStringIds} with the value linked to the given config key
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     */
    public <T> ImmutableList<StringId<T>> getListOfStringIds(Enum<?> key) {
        return parseListOfStringIds(getString(key));
    }

    /**
     * Converts a string of ',' or ':' separated values to an {@link ImmutableList} of {@link StringId}s
     */
    public static <T> ImmutableList<StringId<T>> parseListOfStringIds(String configValue) {
        if (configValue.length() == 0) {
            return ImmutableList.of();
        }
        String[] parts = parseParts(configValue);
        ImmutableList.Builder<StringId<T>> builder = ImmutableList.builder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                builder.add(StringId.create(part));
            }
        }
        return builder.build();
    }
    
    /**
     * @return The same as {@link #getSetOfStringIds} or an empty set if the config key isn't found
     */
    public <T> ImmutableSet<StringId<T>> getSetOfStringIdsIfPresent(Enum<?> key) {
        if (!containsKey(key)) {
            return ImmutableSet.of();
        }
        return getSetOfStringIds(key);
    }

    /**
     * @return The set of ids in the list returned by {@link #parseListOfStringIds} for the given config key
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     */
    public <T> ImmutableSet<StringId<T>> getSetOfStringIds(Enum<?> key) {
        return ImmutableSet.copyOf(parseListOfStringIds(getString(key)));
    }

    private String[] getParts(Enum<?> key) {
        return parseParts(getString(key));
    }

    private static String[] parseParts(String value) {
        String[] splitArray = value.contains(",")
                ? value.split(",")
                : value.split(":");

        for (int i = 0; i < splitArray.length; i++) {
            splitArray[i] = splitArray[i].trim();
        }

        return splitArray;
    }

    public String getString(Enum<?> key) {
        String val = getOrNull(key);
        if (val == null) {
            throw new ConfigKeyNotFoundException(key);
        }
        return val.trim();
    }

    public Optional<String> getStringIfPresent(Enum<?> key) {
        return Optional.ofNullable(getOrNull(key)).map(String::trim);
    }

    /**
     * Returns a Map for config specified as a collection of key-value pairs with keys and values as Strings.
     *
     * Given a config value that is a (semicolon-separated) list of (equals-separated) key-value pairs:
     * <pre>"key1=value1;key2=value2"</pre>
     * Keys and values will be trimmed.
     * Any pair which does not contain the character '=' will be ignored.
     *
     * @param key Config key which contains the key-value pairs as a String.
     * @return a {@code Map<String, String>} of key-value pairs parsed from the config value
     * @throws IllegalArgumentException if duplicate keys are specified
     */
    public ImmutableMap<String, String> getStringMap(Enum<?> key) {
        return getMap(key, Function.identity(), Function.identity());
    }

    /**
     * Returns a typed-Map for config specified as a collection of key-value pairs.
     *
     * Given a config value that is a (semicolon-separated) list of (equals-separated) key-value pairs:
     * <pre>"key1=value1;key2=value2"</pre>
     * Keys and values will be trimmed, before being supplied to the functions that translate them to the
     * correct types.
     * Any pair which does not contain the character '=' will be ignored.
     *
     * @param configKey Config key which contains the key-value pairs as a String.
     * @param keyParser Function to convert a String to a key in the resulting Map.
     * @param valueParser Function to convert a String to a value in the resulting Map.
     * @param <K> The type of key in resulting {@code Map}
     * @param <V> The type of value in resulting {@code Map}
     * @return a Map of key-value pairs parsed from the config value
     * @throws IllegalArgumentException if duplicate keys are specified
     * @throws NullPointerException if the keyParser or valueParser return null for any provided string.
     */
    public <K, V> ImmutableMap<K, V> getMap(Enum<?> configKey, Function<String, K> keyParser, Function<String, V> valueParser) {
        String val = getOrNull(configKey);
        if (val == null) {
            return ImmutableMap.of();
        }
        ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
        for (String pair : val.split(";")) {
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
     * Returns the enum value to which the specified key is mapped.
     *
     * @param key Key for which to lookup the enum value.
     * @param enumClass Enum class defining the set of permitted values.
     * @param <T> Type of {@code enumClass}
     *
     * @return The value from the specified enum class which corresponds to the config value associated with the given
     * key.
     *
     * @throws ConfigKeyNotFoundException when there is no entry for the specified key.
     * @throws IllegalArgumentException when the config value does not match any of the values in the specified enum.
     */
    public <T extends Enum<T>> T getEnum(Enum<?> key, Class<T> enumClass) {
        String value = getString(key);
        return Enum.valueOf(enumClass, value);
    }

    /**
     * Returns the enum value to which the specified key is mapped, if present.
     *
     * @param key Key for which to lookup the enum value.
     * @param enumClass Enum class defining the set of permitted values.
     * @param <T> Type of {@code enumClass}
     *
     * @return The value from the specified enum class which corresponds to the config value associated with the given
     * key, or {@link Optional#empty()} if no entry for that key exists.
     *
     * @throws IllegalArgumentException when the config value does not match any of the values in the specified enum.
     */
    public <T extends Enum<T>> Optional<T> getEnumIfPresent(Enum<?> key, Class<T> enumClass) {
        if (!containsKey(key)) {
            return Optional.empty();
        }
        return Optional.of(getEnum(key, enumClass));
    }

    private String getOrNull(Enum<?> key) {
        if (key.getClass().equals(cls) && values.containsKey(cls.cast(key))) {
            return values.get(cls.cast(key)).currentValue;
        }
        Class<?> declaringClass = key.getDeclaringClass();
        while (declaringClass != null) {
            if (subConfig.containsKey(declaringClass)) {
                return subConfig.get(declaringClass).getOrNull(key);
            }
            declaringClass = declaringClass.getDeclaringClass();
        }
        return null;
    }

    public String getQualifiedKeyName(E key) {
        return qualifier + "." + key.toString();
    }

    @Deprecated
    public ImmutableMap<String, String> getKeyValueStringMap() {
        ImmutableMap.Builder<String, String> map = ImmutableMap.builder();
        consumeConfigValues((k, v, isSecret) -> map.put(k, v), false);
        return map.build();
    }

    public ImmutableMap<String, String> getKeyValueStringMapWithoutSecrets() {
        ImmutableMap.Builder<String, String> map = ImmutableMap.builder();
        consumeConfigValues((k, v, isSecret) -> {
            if (!isSecret) {
                map.put(k, v);
            }
        }, false);
        return map.build();
    }

    public ImmutableMap<String, String> getKeyValueStringMapWithPrefixesWithoutSecrets() {
        ImmutableMap.Builder<String, String> map = ImmutableMap.builder();
        consumeConfigValues((k, v, isSecret) -> {
            if (!isSecret) {
                map.put(k, v);
            }
        }, true);
        return map.build();
    }

    @Deprecated
    public <T extends Enum<T>> ImmutableMap<String, String> getUnqualifiedKeyValueStringMap(Class<T> key) {
        Config<T> subConfig = getSubConfig(key);
        ImmutableMap.Builder<String, String> map = ImmutableMap.builder();
        subConfig.consumeConfigValues((k, v, isSecret) -> map.put(k.substring(subConfig.qualifier.length() + 1), v), false);
        return map.build();
    }

    public <T extends Enum<T>> ImmutableMap<String, String> getUnqualifiedKeyValueStringMapWithoutSecrets(Class<T> key) {
        Config<T> subConfig = getSubConfig(key);
        ImmutableMap.Builder<String, String> map = ImmutableMap.builder();
        subConfig.consumeConfigValues((k, v, isSecret) -> {
            if (!isSecret) {
                map.put(k.substring(subConfig.qualifier.length() + 1), v);
            }
        }, false);
        return map.build();
    }

    private void consumeConfigValues(ToStringHelper toStringHelper, boolean includePrefixes) {
        values.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().toString()))
                .forEach(e -> consumeConfigValue(toStringHelper, includePrefixes, e.getKey(), e.getValue()));

        subConfig.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().toString()))
                .forEach(x -> x.getValue().consumeConfigValues(toStringHelper, includePrefixes));
    }

    private void consumeConfigValue(ToStringHelper toStringHelper, boolean includePrefixes, E key, ConfigValue value) {
        // currentValue is null when a prefixed Config value has no un-prefixed equivalent
        if (value.currentValue != null) {
            toStringHelper.accept(
                    getQualifiedKeyName(key),
                    value.currentValue,
                    isSecretConfig(key));
        }

        if (includePrefixes) {
            value.getValuesByPrefixedKeys(getQualifiedKeyName(key)).forEach((prefixedKey, prefixedValue) ->
                    toStringHelper.accept(
                            prefixedKey,
                            prefixedValue,
                            isSecretConfig(key)));
        }
    }

    private boolean isSecretConfig(E key) {
        try {
            return cls.getField(key.toString()).isAnnotationPresent(SecretConfig.class);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public int compareTo(Config<?> o) {
        return qualifier.compareTo(o.qualifier);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Config<?> config = (Config<?>) o;
        return Objects.equals(cls, config.cls)
                && Objects.equals(values, config.values)
                && Objects.equals(subConfig, config.subConfig)
                && Objects.equals(qualifier, config.qualifier)
                && timeUnit == config.timeUnit
                && lengthUnit == config.lengthUnit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cls, values, subConfig, qualifier, timeUnit, lengthUnit);
    }

    /**
     * @return a superset of all config contained in this and other, with the values from other being given priority.
     */
    @SuppressWarnings("unchecked") //this method needs to be unchecked because subConfig is unchecked.
    Config<E> merge(Config other) {
        Preconditions.checkState(qualifier.equals(other.qualifier), "Mismatched qualifiers:", qualifier, other.qualifier);
        Preconditions.checkState(cls.equals(other.cls), "Mismatched classes:", cls, other.cls);
        HashMap tempValues = new HashMap<>(values);
        other.values.forEach((e, v) -> tempValues.merge(e, v, (a, b) -> b));
        HashMap tempSubConfig = new HashMap<>(subConfig);
        other.subConfig.forEach((clz, conf) -> tempSubConfig.merge(clz, conf, (oldConf, newConf) -> ((Config) oldConf).merge((Config) newConf)));

        return new Config<>(
                cls,
                ImmutableMap.copyOf(tempValues),
                ImmutableMap.copyOf(tempSubConfig),
                qualifier,
                other.timeUnit != null ? other.timeUnit : timeUnit,
                other.lengthUnit != null ? other.lengthUnit : lengthUnit);
    }

    Config<E> setUnits(TimeUnit timeUnit, LengthUnit lengthUnit) {
        ImmutableMap<?, Config<?>> newSubConfig = subConfig.entrySet()
                .stream()
                .collect(ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue()
                        .setUnits(timeUnit, lengthUnit)));
        return new Config<>(
                cls,
                values,
                newSubConfig,
                qualifier,
                timeUnit,
                lengthUnit);
    }

    @Override
    public String toString() {
        Joiner joiner = Joiner.on("\n");
        return joiner.join(qualifier + '{', getStringValues(joiner), '}');
    }

    private String getStringValues(Joiner joiner) {
        List<String> entries = new ArrayList<>();
        consumeConfigValues((k, v, isSecret) -> {
            if (!isSecret) {
                entries.add(k + '=' + v);
            }
        }, false);
        return joiner.join(entries);
    }

    @FunctionalInterface
    private interface ToStringHelper {
        void accept(String key, String value, Boolean isSecret);
    }
}
