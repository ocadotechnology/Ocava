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

import java.io.File;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.tableio.TestTableReader;

public class CSVReaderTest {
    @Test
    void testReadCSVFile() {
        SimpleTableReader consumer = new SimpleTableReader();
        CSVReader reader = new CSVReader();

        File testFile = new File("src/test/test.csv");
        reader.read(testFile, consumer);

        ImmutableMap<String, String> line0 = ImmutableMap.<String, String>builder()
                .put("THIS", "1")
                .put("IS", "2")
                .put("A", "3")
                .put("TEST", "4")
                .build();
        ImmutableMap<String, String> line1 = ImmutableMap.<String, String>builder()
                .put("THIS", "5")
                .put("IS", "6")
                .put("A", "7")
                .put("TEST", "8")
                .build();

        Assertions.assertEquals(line0, consumer.getRow(0));
        Assertions.assertEquals(line1, consumer.getRow(1));
    }

    @Test
    void testFileFinishedIsCalled() {
        TestTableReader tableReader = new TestTableReader();
        new CSVReader().read(new File("src/test/test.csv"), tableReader);

        Assertions.assertTrue(tableReader.hasFileFinishedBeenCalled());
    }
}