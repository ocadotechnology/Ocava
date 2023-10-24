/*
 * Copyright © 2017-2023 Ocado (Ocava)
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
package com.ocadotechnology.utils;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.ocadotechnology.validation.Failer;

public class ImmutableMapFactory {
    private ImmutableMapFactory() {
        throw Failer.fail("This class should not be instantiated.");
    }
    /**
     * This function returns a new {@link ImmutableMap} based on a baseMap. This function takes a keyCreator and valueCreator which
     * are both {@link BiFunction}.
     * The keys of the new map are created by calling the keyCreator on each key value pair of the base map.
     * The values of the new map are created by calling the valueCreator on each key value pair of the base map.
     *
     * @param baseMap      the base ImmutableMap used to generate the new ImmutableMap.
     * @param keyCreator   the function to generate the keys of the new ImmutableMap.
     * @param valueCreator the function to generate the values of the new ImmutableMap.
     * @param <K>          The type of the keys of the base map.
     * @param <L>          the type of the keys of the new map.
     * @param <V>          The type of the values of the base map.
     * @param <W>          the type of the values of the new map.
     * @return The modified ImmutableMap.
     */
    public static <K, L, V, W> ImmutableMap<L, W> create(
            Map<K, V> baseMap,
            BiFunction<? super K, ? super V, ? extends L> keyCreator,
            BiFunction<? super K, ? super V, ? extends W> valueCreator) {
        return baseMap.entrySet().stream()
                .collect(ImmutableMap.toImmutableMap(
                        e -> keyCreator.apply(e.getKey(), e.getValue()),
                        e -> valueCreator.apply(e.getKey(), e.getValue())));
    }

    /**
     * This function returns a new {@link ImmutableMap} based on a baseMap. This function takes a keyMapper and valueMapper which
     * are both {@link Function}.
     * The keys of the new map are created by calling the keyMapper on each key of the base map.
     * The values of the new map are created by calling the valueMapper on each value of the base map.
     *
     * @param baseMap     the base ImmutableMap used to generate the new ImmutableMap.
     * @param keyMapper   the function to map the keys of the new ImmutableMap from the base maps keys.
     * @param valueMapper the function to map the values of the new ImmutableMap from the base maps values.
     * @param <K>         The type of the keys of the base map.
     * @param <L>         the type of the keys of the new map.
     * @param <V>         The type of the values of the base map.
     * @param <W>         the type of the values of the new map.
     * @return The modified ImmutableMap.
     */
    public static <K, L, V, W> ImmutableMap<L, W> create(
            Map<K, V> baseMap,
            Function<? super K, ? extends L> keyMapper,
            Function<? super V, ? extends W> valueMapper) {
        return create(baseMap, (key, value) -> keyMapper.apply(key), (key, value) -> valueMapper.apply(value));
    }

    /**
     * This function returns a new {@link ImmutableMap} based on a base map with reduced collisions.
     * This function takes a keyMapper and valueMapper which are both {@link Function}.
     * The new keys are created by applying the keyMapper to the keys of the base map.
     * The values are created by applying the valueMapper to the values of the base map.
     * If key collisions occur after the keyMapper is applied the values that match that key are reduced into a single value.
     * The collidingKeysAccumulator function is then applied to reduce the values to a single value with the valueIdentity
     * as the starting point.
     *
     * @param baseMap                  the base ImmutableMap used to generate the new ImmutableMap.
     * @param keyMapper                the function to map the keys of the new ImmutableMap from the base maps keys.
     * @param valueMapper              the function to map the values of the new ImmutableMap from the base maps values.
     * @param valueIdentity            the base value used when collisions occur.
     * @param collidingKeysAccumulator the accumulator function to turn the values that match the key collisions into a single
     *                                 value.
     * @param <K>                      The type of the keys of the base map.
     * @param <L>                      the type of the keys of the new map.
     * @param <V>                      The type of the values of the base map.
     * @param <W>                      the type of the values of the new map.
     * @return the new ImmutableMap.
     */
    public static <K, L, V, W> ImmutableMap<L, W> createAndReduceCollisions(
            Map<K, V> baseMap,
            Function<? super K, ? extends L> keyMapper,
            Function<? super V, ? extends W> valueMapper,
            W valueIdentity,
            BinaryOperator<W> collidingKeysAccumulator) {
        Function<Entry<K, V>, Entry<L, W>> entryMapper = e -> new AbstractMap.SimpleEntry<>(
                keyMapper.apply(e.getKey()),
                valueMapper.apply(e.getValue()));

        Collector<Entry<L, W>, ?, W> entriesCollector =
                Collectors.reducing(valueIdentity, Entry::getValue, collidingKeysAccumulator);

        Map<L, W> mutableMap = baseMap.entrySet().stream()
                .map(entryMapper)
                .collect(Collectors.groupingBy(Entry::getKey, entriesCollector));

        return ImmutableMap.copyOf(mutableMap);
    }

    /**
     * This function creates a new {@link ImmutableMap} from a base map. The values from the base map are used as the values for the new map.
     * The keys of the new map are created using a keyMapper {@link Function} applied to the keys of the base map.
     *
     * @param baseMap   the keys used to create the new map.
     * @param keyMapper the valueMapper function which keys from the base map keys.
     * @param <K>       the type of the keys of the base map.
     * @param <L>       the type the keys of the new map.
     * @param <V>       the type of the values of the new map.
     * @return the newly created map.
     */
    public static <K, L, V> ImmutableMap<L, V> createWithNewKeys(
            Map<K, V> baseMap,
            Function<? super K, ? extends L> keyMapper) {
        return create(baseMap, keyMapper, Function.identity());
    }

    /**
     * This function creates a new {@link ImmutableMap} from a base map. The keys from the base map are used as the keys for the new map.
     * The value of the new map are created using a valueMapper {@link Function} applied to the values of the base map.
     *
     * @param baseMap     the keys used to create the new map.
     * @param valueMapper the valueMapper function which are used to create new values from the base map values.
     * @param <K>         the type of the keys of the new map.
     * @param <V>         the type of the values of the base map.
     * @param <W>         the type of the values of the new map.
     * @return the newly created map.
     */
    public static <K, V, W> ImmutableMap<K, W> createWithNewValues(
            Map<K, V> baseMap,
            Function<? super V, ? extends W> valueMapper) {
        return create(baseMap, Function.identity(), valueMapper);
    }

    /**
     * This function creates a new {@link ImmutableMap} from a base map. The keys from the base map are used as the keys for the new map.
     * The value of the new map are created using a valueMapper {@link BiFunction} applied to the key value pairs of the base map.
     *
     * @param baseMap     the keys used to create the new map.
     * @param valueMapper the valueMapper function which are used to create new values from the base map key value pairs.
     * @param <K>         the type of the keys of the new map.
     * @param <V>         the type of the values of the base map.
     * @param <W>         the type of the values of the new map.
     * @return the newly created map.
     */
    public static <K, V, W> ImmutableMap<K, W> createWithNewValues(
            Map<K, V> baseMap,
            BiFunction<? super K, ? super V, ? extends W> valueMapper) {
        return create(baseMap, (key, value) -> key, valueMapper);
    }

    /**
     * This function creates a new {@link ImmutableMap} from a collection of keys and a supplier of values. For each key
     * the valueSupplier gets a new value. These are added to the map as a key value pair.
     *
     * @param keys          the keys used to create the new map.
     * @param valueSupplier the value supplier which gets the values of the new map.
     * @param <K>           the type of the keys of the new map.
     * @param <V>           the type of the values of the new map.
     * @return the newly created map.
     */
    public static <K, V> ImmutableMap<K, V> createFromKeys(
            Collection<K> keys,
            Supplier<V> valueSupplier) {
        return createFromKeys(keys.stream(), valueSupplier);
    }
    /**
     *
     * This function creates a new {@link ImmutableMap} from a stream of keys and a supplier of values. For each key
     * the valueSupplier gets a new value. These are added to the map as a key value pair.
     *
     * @param keys          the keys used to create the new map.
     * @param valueSupplier the value supplier which gets the values of the new map.
     * @param <K>           the type of the keys of the new map.
     * @param <V>           the type of the values of the new map.
     * @return the newly created map.
     */
    public static <K, V> ImmutableMap<K, V> createFromKeys(
            Stream<K> keys,
            Supplier<V> valueSupplier) {
        return keys.collect(ImmutableMap.toImmutableMap(
                        Function.identity(),
                        key -> valueSupplier.get()));
    }

    /**
     * This function creates a new {@link ImmutableMap} from a collection of keys and a function to create the values. For each key
     * the valueCreator function is applied to create a new value. These are added to the map as a key value pair.
     *
     * @param keys         the keys used to create the new map.
     * @param valueCreator the valueCreator function used to create the values of the new map.
     * @param <K>          the type of the keys of the new map.
     * @param <V>          the type of the values of the new map.
     * @return the newly created map.
     */
    public static <K, V> ImmutableMap<K, V> createFromKeys(
            Collection<K> keys,
            Function<K, V> valueCreator) {
        return createFromKeys(keys.stream(), valueCreator);
    }

    /**
     * This function creates a new {@link ImmutableMap} from a stream of keys and a function to create the values. For each key
     * the valueCreator function is applied to create a new value. These are added to the map as a key value pair.
     *
     * @param keys         the keys used to create the new map.
     * @param valueCreator the valueCreator function used to create the values of the new map.
     * @param <K>          the type of the keys of the new map.
     * @param <V>          the type of the values of the new map.
     * @return the newly created map.
     */
    public static <K, V> ImmutableMap<K, V> createFromKeys(
            Stream<K> keys,
            Function<K, V> valueCreator) {
        return keys.collect(ImmutableMap.toImmutableMap(
                Function.identity(),
                valueCreator));
    }

    /**
     * An alternative to {@link Collectors#groupingBy} that returns an immutable map whose values are also immutable lists.
     * @param classifier The grouping to apply to each element in the stream
     * @return A collector that does the grouping
     * @param <K> The type of the key in the newly collected map
     * @param <T> The type of the element in the stream being collected
     */
    public static <K, T> Collector<T, ?, ImmutableMap<K, ImmutableList<T>>> groupingBy(Function<? super T, ? extends K> classifier) {
        return Collectors.collectingAndThen(Collectors.groupingBy(classifier, ImmutableList.toImmutableList()), ImmutableMap::copyOf);
    }

    /**
     * An alternative to {@link Collectors#groupingBy} that returns an immutable map
     * @param classifier The grouping to apply to each element in the stream
     * @param downstream The collector to apply to the stream of all the elements that are grouped togeter
     * @return A collector that does the grouping
     * @param <T> The type of the element in the stream being collected
     * @param <K> The type of the key in the newly collected map
     * @param <D> The value in the newly created map
     */
    public static <T, K, A, D> Collector<T, ?, ImmutableMap<K, D>> groupingBy(Function<? super T, ? extends K> classifier, Collector<? super T, A, D> downstream) {
        return Collectors.collectingAndThen(Collectors.groupingBy(classifier, downstream), ImmutableMap::copyOf);
    }

    /**
     * This function creates a new {@link ImmutableMap} from a list of keys and values. The keys and values are paired in order to create
     * the new map.
     * If the  number of keys and values do not match a {@link IllegalArgumentException} is thrown.
     *
     * @param keys   the keys to zip.
     * @param values the values to zip.
     * @param <K>    The type of the keys of the new map.
     * @param <V>    the type of the values of the new map.
     * @return the new map made from the sets of keys and values.
     */
    public static <K, V> ImmutableMap<K, V> zip(
            List<K> keys,
            List<V> values) {
        Preconditions.checkArgument(
                keys.size() == values.size(),
                "Size of keys and values should match");
        return ImmutableMapFactory.createFromKeys(keys, values.iterator()::next);
    }

    /**
     * This function creates a new filtered {@link ImmutableMap} from a baseMap. The filter parameter, which is a {@link BiPredicate},
     * is used to test each key value pair in the base map. If the result of the test is true that key value pair is
     * added to the new ImmutableMap. Otherwise the pair is not.
     *
     * @param baseMap the base ImmutableMap used to generate the new ImmutableMap.
     * @param filter  the predicate used to filter the key value pairs.
     * @param <K>     The type of the keys of the new map.
     * @param <V>     the type of the values of the new map.
     * @return the filtered ImmutableMap.
     */
    public static <K, V> ImmutableMap<K, V> filter(
            Map<K, V> baseMap,
            BiPredicate<K, V> filter) {
        return baseMap.entrySet().stream()
                .filter(e -> filter.test(e.getKey(), e.getValue()))
                .collect(ImmutableMap.toImmutableMap(
                        Entry::getKey,
                        Entry::getValue));
    }

    /**
     * This function creates a new filtered {@link ImmutableMap} from a baseMap. The filter parameter, which is a {@link Predicate},
     * is used to test each key in the base map. If the result of the test is true that key, along with it's value, is
     * added to the new ImmutableMap. Otherwise they are not.
     *
     * @param baseMap   the base ImmutableMap used to generate the new ImmutableMap.
     * @param keyFilter the predicate used to filter the keys of the base map.
     * @param <K>       The type of the keys of the new map.
     * @param <V>       the type of the values of the new map.
     * @return the filtered ImmutableMap.
     */
    public static <K, V> ImmutableMap<K, V> filterByKeys(
            Map<K, V> baseMap,
            Predicate<K> keyFilter) {
        return filter(baseMap, (key, value) -> keyFilter.test(key));
    }

    /**
     * This function creates a new filtered {@link ImmutableMap} from a baseMap. The filter parameter, which is a {@link Predicate},
     * is used to test each value in the base map. If the result of the test is true that value, along with it's key, is
     * added to the new ImmutableMap. Otherwise they are not.
     *
     * @param baseMap     the base ImmutableMap used to generate the new ImmutableMap.
     * @param valueFilter the predicate used to filter the values of the base map.
     * @param <K>         The type of the keys of the new map.
     * @param <V>         the type of the values of the new map.
     *                    * @return the filtered ImmutableMap.
     */
    public static <K, V> ImmutableMap<K, V> filterByValues(
            Map<K, V> baseMap,
            Predicate<V> valueFilter) {
        return filter(baseMap, (key, value) -> valueFilter.test(value));
    }

    /**
     * This function parses a string of the form "value1:key1;value2:key2;..." into a {@link ImmutableMap}.
     * Each key has the KeyParser {@link Function} called on it to create a key from the String.
     * Each value has the valueParser {@link Function} called on it to create a value from the String.
     *
     * @param s           The string to parse.
     * @param keyParser   the KeyParser function to create the keys.
     * @param valueParser the ValueParser function to create the values.
     * @param <K>         The type of the Keys of the new map.
     * @param <V>         The type of the values of the new map.
     * @return the new map parsed from the string.
     */

    public static <K, V> ImmutableMap<K, V> parse(
            String s,
            Function<String, K> keyParser,
            Function<String, V> valueParser) {
        return parseAndFilter(s, keyParser, valueParser, (x, y) -> true);
    }

    /**
     * This function parses a string of the form "value1:key1;value2:key2;..." into a {@link ImmutableMap}.
     * Each key has the KeyParser {@link Function} called on it to create a key from the String.
     * Each value has the valueParser {@link Function} called on it to create a value from the String.
     * Then each key/value pair is tested using the filter {@link BiFunction}. If the function returns true that pair is added to
     * the new map. Otherwise they are not.
     *
     * @param s           The string to parse and filter
     * @param keyParser   the keyParser function to create the keys.
     * @param valueParser the valueParser function to create the values.
     * @param filter      the filter function to filter the newly created key value pairs.
     * @param <K>         The type of the Keys of the new map.
     * @param <V>         The type of the values of the new map.
     * @return the new map parsed from the string with filtered keys and values.
     */
    public static <K, V> ImmutableMap<K, V> parseAndFilter(
            String s,
            Function<String, K> keyParser,
            Function<String, V> valueParser,
            BiPredicate<K, V> filter) {
        Builder<K, V> mapBuilder = ImmutableMap.builder();

        for (String objectAndWeight : s.split(";")) {
            String[] keyAndWeight = objectAndWeight.split(":");
            K key = keyParser.apply(keyAndWeight[0]);
            V value = valueParser.apply(keyAndWeight[1]);

            if (filter.test(key, value)) {
                mapBuilder.put(key, value);
            }
        }

        return mapBuilder.build();
    }
}
