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
package com.ocadotechnology.utils;

import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

class ImmutableMapFactoryTest {

    @Test
    void testParse() {
        String toParse = "A:1;B:2";

        ImmutableMap<String, Integer> parsed = ImmutableMapFactory.parse(
                toParse,
                s -> s,
                Integer::parseInt);

        Assertions.assertEquals(ImmutableMap.of("A", 1, "B", 2), parsed);

        String composed = composeString(
                parsed,
                s -> s,
                i -> Integer.toString(i));

        Assertions.assertEquals(toParse, composed);
    }

    @Test
    void testParseOneObject() {
        String toParse = "A:1";

        ImmutableMap<String, Integer> parsed = ImmutableMapFactory.parse(
                toParse,
                s -> s,
                Integer::parseInt);

        Assertions.assertEquals(ImmutableMap.of("A", 1), parsed);
    }

    @Test
    void testParseAndFilter() {
        String toParse = "A:1;B:2;C:3;D:4";

        ImmutableMap<String, Integer> parsed = ImmutableMapFactory.parseAndFilter(
                toParse,
                s -> s,
                Integer::parseInt,
                (key, value) -> !key.equals("A") && value < 4);

        Assertions.assertEquals(ImmutableMap.of("B", 2, "C", 3), parsed);
    }

    @Test
    void testParseAndFilterNoneRemain() {
        String toParse = "A:1;B:2;C:3;D:4";

        ImmutableMap<String, Integer> parsed = ImmutableMapFactory.parseAndFilter(
                toParse,
                s -> s,
                Integer::parseInt,
                (key, value) -> !key.equals("A") && value > 4);
        ImmutableMap<String, Integer> expected = ImmutableMap.of();

        Assertions.assertEquals(expected, parsed);
    }

    // Composes a map to a string of the form "value1:key1;value2:key2;..."
    private static <K, V> String composeString(
            Map<K, V> toCompose,
            Function<K, String> keyToString,
            Function<V, String> valueToString) {
        StringJoiner semiColonJoiner = new StringJoiner(";");

        toCompose.entrySet().stream()
                .map(e -> String.format(
                        "%s:%s",
                        keyToString.apply(e.getKey()),
                        valueToString.apply(e.getValue())))
                .forEach(semiColonJoiner::add);

        return semiColonJoiner.toString();
    }

    @Test
    void testCreateAndReduce() {
        ImmutableMap<String, Integer> toReduce = ImmutableMap.of(
                "A0", 1,
                "A1", 2,
                "B", 3,
                "A2", 4);

        ImmutableMap<String, Integer> reduced = ImmutableMapFactory.createAndReduceCollisions(
                toReduce,
                s -> s.substring(0, 1),
                i -> i,
                0,
                Integer::sum);

        ImmutableMap<String, Integer> expected = ImmutableMap.of(
                "A", 7,
                "B", 3);

        Assertions.assertEquals(expected, reduced);
    }

    @Test
    void testCreateWithUnnecessaryReduction() {
        ImmutableMap<String, Integer> toReduce = ImmutableMap.of(
                "A", 1,
                "B", 2,
                "C", 3,
                "D", 4);

        ImmutableMap<String, Integer> reduced = ImmutableMapFactory.createAndReduceCollisions(
                toReduce,
                s -> s.substring(0, 1),
                i -> i,
                0,
                Integer::sum);

        ImmutableMap<String, Integer> expected = ImmutableMap.of(
                "A", 1,
                "B", 2,
                "C", 3,
                "D", 4);

        Assertions.assertEquals(expected, reduced);
    }

    @Test
    void testZip() {
        ImmutableList<String> keys = ImmutableList.of("a", "b", "c");
        ImmutableList<Integer> values = ImmutableList.of(1, 2, 3);

        ImmutableMap<String, Integer> zipped = ImmutableMapFactory.zip(keys, values);

        Assertions.assertEquals(ImmutableSet.copyOf(keys), zipped.keySet());
        Assertions.assertEquals(values, ImmutableList.copyOf(zipped.values()));
    }

