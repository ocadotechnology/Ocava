/*
 * Copyright © 2017-2019 Ocado (Ocava)
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

import com.ocadotechnology.tableio.ExampleEnum;

class CSVRowTest {
    private final CSVRow<TestCSVColumn> FULL_ROW = CSVRowBuilder.create(TestCSVColumn.class)
            .withValue(TestCSVColumn.INTEGER, 1)
            .withValue(TestCSVColumn.DOUBLE, 2d)
            .withValue(TestCSVColumn.BIG_DECIMAL, 3d)
            .withValue(TestCSVColumn.BOOLEAN, "True")
            .withValue(TestCSVColumn.ENUM, "EXAMPLE")
            .build();
    private final CSVRow<TestCSVColumn> EMPTY_ROW = CSVRowBuilder.create(TestCSVColumn.class)
            .withValue(TestCSVColumn.INTEGER, "")
            .withValue(TestCSVColumn.DOUBLE, "")
            .withValue(TestCSVColumn.BIG_DECIMAL, "")
            .withValue(TestCSVColumn.BOOLEAN, "")
            .withValue(TestCSVColumn.ENUM, "")
            .build();
    private final CSVRow<TestCSVColumn> INCORRECT_ROW = CSVRowBuilder.create(TestCSVColumn.class)
            .withValue(TestCSVColumn.INTEGER, 1d)
            .withValue(TestCSVColumn.DOUBLE, "NULL")
            .withValue(TestCSVColumn.BIG_DECIMAL, "NULL")
            .withValue(TestCSVColumn.BOOLEAN, "NULL")
            .withValue(TestCSVColumn.ENUM, "NOTAVALUE")
            .withValue(TestCSVColumn.MISSING, "")
            .build();

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
    void testHasEntry() {
        Assertions.assertTrue(FULL_ROW.hasEntry(TestCSVColumn.DOUBLE));
        Assertions.assertFalse(FULL_ROW.hasEntry(TestCSVColumn.MISSING));
    }

    @Test
    void testParseNotNullableNullColumn() {
        Assertions.assertThrows(
                IllegalStateException.class,
                () -> INCORRECT_ROW.parse(TestCSVColumn.MISSING),
                "Setting Not-Nullable column MISSING to null did not cause an exception");
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
}