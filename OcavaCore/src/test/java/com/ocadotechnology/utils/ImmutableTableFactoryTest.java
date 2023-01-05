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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ImmutableTable.Builder;
import com.google.common.collect.Table;

public class ImmutableTableFactoryTest {
    private final String FIRST_ROW = "test0";
    private final String SECOND_ROW = "test1";
    private final Double EPSILON = 0.05;

    private Table<String, Integer, Double> nonFullTable;
    private Map<Integer, Double> baseMap = new HashMap<>();

    @BeforeEach
    void before() {
        Builder<String, Integer, Double> nonFullTableBuilder = ImmutableTable.builder();
        nonFullTableBuilder.put(FIRST_ROW, 0, 0.0);
        nonFullTableBuilder.put(FIRST_ROW, 1, 0.1);
        nonFullTableBuilder.put(SECOND_ROW, 0, 1.0);
        nonFullTable = nonFullTableBuilder.build();

        baseMap.put(0, 0.0);
        baseMap.put(1, 0.1);
    }

    @Test
    void testCreateFromTableAndMapper() {
        ImmutableTable<String, Integer, String> outputTable = ImmutableTableFactory.create(
                nonFullTable, value -> String.valueOf(value + 10));

        Assertions.assertEquals(3, outputTable.size());
        Assertions.assertEquals("10.0", outputTable.get(FIRST_ROW, 0));
        Assertions.assertEquals("10.1", outputTable.get(FIRST_ROW, 1));
        Assertions.assertEquals("11.0", outputTable.get(SECOND_ROW, 0));
    }

    @Test
    void testCreateFromTableCells() {
        ImmutableTable<String, Integer, String> outputTable = ImmutableTableFactory.create(
                nonFullTable, (row, column) -> row + column);

        Assertions.assertEquals(3, outputTable.size());
        Assertions.assertEquals(FIRST_ROW + "0", outputTable.get(FIRST_ROW, 0));
        Assertions.assertEquals(FIRST_ROW + "1", outputTable.get(FIRST_ROW, 1));
        Assertions.assertEquals(SECOND_ROW + "0", outputTable.get(SECOND_ROW, 0));
    }

    @Test
    void testCreateFromMapAndSingleRow() {
        ImmutableTable<String, Integer, Double> outputTable = ImmutableTableFactory.createWithRows(
                baseMap,
                ImmutableSet.of(FIRST_ROW));

        Assertions.assertEquals(2, outputTable.size());
        Assertions.assertEquals(0.0, outputTable.get(FIRST_ROW, 0), EPSILON);
        Assertions.assertEquals(0.1, outputTable.get(FIRST_ROW, 1), EPSILON);
    }

    @Test
    void testCreateFromMapAndSingleColumn() {
        ImmutableTable<Integer, String, Double> outputTable = ImmutableTableFactory.createWithColumns(
                baseMap,
                ImmutableSet.of(FIRST_ROW));

        Assertions.assertEquals(2, outputTable.size());
        Assertions.assertEquals(0.0, outputTable.get(0, FIRST_ROW), EPSILON);
        Assertions.assertEquals(0.1, outputTable.get(1, FIRST_ROW), EPSILON);
    }

    @Test
    void testCreateFromOtherTableFullMapping() {
        Table<Integer, String, Integer> outputTable = ImmutableTableFactory.create(
                nonFullTable,
                s -> Integer.valueOf(s.substring(4)),
                String::valueOf,
                Double::intValue);

        Assertions.assertEquals(3, outputTable.size());
        Assertions.assertEquals( 0, (int) outputTable.get(0, "0"));
        Assertions.assertEquals(0, (int) outputTable.get(0, "1"));
        Assertions.assertEquals(1, (int) outputTable.get(1, "0"));
    }
}
