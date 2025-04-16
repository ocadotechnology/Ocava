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

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
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
 * {@link #getPrefixedConfigItems(String)} and then call {@link #getValue(Enum)}
 *
 * <pre>
 *     var siteConfig = config.getPrefixedConfigItems("site10");
 *     var key = siteConfig.getValue(MyConfig.ROOT_KEY).asString();
 * </pre>
 * <p>
 * Note that in the properties file site10 is not one of the listed prefixes, in this case it falls back to the default
 * value of "root_key". Prefixes can be nested, so it would be possible to do:
 *
 * <pre>
 *     var nodeConfig = config.getPrefixedConfigItems("site1").getPrefixedConfigItems("node1");
 *     var key = nodeConfig.getValue(MyConfig.ROOT_KEY).asString();
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

    public Stream<E> streamConfigKeys() {
        return this.values.keySet().stream();
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
     * Check if the value of key is not the empty string.
     *
     * @throws ConfigKeyNotFoundException if the key has not been explicitly defined
     */
    public boolean isValueDefined(Enum<?> key) {
        return getIfKeyDefined(key)
                .map(s -> !s.isEmpty())
                .orElseThrow(() -> new ConfigKeyNotFoundException(key));
    }

    /**
     * Check that the key has been explicitly defined and not to the empty string.
     * The two checks are done simultaneously because key presence on its own should not
     * be used for flow control (see class javadoc for reasoning).
     */
    public boolean areKeyAndValueDefined(Enum<?> key) {
        return getIfKeyDefined(key)
                .map(s -> !s.isEmpty())
                .orElse(false);
    }

    /**
     * Check that this config's enum type matches the one provided.
     * For example, a Config&lt;ExampleConfig&gt; is not the same thing as a Config&lt;CounterExample&gt;.
     */
    public boolean enumTypeMatches(Class<? extends Enum> enumClazz) {
        Class<?> clazz = enumClazz;
        while (clazz != null) {
            if (clazz.equals(cls)) {
                return true;
            }
            clazz = clazz.getEnclosingClass();
        }

        return false;
    }

    /**
     * Check that this config enum type contains the enum key independently from whether the key's value is set.
     * For example, ExampleConfig.VALUE does have the same enum type of Config&lt;ExampleConfig&gt;,
     * CounterExample.VALUE does not.
     */
    public boolean enumTypeIncludes(Enum<?> key) {
        return enumTypeMatches(key.getClass());
    }

    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> Config<T> getSubConfig(Class<T> key) {
        return (Config<T>) subConfig.get(key);
    }

    ImmutableCollection<Config<?>> getSubConfigValues() {
        return subConfig.values();
    }

    /**
     * Create a {@link StrictValueParser} object to parse the value associated with the given key.
     *
     * @param key the key to look up in this config object.
     * @return a {@link StrictValueParser} object built from the value associated with the specified key.
     * @throws ConfigKeyNotFoundException if the key is not defined in this Config object.
     */
    public StrictValueParser getValue(Enum<?> key) {
        String value = getIfKeyDefined(key)
                .orElseThrow(() -> new ConfigKeyNotFoundException(key));
        return new StrictValueParser(key, value, timeUnit, lengthUnit);
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
        String value = getIfKeyDefined(key)
                .orElseThrow(() -> new ConfigKeyNotFoundException(key));
        return new OptionalValueParser(key, value, timeUnit, lengthUnit);
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
        String value = getIfKeyDefined(key).orElse("");

        return new OptionalValueParser(key, value, timeUnit, lengthUnit);
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

    /**
     * Return the full config object as a map.
     * This includes prefixes and secrets
     * This is a package only function
     *
     * @return {@link ImmutableMap} of config pairs
     */
    ImmutableMap<String, String> getFullMap() {
        ImmutableMap.Builder<String, String> map = ImmutableMap.builder();
        consumeConfigValues((k, v, isSecret) -> map.put(k, v), true);
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

    private Optional<String> getIfKeyDefined(Enum<?> key) {
        if (key.getClass().equals(cls) && values.containsKey(cls.cast(key))) {
            return Optional.ofNullable(values.get(cls.cast(key)).currentValue)
                    .map(String::trim);
        }
        Class<?> declaringClass = key.getDeclaringClass();
        while (declaringClass != null) {
            if (subConfig.containsKey(declaringClass)) {
                return subConfig.get(declaringClass).getIfKeyDefined(key);
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
