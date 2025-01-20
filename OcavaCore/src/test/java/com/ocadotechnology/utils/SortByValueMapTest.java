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
package com.ocadotechnology.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.wrappers.Pair;

class SortByValueMapTest {
    private static final String KEY_1 = "Alpha";
    private static final String KEY_2 = "Bravo";
    private static final String KEY_3 = "Charlie";
    private final SortByValueMap<String, Integer> testMap = SortByValueMap.createWithNaturalOrderComparators();

    @Test
    void put_whenInconsistentEquals_thenThrowsException() {
        SortByValueMap<String, Integer> clashingTestMap = SortByValueMap.createWithNaturalOrderValueComparator((s1, s2) -> 0);
        clashingTestMap.put(KEY_1, 1);
        assertThatThrownBy(() -> clashingTestMap.put(KEY_2, 1)).isInstanceOf(IllegalStateException.class);
    }

    /*
     * We could consider how to proactively detect non-equal keys with an indeterminate sort order, but for now, these
     * are only detected when the values are the same. This is because any proactive detection would incur a performance
     * penalty which is not obviously justified.
     */
    @Test
    void put_whenInconsistentEqualsButDifferentValues_thenDoesNotThrow() {
        SortByValueMap<String, Integer> clashingTestMap = SortByValueMap.createWithNaturalOrderValueComparator((s1, s2) -> 0);
        clashingTestMap.put(KEY_1, 1);
        clashingTestMap.put(KEY_2, 2);

        assertThat(clashingTestMap.get(KEY_1)).isEqualTo(1);
        assertThat(clashingTestMap.get(KEY_2)).isEqualTo(2);
    }

    @Test
    void get_whenMappingIsPresent_returnsValue() {
        testMap.put(KEY_1, 1);

        assertThat(testMap.get(KEY_1)).isEqualTo(1);
    }

    @Test
    void get_whenMappingIsAbsent_returnsNull() {
        testMap.put(KEY_1, 1);

        assertThat(testMap.get(KEY_2)).isNull();
    }

    @Test
    void get_whenMultipleMappingsPresent_returnsValues() {
        testMap.put(KEY_1, 1);
        testMap.put(KEY_2, 2);

        assertThat(testMap.get(KEY_1)).isEqualTo(1);
        assertThat(testMap.get(KEY_2)).isEqualTo(2);
    }

    @Test
    void get_whenDuplicateValuesPresent_returnsValues() {
        testMap.put(KEY_1, 1);
        testMap.put(KEY_2, 1);

        assertThat(testMap.get(KEY_1)).isEqualTo(1);
        assertThat(testMap.get(KEY_2)).isEqualTo(1);
    }

    @Test
    void remove_whenKeyExists_returnsValue() {
        testMap.put(KEY_1, 1);
        testMap.put(KEY_2, 2);

        assertThat(testMap.remove(KEY_1)).isEqualTo(1);
        assertThat(testMap.get(KEY_1)).isNull();
    }

    @Test
    void remove_whenKeyDoesNotExist_returnsNull() {
        testMap.put(KEY_1, 1);

        assertThat(testMap.remove(KEY_2)).isNull();
    }

    @Test
    void putAll_whenKeysNotPresent_valuesAdded() {
        Map<String, Integer> newMap = ImmutableMap.of(KEY_1, 1, KEY_2, 2);
        testMap.putAll(newMap);

        assertThat(testMap.get(KEY_1)).isEqualTo(1);
        assertThat(testMap.get(KEY_2)).isEqualTo(2);
    }

    @Test
    void putAll_whenKeysPresent_valuesReplaced() {
        testMap.put(KEY_1, 1);
        testMap.put(KEY_2, 2);

        Map<String, Integer> newMap = ImmutableMap.of(KEY_1, 2, KEY_2, 1);
        testMap.putAll(newMap);

        assertThat(testMap.get(KEY_1)).isEqualTo(2);
        assertThat(testMap.get(KEY_2)).isEqualTo(1);
    }

