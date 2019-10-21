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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.tableio.TableReader;

public class SQLReader {
    private static final Logger logger = LoggerFactory.getLogger(SQLReader.class);

    /**
     * This function first creates a {@link SQLiteConnection} for the required database file. Then after executing a query to
     * retrieve all elements from the desired table of the database consumes each row individually using the {@link TableReader}
     *
     * If any {@link SQLException} is thrown an error will be logged.
     * @param tableReader the tableReader which is used to read the results row by row
     * @param fileName    the name of the file to read from
     * @param tableName   the name of the table to read from
     */
    public void read(TableReader tableReader, String fileName, String tableName) {
        try (SQLiteConnection conn = SQLiteConnection.create(fileName)) {
            ResultConsumer rowConsumer = new ResultConsumer(tableReader, tableName);

            conn.consumeTable(tableName, rowConsumer);
        } catch (SQLException e) {
            logger.error("Failed to read SQL from table: " + tableName, e);
        }
        tableReader.fileFinished();
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
         * If the {@link ResultSet} throws an {@link SQLException} this is caught and logged.
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
                logger.error("Failed to read line from SQL table: " + tableName, e);
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
            ImmutableMap.Builder lineByHeaderBuilder = ImmutableMap.<String, String>builder();

            for (String columnName : columnNames) {
                String columnValue = Objects.toString(resultSet.getString(columnName), "");
                lineByHeaderBuilder.put(columnName, columnValue);
            }

            return lineByHeaderBuilder.build();
        }
    }
}