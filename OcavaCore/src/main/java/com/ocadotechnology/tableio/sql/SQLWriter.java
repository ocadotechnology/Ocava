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

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.tableio.WritableToTable;

public class SQLWriter {
    private static final Logger logger = LoggerFactory.getLogger(SQLWriter.class);

    /**
     * First this function checks if the {@link WritableToTable} supplier has any headers to write to the file. If no headers are found
     * an error is logged and the function ends. If the supplier does have headers an {@link SQLiteConnection} is created to the desired file.
     * then the data from the supplier is written to the chosen table.
     * If an {@link SQLException} is thrown the error is logged.
     *
     * @param supplier  the supplier which holds the data to write. This includes the table headers and the individual row data.
     * @param fileName  the file the data should be written to.
     * @param tableName the table the data should be written to
     */
    public void write(WritableToTable supplier, String fileName, String tableName) {
        ImmutableSet<String> header = supplier.getHeaders();
        if (header.isEmpty()) {
            logger.info("Nothing to write to " + fileName);
            return;
        }
        writeFile(header, supplier, fileName, tableName);
    }

    private void writeFile(ImmutableSet<String> header, WritableToTable supplier, String fileName, String tableName) {
        try (SQLiteConnection conn = SQLiteConnection.create(fileName)) {
            conn.createTable(tableName, header);

            conn.insertEntries(tableName, supplier.streamLines());
        } catch (SQLException e) {
            logger.error("Failed to write SQl to table: " + tableName, e);
        }
    }
}