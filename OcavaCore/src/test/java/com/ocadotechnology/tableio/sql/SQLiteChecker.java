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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;

import com.google.common.collect.ImmutableMap;

class SQLiteChecker {
    private final SQLiteConnection conn;

    SQLiteChecker(SQLiteConnection conn) {
        this.conn = conn;
    }

    void queryAndCheckResultLine(
            ImmutableMap<String, Object> expectedValueByColumnHeader,
            String tableName,
            String filter) throws SQLException {
        Consumer<ResultSet> resultChecker = result -> expectedValueByColumnHeader.forEach((
                        column, expectedValue) -> check(result, column, expectedValue));

        conn.executeQuery(
                resultChecker,
                "SELECT * FROM " + tableName,
                "WHERE " + filter);
    }

    void getNumberOfRowsInTable(int expectedNumberOfResults, String tableName) throws SQLException {
        Consumer<ResultSet> resultChecker = result -> intChecker(
                result,
                "count",
                expectedNumberOfResults,
                "Number of rows did not match what was expected");

        conn.executeQuery(resultChecker, "SELECT COUNT(*) AS count FROM " + tableName);
    }

    void checkAll(ResultSet resultSet, int expectedNumberOfRows, Consumer<ResultSet> lineConsumer) {
        try {
            int numRows = 0;

            while (resultSet.next()) {
                lineConsumer.accept(resultSet);
                numRows++;
            }

            Assertions.assertEquals(expectedNumberOfRows, numRows,
                    "Number of rows did not match what was expected");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to iterate over results");
        }
    }

    void intChecker(ResultSet result, String column, int expected, String failureMessage) {
        try {
            Assertions.assertEquals(expected, result.getInt(column), failureMessage);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get value from result: " + column, e);
        }
    }

    private void check(ResultSet result, String column, Object expectedValue) {
        try {
            String value = result.getString(column);

            Assertions.assertEquals(expectedValue.toString(), value,
                    column + " values does not match what was expected");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get value from result: " + column, e);
        }
    }
}