    @Test
    void forEach_whenValuesDistinct_respectsSortOrder() {
        testMap.put(KEY_1, 1);
        testMap.put(KEY_2, 2);

        List<Pair<String, Integer>> invocations = new ArrayList<>();
        testMap.forEach((k, v) -> invocations.add(Pair.of(k, v)));
        assertThat(invocations).containsExactly(Pair.of(KEY_1, 1), Pair.of(KEY_2, 2));

        testMap.put(KEY_1, 2);
        testMap.put(KEY_2, 1);

        invocations.clear();
        testMap.forEach((k, v) -> invocations.add(Pair.of(k, v)));
        assertThat(invocations).containsExactly(Pair.of(KEY_2, 1), Pair.of(KEY_1, 2));
    }

    @Test
    void forEach_whenValuesEqual_respectsKeySortOrder() {
        testMap.put(KEY_2, 1);
        testMap.put(KEY_1, 1);

        List<Pair<String, Integer>> invocations = new ArrayList<>();
        testMap.forEach((k, v) -> invocations.add(Pair.of(k, v)));
        assertThat(invocations).containsExactly(Pair.of(KEY_1, 1), Pair.of(KEY_2, 1));
    }

    @Test
    void firstKey_whenEmpty_throwsException() {
        assertThatThrownBy(testMap::firstKey).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void firstKey_whenNotEmpty_returnsFirstKeyByValueOrdering() {
        testMap.put(KEY_1, 2);
        testMap.put(KEY_2, 1);

        assertThat(testMap.firstKey()).isEqualTo(KEY_2);
    }

    @Test
    void firstKey_whenNotEmpty_returnsFirstKeyByKeyOrdering() {
        testMap.put(KEY_2, 1);
        testMap.put(KEY_1, 1);

        assertThat(testMap.firstKey()).isEqualTo(KEY_1);
    }

    @Test
    void lastKey_whenEmpty_throwsException() {
        assertThatThrownBy(testMap::lastKey).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void lastKey_whenNotEmpty_returnsLastKeyByValueOrdering() {
        testMap.put(KEY_1, 2);
        testMap.put(KEY_2, 1);

        assertThat(testMap.lastKey()).isEqualTo(KEY_1);
    }

    @Test
    void lastKey_whenNotEmpty_returnsLastKeyByKeyOrdering() {
        testMap.put(KEY_2, 1);
        testMap.put(KEY_1, 1);

        assertThat(testMap.lastKey()).isEqualTo(KEY_2);
    }

    @Test
    void subMap_whenKeyNotInMap_throwsException() {
        assertThatThrownBy(() -> testMap.subMap(KEY_1, KEY_2)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void subMap_whenFromKeyIsGreaterThanToKey_throwsException() {
        testMap.put(KEY_1, 1);
        testMap.put(KEY_2, 2);

        assertThatThrownBy(() -> testMap.subMap(KEY_2, KEY_1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void subMap_whenFromKeyIsEqualToToKey_returnsEmptyMap() {
        testMap.put(KEY_1, 1);
        testMap.put(KEY_2, 2);

        assertThat(testMap.subMap(KEY_1, KEY_1)).isEmpty();
    }

    @Test
    void subMap_whenFromKeyIsLessThanToKey_returnsMapWithKeysInRange() {
        testMap.put(KEY_1, 1);
        testMap.put(KEY_2, 2);
        testMap.put(KEY_3, 3);

        SimpleImmutableEntry<String, Integer> firstEntry = new SimpleImmutableEntry<>(KEY_1, 1);
        SimpleImmutableEntry<String, Integer> secondEntry = new SimpleImmutableEntry<>(KEY_2, 2);

        assertThat(testMap.subMap(KEY_1, KEY_2).entrySet()).containsExactly(firstEntry);
        assertThat(testMap.subMap(KEY_1, KEY_3).entrySet()).containsExactly(firstEntry, secondEntry);
        assertThat(testMap.subMap(KEY_2, KEY_3).entrySet()).containsExactly(secondEntry);
    }

    @Test
    void subMap_whenFromInclusiveFalse_excludesFromKey() {
        testMap.put(KEY_1, 1);
        testMap.put(KEY_2, 2);
        testMap.put(KEY_3, 3);

        SimpleImmutableEntry<String, Integer> secondEntry = new SimpleImmutableEntry<>(KEY_2, 2);

        assertThat(testMap.subMap(KEY_1, false, KEY_2, false).entrySet()).isEmpty();
        assertThat(testMap.subMap(KEY_1, false, KEY_3, false).entrySet()).containsExactly(secondEntry);
        assertThat(testMap.subMap(KEY_2, false, KEY_3, false).entrySet()).isEmpty();
    }

    @Test
    void subMap_whenToInclusiveTrue_includesToKey() {
        testMap.put(KEY_1, 1);
        testMap.put(KEY_2, 2);
        testMap.put(KEY_3, 3);

        SimpleImmutableEntry<String, Integer> firstEntry = new SimpleImmutableEntry<>(KEY_1, 1);
        SimpleImmutableEntry<String, Integer> secondEntry = new SimpleImmutableEntry<>(KEY_2, 2);
        SimpleImmutableEntry<String, Integer> thirdEntry = new SimpleImmutableEntry<>(KEY_3, 3);

        assertThat(testMap.subMap(KEY_1, true, KEY_2, true).entrySet()).containsExactly(firstEntry, secondEntry);
        assertThat(testMap.subMap(KEY_1, true, KEY_3, true).entrySet()).containsExactly(firstEntry, secondEntry, thirdEntry);
        assertThat(testMap.subMap(KEY_2, true, KEY_3, true).entrySet()).containsExactly(secondEntry, thirdEntry);
    }

    @Test
    void compute_whenKeyNotInMap_returnsNewValue() {
        testMap.put(KEY_1, 1);

        assertThat(testMap.compute(KEY_2, (k, v) -> {
            assertThat(v).isNull();
            return 2;
        })).isEqualTo(2);
        assertThat(testMap.get(KEY_2)).isEqualTo(2);
    }

    @Test
    void compute_whenKeyInMap_returnsNewValue() {
        testMap.put(KEY_1, 1);

        assertThat(testMap.compute(KEY_1, (k, v) -> {
            assertThat(v).isEqualTo(1);
            return 2;
        })).isEqualTo(2);
        assertThat(testMap.get(KEY_1)).isEqualTo(2);
    }

    @Test
    void computeIfAbsent_whenKeyNotInMap_returnsNewValue() {
        testMap.put(KEY_1, 1);

        assertThat(testMap.computeIfAbsent(KEY_2, k -> 2)).isEqualTo(2);
        assertThat(testMap.get(KEY_2)).isEqualTo(2);
    }

    @Test
    void computeIfAbsent_whenKeyInMap_returnsExistingValue() {
        testMap.put(KEY_1, 1);

        assertThat(testMap.computeIfAbsent(KEY_1, k -> Assertions.fail()))
                .isEqualTo(1);
        assertThat(testMap.get(KEY_1)).isEqualTo(1);
    }

    @Test
    void computeIfPresent_whenKeyNotInMap_returnsNull() {
        testMap.put(KEY_1, 1);

        assertThat(testMap.computeIfPresent(KEY_2, (k, v) -> Assertions.fail()))
                .isNull();
    }

    @Test
    void computeIfPresent_whenKeyInMap_returnsNewValue() {
        testMap.put(KEY_1, 1);

        assertThat(testMap.computeIfPresent(KEY_1, (k, v) -> {
            assertThat(v).isEqualTo(1);
            return 2;
        })).isEqualTo(2);
        assertThat(testMap.get(KEY_1)).isEqualTo(2);
    }

    @Test
    void headMap_whenKeyNotInMap_throwsException() {
        assertThatThrownBy(() -> testMap.headMap(KEY_1)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void headMap_whenKeyIsFirstKey_returnsEmptyMap() {
        testMap.put(KEY_1, 1);
        testMap.put(KEY_2, 2);

        assertThat(testMap.headMap(KEY_1).entrySet()).isEmpty();
    }

    @Test
    void headMap_whenKeyIsNotFirstKey_returnsMapWithKeysLessThanKey() {
        testMap.put(KEY_1, 1);
        testMap.put(KEY_2, 2);
        testMap.put(KEY_3, 3);

        SimpleImmutableEntry<String, Integer> firstEntry = new SimpleImmutableEntry<>(KEY_1, 1);
        SimpleImmutableEntry<String, Integer> secondEntry = new SimpleImmutableEntry<>(KEY_2, 2);

        assertThat(testMap.headMap(KEY_2).entrySet()).containsExactly(firstEntry);
        assertThat(testMap.headMap(KEY_3).entrySet()).containsExactly(firstEntry, secondEntry);
    }

    @Test
    void headMap_whenInclusiveTrue_includesKey() {
        testMap.put(KEY_1, 1);
        testMap.put(KEY_2, 2);
        testMap.put(KEY_3, 3);

        SimpleImmutableEntry<String, Integer> firstEntry = new SimpleImmutableEntry<>(KEY_1, 1);
        SimpleImmutableEntry<String, Integer> secondEntry = new SimpleImmutableEntry<>(KEY_2, 2);
        SimpleImmutableEntry<String, Integer> thirdEntry = new SimpleImmutableEntry<>(KEY_3, 3);

        assertThat(testMap.headMap(KEY_1, true).entrySet()).containsExactly(firstEntry);
        assertThat(testMap.headMap(KEY_2, true).entrySet()).containsExactly(firstEntry, secondEntry);
        assertThat(testMap.headMap(KEY_3, true).entrySet()).containsExactly(firstEntry, secondEntry, thirdEntry);
    }

    @Test
    void tailMap_whenKeyNotInMap_throwsException() {
        assertThatThrownBy(() -> testMap.tailMap(KEY_1)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void tailMap_whenKeyIsLastKey_returnsSingleEntryMap() {
        testMap.put(KEY_1, 1);
        testMap.put(KEY_2, 2);

        SimpleImmutableEntry<String, Integer> secondEntry = new SimpleImmutableEntry<>(KEY_2, 2);

        assertThat(testMap.tailMap(KEY_2).entrySet()).containsExactly(secondEntry);
    }

    @Test
    void tailMap_whenKeyIsNotLastKey_returnsMapWithKeysGreaterThanOrEqualKey() {
        testMap.put(KEY_1, 1);
        testMap.put(KEY_2, 2);
        testMap.put(KEY_3, 3);

        SimpleImmutableEntry<String, Integer> firstEntry = new SimpleImmutableEntry<>(KEY_1, 1);
        SimpleImmutableEntry<String, Integer> secondEntry = new SimpleImmutableEntry<>(KEY_2, 2);
        SimpleImmutableEntry<String, Integer> thirdEntry = new SimpleImmutableEntry<>(KEY_3, 3);

        assertThat(testMap.tailMap(KEY_1).entrySet()).containsExactly(firstEntry, secondEntry, thirdEntry);
        assertThat(testMap.tailMap(KEY_2).entrySet()).containsExactly(secondEntry, thirdEntry);
        assertThat(testMap.tailMap(KEY_3).entrySet()).containsExactly(thirdEntry);
    }

    @Test
    void tailMap_whenInclusiveFalse_excludesKey() {
        testMap.put(KEY_1, 1);
        testMap.put(KEY_2, 2);
        testMap.put(KEY_3, 3);

        SimpleImmutableEntry<String, Integer> secondEntry = new SimpleImmutableEntry<>(KEY_2, 2);
        SimpleImmutableEntry<String, Integer> thirdEntry = new SimpleImmutableEntry<>(KEY_3, 3);

        assertThat(testMap.tailMap(KEY_1, false).entrySet()).containsExactly(secondEntry, thirdEntry);
        assertThat(testMap.tailMap(KEY_2, false).entrySet()).containsExactly(thirdEntry);
        assertThat(testMap.tailMap(KEY_3, false).entrySet()).isEmpty();
    }
}