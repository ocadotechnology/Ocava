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
package com.ocadotechnology.tableio.sql;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.tableio.TableLine;
import com.ocadotechnology.tableio.TableLineBuilder;

class SQLiteConnectionTest {
    private static final File FILE = new File("src/test/SQLiteConnectionTest.db");

    private SQLiteConnection conn;
    private SQLiteChecker checker;

    @BeforeEach
    void setup() throws SQLException {
        conn = SQLiteConnection.create(FILE);
        checker = new SQLiteChecker(conn);
    }

    @AfterEach
    void cleanup() throws IOException, SQLException {
        if (conn != null) {
            conn.close();
        }

        Files.delete(FILE.toPath());
    }

    @Test
    void testCreatingTable() throws SQLException {
        String tableName = "test_table";

        boolean tableExists = conn.tableExists(tableName);

        Assertions.assertFalse(tableExists);

        conn.createTable(tableName, ImmutableSet.of("id", "quantity"));

        tableExists = conn.tableExists(tableName);

        Assertions.assertTrue(tableExists);
    }

    @Test
    void testInsertingEntries() throws SQLException {
        String tableName = "test_table";

        conn.createTable(tableName, ImmutableSet.of("id", "quantity"));

        conn.insertEntry(tableName, getLine(1, 100));
        conn.insertEntry(tableName, getLine(2, 50));

        checker.queryAndCheckResultLine(
                ImmutableMap.of(
                        "id", 1,
                        "quantity", 100),
                tableName,
                "id == 1");

        checker.getNumberOfRowsInTable(2, tableName);
    }

    @Test
    void testInsertingBatchedEntries() throws SQLException {
        String tableName = "test_table";

        conn.createTable(tableName, ImmutableSet.of("id", "quantity"));

        conn.insertEntries(tableName, Stream.of(
                getLine(1, 100),
                getLine(2, 50)));

        checker.queryAndCheckResultLine(
                ImmutableMap.of(
                        "id", 1,
                        "quantity", 100),
                tableName,
                "id == 1");

        checker.getNumberOfRowsInTable(2, tableName);
    }

    @Test
    void testReadingTable() throws SQLException {
        String tableName = "test_table";

        conn.createTable(tableName, ImmutableSet.of("id", "quantity"));

        conn.insertEntries(tableName, Stream.of(
                getLine(1, 100),
                getLine(2, 100)));

        Consumer<ResultSet> lineChecker = resultSet -> checker.intChecker(
                resultSet,
                "quantity",
                100,
                "Quantity did not match what was expected");

        conn.consumeTable(tableName, resultSet -> checker.checkAll(resultSet, 2, lineChecker));
    }

    private TableLine getLine(int id, int quantity) {
        return new TableLineBuilder()
                .withInt("id", id)
                .withInt("quantity", quantity)
                .build();
    }
}