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
package com.ocadotechnology.utils;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;

/**
 * HashMap with iteration order defined by order of values.  Sorts by values then keys.
 * Non-equal keys must not compare as equal, as this will cause inconsistent ordering
 * errors. This is only enforced lazily as active enforcement would incur a performance
 * penalty.
 * <br>
 * Note: Every view returned by this class is an immutable copy which will not reflect
 * subsequent changes to the parent map. This has been done in order to make the
 * development tractable.
 */
public class SortByValueMap<K, V> implements SortedMap<K, V> {
    /**
     * It is necessary to store the sequence and the mapping separately, since the sequence
     * is dependent on the mapping. We could order entries instead of keys, but we still need
     * to be able to query the value from the key in order to satisfy the map API.
     */
    private final Map<K, V> map = new HashMap<>();
    private final Comparator<K> comparator;
    private final SortedSet<K> orderedKeys;

    /**
     * Creates a new instance of SortByValueMap with natural order comparators for both keys and
     * values. Requires both classes to be Comparable.
     */
    public static <K extends Comparable<K>, V extends Comparable<V>> SortByValueMap<K, V> createWithNaturalOrderComparators() {
        return new SortByValueMap<>(Comparator.naturalOrder(), Comparator.naturalOrder());
    }

    /**
     * Creates a new instance of SortByValueMap with a custom comparator for keys and natural order
     * comparator for values. Requires values to be Comparable.
     */
    public static <K, V extends Comparable<V>> SortByValueMap<K, V> createWithNaturalOrderValueComparator(Comparator<K> keyComparator) {
        return new SortByValueMap<>(keyComparator, Comparator.naturalOrder());
    }

    /**
     * Creates a new instance of SortByValueMap with a natural order comparator for keys and a custom
     * comparator for values. Requires keys to be Comparable.
     */
    public static <K extends Comparable<K>, V> SortByValueMap<K, V> createWithNaturalOrderKeyComparator(Comparator<V> valueComparator) {
        return new SortByValueMap<>(Comparator.naturalOrder(), valueComparator);
    }

    /**
     * Creates a new instance of SortByValueMap with custom comparators for both keys and values.
     */
    public static <K, V> SortByValueMap<K, V> createWithCustomComparators(Comparator<K> keyComparator, Comparator<V> valueComparator) {
        return new SortByValueMap<>(keyComparator, valueComparator);
    }

    private SortByValueMap(Comparator<? super K> keyComparator, Comparator<? super V> valueComparator) {
        this.comparator = buildComparator(keyComparator, valueComparator);
        this.orderedKeys = new TreeSet<>(comparator);
    }

    private Comparator<K> buildComparator(Comparator<? super K> keyComparator, Comparator<? super V> valueComparator) {
        return (k1, k2) -> {
            int result = valueComparator.compare(map.get(k1), map.get(k2));
            if (result == 0) {
                result = keyComparator.compare(k1, k2);
            }
            if (result == 0) {
                Preconditions.checkState(k1.equals(k2), "Keys %s and %s are non-equal but return zero in their comparator", k1, k2);
            }
            return result;
        };
    }

    @Override
    public int size() {
        return orderedKeys.size();
    }

    @Override
    public boolean isEmpty() {
        return orderedKeys.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        clearSortOrderIfKnown(key);
        V previous = map.put(key, value);
        orderedKeys.add(key);
        return previous;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (K key : m.keySet()) {
            clearSortOrderIfKnown(key);
        }
        map.putAll(m);
        orderedKeys.addAll(m.keySet());
    }

    @Override
    public V remove(Object key) {
        clearSortOrderIfKnown(key);
        return map.remove(key);
    }

    private void clearSortOrderIfKnown(Object key) {
        if (map.containsKey(key)) {
            orderedKeys.remove(key);
        }
    }

    @Override
    public void clear() {
        orderedKeys.clear();
        map.clear();
    }

    /**
     * Returns an immutable copy of the key set, with iteration order defined by the
     * iteration order of the map. The set is a snapshot of the keys at the time of
     * the call, and will not reflect subsequent changes to the map.
     */
    @Override
    @Nonnull
    public ImmutableSet<K> keySet() {
        return ImmutableSet.copyOf(orderedKeys);
    }

    /**
     * Returns an immutable copy of the value collection, with iteration order defined by the
     * iteration order of the map. The collection is a snapshot of the values at the time of
     * the call, and will not reflect subsequent changes to the map.
     */
    @Override
    @Nonnull
    public ImmutableList<V> values() {
        return orderedKeys.stream().map(map::get).collect(ImmutableList.toImmutableList());
    }

