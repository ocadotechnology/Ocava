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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.tableio.TableReader;

public class CSVReader {
    private static final Logger logger = LoggerFactory.getLogger(CSVReader.class);

    public static final String COLUMN_DELIMITER = ",";

    /**
     * This function first uses the lineConsumer parameter to consume the headings. Following that it consumes
     * each line in the file one by one using lineConsumer.
     * This method will throw an {@link IllegalStateException} if the number of columns in an row does not match the size of the
     * number of headers.
     * This method will log any {@link IOException}'s if the file is unsuccessfully read.
     *
     * @param reader       the BufferedReader to read from.
     * @param lineConsumer the tableReader which consumes each line in the file individually.
     */
    public void read(BufferedReader reader, TableReader lineConsumer) {
        try {
            String line = reader.readLine();
            if (line == null) {
                return;
            }
            ImmutableList<String> header = Arrays.stream(line.split(COLUMN_DELIMITER, -1))
                    .collect(ImmutableList.toImmutableList());
            int headerLength = header.size();
            lineConsumer.consumeHeader(header);
            line = reader.readLine();
            while (line != null) {
                if (!line.isEmpty()) {
                    String[] splitLine = line.split(COLUMN_DELIMITER, -1);
                    Preconditions.checkState(splitLine.length == headerLength,
                            String.format("Line incompatible with header.%n Line: %s%n Header: %s", Arrays.toString(splitLine), header));
                    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
                    for (int i = 0; i < headerLength; i++) {
                        builder.put(header.get(i), splitLine[i]);
                    }
                    lineConsumer.consumeLine(builder.build());
                }
                line = reader.readLine();
            }
            lineConsumer.fileFinished();
            reader.close();
        } catch (IOException e) {
            logger.error("Failed to read file", e);
        }
    }

    /**
     * Reads a file by first converting it into a {@link BufferedReader} and then calling
     * {@link #read(BufferedReader, TableReader)}. If an {@link IOException} is thrown the error message is logged.
     *
     * @param file         the file to read from
     * @param lineConsumer the tableReader which consumes each line in the file individually.
     */
    public void read(File file, TableReader lineConsumer) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            read(reader, lineConsumer);
        } catch (IOException e) {
            logger.error("Failed to create reader for specified file", e);
        }
    }
}