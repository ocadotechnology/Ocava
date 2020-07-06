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

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
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
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.StringId;
import com.ocadotechnology.physics.units.LengthUnit;

/**
 * Ocava Configuration is a properties file parser where keys are backed by an Enum.
 * <h2>Basic Usage</h2>
 * The Enum for Config can contain nested Enumeration, i.e.
 * <pre>
 *   public Enum MyConfig {
 *       ROOT_KEY;
 *       public Enum MyChildConfig {
 *           CHILD_KEY,
 *           SECOND_KEY;
 *           ...
 *       }
 *   }
 * </pre>
 * <p>
 * Each Key in the properties file ends with a Enum value (i.e. CHILD_KEY) and is prefixed by each class dot separated
 * (i.e. MyConfig.MyChildConfig.CHILD_KEY).
 * <p>
 * So a simple example properties file could look like:
 * <pre>
 *     MyConfig.ROOT_KEY=A root key
 *     MyConfig.MyChildConfig.CHILD_KEY=1, SECONDS
 *     MyConfig.MyChildConfig.SECOND_KEY=false
 * </pre>
 * <h3>Data Types</h3>
 * The configuration object has support for many data types (some can be seen in the example) some of these
 * data types are (For the full list inspect the value parsers {@link StrictValueParser} and {@link OptionalValueParser}):
 * <ul>
 *     <li><strong>String</strong></li>
 *     <li><strong>Numeric</strong> - Integer, Long, Double</li>
 *     <li><strong>Boolean</strong></li>
 *     <li><strong>Time</strong> - Either as Long, Double or {@link Duration}</li>
 *     <li><strong>Speed</strong> - See {@link StrictValueParser#asSpeed()}</li>
 *     <li><strong>Acceleration</strong> - See {@link StrictValueParser#asAcceleration()}</li>
 *     <li><strong>Enum</strong> - See {@link StrictValueParser#asEnum(Class)}</li>
 *     <li><strong>List</strong> - See {@link StrictValueParser#asList()} and {@link ListValueParser}</li>
 *     <li><strong>Set</strong> - See {@link StrictValueParser#asSet()} and {@link SetValueParser}</li>
 *     <li><strong>Map</strong> - See {@link StrictValueParser#asMap()} and {@link MapValueParser}</li>
 *     <li><strong>Multimap</strong> - See {@link StrictValueParser#asSetMultimap()} and {@link SetMultimapValueParser}</li>
 * </ul>
 *
 * <h3>{@link #getValue} vs {@link #getIfValueDefined} and {@link #getIfKeyAndValueDefined}</h3>
 * {@link #getValue} requires that the config key is present in the object, and will throw an exception if it is not. In
 * some use-cases, the idea of querying the presence of a config key may seem useful, but can lead to complications due
 * to the way the configuration library works by layering multiple configuration on top of each other.
 * <p>
 * If the presence of a config value is used to change the flow of logic (i.e. a config flag) it might be that you wish
 * to override it's presence by command line or specific additional config file. However once a config key is present it
 * cannot be removed.
 * <p>
 * To overcome this limitation, the method {@link #getIfKeyAndValueDefined} treats a key which maps to an empty string
 * the same as a key which is entirely absent, returning {@link Optional#empty()} from all parsers in that case. {@link
 * #getIfValueDefined} works similarly for an empty string, but requires that the key be present in the object, for
 * those users who which to ensure that a config value was deliberately removed.
 *
 * <h2>Prefixes</h2>
 * Additionally to the configuration Object and the different data types it can handle there is also the concept of
 * prefixes.
 * <p>
 * Prefixes allow for multiple layers for a single config key, or for a single config key to be defined multiple times.
 * Taking the configuration enum example from above we could write a properties file like:
 * <pre>
 *     MyConfig.ROOT_KEY=root_key
 *     site1@MyConfig.ROOT_KEY=site1_key
 *     site1@node1@MyConfig.ROOT_KEY=node1_key
 *     site2@MyConfig.ROOT_KEY=site2_key
 * </pre>
 * <p>
 * With the above when we want to get the root key for say a specific site we can use use
 * {@link #getPrefixedConfigItems(String)} and then call {@link #getString(Enum)}
 *
 * <pre>
 *     var siteConfig = config.getPrefixedConfigItems("site10");
 *     var key = siteConfig.getString(MyConfig.ROOT_KEY)
 * </pre>
 * <p>
 * Note that in the properties file site10 is not one of the listed prefixes, in this case it falls back to the default
 * value of "root_key". Prefixes can be nested, so it would be possible to do:
 *
 * <pre>
 *     var nodeConfig = config.getPrefixedConfigItems("site1").getPrefixedConfigItems("node1");
 *     var key = nodeConfig.getString(MyConfig.ROOT_KEY)
 * </pre>
 * <p>
 * In the above case key would equal node1_key, as the prefix site1 exists and the prefix node1 exists under site1
 *
 * @param <E> Configuration Enumeration containing all allowed property keys
 */
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
     *
     * @param prefix The prefix to use for the current value / the prefix tree to use for multi-prefix configs
     * @return a new Config object with config values and prefixes from the given prefix
     */
    public Config<E> getPrefixedConfigItems(String prefix) {
        return map(configValue -> configValue.getPrefix(prefix));
    }

    /**
     * Returns a new Config where for each config item, it will set the value to be that of the prefix value if present,
     * otherwise it will keep its original value.
     * The Config returned will have the same prefix data as the original, so multi-prefixed values are not accessible
     * and prefixed values with a different root are still present.
     *
     * @param prefix The prefix to use for the current value / the prefix tree to use for multi-prefix configs
     * @return a new Config object with config values and prefixes from the given prefix
     */

    public Config<E> getPrefixBiasedConfigItems(String prefix) {
        return map(configValue -> configValue.getWithPrefixBias(prefix));
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
        return Preconditions.checkNotNull(timeUnit, "timeUnit not set. See ConfigManager.Builder.setTimeUnit.");
    }

    public LengthUnit getLengthUnit() {
        return Preconditions.checkNotNull(lengthUnit, "lengthUnit not set. See ConfigManager.Builder.setLengthUnit.");
    }

    /**
     * @deprecated to discourage key presence on its own to be used for flow control
     * (see class javadoc for reasoning). Use {@link #isValueDefined(Enum)}
     * or {@link #areKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public boolean containsKey(Enum<?> key) {
        return getOrNone(key).isPresent();
    }

    /**
     * Check if the value of key is not the empty string.
     * @throws ConfigKeyNotFoundException if the key has not been explicitly defined
     */
    public boolean isValueDefined(Enum<?> key) {
        Optional<String> value = getOrNone(key);

        return value.map(s -> !s.trim().isEmpty()).orElseThrow(() -> new ConfigKeyNotFoundException(key));
    }

    /**
     * Check that the key has been explicitly defined and not to the empty string.
     * The two checks are done simultaneously because key presence on its own should not
     * be used for flow control (see class javadoc for reasoning).
     */
    public boolean areKeyAndValueDefined(Enum<?> key) {
        Optional<String> value = getOrNone(key);

        return value.map(s -> !s.trim().isEmpty()).orElse(false);
    }

    /**
     * Check that the this config enum type contains the enum key independently from whether the key's value is set.
     * For example, ExampleConfig.VALUE does have the same enum type of Config&lt;ExampleConfig&gt;,
     * CounterExample.VALUE does not.
     */
    public boolean enumTypeIncludes(Enum<?> key) {
        Class<?> clazz = key.getClass();

        while (clazz != null) {
            if (clazz.equals(cls)) {
                return true;
            }

            clazz = clazz.getEnclosingClass();
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> Config<T> getSubConfig(Class<T> key) {
        return (Config<T>) subConfig.get(key);
    }

    /**
     * Create a {@link StrictValueParser} object to parse the value associated with the given key.
     *
     * @param key the key to look up in this config object.
     * @return a {@link StrictValueParser} object built from the value associated with the specified key.
     * @throws ConfigKeyNotFoundException if the key is not defined in this Config object.
     */
    public StrictValueParser getValue(Enum<?> key) {
        return new StrictValueParser(getString(key), timeUnit, lengthUnit);
    }

    /**
     * Create an {@link OptionalValueParser} object to parse the value associated with the given key.  This will return
     * {@link Optional#empty()} if the value is defined as an empty string.
     *
     * @param key the key to look up in this config object.
     * @return an {@link OptionalValueParser} object built from the value associated with the specified key.
     * @throws ConfigKeyNotFoundException if the key is not defined in this Config object.
     */
    public OptionalValueParser getIfValueDefined(Enum<?> key) {
        return new OptionalValueParser(getString(key), timeUnit, lengthUnit);
    }

    /**
     * Create an {@link OptionalValueParser} object to parse the value associated with the given key.  This will return
     * {@link Optional#empty()} if the key is not defined in this config object or if the associated value is an empty
     * string.
     *
     * @param key the key to look up in this config object.
     * @return an {@link OptionalValueParser} object built from the value associated with the specified key.
     */
    public OptionalValueParser getIfKeyAndValueDefined(Enum<?> key) {
        String value = getOrNone(key).map(String::trim).orElse("");

        return new OptionalValueParser(value, timeUnit, lengthUnit);
    }

    /**
     * Interprets a config value as an integer. If it is the String "max" or "min" (case insensitive) returns
     * {@link Integer#MAX_VALUE} or {@link Integer#MIN_VALUE} respectively.
     *
     * @throws NumberFormatException if the value is not equal to either the string "max" or "min", ignoring case and is
     *          not a valid base-10 integer.
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object.
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public int getInt(Enum<?> key) {
        return ConfigParsers.parseInt(getString(key));
    }

    /**
     * Interprets a config value as an integer, if it is present. If the value is not present, returns the default
     * value.
     *
     * If the value is the String "max" or "min" (case insensitive), returns {@link Integer#MAX_VALUE} or
     * {@link Integer#MIN_VALUE} respectively.
     *
     * @throws NumberFormatException if the value is not equal to either the string "max" or "min", ignoring case and is
     *          not a valid base-10 integer.
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public int getIntOrDefault(Enum<?> key, int defaultValue) {
        return getOrDefault(key, ConfigParsers::parseInt, defaultValue);
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public OptionalInt getIntIfPresent(Enum<?> key) {
        if (!containsKey(key)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(getInt(key));
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public Optional<Double> getDoubleIfPresent(Enum<?> key) {
        if (!containsKey(key)) {
            return Optional.empty();
        }

        return Optional.of(getDouble(key));
    }

    /**
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public double getDouble(Enum<?> key) {
        return ConfigParsers.parseDouble(getString(key));
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public double getDoubleOrDefault(Enum<?> key, double defaultValue) {
        return getOrDefault(key, ConfigParsers::parseDouble, defaultValue);
    }

    /**
     * Interprets a config value as a boolean.
     *
     * @throws IllegalStateException if the value is not equal to either the string "true" or "false", ignoring case.
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public boolean getBoolean(Enum<?> key) {
        return ConfigParsers.parseBoolean(getString(key));
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public boolean getBooleanOrDefault(Enum<?> key, boolean defaultValue) {
        return getOrDefault(key, ConfigParsers::parseBoolean, defaultValue);
    }

    /**
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public long getLong(Enum<?> key) {
        return ConfigParsers.parseLong(getString(key));
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public long getLongOrDefault(Enum<?> key, long defaultValue) {
        return getOrDefault(key, ConfigParsers::parseLong, defaultValue);
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public OptionalLong getLongIfPresent(Enum<?> key) {
        if (!containsKey(key)) {
            return OptionalLong.empty();
        }

        return OptionalLong.of(getLong(key));
    }

    /**
     * Interprets a config value as a length using the declared application length unit.
     * <p>
     * Length config values can be given either
     * - as a double, in which case Config will assume that the value is being specified in m
     * - in the form {@code <value>,<length unit>} or {@code <value>:<length unit>}
     *
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @throws NullPointerException       if the application length unit has not been set
     * @throws IllegalStateException      if the config value does not satisfy one of the formats given above
     * @throws IllegalArgumentException   if the length unit in the config value does not match an enum value
     * @throws NumberFormatException      if the value given cannot be parsed as a double
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public double getLength(Enum<?> key) {
        return ConfigParsers.parseLength(getString(key), getLengthUnit());
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public double getLengthOrDefault(Enum<?> key, double defaultValue) {
        return getOrDefault(key, v -> ConfigParsers.parseLength(v, getLengthUnit()), defaultValue);
    }

    /**
     * Interprets a config value as a time using the declared application time unit.
     * <p>
     * Time config values can be given either
     * - as a double, in which case Config will assume that the value is being specified in s
     * - in the form {@code <value>,<time unit>} or {@code <value>:<time unit>}
     *
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @throws NullPointerException       if the application time unit has not been set
     * @throws IllegalStateException      if the config value does not satisfy one of the formats given above
     * @throws IllegalArgumentException   if the time unit in the config value does not match an enum value
     * @throws NumberFormatException      if the value given cannot be parsed as a double
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public double getFractionalTime(Enum<?> key) {
        return ConfigParsers.parseFractionalTime(getString(key), getTimeUnit());
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public double getFractionalTimeOrDefault(Enum<?> key, double defaultValue) {
        return getOrDefault(key, v -> ConfigParsers.parseFractionalTime(v, getTimeUnit()), defaultValue);
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     *
     * @return the result of {@link Config#getFractionalTime} if the config key has a defined value, else {@link Optional#empty()}
     * Optional is used in place of OptionalDouble as OptionalDouble is missing some features.
     */
    @Deprecated
    public Optional<Double> getFractionalTimeIfPresent(Enum<?> key) {
        if (!containsKey(key)) {
            return Optional.empty();
        }
        return Optional.of(getFractionalTime(key));
    }

    /**
     * Interprets a config value as a time using the declared application time unit.
     * <p>
     * Time config values can be given either
     * - as a double, in which case Config will assume that the value is being specified in s
     * - in the form {@code <value>,<time unit>} or {@code <value>:<time unit>}
     *
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @throws NullPointerException       if the application time unit has not been set
     * @throws IllegalStateException      if the config value does not satisfy one of the formats given above
     * @throws IllegalArgumentException   if the time unit in the config value does not match an enum value
     * @throws NumberFormatException      if the value given cannot be parsed as a double
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public long getTime(Enum<?> key) {
        return Math.round(getFractionalTime(key));
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public long getTimeOrDefault(Enum<?> key, long defaultValue) {
        return getOrDefault(key, v -> Math.round(ConfigParsers.parseFractionalTime(v, getTimeUnit())), defaultValue);
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     *
     * @return the result of {@link Config#getTime} if the config key has a defined value, else {@link Optional#empty()}
     * Optional is used in place of OptionalLong as OptionalLong is missing some features.
     */
    @Deprecated
    public Optional<Long> getTimeIfPresent(Enum<?> key) {
        if (!containsKey(key)) {
            return Optional.empty();
        }
        return Optional.of(getTime(key));
    }

    /**
     * Interprets a config value as a {@link Duration} rounded to the nearest nanosecond
     * <p>
     * Duration config values can be given either:
     * - As a double on its own, in which case it will be assumed that the value is being specified in seconds
     * - In the form {@code <value>,<time unit>} or {@code <value>:<time unit>}
     *
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @throws IllegalStateException      if the config value does not satisfy one of the formats given above
     * @throws IllegalArgumentException   if the time unit in the config value does not match an enum value
     * @throws NumberFormatException      if the value given cannot be parsed as a double
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public Duration getDuration(Enum<?> key) {
        return ConfigParsers.parseDuration(getString(key));
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public Duration getDurationOrDefault(Enum<?> key, Duration defaultValue) {
        return getOrDefault(key, ConfigParsers::parseDuration, defaultValue);
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     *
     * @return the result of {@link Config#getDuration} if the config key has a defined value, else {@link Optional#empty()}
     */
    @Deprecated
    public Optional<Duration> getDurationIfPresent(Enum<?> key) {
        if (!containsKey(key)) {
            return Optional.empty();
        }
        return Optional.of(getDuration(key));
    }

    /**
     * Interprets a config value as a speed using the declared application time and length units.
     * <p>
     * Speed config values can be given either
     * - as a double, in which case Config will assume that the value is being specified in m/s
     * - in the form {@code <value>,<length unit>,<time unit>} or {@code <value>:<length unit>:<time unit>}
     *
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @throws NullPointerException       if the application time or length units have not been set
     * @throws IllegalStateException      if the config value does not satisfy one of the formats given above
     * @throws IllegalArgumentException   if the time or length units in the config value do not match an enum value
     * @throws NumberFormatException      if the value given cannot be parsed as a double
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public double getSpeed(Enum<?> key) {
        return ConfigParsers.parseSpeed(getString(key), getLengthUnit(), getTimeUnit());
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public double getSpeedOrDefault(Enum<?> key, double defaultValue) {
        return getOrDefault(key, v -> ConfigParsers.parseSpeed(v, getLengthUnit(), getTimeUnit()), defaultValue);
    }

    /**
     * Interprets a config value as an acceleration using the declared application time and length units.
     * <p>
     * Acceleration config values can be given either
     * - as a double, in which case Config will assume that the value is being specified in m/s^2
     * - in the form {@code <value>,<length unit>,<time unit>} or {@code <value>:<length unit>:<time unit>}
     *
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @throws NullPointerException       if the application time or length units have not been set
     * @throws IllegalStateException      if the config value does not satisfy one of the formats given above
     * @throws IllegalArgumentException   if the time or length units in the config value do not match an enum value
     * @throws NumberFormatException      if the value given cannot be parsed as a double
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public double getAcceleration(Enum<?> key) {
        return ConfigParsers.parseAcceleration(getString(key), getLengthUnit(), getTimeUnit());
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public double getAccelerationOrDefault(Enum<?> key, double defaultValue) {
        return getOrDefault(key, v -> ConfigParsers.parseAcceleration(v, getLengthUnit(), getTimeUnit()), defaultValue);
    }

    /**
     * Returns a list of T
     *
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     *
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public <T> ImmutableList<T> getListOf(Enum<?> key, Function<String, T> valueFunction) {
        return ConfigParsers.getListOf(valueFunction).apply(getString(key));
    }

    /**
     * Returns a list of T or empty if the key is not present
     *
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <T> ImmutableList<T> getListOfOrEmpty(Enum<?> key, Function<String, T> valueFunction) {
        return getListOfOrDefault(key, valueFunction, ImmutableList.of());
    }

    /**
     * Returns a list of T or the default value if the key is not present
     *
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <T> ImmutableList<T> getListOfOrDefault(Enum<?> key, Function<String, T> valueFunction, ImmutableList<T> defaultValue) {
        return getOrDefault(key, ConfigParsers.getListOf(valueFunction), defaultValue);
    }

    /**
     * Returns a set of T
     *
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public <T> ImmutableSet<T> getSetOf(Enum<?> key, Function<String, T> valueFunction) {
        return ConfigParsers.getSetOf(valueFunction).apply(getString(key));
    }

    /**
     * Returns a set of T or empty if the key is not present
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <T> ImmutableSet<T> getSetOfOrEmpty(Enum<?> key, Function<String, T> valueFunction) {
        return getSetOfOrDefault(key, valueFunction, ImmutableSet.of());
    }

    /**
     * Returns a set of T or the default value if the key is not present
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <T> ImmutableSet<T> getSetOfOrDefault(Enum<?> key, Function<String, T> valueFunction, ImmutableSet<T> defaultValue) {
        return getOrDefault(key, ConfigParsers.getSetOf(valueFunction), defaultValue);
    }

    /**
     * Returns a list of Integers. Defers value parsing to {@link ConfigParsers#parseInt(String)}
     *
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     *
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public ImmutableList<Integer> getListOfIntegers(Enum<?> key) {
        return ConfigParsers.getListOfIntegers().apply(getString(key));
    }

    /**
     * Returns a list of Integers or empty if the key is not present.
     * Defers value parsing to {@link ConfigParsers#parseInt(String)}
     *
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public ImmutableList<Integer> getListOfIntegersOrEmpty(Enum<?> key) {
        return getListOfIntegersOrDefault(key, ImmutableList.of());
    }

    /**
     * Returns a list of Integers or the default value if the key is not present.
     * Defers value parsing to {@link ConfigParsers#parseInt(String)}
     *
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public ImmutableList<Integer> getListOfIntegersOrDefault(Enum<?> key, ImmutableList<Integer> defaultValue) {
        return getOrDefault(key, ConfigParsers.getListOfIntegers(), defaultValue);
    }

    /**
     * Returns a Set of Integers. Defers value parsing to {@link ConfigParsers#parseInt(String)}
     *
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public ImmutableSet<Integer> getSetOfIntegers(Enum<?> key) {
        return ConfigParsers.getSetOfIntegers().apply(getString(key));
    }

    /**
     * Returns a Set of Integers or empty if the key is not present.
     * Defers value parsing to {@link ConfigParsers#parseInt(String)}
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public ImmutableSet<Integer> getSetOfIntegersOrEmpty(Enum<?> key) {
        return getSetOfIntegersOrDefault(key, ImmutableSet.of());
    }

    /**
     * Returns a Set of Integers or the default value if the key is not present.
     * Defers value parsing to {@link ConfigParsers#parseInt(String)}
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public ImmutableSet<Integer> getSetOfIntegersOrDefault(Enum<?> key, ImmutableSet<Integer> defaultValue) {
        return getOrDefault(key, ConfigParsers.getSetOfIntegers(), defaultValue);
    }

    /**
     * Returns a list of Longs. Defers value parsing to {@link ConfigParsers#parseLong(String)}
     *
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     *
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public ImmutableList<Long> getListOfLongs(Enum<?> key) {
        return ConfigParsers.getListOfLongs().apply(getString(key));
    }

    /**
     * Returns a list of Longs or empty if the key is not present.
     * Defers value parsing to {@link ConfigParsers#parseLong(String)}
     *
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public ImmutableList<Long> getListOfLongsOrEmpty(Enum<?> key) {
        return getListOfLongsOrDefault(key, ImmutableList.of());
    }

    /**
     * Returns a list of Longs or the default value if the key is not present.
     * Defers value parsing to {@link ConfigParsers#parseLong(String)}
     *
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public ImmutableList<Long> getListOfLongsOrDefault(Enum<?> key, ImmutableList<Long> defaultValue) {
        return getOrDefault(key, ConfigParsers.getListOfLongs(), defaultValue);
    }

    /**
     * Returns a Set of Longs. Defers value parsing to {@link ConfigParsers#parseLong(String)}
     *
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public ImmutableSet<Long> getSetOfLongs(Enum<?> key) {
        return ConfigParsers.getSetOfLongs().apply(getString(key));
    }

    /**
     * Returns a Set of Longs or empty if the key is not present.
     * Defers value parsing to {@link ConfigParsers#parseLong(String)}
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public ImmutableSet<Long> getSetOfLongsOrEmpty(Enum<?> key) {
        return getSetOfLongsOrDefault(key, ImmutableSet.of());
    }

    /**
     * Returns a Set of Longs or the default value if the key is not present.
     * Defers value parsing to {@link ConfigParsers#parseLong(String)}
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public ImmutableSet<Long> getSetOfLongsOrDefault(Enum<?> key, ImmutableSet<Long> defaultValue) {
        return getOrDefault(key, ConfigParsers.getSetOfLongs(), defaultValue);
    }

    /**
     * Returns a list of Doubles. Defers value parsing to {@link ConfigParsers#parseDouble(String)}
     *
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     *
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public ImmutableList<Double> getListOfDoubles(Enum<?> key) {
        return ConfigParsers.getListOfDoubles().apply(getString(key));
    }

    /**
     * Returns a list of Doubles or empty if the key is not present.
     * Defers value parsing to {@link ConfigParsers#parseDouble(String)}
     *
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public ImmutableList<Double> getListOfDoublesOrEmpty(Enum<?> key) {
        return getListOfDoublesOrDefault(key, ImmutableList.of());
    }

    /**
     * Returns a list of Doubles or the default value if the key is not present.
     * Defers value parsing to {@link ConfigParsers#parseDouble(String)}
     *
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public ImmutableList<Double> getListOfDoublesOrDefault(Enum<?> key, ImmutableList<Double> defaultValue) {
        return getOrDefault(key, ConfigParsers.getListOfDoubles(), defaultValue);
    }

    /**
     * Returns a Set of Doubles. Defers value parsing to {@link ConfigParsers#parseDouble(String)}
     *
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public ImmutableSet<Double> getSetOfDoubles(Enum<?> key) {
        return ConfigParsers.getSetOfDoubles().apply(getString(key));
    }

    /**
     * Returns a Set of Doubles or empty if the key is not present.
     * Defers value parsing to {@link ConfigParsers#parseDouble(String)}
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public ImmutableSet<Double> getSetOfDoublesOrEmpty(Enum<?> key) {
        return getSetOfDoublesOrDefault(key, ImmutableSet.of());
    }

    /**
     * Returns a Set of Doubles or the default value if the key is not present.
     * Defers value parsing to {@link ConfigParsers#parseDouble(String)}
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public ImmutableSet<Double> getSetOfDoublesOrDefault(Enum<?> key, ImmutableSet<Double> defaultValue) {
        return getOrDefault(key, ConfigParsers.getSetOfDoubles(), defaultValue);
    }

    /**
     * {@link #getListOfIdsOrEmpty(Enum)} instead.
     *
     * @return the same as {@link #getListOfIds} or an empty list if the config key isn't found
     * @throws NumberFormatException if the values given cannot be parsed as longs
     *
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <T> ImmutableList<Id<T>> getListOfIdsIfPresent(Enum<?> key) {
        if (!containsKey(key)) {
            return ImmutableList.of();
        }
        return getListOfIds(key);
    }

    /**
     * @return the same as {@link ConfigParsers#getListOfIds()} with the value linked to the given config key
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @throws NumberFormatException      if the values given cannot be parsed as longs
     *
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public <T> ImmutableList<Id<T>> getListOfIds(Enum<?> key) {
        return ConfigParsers.<T>getListOfIds().apply(getString(key));
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <T> ImmutableList<Id<T>> getListOfIdsOrEmpty(Enum<?> key) {
        return getListOfIdsOrDefault(key, ImmutableList.of());
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <T> ImmutableList<Id<T>> getListOfIdsOrDefault(Enum<?> key, ImmutableList<Id<T>> defaultValue) {
        return getOrDefault(key, ConfigParsers.getListOfIds(), defaultValue);
    }

    /**
     * @deprecated - see class javadoc for reasoning - use {@link #getSetOfIdsOrEmpty(Enum)} or
     * {@link #getSetOfIdsOrDefault(Enum, ImmutableSet)} instead.
     *
     * @return the same as {@link #getSetOfIds} or an empty set if the config key isn't found
     * @throws NumberFormatException if the values given cannot be parsed as longs
     */
    @Deprecated
    public <T> ImmutableSet<Id<T>> getSetOfIdsIfPresent(Enum<?> key) {
        if (!containsKey(key)) {
            return ImmutableSet.of();
        }
        return getSetOfIds(key);
    }

    /**
     * @return the set of ids in the list returned by {@link ConfigParsers#getSetOfIds()} for the given config key
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @throws NumberFormatException      if the values given cannot be parsed as longs
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public <T> ImmutableSet<Id<T>> getSetOfIds(Enum<?> key) {
        return ImmutableSet.copyOf(getListOfIds(key));
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <T> ImmutableSet<Id<T>> getSetOfIdsOrEmpty(Enum<?> key) {
        return getSetOfIdsOrDefault(key, ImmutableSet.of());
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <T> ImmutableSet<Id<T>> getSetOfIdsOrDefault(Enum<?> key, ImmutableSet<Id<T>> defaultValue) {
        return getOrDefault(key, ConfigParsers.getSetOfIds(), defaultValue);
    }

    /**
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public <T extends Enum<T>> ImmutableSet<T> getSetOfEnums(Enum<?> key, Class<T> enumClass) {
        return ConfigParsers.getSetOfEnums(enumClass).apply(getString(key));
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <T extends Enum<T>> ImmutableSet<T> getSetOfEnumsOrEmpty(Enum<?> key, Class<T> enumClass) {
        return getSetOfEnumsOrDefault(key, enumClass, ImmutableSet.of());
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <T extends Enum<T>> ImmutableSet<T> getSetOfEnumsOrDefault(Enum<?> key, Class<T> enumClass, ImmutableSet<T> defaultValue) {
        return getOrDefault(key, v -> ConfigParsers.getSetOfEnums(enumClass).apply(v), defaultValue);
    }

    /**
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public <T extends Enum<T>> ImmutableList<T> getListOfEnums(Enum<?> key, Class<T> enumClass) {
        return ConfigParsers.getListOfEnums(enumClass).apply(getString(key));
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <T extends Enum<T>> ImmutableList<T> getListOfEnumsOrEmpty(Enum<?> key, Class<T> enumClass) {
        return getOrDefault(key, ConfigParsers.getListOfEnums(enumClass), ImmutableList.of());
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <T extends Enum<T>> ImmutableList<T> getListOfEnumsOrDefault(Enum<?> key, Class<T> enumClass, ImmutableList<T> defaultValue) {
        return getOrDefault(key, ConfigParsers.getListOfEnums(enumClass), defaultValue);
    }

    /**
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public ImmutableList<String> getListOfStrings(Enum<?> key) {
        return ConfigParsers.getListOf(Function.identity()).apply(getString(key));
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public ImmutableList<String> getListOfStringsOrEmpty(Enum<?> key) {
        return getListOfStringsOrDefault(key, ImmutableList.of());
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public ImmutableList<String> getListOfStringsOrDefault(Enum<?> key, ImmutableList<String> defaultValue) {
        return getOrDefault(key, ConfigParsers.getListOfStrings(), defaultValue);
    }

    /**
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public ImmutableSet<String> getSetOfStrings(Enum<?> key) {
        return ConfigParsers.getSetOf(Function.identity()).apply(getString(key));
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public ImmutableSet<String> getSetOfStringsOrEmpty(Enum<?> key) {
        return getSetOfStringsOrDefault(key, ImmutableSet.of());
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public ImmutableSet<String> getSetOfStringsOrDefault(Enum<?> key, ImmutableSet<String> defaultValue) {
        return getOrDefault(key, ConfigParsers.getSetOfStrings(), defaultValue);
    }

    /**
     * {@link #getListOfStringIdsOrDefault(Enum, ImmutableList)} instead.
     *
     * @return the same as {@link #getListOfStringIds} or an empty list if the config key isn't found
     *
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <T> ImmutableList<StringId<T>> getListOfStringIdsIfPresent(Enum<?> key) {
        if (!containsKey(key)) {
            return ImmutableList.of();
        }
        return getListOfStringIds(key);
    }

    /**
     * @return the same as {@link ConfigParsers#getListOfStringIds()} with the value linked to the given config key
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public <T> ImmutableList<StringId<T>> getListOfStringIds(Enum<?> key) {
        return ConfigParsers.<T>getListOfStringIds().apply(getString(key));
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <T> ImmutableList<StringId<T>> getListOfStringIdsOrEmpty(Enum<?> key) {
        return getListOfStringIdsOrDefault(key, ImmutableList.of());
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <T> ImmutableList<StringId<T>> getListOfStringIdsOrDefault(Enum<?> key, ImmutableList<StringId<T>> defaultValue) {
        return getOrDefault(key, ConfigParsers.getListOfStringIds(), defaultValue);
    }

    /**
     * @return the same as {@link #getSetOfStringIds} or an empty set if the config key isn't found
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <T> ImmutableSet<StringId<T>> getSetOfStringIdsIfPresent(Enum<?> key) {
        if (!containsKey(key)) {
            return ImmutableSet.of();
        }
        return getSetOfStringIds(key);
    }

    /**
     * @return the set of ids in the list returned by {@link ConfigParsers#getSetOfStringIds()} for the given config key
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public <T> ImmutableSet<StringId<T>> getSetOfStringIds(Enum<?> key) {
        return ImmutableSet.copyOf(getListOfStringIds(key));
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <T> ImmutableSet<StringId<T>> getSetOfStringIdsOrEmpty(Enum<?> key) {
        return getSetOfStringIdsOrDefault(key, ImmutableSet.of());
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <T> ImmutableSet<StringId<T>> getSetOfStringIdsOrDefault(Enum<?> key, ImmutableSet<StringId<T>> defaultValue) {
        return getOrDefault(key, ConfigParsers.getSetOfStringIds(), defaultValue);
    }

    /**
     * Get a value from the Config set.
     *
     * @param key Configuration key
     * @return the raw {@link String} value of that key
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public String getString(Enum<?> key) {
        Optional<String> value = getOrNone(key);

        return value.map(String::trim).orElseThrow(() -> new ConfigKeyNotFoundException(key));
    }

    /**
     * Get a value from the Config set or return a specified default if the key does not have a defined value.
     *
     * @param key          Configuration key
     * @param defaultValue The value to return if the key is not specified
     * @return             the raw {@link String} value of that key or {@code defaultValue} if no value is found
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public String getStringOrDefault(Enum<?> key, String defaultValue) {
        return getOrDefault(key, Function.identity(), defaultValue);
    }

    /**
     * Get a single value and transform it using the valueFunction.
     *
     * @param key           Configuration key
     * @param valueFunction Function for extracting the value from the config
     * @param <V>           Result type
     * @return the value of the key after valueFunction is applied
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public <V> V get(Enum<?> key, Function<String, V> valueFunction) {
        return valueFunction.apply(getString(key));
    }

    /**
     * Wrapper for handling a get or default.
     *
     * @param key            Configuration key
     * @param valueExtractor Function for extracting the value from a String
     * @param defaultValue   Default value to return if the key is not present in the config
     * @param <V>            Result type
     * @return if the Key exists in the config then the result of valueExtractor otherwise defaultValue
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <V> V getOrDefault(Enum<?> key, Function<String, V> valueExtractor, V defaultValue) {
        if (containsKey(key)) {
            return get(key, valueExtractor);
        } else {
            return defaultValue;
        }
    }

    /**
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public Optional<String> getStringIfPresent(Enum<?> key) {
        return getOrNone(key).map(String::trim);
    }

    /**
     * Returns a Map for config specified as a collection of key-value pairs with keys and values as Strings.
     * <p>
     * Given a config value that is a (semicolon-separated) list of (equals-separated) key-value pairs:
     * <pre>"key1=value1;key2=value2"</pre>
     * Keys and values will be trimmed.
     * Any pair which does not contain the character '=' will be ignored.
     *
     * @param key Config key which contains the key-value pairs as a String.
     * @return a {@code Map<String, String>} of key-value pairs parsed from the config value
     * @throws IllegalArgumentException   if duplicate keys are specified
     * @throws ConfigKeyNotFoundException if the config key does not have a value in this Config object
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public ImmutableMap<String, String> getStringMap(Enum<?> key) {
        return getMap(key, Function.identity(), Function.identity());
    }

    /**
     * Returns a Map for config specified as a collection of key-value pairs with keys and values as Strings.
     * <p>
     * Given a config value that is a (semicolon-separated) list of (equals-separated) key-value pairs:
     * <pre>"key1=value1;key2=value2"</pre>
     * Keys and values will be trimmed.
     * Any pair which does not contain the character '=' will be ignored.
     *
     * If no value is specified for the config key, returns an empty map.
     *
     * @param key Config key which contains the key-value pairs as a String.
     * @return a {@code Map<String, String>} of key-value pairs parsed from the config value, or an empty map if the
     *          config key has no specified value
     * @throws IllegalArgumentException   if duplicate keys are specified
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public ImmutableMap<String, String> getStringMapOrEmpty(Enum<?> key) {
        return getStringMapOrDefault(key, ImmutableMap.of());
    }

    /**
     * Returns a Map for config specified as a collection of key-value pairs with keys and values as Strings.
     * <p>
     * Given a config value that is a (semicolon-separated) list of (equals-separated) key-value pairs:
     * <pre>"key1=value1;key2=value2"</pre>
     * Keys and values will be trimmed.
     * Any pair which does not contain the character '=' will be ignored.
     *
     * If no value is specified for the config key, returns the specified default value.
     *
     * @param key Config key which contains the key-value pairs as a String.
     * @param defaultValue the value to return if the config key has no specified value
     * @return a {@code Map<String, String>} of key-value pairs parsed from the config value, or {@code defaultValue} if
     *          the config key has no specified value
     * @throws IllegalArgumentException   if duplicate keys are specified
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public ImmutableMap<String, String> getStringMapOrDefault(Enum<?> key, ImmutableMap<String, String> defaultValue) {
        return getOrDefault(key, v -> ConfigParsers.parseMap(v, Function.identity(), Function.identity()), defaultValue);
    }

    /**
     * Returns a typed-Map for config specified as a collection of key-value pairs.
     * <p>
     * Given a config value that is a (semicolon-separated) list of (equals-separated) key-value pairs:
     * <pre>"key1=value1;key2=value2"</pre>
     * Keys and values will be trimmed, before being supplied to the functions that translate them to the
     * correct types.
     * Any pair which does not contain the character '=' will be ignored.
     *
     * @param configKey   Config key which contains the key-value pairs as a String.
     * @param keyParser   Function to convert a String to a key in the resulting Map.
     * @param valueParser Function to convert a String to a value in the resulting Map.
     * @param <K>         The type of key in resulting {@code Map}
     * @param <V>         The type of value in resulting {@code Map}
     * @return a Map of key-value pairs parsed from the config value
     * @throws IllegalArgumentException   if duplicate map keys are specified
     * @throws NullPointerException       if the keyParser or valueParser return null for any provided string
     * @throws ConfigKeyNotFoundException if the config key does not have a value in this Config object
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public <K, V> ImmutableMap<K, V> getMap(Enum<?> configKey, Function<String, K> keyParser, Function<String, V> valueParser) {
        String val = getString(configKey);
        return ConfigParsers.parseMap(val, keyParser, valueParser);
    }

    /**
     * Returns a typed-Map for config specified as a collection of key-value pairs.
     * <p>
     * Given a config value that is a (semicolon-separated) list of (equals-separated) key-value pairs:
     * <pre>"key1=value1;key2=value2"</pre>
     * Keys and values will be trimmed, before being supplied to the functions that translate them to the
     * correct types.
     * Any pair which does not contain the character '=' will be ignored.
     *
     * If no value is specified for the config key, returns an empty map.
     *
     * @param configKey   Config key which contains the key-value pairs as a String.
     * @param keyParser   Function to convert a String to a key in the resulting Map.
     * @param valueParser Function to convert a String to a value in the resulting Map.
     * @param <K>         The type of key in the resulting {@code Map}
     * @param <V>         The type of value in the resulting {@code Map}
     * @return a Map of key-value pairs parsed from the config value or an empty map if the config key has no value
     * @throws IllegalArgumentException   if duplicate map keys are specified
     * @throws NullPointerException       if the keyParser or valueParser return null for any provided string
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <K, V> ImmutableMap<K, V> getMapOrEmpty(Enum<?> configKey, Function<String, K> keyParser, Function<String, V> valueParser) {
        return getMapOrDefault(configKey, keyParser, valueParser, ImmutableMap.of());
    }

    /**
     * Returns a typed-Map for config specified as a collection of key-value pairs.
     * <p>
     * Given a config value that is a (semicolon-separated) list of (equals-separated) key-value pairs:
     * <pre>"key1=value1;key2=value2"</pre>
     * Keys and values will be trimmed, before being supplied to the functions that translate them to the
     * correct types.
     * Any pair which does not contain the character '=' will be ignored.
     *
     * If no value is specified for the config key, returns the specified default value.
     *
     * @param configKey    Config key which contains the key-value pairs as a String.
     * @param keyParser    Function to convert a String to a key in the resulting Map.
     * @param valueParser  Function to convert a String to a value in the resulting Map.
     * @param defaultValue The value to return if the config key has no specified value.
     * @param <K>          The type of key in the resulting {@code Map}.
     * @param <V>          The type of value in the resulting {@code Map}.
     * @return a Map of key-value pairs parsed from the config value or {@code defaultValue} if the config key has no
     *          value.
     * @throws IllegalArgumentException   if duplicate map keys are specified.
     * @throws NullPointerException       if the keyParser or valueParser return null for any provided string.
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <K, V> ImmutableMap<K, V> getMapOrDefault(Enum<?> configKey, Function<String, K> keyParser, Function<String, V> valueParser, ImmutableMap<K, V> defaultValue) {
        return getOrDefault(configKey, v -> ConfigParsers.parseMap(v, keyParser, valueParser), defaultValue);
    }

    /**
     * Returns a Multimap for config specified as a collection of key-value pairs, with repeating keys.
     * <p>Given a config value that is a (semicolon-separated) list of (equals-separated) key-value paris:
     * <pre>"key1=value1;key1=value2;key2=value3"</pre>
     * Keys and values will be trimmed.
     * Any pair which does not contain the character '=' will be ignored.
     * Each additional value to a key has to come as a new key=value, and cannot be provided as a list of elements (i.e.
     * key=value1,value2) as this would not make it possible to have values as a List type.
     *
     * @param configKey   Config key which contains the key-value pairs as a String.
     * @return a Multimap of key-value pairs parsed from the config value
     * @throws ConfigKeyNotFoundException if the config key does not have a value in this Config object
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public ImmutableSetMultimap<String, String> getStringSetMultimap(Enum<?> configKey) {
        return getSetMultimap(configKey, Function.identity(), Function.identity());
    }

    /**
     * Returns a Multimap for config specified as a collection of key-value pairs, with repeating keys.
     * <p>Given a config value that is a (semicolon-separated) list of (equals-separated) key-value paris:
     * <pre>"key1=value1;key1=value2;key2=value3"</pre>
     * Keys and values will be trimmed.
     * Any pair which does not contain the character '=' will be ignored.
     * Each additional value to a key has to come as a new key=value, and cannot be provided as a list of elements (i.e.
     * key=value1,value2) as this would not make it possible to have values as a List type.
     *
     * If no value is specified for the config key, returns an empty map.
     *
     * @param configKey   Config key which contains the key-value pairs as a String.
     * @return a Multimap of key-value pairs parsed from the config value or an empty map if there is no value
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public ImmutableSetMultimap<String, String> getStringSetMultimapOrEmpty(Enum<?> configKey) {
        return getStringSetMultimapOrDefault(configKey, ImmutableSetMultimap.of());
    }

    /**
     * Returns a Multimap for config specified as a collection of key-value pairs, with repeating keys.
     * <p>Given a config value that is a (semicolon-separated) list of (equals-separated) key-value paris:
     * <pre>"key1=value1;key1=value2;key2=value3"</pre>
     * Keys and values will be trimmed.
     * Any pair which does not contain the character '=' will be ignored.
     * Each additional value to a key has to come as a new key=value, and cannot be provided as a list of elements (i.e.
     * key=value1,value2) as this would not make it possible to have values as a List type.
     *
     * If no value is specified for the config key, returns the specified default value.
     *
     * @param configKey    Config key which contains the key-value pairs as a String.
     * @param defaultValue The value to return if the config key has no specified value.
     * @return a Multimap of key-value pairs parsed from the config value or {@code defaultValue} if there is no value
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public ImmutableSetMultimap<String, String> getStringSetMultimapOrDefault(
            Enum<?> configKey,
            ImmutableSetMultimap<String, String> defaultValue) {
        return getSetMultimapOrDefault(configKey, Function.identity(), Function.identity(), defaultValue);
    }

    /**
     * Returns a typed-Multimap for config specified as a collection of key-value pairs, with repeating keys.
     * <p>Given a config value that is a (semicolon-separated) list of (equals-separated) key-value paris:
     * <pre>"key1=value1;key1=value2;key2=value3"</pre>
     * Keys and values will be trimmed, before being supplied to the functions that translate them to the
     * correct types.
     * Any pair which does not contain the character '=' will be ignored.
     * Each additional value to a key has to come as a new key=value, and cannot be provided as a list of elements (i.e.
     * key=value1,value2) as this would not make it possible to have values as a List type.
     *
     * If no value is specified for the config key, returns an empty multimap.
     *
     * @param configKey   Config key which contains the key-value pairs as a String.
     * @param keyParser   Function to convert a String to a key in the resulting Map.
     * @param valueParser Function to convert a String to a value in the resulting Map.
     * @param <K>         The type of key in the resulting {@code Map}
     * @param <V>         The type of value in the resulting {@code Map}
     * @return a Multimap of key-value pairs parsed from the config value or an empty map if there is no specified value
     * @throws NullPointerException       if the keyParser or valueParser return null for any provided string
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <K, V> ImmutableSetMultimap<K, V> getSetMultimapOrEmpty(
            Enum<?> configKey,
            Function<String, K> keyParser,
            Function<String, V> valueParser) {
        return getSetMultimapOrDefault(configKey, keyParser, valueParser, ImmutableSetMultimap.of());
    }

    /**
     * Returns a typed-Multimap for config specified as a collection of key-value pairs, with repeating keys.
     * <p>Given a config value that is a (semicolon-separated) list of (equals-separated) key-value paris:
     * <pre>"key1=value1;key1=value2;key2=value3"</pre>
     * Keys and values will be trimmed, before being supplied to the functions that translate them to the
     * correct types.
     * Any pair which does not contain the character '=' will be ignored.
     * Each additional value to a key has to come as a new key=value, and cannot be provided as a list of elements (i.e.
     * key=value1,value2) as this would not make it possible to have values as a List type.
     *
     * If no value is specified for the config key, returns the specified default value.
     *
     * @param configKey    Config key which contains the key-value pairs as a String.
     * @param keyParser    Function to convert a String to a key in the resulting Map.
     * @param valueParser  Function to convert a String to a value in the resulting Map.
     * @param defaultValue The value to return if the config key has no specified value.
     * @param <K>          The type of key in the resulting {@code Map}
     * @param <V>          The type of value in the resulting {@code Map}
     * @return a Multimap of key-value pairs parsed from the config value or the default value
     * @throws NullPointerException       if the keyParser or valueParser return null for any provided string
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <K, V> ImmutableSetMultimap<K, V> getSetMultimapOrDefault(
            Enum<?> configKey,
            Function<String, K> keyParser,
            Function<String, V> valueParser,
            ImmutableSetMultimap<K, V> defaultValue) {
        return getOrDefault(
                configKey,
                v -> ConfigParsers.parseSetMultimap(v, keyParser, valueParser),
                defaultValue);
    }

    /**
     * Returns a typed-Multimap for config specified as a collection of key-value pairs, with repeating keys.
     * <p>Given a config value that is a (semicolon-separated) list of (equals-separated) key-value paris:
     * <pre>"key1=value1;key1=value2;key2=value3"</pre>
     * Keys and values will be trimmed, before being supplied to the functions that translate them to the
     * correct types.
     * Any pair which does not contain the character '=' will be ignored.
     * Each additional value to a key has to come as a new key=value, and cannot be provided as a list of elements (i.e.
     * key=value1,value2) as this would not make it possible to have values as a List type.
     *
     * @param configKey   Config key which contains the key-value pairs as a String.
     * @param keyParser   Function to convert a String to a key in the resulting Map.
     * @param valueParser Function to convert a String to a value in the resulting Map.
     * @param <K>         The type of key in the resulting {@code Map}
     * @param <V>         The type of value in the resulting {@code Map}
     * @return a Multimap of key-value pairs parsed from the config value
     * @throws NullPointerException       if the keyParser or valueParser return null for any provided string
     * @throws ConfigKeyNotFoundException if the config key does not have a value in this Config object
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public <K, V> ImmutableSetMultimap<K, V> getSetMultimap(
            Enum<?> configKey,
            Function<String, K> keyParser,
            Function<String, V> valueParser) {
        String val = getString(configKey);
        return ConfigParsers.parseSetMultimap(val, keyParser, valueParser);
    }

    /**
     * Returns the enum value to which the specified key is mapped.
     *
     * @param key       Key for which to lookup the enum value.
     * @param enumClass Enum class defining the set of permitted values.
     * @param <T>       Type of {@code enumClass}
     * @return the value from the specified enum class which corresponds to the config value associated with the given
     * key.
     * @throws IllegalArgumentException   if the config value does not match any of the values in the specified enum.
     * @throws ConfigKeyNotFoundException if the key does not have a value in this Config object.
     * @deprecated use {@link Config#getValue(Enum)} instead.
     */
    @Deprecated
    public <T extends Enum<T>> T getEnum(Enum<?> key, Class<T> enumClass) {
        return Enum.valueOf(enumClass, getString(key));
    }

    /**
     * Like {@link #getEnum(Enum, Class)} but if the key is not present return the default value instead of an exception
     *
     * @param defaultValue Value to return if the key is not present
     * @return the value from the specified enum class which corresponds to the config value associated with the given
     * key, or default if the key is not present
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <T extends Enum<T>> T getEnumOrDefault(Enum<?> key, Class<T> enumClass, T defaultValue) {
        return getOrDefault(key, v -> ConfigParsers.parseEnum(v, enumClass), defaultValue);
    }

    /**
     * Returns the enum value to which the specified key is mapped, if present.
     *
     * @param key       Key for which to lookup the enum value.
     * @param enumClass Enum class defining the set of permitted values.
     * @param <T>       Type of {@code enumClass}
     * @return the value from the specified enum class which corresponds to the config value associated with the given
     * key, or {@link Optional#empty()} if no entry for that key exists.
     * @throws IllegalArgumentException when the config value does not match any of the values in the specified enum.
     * @deprecated use {@link Config#getIfValueDefined(Enum)} or {@link Config#getIfKeyAndValueDefined(Enum)} instead.
     */
    @Deprecated
    public <T extends Enum<T>> Optional<T> getEnumIfPresent(Enum<?> key, Class<T> enumClass) {
        if (!containsKey(key)) {
            return Optional.empty();
        }
        return Optional.of(getEnum(key, enumClass));
    }

    public String getQualifiedKeyName(E key) {
        return qualifier + "." + key.toString();
    }

    /**
     * @deprecated - exposes secret keys. Use {@link #getKeyValueStringMapWithoutSecrets()} instead
     */
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

    /**
     * @deprecated - exposes secret keys. Use {@link #getUnqualifiedKeyValueStringMapWithoutSecrets(Class)} instead
     */
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
    @SuppressWarnings("unchecked")
    //this method needs to be unchecked because subConfig is unchecked.
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
        }, true);
        return joiner.join(entries);
    }

    @FunctionalInterface
    private interface ToStringHelper {
        void accept(String key, String value, Boolean isSecret);
    }

    private Optional<String> getOrNone(Enum<?> key) {
        if (key.getClass().equals(cls) && values.containsKey(cls.cast(key))) {
            return Optional.ofNullable(values.get(cls.cast(key)).currentValue);
        }
        Class<?> declaringClass = key.getDeclaringClass();
        while (declaringClass != null) {
            if (subConfig.containsKey(declaringClass)) {
                return subConfig.get(declaringClass).getOrNone(key);
            }
            declaringClass = declaringClass.getDeclaringClass();
        }
        return Optional.empty();
    }

    /**
     * Perform a function on all config values in the config tree
     *
     * @param mutator function to apply
     * @return a new config with the function applied
     */
    private Config<E> map(Function<ConfigValue, ConfigValue> mutator) {
        ImmutableMap<E, ConfigValue> values = this.values.entrySet().stream()
                .collect(Maps.toImmutableEnumMap(Entry::getKey, e -> mutator.apply(e.getValue())));
        ImmutableMap<?, Config<?>> subConfig = this.subConfig.entrySet().stream()
                .collect(ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue().map(mutator)));
        return new Config<>(this.cls, values, subConfig, this.qualifier, this.timeUnit, this.lengthUnit);
    }
}
