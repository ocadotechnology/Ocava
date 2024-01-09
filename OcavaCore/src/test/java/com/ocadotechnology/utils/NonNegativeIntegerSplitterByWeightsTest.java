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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;

public class NonNegativeIntegerSplitterByWeightsTest {
    @Test
    void testSingleWeight() {
        NonNegativeIntegerSplitterByWeights<String> splitter = new NonNegativeIntegerSplitterByWeights<>(ImmutableMap.of(
                "A", 1d));
        Assertions.assertEquals(1, (int)splitter.split(1).get("A"));
        Assertions.assertEquals(100, (int)splitter.split(100).get("A"));
    }

    @Test
    void testMultipleWeights() {
        NonNegativeIntegerSplitterByWeights<String> splitter = new NonNegativeIntegerSplitterByWeights<>(ImmutableMap.of(
                "A", 1d,
                "B", 2d));

        ImmutableMap<String, Integer> splitInts = splitter.split(1);
        Assertions.assertEquals(0, (int)splitInts.get("A"));
        Assertions.assertEquals(1, (int)splitInts.get("B"));

        splitInts = splitter.split(2);
        Assertions.assertEquals(1, (int)splitInts.get("A"));
        Assertions.assertEquals(1, (int)splitInts.get("B"));

        splitInts = splitter.split(100);
        Assertions.assertEquals(33, (int)splitInts.get("A"));
        Assertions.assertEquals(67, (int)splitInts.get("B"));

        splitInts = splitter.split(101);
        Assertions.assertEquals(34, (int)splitInts.get("A"));
        Assertions.assertEquals(67, (int)splitInts.get("B"));
    }

    @Test
    void testWithNoWeights() {
        NonNegativeIntegerSplitterByWeights<String> splitter = new NonNegativeIntegerSplitterByWeights<>(ImmutableMap.of());
        Assertions.assertTrue(splitter.split(1).isEmpty());
        Assertions.assertTrue(splitter.split(100).isEmpty());
    }

    @Test
    void testSplitSWithLargeRemainder() {
        NonNegativeIntegerSplitterByWeights<String> splitter = new NonNegativeIntegerSplitterByWeights<>(ImmutableMap.of(
                "A", 1d,
                "B", 2d,
                "C", 3d,
                "D", 2d));

        ImmutableMap<String, Integer> splitInts = splitter.split(7);
        Assertions.assertEquals(1, (int) splitInts.get("A"));
        Assertions.assertEquals(2, (int) splitInts.get("B"));
        Assertions.assertEquals(2, (int) splitInts.get("C"));
        Assertions.assertEquals(2, (int) splitInts.get("D"));
    }

    @Test
    void testFavourFirstKeysInWeightMapForTies() {
        NonNegativeIntegerSplitterByWeights<String> splitter = new NonNegativeIntegerSplitterByWeights<>(ImmutableMap.of(
                "A", 1d,
                "B", 2d,
                "C", 1d
        ));

        ImmutableMap<String, Integer> splitInts = splitter.split(2);

        Assertions.assertEquals(1, (int) splitInts.get("A"));
        Assertions.assertEquals(1, (int) splitInts.get("B"));
        Assertions.assertEquals(0, (int) splitInts.get("C"));
    }

    @Test
    void testFavourLargerKeysForTiesWhenSorting() {
        NonNegativeIntegerSplitterByWeights<String> splitter = NonNegativeIntegerSplitterByWeights.createWithSorting(ImmutableMap.of(
                "A", 1d,
                "B", 2d,
                "C", 1d
        ));

        ImmutableMap<String, Integer> splitInts = splitter.split(2);

        Assertions.assertEquals(0, (int) splitInts.get("A"));
        Assertions.assertEquals(1, (int) splitInts.get("B"));
        Assertions.assertEquals(1, (int) splitInts.get("C"));
    }

    @Test
    void testFavourFirstKeysInWeightMapForTiesWithIntegers() {
        NonNegativeIntegerSplitterByWeights<String> splitter = NonNegativeIntegerSplitterByWeights.createFromInts(ImmutableMap.of(
                "A", 1,
                "B", 2,
                "C", 1
        ));

        ImmutableMap<String, Integer> splitInts = splitter.split(2);

        Assertions.assertEquals(1, (int) splitInts.get("A"));
        Assertions.assertEquals(1, (int) splitInts.get("B"));
        Assertions.assertEquals(0, (int) splitInts.get("C"));
    }

    @Test
    void testFavourLargerKeysForTiesWhenSortingWithIntegers() {
        NonNegativeIntegerSplitterByWeights<String> splitter = NonNegativeIntegerSplitterByWeights.createFromIntsWithSorting(ImmutableMap.of(
                "A", 1,
                "B", 2,
                "C", 1
        ));

        ImmutableMap<String, Integer> splitInts = splitter.split(2);

        Assertions.assertEquals(0, (int) splitInts.get("A"));
        Assertions.assertEquals(1, (int) splitInts.get("B"));
        Assertions.assertEquals(1, (int) splitInts.get("C"));
    }

    @Test
    void testTableSplitting() {
        ImmutableTable<String, String, Double> tableWeights = ImmutableTableFactory.createWithRows(
                ImmutableMap.of(
                        "Column1", 1d,
                        "Column2", 2d),
                ImmutableList.of("Row1", "Row2"));

        ImmutableTable<String, String, Integer> splitInts = NonNegativeIntegerSplitterByWeights.splitByTableWeights(tableWeights, 2);

        Assertions.assertEquals(0, (int) splitInts.get("Row1", "Column1"));
        Assertions.assertEquals(1, (int) splitInts.get("Row1", "Column2"));
        Assertions.assertEquals(0, (int) splitInts.get("Row2", "Column1"));
        Assertions.assertEquals(1, (int) splitInts.get("Row2", "Column2"));
    }

    @Test
    void testIntegerTableSplitting() {
        ImmutableTable<String, String, Integer> tableWeights = ImmutableTableFactory.createWithRows(
                ImmutableMap.of(
                        "Column1", 1,
                        "Column2", 2),
                ImmutableList.of("Row1", "Row2"));

        ImmutableTable<String, String, Integer> splitInts = NonNegativeIntegerSplitterByWeights.splitByIntegerTableWeights(tableWeights, 2);

        Assertions.assertEquals(0, (int) splitInts.get("Row1", "Column1"));
        Assertions.assertEquals(1, (int) splitInts.get("Row1", "Column2"));
        Assertions.assertEquals(0, (int) splitInts.get("Row2", "Column1"));
        Assertions.assertEquals(1, (int) splitInts.get("Row2", "Column2"));
    }
}