    /**
     * Returns an immutable copy of the entry set, with iteration order defined by the
     * iteration order of the map. The set is a snapshot of the entries at the time of
     * the call, and will not reflect subsequent changes to the map.
     */
    @Override
    @Nonnull
    public ImmutableSet<Entry<K, V>> entrySet() {
        return orderedKeys.stream().map(k -> new SimpleImmutableEntry<>(k, map.get(k))).collect(ImmutableSet.toImmutableSet());
    }

    @Override
    public K firstKey() {
        return orderedKeys.first();
    }

    @Override
    public K lastKey() {
        return orderedKeys.last();
    }

    /**
     * Returns an immutable copy of the portion of this map whose keys range from fromKey to toKey.
     * Iteration order of the returned copy will match the parent map. The map is a snapshot of the
     * entries at the time of the call, and will not reflect subsequent changes to the map.
     */
    public ImmutableSortedMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        ImmutableSortedMap.Builder<K, V> builder = ImmutableSortedMap.orderedBy(comparator);
        // orderedKeys.subSet is fromInclusive/toExclusive by default, so I need to potentially remove the first key and
        // add the last key depending on the values provided by the caller.
        for (K k : orderedKeys.subSet(fromKey, toKey)) {
            if (!fromInclusive && k.equals(fromKey)) {
                continue;
            }
            builder.put(k, map.get(k));
        }
        if (toInclusive && map.containsKey(toKey)) {
            builder.put(toKey, map.get(toKey));
        }
        return builder.build();
    }
    /**
     * Returns an immutable copy of the portion of this map whose keys range from fromKey inclusive
     * to toKey exclusive. Iteration order of the returned copy will match the parent map. The map
     * is a snapshot of the entries at the time of the call, and will not reflect subsequent changes
     * to the map.
     */
    @Override
    @Nonnull
    public ImmutableSortedMap<K, V> subMap(K fromKey, K toKey) {
        return subMap(fromKey, true, toKey, false);
    }

    /**
     * Returns an immutable copy of the portion of this map whose keys are less than (or equal to if
     * inclusive is set to true) toKey. Iteration order of the returned copy will match the parent map.
     * The map is a snapshot of the entries at the time of the call, and will not reflect subsequent
     * changes to the map.
     */
    public ImmutableSortedMap<K, V> headMap(K toKey, boolean inclusive) {
        ImmutableSortedMap.Builder<K, V> builder = ImmutableSortedMap.orderedBy(comparator);
        orderedKeys.headSet(toKey).forEach(k -> builder.put(k, map.get(k)));
        if (inclusive && map.containsKey(toKey)) {
            builder.put(toKey, map.get(toKey));
        }
        return builder.build();
    }

    /**
     * Returns an immutable copy of the portion of this map whose keys are strictly less than toKey.
     * Iteration order of the returned copy will match the parent map. The map is a snapshot of the
     * entries at the time of the call, and will not reflect subsequent changes to the map.
     */
    @Override
    @Nonnull
    public ImmutableSortedMap<K, V> headMap(K toKey) {
        return headMap(toKey, false);
    }

    /**
     * Returns an immutable copy of the portion of this map whose keys are greater than (or equal to if
     * inclusive is set to true) fromKey. Iteration order of the returned copy will match the parent map.
     * The map is a snapshot of the entries at the time of the call, and will not reflect subsequent
     * changes to the map.
     */
    public ImmutableSortedMap<K, V> tailMap(K fromKey, boolean inclusive) {
        ImmutableSortedMap.Builder<K, V> builder = ImmutableSortedMap.orderedBy(comparator);
        for (K k : orderedKeys.tailSet(fromKey)) {
            if (inclusive || !k.equals(fromKey)) {
                builder.put(k, map.get(k));
            }
        }
        return builder.build();
    }

    /**
     * Returns an immutable copy of the portion of this map whose keys are greater than or equal to fromKey.
     * Iteration order of the returned copy will match the parent map. The map is a snapshot of the entries
     * at the time of the call, and will not reflect subsequent changes to the map.
     */
    @Override
    @Nonnull
    public ImmutableSortedMap<K, V> tailMap(K fromKey) {
        return tailMap(fromKey, true);
    }

    @Override
    public Comparator<? super K> comparator() {
        return orderedKeys.comparator();
    }
}
