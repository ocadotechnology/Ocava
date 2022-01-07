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

import java.nio.file.Path;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.tableio.WritableToTable;

public class SQLWriter {
    private static final Logger logger = LoggerFactory.getLogger(SQLWriter.class);

    /**
     * First this function checks if the {@link WritableToTable} supplier has any headers to write to the db file at the {@link Path}. If no headers are found
     * an error is logged and the function ends. If the supplier does have headers an {@link SQLiteConnection} is created to the desired file.
     * then the data from the supplier is written to the chosen table.
     * <p>
     * A {@link RuntimeException} is thrown if an {@link SQLException} is caught while writing the SQL.
     *
     * @param pathToFile the path to the file the data should be written at.
     * @param supplier   the supplier which holds the data to write. This includes the table headers and the individual row data.
     * @param tableName  the table to write to.
     */
    public void write(Path pathToFile, WritableToTable supplier, String tableName) {
        ImmutableSet<String> header = supplier.getHeaders();
        if (header.isEmpty()) {
            logger.info("Nothing to write to " + pathToFile);
            return;
        }
        writeFile(header, supplier, pathToFile, tableName);
    }

    private void writeFile(ImmutableSet<String> header, WritableToTable supplier, Path pathToFile, String tableName) {
        try (SQLiteConnection conn = SQLiteConnection.create(pathToFile)) {
            conn.createTable(tableName, header);

            conn.insertEntries(tableName, supplier.streamLines());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to write SQl to table: " + tableName, e);
        }
    }
}