    @Test
    void zipThrowsForDifferentSizesLists() {
        ImmutableList<String> tooManyKeys = ImmutableList.of("a", "b", "c");
        ImmutableList<Integer> notEnoughValues = ImmutableList.of(1, 2);

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> ImmutableMapFactory.zip(tooManyKeys, notEnoughValues));
    }

    @Test
    void testFilter() {
        ImmutableMap<String, Integer> toFilter = ImmutableMap.of(
                "A", 1,
                "B", 2,
                "C", 3,
                "D", 4);

        ImmutableMap<String, Integer> expected = ImmutableMap.of(
                "B", 2,
                "C", 3);
        ImmutableMap<String, Integer> actual =
                ImmutableMapFactory.filter(toFilter, (key, value) -> value < 4 && !key.equals("A"));
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testFilterNoneRemain() {
        ImmutableMap<String, Integer> toFilter = ImmutableMap.of(
                "A", 1,
                "B", 2,
                "C", 3,
                "D", 4);

        ImmutableMap<String, Integer> expected = ImmutableMap.of();
        ImmutableMap<String, Integer> actual =
                ImmutableMapFactory.filter(toFilter, (key, value) -> value > 50 && !key.equals("A"));
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testFilterByKeys() {
        ImmutableMap<String, Integer> toFilter = ImmutableMap.of(
                "A", 1,
                "B", 2,
                "C", 3,
                "D", 4);

        ImmutableMap<String, Integer> expected = ImmutableMap.of(
                "B", 2,
                "C", 3,
                "D", 4);
        ImmutableMap<String, Integer> actual = ImmutableMapFactory.filterByKeys(toFilter, key -> !key.equals("A"));
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testFilterByKeysNoneRemain() {
        ImmutableMap<String, Integer> toFilter = ImmutableMap.of(
                "A", 1,
                "B", 2,
                "C", 3,
                "D", 4);

        ImmutableMap<String, Integer> expected = ImmutableMap.of();
        ImmutableMap<String, Integer> actual = ImmutableMapFactory.filterByKeys(toFilter, key -> key.equals("E"));
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testFilterByValues() {
        ImmutableMap<String, Integer> toFilter = ImmutableMap.of(
                "A", 1,
                "B", 2,
                "C", 3,
                "D", 4);

        ImmutableMap<String, Integer> expected = ImmutableMap.of(
                "C", 3,
                "D", 4);
        ImmutableMap<String, Integer> actual = ImmutableMapFactory.filterByValues(toFilter, value -> value > 2);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testFilterByValuesNoneRemain() {
        ImmutableMap<String, Integer> toFilter = ImmutableMap.of(
                "A", 1,
                "B", 2,
                "C", 3,
                "D", 4);

        ImmutableMap<String, Integer> expected = ImmutableMap.of();
        ImmutableMap<String, Integer> actual = ImmutableMapFactory.filterByValues(toFilter, value -> value > 15);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testCreateFromKeysWithFunction() {
        ImmutableList<Integer> keys = ImmutableList.of(1, 2, 3, 4);
        ImmutableMap<Integer, Integer> expected = ImmutableMap.of(
                1, 1,
                2, 4,
                3, 9,
                4, 16);
        ImmutableMap<Integer, Integer> actual = ImmutableMapFactory.createFromKeys(keys, (key) -> key * key);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testCreateFromKeysWithDifferentType() {
        ImmutableList<String> keys = ImmutableList.of("1", "2", "3", "4");
        ImmutableMap<String, Integer> expected = ImmutableMap.of(
                "1", 1,
                "2", 2,
                "3", 3,
                "4", 4);
        ImmutableMap<String, Integer> actual = ImmutableMapFactory.createFromKeys(keys, Integer::parseInt);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testCreateFromKeysWithValueSupplier() {
        ImmutableList<Integer> keys = ImmutableList.of(1, 2, 3, 4);
        ImmutableMap<Integer, Integer> expected = ImmutableMap.of(
                1, 1,
                2, 1,
                3, 1,
                4, 1);
        ImmutableMap<Integer, Integer> actual = ImmutableMapFactory.createFromKeys(keys, () -> 1);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testCreateWithNewValuesUsingValueMapperWhichUsesBothKeysAndValues() {
        ImmutableMap<Integer, Integer> baseMap = ImmutableMap.of(
                3, 1,
                7, 2,
                14, 3,
                1, 4);

        ImmutableMap<Integer, Integer> expected = ImmutableMap.of(
                3, 4,
                7, 9,
                14, 17,
                1, 5);
        ImmutableMap<Integer, Integer> actual = ImmutableMapFactory.
                createWithNewValues(baseMap, Integer::sum);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testCreateWithNewValuesUsingValueMapperWhichUsesBothKeysAndValuesToADifferentType() {
        ImmutableMap<Integer, Integer> baseMap = ImmutableMap.of(
                3, 1,
                7, 2,
                14, 3,
                1, 4);

        ImmutableMap<Integer, String> expected = ImmutableMap.of(
                3, "4",
                7, "9",
                14, "17",
                1, "5");
        ImmutableMap<Integer, String> actual = ImmutableMapFactory.
                createWithNewValues(baseMap, (key, value) -> String.valueOf(key + value));
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testCreateWithNewValuesUsingValueMapperWhichUsesOnlyValues() {
        ImmutableMap<Integer, Integer> baseMap = ImmutableMap.of(
                3, 1,
                7, 2,
                14, 3,
                1, 4);

        ImmutableMap<Integer, Integer> expected = ImmutableMap.of(
                3, 1,
                7, 4,
                14, 9,
                1, 16);
        ImmutableMap<Integer, Integer> actual = ImmutableMapFactory.
                createWithNewValues(baseMap, value -> value * value);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testCreateWithNewValuesUsingValueMapperWhichUsesOnlyValuesToADifferentType() {
        ImmutableMap<Integer, String> baseMap = ImmutableMap.of(
                3, "1",
                7, "2",
                14, "3",
                1, "4");

        ImmutableMap<Integer, Integer> expected = ImmutableMap.of(
                3, 1,
                7, 2,
                14, 3,
                1, 4);
        ImmutableMap<Integer, Integer> actual = ImmutableMapFactory.
                createWithNewValues(baseMap, (Function<String, Integer>) Integer::parseInt);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testCreateWithNewKeys() {
        ImmutableMap<Integer, Integer> baseMap = ImmutableMap.of(
                3, 1,
                7, 2,
                14, 3,
                1, 4);

        ImmutableMap<Integer, Integer> expected = ImmutableMap.of(
                6, 1,
                14, 2,
                28, 3,
                2, 4);

        ImmutableMap<Integer, Integer> actual = ImmutableMapFactory.
                createWithNewKeys(baseMap, key -> key * 2);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testCreateWithNewKeysToADifferentType() {
        ImmutableMap<Integer, Integer> baseMap = ImmutableMap.of(
                3, 1,
                7, 2,
                14, 3,
                1, 4);

        ImmutableMap<String, Integer> expected = ImmutableMap.of(
                "3", 1,
                "7", 2,
                "14", 3,
                "1", 4);

        ImmutableMap<String, Integer> actual = ImmutableMapFactory.
                createWithNewKeys(baseMap, String::valueOf);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testCreateWithBothKeyMapperAndValueMapper() {
        ImmutableMap<Integer, Integer> baseMap = ImmutableMap.of(
                1, 2,
                2, 3,
                3, 4,
                4, 5);

        ImmutableMap<Integer, Integer> expected = ImmutableMap.of(
                1, 4,
                4, 6,
                9, 8,
                16, 10);

        ImmutableMap<Integer, Integer> actual = ImmutableMapFactory.create(
                baseMap,
                key -> key * key,
                value -> value * 2);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testCreateWithBothKeyMapperAndValueMapperToADifferentType() {
        ImmutableMap<Integer, Integer> baseMap = ImmutableMap.of(
                1, 1,
                2, 2,
                3, 3,
                4, 4);

        ImmutableMap<String, String> expected = ImmutableMap.of(
                "1", "2",
                "4", "4",
                "9", "6",
                "16", "8");

        ImmutableMap<String, String> actual = ImmutableMapFactory.create(
                baseMap,
                key -> String.valueOf(key * key),
                value -> String.valueOf(value * 2));

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testCreateWithBothKeyCreatorAndValueCreator() {
        ImmutableMap<Integer, Integer> baseMap = ImmutableMap.of(
                2, 1,
                4, 2,
                6, 3,
                8, 4);

        ImmutableMap<Integer, Integer> expected = ImmutableMap.of(
                2, 3,
                8, 6,
                18, 9,
                32, 12);

        ImmutableMap<Integer, Integer> actual = ImmutableMapFactory.create(
                baseMap,
                (key, value) -> key * value,
                (key, value) -> key + value);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testCreateWithBothKeyCreatorAndValueCreatorToADifferentType() {
        ImmutableMap<Integer, Integer> baseMap = ImmutableMap.of(
                2, 1,
                4, 2,
                6, 3,
                8, 4);

        ImmutableMap<String, String> expected = ImmutableMap.of(
                "2", "3",
                "8", "6",
                "18", "9",
                "32", "12");

        ImmutableMap<String, String> actual = ImmutableMapFactory.create(
                baseMap,
                (key, value) -> String.valueOf(key * value),
                (key, value) -> String.valueOf(key + value));

        Assertions.assertEquals(expected, actual);
    }
}