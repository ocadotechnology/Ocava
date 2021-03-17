/*
 * Copyright © 2017-2021 Ocado (Ocava)
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
package com.ocadotechnology.tableio;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ImmutableTable.Builder;
import com.google.common.collect.Table;
import com.ocadotechnology.utils.TableUtils;

class TableUtilsTest {
    private final String FIRST_ROW = "test0";
    private final String SECOND_ROW = "test1";
    private final Double EPSILON = 0.05;

    private Table<String, Integer, Double> nonFullTable;
    private Table<String, Integer, Double> fullTable;

    @BeforeEach
    void before() {
        Builder<String, Integer, Double> nonFullTableBuilder = ImmutableTable.builder();
        nonFullTableBuilder.put(FIRST_ROW, 0, 0.0);
        nonFullTableBuilder.put(FIRST_ROW, 1, 0.1);
        nonFullTableBuilder.put(SECOND_ROW, 0, 1.0);
        nonFullTable = nonFullTableBuilder.build();

        Builder<String, Integer, Double> fullTableBuilder = ImmutableTable.builder();
        fullTableBuilder.put(FIRST_ROW, 0, 0.0);
        fullTableBuilder.put(FIRST_ROW, 1, 0.0);
        fullTableBuilder.put(SECOND_ROW, 0, 0.0);
        fullTableBuilder.put(SECOND_ROW, 1, 0.0);
        fullTable = fullTableBuilder.build();
    }

    @Test
    void testIsFull() {
        Assertions.assertTrue(TableUtils.isFull(fullTable));
        Assertions.assertFalse(TableUtils.isFull(nonFullTable));
    }

    @Test
    void testForEach() {
        Table<String, Integer, Double> outputTable = HashBasedTable.create();
        TableUtils.forEach(
                nonFullTable,
                (r, c, v) -> outputTable.put(r, c, v + 10));

        Assertions.assertEquals(3, outputTable.size());
        Assertions.assertEquals(10.0, outputTable.get(FIRST_ROW, 0), EPSILON);
        Assertions.assertEquals(10.1, outputTable.get(FIRST_ROW, 1), EPSILON);
        Assertions.assertEquals(11.0, outputTable.get(SECOND_ROW, 0), EPSILON);
    }

    @Test
    void testUpdate() {
        Table<String, Integer, Double> tableToUpdate = HashBasedTable.create(nonFullTable);
        TableUtils.update(
                tableToUpdate,
                value -> value * 2);

        Assertions.assertEquals(3, tableToUpdate.size());
        Assertions.assertEquals(0, tableToUpdate.get(FIRST_ROW, 0), EPSILON);
        Assertions.assertEquals(0.2, tableToUpdate.get(FIRST_ROW, 1), EPSILON);
        Assertions.assertEquals(2.0, tableToUpdate.get(SECOND_ROW, 0), EPSILON);
    }

    @Test
    void testPutAll() {
        Table<String, Integer, String> outputTable = HashBasedTable.create();
        TableUtils.putAll(
                nonFullTable,
                outputTable,
                value -> String.valueOf(value + 10));

        Assertions.assertEquals(3, outputTable.size());
        Assertions.assertEquals("10.0", outputTable.get(FIRST_ROW, 0));
        Assertions.assertEquals("10.1", outputTable.get(FIRST_ROW, 1));
        Assertions.assertEquals("11.0", outputTable.get(SECOND_ROW, 0));
    }

    @Test
    void testGetOrDefaultWhenValuePresent() {
        Assertions.assertEquals(1.0, TableUtils.getOrDefault(nonFullTable, SECOND_ROW, 0, 0.0), EPSILON);
    }

    @Test
    void testGetOrDefaultWhenValueNotPresent() {
        Assertions.assertEquals(7.0, TableUtils.getOrDefault(nonFullTable, SECOND_ROW, 1, 7.0), EPSILON);
    }

    @Test
    void testGetOrDefaultWhenRowNotPresent() {
        Assertions.assertEquals(8.0, TableUtils.getOrDefault(nonFullTable, "NON_EXISTENT_ROW", 1, 8.0), EPSILON);
    }
}
