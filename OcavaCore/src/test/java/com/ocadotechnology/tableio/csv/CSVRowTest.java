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
package com.ocadotechnology.tableio.csv;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.tableio.ExampleEnum;

class CSVRowTest {
    private final CSVRow<TestCSVColumn> FULL_ROW = CSVRowBuilder.create(TestCSVColumn.class)
            .withValue(TestCSVColumn.STRING, "test string")
            .withValue(TestCSVColumn.INTEGER, 1)
            .withValue(TestCSVColumn.DOUBLE, 2d)
            .withValue(TestCSVColumn.BIG_DECIMAL, 3d)
            .withValue(TestCSVColumn.BOOLEAN, "True")
            .withValue(TestCSVColumn.ENUM, "EXAMPLE")
            .build();
    private final CSVRow<TestCSVColumn> EMPTY_ROW = CSVRowBuilder.create(TestCSVColumn.class)
            .withValue(TestCSVColumn.STRING, "")
            .withValue(TestCSVColumn.INTEGER, "")
            .withValue(TestCSVColumn.DOUBLE, "")
            .withValue(TestCSVColumn.BIG_DECIMAL, "")
            .withValue(TestCSVColumn.BOOLEAN, "")
            .withValue(TestCSVColumn.ENUM, "")
            .withValue(TestCSVColumn.ALLOW_MISSING_COLUMN, "")
            .build();
    private final CSVRow<TestCSVColumn> INCORRECT_ROW = CSVRowBuilder.create(TestCSVColumn.class)
            .withValue(TestCSVColumn.INTEGER, 1d)
            .withValue(TestCSVColumn.DOUBLE, "NULL")
            .withValue(TestCSVColumn.BIG_DECIMAL, "NULL")
            .withValue(TestCSVColumn.BOOLEAN, "NULL")
            .withValue(TestCSVColumn.ENUM, "NOTAVALUE")
            .withValue(TestCSVColumn.DO_NOT_ALLOW_MISSING_VALUE, "")
            .build();
    private final CSVRow<TestCSVColumn> MISSING_COLUMNS = new CSVRow<>(ImmutableMap.of());

    @Test
    void testParseString() {
        Assertions.assertEquals("test string", FULL_ROW.parse(TestCSVColumn.STRING));
    }

    @Test
    void testParseMissingStringReturnsEmptyString() {
        Assertions.assertEquals("", EMPTY_ROW.parse(TestCSVColumn.STRING));
    }

    @Test
    void testParseMissingStringColumnThrowsNpeIfNotAllowed() {
        Assertions.assertThrows(NullPointerException.class, () -> MISSING_COLUMNS.parse(TestCSVColumn.STRING));
    }

    @Test
    void testParseMissingStringColumnReturnsNullIfAllowed() {
        Assertions.assertNull(MISSING_COLUMNS.parse(TestCSVColumn.ALLOW_MISSING_COLUMN));
    }

    @Test
    void testParseAllowedMissingStringColumnReturnsNullEvenIfMissingValueIsNotAllowed() {
        Assertions.assertNull(MISSING_COLUMNS.parse(TestCSVColumn.ALLOW_MISSING_COLUMN_NOT_MISSING_VALUE));
    }

    @Test
    void testParseInt() {
        Assertions.assertEquals(1, (int) FULL_ROW.parseInt(TestCSVColumn.INTEGER));
    }

    @Test
    void testParseMissingInt() {
        Assertions.assertNull(EMPTY_ROW.parseInt(TestCSVColumn.INTEGER));
    }

    @Test
    void testParseIncorrectInt() {
        Assertions.assertThrows(NumberFormatException.class, () -> INCORRECT_ROW.parseInt(TestCSVColumn.INTEGER));
    }

    @Test
    void testParseMissingIntegerColumnThrowsNpeIfNotAllowed() {
        Assertions.assertThrows(NullPointerException.class, () -> MISSING_COLUMNS.parseInt(TestCSVColumn.INTEGER));
    }

    @Test
    void testParseMissingIntegerColumnReturnsNullIfAllowed() {
        Assertions.assertNull(MISSING_COLUMNS.parseInt(TestCSVColumn.ALLOW_MISSING_COLUMN));
    }

    @Test
    void testParseDouble() {
        Assertions.assertEquals(2d, (double) FULL_ROW.parseDouble(TestCSVColumn.DOUBLE));
    }

    @Test
    void testParseMissingDouble() {
        Assertions.assertNull(EMPTY_ROW.parseDouble(TestCSVColumn.DOUBLE));
    }

    @Test
    void testParseIncorrectDouble() {
        Assertions.assertThrows(NumberFormatException.class, () -> INCORRECT_ROW.parseDouble(TestCSVColumn.DOUBLE));
    }

    @Test
    void testParseMissingDoubleColumnThrowsNpe() {
        Assertions.assertThrows(NullPointerException.class, () -> MISSING_COLUMNS.parseDouble(TestCSVColumn.DOUBLE));
    }

    @Test
    void testParseMissingDoubleColumnReturnsNullIfAllowed() {
        Assertions.assertNull(MISSING_COLUMNS.parseDouble(TestCSVColumn.ALLOW_MISSING_COLUMN));
    }

    @Test
    void testParseBigDecimal() {
        Assertions.assertEquals(new BigDecimal("3.0"), FULL_ROW.parseBigDecimal(TestCSVColumn.BIG_DECIMAL));
    }

