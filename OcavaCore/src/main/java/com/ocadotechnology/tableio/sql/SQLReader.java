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
package com.ocadotechnology.tableio.sql;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.tableio.TableReader;

public class SQLReader {
    /**
     * This function first creates a {@link SQLiteConnection} for the database at the given {@link Path}. Then after executing a query to
     * retrieve all elements from the desired table of the database consumes each row individually using the {@link TableReader}
     * <p>
     * A {@link RuntimeException} is thrown if an {@link SQLException} is caught while reading the SQL.
     *
     * @param pathToFile   the path to the db file to read from.
     * @param lineConsumer the tableReader which is used to read the results row by row.
     * @param tableName    the table to read from.
     */
    public void read(Path pathToFile, TableReader lineConsumer, String tableName) {
        try (SQLiteConnection conn = SQLiteConnection.create(pathToFile)) {
            ResultConsumer rowConsumer = new ResultConsumer(lineConsumer, tableName);

            conn.consumeTable(tableName, rowConsumer);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read SQL from table: " + tableName, e);
        }
        lineConsumer.fileFinished();
    }

    private static class ResultConsumer implements Consumer<ResultSet> {
        private final TableReader tableReader;
        private final String tableName;

        private ResultConsumer(TableReader tableReader, String tableName) {
            this.tableReader = tableReader;
            this.tableName = tableName;
        }

        /**
         * Using the {@link TableReader} provided to the {@link SQLReader} first calls the {@link TableReader#consumeHeader(ImmutableList)}
         * on the columns of the resultSet. Following that each line is individually consumed by the {@link TableReader#consumeLine(ImmutableMap)}.
         * <p>
         * A {@link RuntimeException} if the {@link ResultSet} throws an {@link SQLException} while reading one of the lines.
         *
         * @param resultSet The results read from the sql database
         */
        @Override
        public void accept(ResultSet resultSet) {
            try {
                ImmutableList<String> columnNames = getColumnNames(resultSet);
                tableReader.consumeHeader(columnNames);

                while (resultSet.next()) {
                    ImmutableMap<String, String> lineByHeader = getLineByHeader(columnNames, resultSet);
                    tableReader.consumeLine(lineByHeader);
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to read line from SQL table: " + tableName, e);
            }
        }

        private ImmutableList<String> getColumnNames(ResultSet resultSet) throws SQLException {
            ResultSetMetaData metaData = resultSet.getMetaData();

            ImmutableList.Builder<String> columnNamesBuilder = ImmutableList.builder();

            // Note the column count starts from one.
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                columnNamesBuilder.add(metaData.getColumnName(i).toUpperCase());
            }

            return columnNamesBuilder.build();
        }

        private ImmutableMap<String, String> getLineByHeader(
                ImmutableList<String> columnNames,
                ResultSet resultSet) throws SQLException {
            ImmutableMap.Builder<String, String> lineByHeaderBuilder = ImmutableMap.builder();

            for (String columnName : columnNames) {
                String columnValue = Objects.toString(resultSet.getString(columnName), "");
                lineByHeaderBuilder.put(columnName, columnValue);
            }

            return lineByHeaderBuilder.build();
        }
    }
}