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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.tableio.TestTableReader;

class SQLReaderTest {
    private static final Path PATH_TO_FILE = new File("src/test/SQLReaderTest.db").toPath();
    private static final String TABLE_NAME = "TABLE_NAME";
    private static final TestWritableToTable writable = setupTableToWrite();

    private final SQLReader reader = new SQLReader();

    private static TestWritableToTable setupTableToWrite() {
        ImmutableList<ImmutableMap<String, String>> contents = ImmutableList.of(
                ImmutableMap.of("ID", "1", "QUANTITY", "15", "RETAILER", "OCADO"),
                ImmutableMap.of("ID", "2", "QUANTITY", "45", "RETAILER", "OTHER"));

        return new TestWritableToTable(contents, ImmutableSet.of("RETAILER"));
    }

    @BeforeEach
    void setup() {
        SQLWriter sqlWriter = new SQLWriter();

        sqlWriter.write(PATH_TO_FILE, writable, TABLE_NAME);
    }

    @AfterEach
    void cleanup() throws IOException {
        Files.delete(PATH_TO_FILE);
    }

    @Test
    void testReadingFile() {
        TestTableReader tableReader = new TestTableReader();

        reader.read(PATH_TO_FILE, tableReader, TABLE_NAME);

        Assertions.assertEquals(writable.getLines(), tableReader.getLines());
    }

    @Test
    void testFileFinishedIsCalled() {
        TestTableReader tableReader = new TestTableReader();
        reader.read(PATH_TO_FILE, tableReader, TABLE_NAME);

        Assertions.assertTrue(tableReader.hasFileFinishedBeenCalled());
    }

}