    @Test
    void testParseMissingBigDecimal() {
        Assertions.assertNull(EMPTY_ROW.parseBigDecimal(TestCSVColumn.BIG_DECIMAL));
    }

    @Test
    void testParseIncorrectBigDecimal() {
        Assertions.assertThrows(NumberFormatException.class, () -> INCORRECT_ROW.parseBigDecimal(TestCSVColumn.BIG_DECIMAL));
    }

    @Test
    void testParseMissingBigDecimalColumnThrowsNpe() {
        Assertions.assertThrows(NullPointerException.class, () -> MISSING_COLUMNS.parseBigDecimal(TestCSVColumn.DOUBLE));
    }

    @Test
    void testParseMissingBigDecimalColumnReturnsNullIfAllowed() {
        Assertions.assertNull(MISSING_COLUMNS.parseBigDecimal(TestCSVColumn.ALLOW_MISSING_COLUMN));
    }

    @Test
    void testParseTrueBoolean() {
        CSVRow<TestCSVColumn> mixedCaseTrue = CSVRowBuilder.create(TestCSVColumn.class)
                .withValue(TestCSVColumn.BOOLEAN, "tRue")
                .build();
        CSVRow<TestCSVColumn> numberRepresentationTrue = CSVRowBuilder.create(TestCSVColumn.class)
                .withValue(TestCSVColumn.BOOLEAN, 1)
                .build();
        Assertions.assertTrue(mixedCaseTrue.parseBoolean(TestCSVColumn.BOOLEAN));
        Assertions.assertTrue(numberRepresentationTrue.parseBoolean(TestCSVColumn.BOOLEAN));
    }

    @Test
    void testParseFalseBoolean() {
        CSVRow<TestCSVColumn> mixedCaseFalse = CSVRowBuilder.create(TestCSVColumn.class)
                .withValue(TestCSVColumn.BOOLEAN, "FAlsE")
                .build();
        CSVRow<TestCSVColumn> numberRepresentationFalse = CSVRowBuilder.create(TestCSVColumn.class)
                .withValue(TestCSVColumn.BOOLEAN, 0)
                .build();
        Assertions.assertFalse(mixedCaseFalse.parseBoolean(TestCSVColumn.BOOLEAN));
        Assertions.assertFalse(numberRepresentationFalse.parseBoolean(TestCSVColumn.BOOLEAN));
    }

    @Test
    void testParseMissingBoolean() {
        Assertions.assertNull(EMPTY_ROW.parseBoolean(TestCSVColumn.BOOLEAN));
    }

    @Test
    void testParseIncorrectBoolean() {
        Assertions.assertThrows(NumberFormatException.class, () -> INCORRECT_ROW.parseBoolean(TestCSVColumn.BOOLEAN));
    }

    @Test
    void testParseMissingBooleanColumnThrowsNpe() {
        Assertions.assertThrows(NullPointerException.class, () -> MISSING_COLUMNS.parseBoolean(TestCSVColumn.DOUBLE));
    }

    @Test
    void testParseMissingBooleanColumnReturnsNullIfAllowed() {
        Assertions.assertNull(MISSING_COLUMNS.parseBoolean(TestCSVColumn.ALLOW_MISSING_COLUMN));
    }

    @Test
    void testHasEntry() {
        Assertions.assertTrue(FULL_ROW.hasEntry(TestCSVColumn.DOUBLE));
        Assertions.assertFalse(FULL_ROW.hasEntry(TestCSVColumn.DO_NOT_ALLOW_MISSING_VALUE));
    }

    @Test
    void testParseNullValueInNonNullableColumn() {
        Assertions.assertThrows(
                IllegalStateException.class,
                () -> INCORRECT_ROW.parse(TestCSVColumn.DO_NOT_ALLOW_MISSING_VALUE),
                "Setting Non-Nullable column MISSING to null did not cause an exception");
    }

    @Test
    void testAcceptMissingColumnEvenIfMissingValueIsNotAccepted() {
        Assertions.assertThrows(
                IllegalStateException.class,
                () -> INCORRECT_ROW.parse(TestCSVColumn.DO_NOT_ALLOW_MISSING_VALUE),
                "Setting Non-Nullable column MISSING to null did not cause an exception");
    }

    @Test
    void testParseEnum() {
        Assertions.assertEquals(ExampleEnum.EXAMPLE, FULL_ROW.parseEnum(TestCSVColumn.ENUM, ExampleEnum.class));
    }

    @Test
    void testParseMissingEnum() {
        Assertions.assertNull(EMPTY_ROW.parseEnum(TestCSVColumn.ENUM, ExampleEnum.class));
    }

    @Test
    void testParseIncorrectEnum() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                INCORRECT_ROW.parseEnum(TestCSVColumn.ENUM, ExampleEnum.class));
    }

    @Test
    void testParseMissingEnumColumnThrowsNpe() {
        Assertions.assertThrows(NullPointerException.class, () -> MISSING_COLUMNS.parseEnum(TestCSVColumn.DOUBLE, ExampleEnum.class));
    }

    @Test
    void testParseMissingEnumColumnReturnsNullIfAllowed() {
        Assertions.assertNull(MISSING_COLUMNS.parseEnum(TestCSVColumn.ALLOW_MISSING_COLUMN, ExampleEnum.class));
    }
}