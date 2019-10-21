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
package com.ocadotechnology.tableio.sql;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.tableio.TableLine;
import com.ocadotechnology.tableio.TableLineBuilder;
import com.ocadotechnology.tableio.WritableToTable;

class SQLWriterTest {
    private static final File FILE = new File("src/test/SQLWriterTest.db");

    @AfterEach
    void cleanup() throws IOException {
        Files.delete(FILE.toPath());
    }

    @Test
    void writeSimpleFile() {
        String tableName = "test";
        int numberOfDataPoints = 10;

        TestLineSupplier lineSupplier = new TestLineSupplier(numberOfDataPoints, i -> i * i, false);

        new SQLWriter().write(lineSupplier, FILE.getAbsolutePath(), tableName);

        try (SQLiteConnection conn = SQLiteConnection.create(FILE.getAbsolutePath())) {
            SQLiteChecker checker = new SQLiteChecker(conn);

            checker.getNumberOfRowsInTable(numberOfDataPoints, tableName);

            checkQuantity(conn, tableName, 5, 5 * 5);
        } catch (SQLException e) {
            Assertions.fail("Failed to read file\n" + e);
        }
    }

    @Test
    void testWritingMultipleTables() {
        String tableName_1 = "test_1";
        String tableName_2 = "test_2";

        TestLineSupplier lineSupplier_1 = new TestLineSupplier(10, i -> i, false);
        TestLineSupplier lineSupplier_2 = new TestLineSupplier(15, i -> i * i, false);

        new SQLWriter().write(lineSupplier_1, FILE.getAbsolutePath(), tableName_1);
        new SQLWriter().write(lineSupplier_2, FILE.getAbsolutePath(), tableName_2);

        try (SQLiteConnection conn = SQLiteConnection.create(FILE.getAbsolutePath())) {
            checkQuantity(conn, tableName_1, 5, 5);
            checkQuantity(conn, tableName_2, 12, 12 * 12);
        } catch (SQLException e) {
            Assertions.fail("Failed to read file\n" + e);
        }
    }

    @Test
    void testWritingStringEntries() {
        String tableName = "string_test";
        int numberOfDataPoints = 10;

        TestLineSupplier lineSupplier = new TestLineSupplier(numberOfDataPoints, i -> i, true);

        new SQLWriter().write(lineSupplier, FILE.getAbsolutePath(), tableName);

        try (SQLiteConnection conn = SQLiteConnection.create(FILE.getAbsolutePath())) {
            checkQuantity(conn, tableName, 1, 1);
        } catch (SQLException e) {
            Assertions.fail("Failed to read file\n" + e);
        }
    }

    private void checkQuantity(SQLiteConnection conn, String tableName, int line, int expectedQuantity) throws SQLException {
        SQLiteChecker checker = new SQLiteChecker(conn);

        ImmutableMap<String, Object> expectedRow = ImmutableMap.<String, Object>of(
                "id", line,
                "quantity", expectedQuantity);

        checker.queryAndCheckResultLine(
                expectedRow,
                tableName,
                "id == " + Integer.toString(line));
    }

    private class TestLineSupplier implements WritableToTable {
        private final int numberOfDataPoints;
        private final Function<Integer, Integer> quantityCalculator;
        private final boolean useString;

        private TestLineSupplier(int numberOfDataPoints, Function<Integer, Integer> quantityCalculator, boolean useString) {
            this.numberOfDataPoints = numberOfDataPoints;
            this.quantityCalculator = quantityCalculator;
            this.useString = useString;
        }

        @Override
        public Stream<TableLine> streamLines() {
            return IntStream.range(0, numberOfDataPoints)
                    .boxed()
                    .map(this::getLine);
        }

        private TableLine getLine(int i) {
            TableLineBuilder line = new TableLineBuilder().withInt("id", i);

            int value = quantityCalculator.apply(i);

            if (useString) {
                line.withString("quantity", Integer.toString(value));
            } else {
                line.withInt("quantity", value);
            }

            return line.build();
        }
    }
}