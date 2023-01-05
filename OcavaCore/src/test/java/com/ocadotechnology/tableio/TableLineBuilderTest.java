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
package com.ocadotechnology.tableio;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ImmutableTable.Builder;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.StringId;

class TableLineBuilderTest {
    private final TableLineBuilder builder = new TableLineBuilder();

    private enum TestEnum {
        PRESENT_ELEMENTS,
        MISSING_ELEMENTS,
    }

    public static class TestTableLineExtender implements TableLineExtender {
        @Override
        public void addData(TableLineBuilder line) {
            line.withString("TestTableLine", "Test")
                    .withBoolean("HasThisBeenAdded", true);
        }
    }

    @Test
    void testWithInt() {
        String columnName = "ColumnName";
        TableLine tableLine = builder.withInt(columnName, 1).build();

        TableLine expectedTableLine = new TableLine(ImmutableMap.of(
                columnName, "1"),
                ImmutableSet.of());
        checkTableLinesAreTheSame(expectedTableLine, tableLine);
    }

    @Test
    void testWithLong() {
        String columnName = "ColumnName";
        TableLine tableLine = builder.withLong(columnName, 1L).build();

        TableLine expectedTableLine = new TableLine(ImmutableMap.of(
                columnName, "1"),
                ImmutableSet.of());
        checkTableLinesAreTheSame(expectedTableLine, tableLine);
    }

    @Test
    void testWithId() {
        StringId.create("Test");
        String columnName = "ColumnName";
        TableLine tableLine = builder.withId(columnName, Id.create(1)).build();

        TableLine expectedTableLine = new TableLine(ImmutableMap.of(
                columnName, "1"),
                ImmutableSet.of());
        checkTableLinesAreTheSame(expectedTableLine, tableLine);
    }

    @Test
    void testWithStringId() {
        String columnName = "ColumnName";
        TableLine tableLine = builder.withStringId(columnName, StringId.create("Test")).build();

        TableLine expectedTableLine = new TableLine(ImmutableMap.of(
                columnName, "Test"),
                ImmutableSet.of(columnName));
        checkTableLinesAreTheSame(expectedTableLine, tableLine);
    }

    @Test
    void testWithDouble() {
        String columnName = "ColumnName";
        TableLine tableLine = builder.withDouble(columnName, 1.0).build();

        TableLine expectedTableLine = new TableLine(ImmutableMap.of(
                columnName, "1.0"),
                ImmutableSet.of());
        checkTableLinesAreTheSame(expectedTableLine, tableLine);
    }

    @Test
    void testWithBoolean() {
        String columnName = "ColumnName";
        TableLine tableLine = builder.withBoolean(columnName, false).build();

        TableLine expectedTableLine = new TableLine(ImmutableMap.of(
                columnName, "false"),
                ImmutableSet.of());
        checkTableLinesAreTheSame(expectedTableLine, tableLine);
    }

    @Test
    void testWithTime() {
        String columnName = "ColumnName";
        LocalDateTime toAdd = LocalDateTime.now();
        TableLine tableLine = builder.withTime(columnName, toAdd).build();

        TableLine expectedTableLine = new TableLine(ImmutableMap.of(
                columnName, toAdd.toString()),
                ImmutableSet.of());
        checkTableLinesAreTheSame(expectedTableLine, tableLine);
    }

    @Test
    void testWithString() {
        String columnName = "ColumnName";
        String toAdd = "TestString";
        TableLine tableLine = builder.withString(columnName, toAdd).build();

        TableLine expectedTableLine = new TableLine(ImmutableMap.of(
                columnName, toAdd),
                ImmutableSet.of(columnName));
        checkTableLinesAreTheSame(expectedTableLine, tableLine);
    }

    @Test
    void testWithEnumValue() {
        String columnName = "ColumnName";
        ExampleEnum toAdd = ExampleEnum.EXAMPLE;
        TableLine tableLine = builder.withEnumValue(columnName, toAdd).build();

        TableLine expectedTableLine = new TableLine(ImmutableMap.of(
                columnName, toAdd.name()),
                ImmutableSet.of());
        checkTableLinesAreTheSame(expectedTableLine, tableLine);
    }

    @Test
    void testWithTableEnumCount() {
        Builder<TestEnum, ExampleEnum, Integer> tBuilder = ImmutableTable.builder();
        tBuilder.put(TestEnum.PRESENT_ELEMENTS, ExampleEnum.EXAMPLE, 3)
                .put(TestEnum.PRESENT_ELEMENTS, ExampleEnum.SECOND_EXAMPLE, 15)
                .put(TestEnum.MISSING_ELEMENTS, ExampleEnum.EXAMPLE, 1);

        ImmutableTable<TestEnum, ExampleEnum, Integer> testEnumCount = tBuilder.build();
        TableLine tableLine = builder.withTableEnumCount(
                "COUNT",
                testEnumCount,
                TestEnum.class,
                ExampleEnum.class).build();

        TableLine expectedTableLine = new TableLine(ImmutableMap.of(
                "EXAMPLE_PRESENT_ELEMENTS_COUNT", "3",
                "SECOND_EXAMPLE_PRESENT_ELEMENTS_COUNT", "15",
                "EXAMPLE_MISSING_ELEMENTS_COUNT", "1",
                "SECOND_EXAMPLE_MISSING_ELEMENTS_COUNT", "0"),
                ImmutableSet.of());

        Assertions.assertEquals(expectedTableLine.getLineMap(), tableLine.getLineMap());
    }

    @Test
    void testWithEnumCount() {
        ImmutableMap<TestEnum, Integer> testEnumCount = ImmutableMap.of(TestEnum.PRESENT_ELEMENTS, 1);
        TableLine tableLine = builder
                .withEnumCount("COUNT", testEnumCount, TestEnum.class)
                .build();

        TableLine expectedTableLine = new TableLine(ImmutableMap.of(
                "PRESENT_ELEMENTS_COUNT", "1",
                "MISSING_ELEMENTS_COUNT", "0"),
                ImmutableSet.of());

        Assertions.assertEquals(expectedTableLine.getLineMap(), tableLine.getLineMap());
    }

    @Test
    void testWithExtension() {
        TableLineExtender extender = new TestTableLineExtender();
        TableLine tableLine = builder.withExtension(extender).build();

        TableLine expectedTableLine = new TableLine(ImmutableMap.of(
                "TestTableLine", "Test",
                "HasThisBeenAdded", "true"),
                ImmutableSet.of("TestTableLine"));
        checkTableLinesAreTheSame(expectedTableLine, tableLine);
    }

    @Test
    void testWithMultipleValuesAdded() {
        TableLine tableLine = builder
                .withString("Header1", "TestString")
                .withBoolean("Header2", true)
                .withInt("Header3", 4)
                .withString("Header4", "SecondTestString")
                .build();

        TableLine expectedTableLine = new TableLine(ImmutableMap.of(
                "Header1", "TestString",
                "Header2", "true",
                "Header3", "4",
                "Header4", "SecondTestString"),
                ImmutableSet.of("Header1", "Header4"));

        Assertions.assertEquals(expectedTableLine.getLineMap(), tableLine.getLineMap());

    }

    void checkTableLinesAreTheSame(TableLine expectedTableLine, TableLine actualTableLine) {
        Assertions.assertEquals(expectedTableLine.getLineMap(), actualTableLine.getLineMap());
        Assertions.assertEquals(expectedTableLine.getLineMapWithStringsQuoted(), actualTableLine.getLineMapWithStringsQuoted());
    }

}
