/*
 * Copyright © 2017-2020 Ocado (Ocava)
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.StringJoiner;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.tableio.TableLine;
import com.ocadotechnology.tableio.WritableToTable;

public class CSVWriter {
    private static final Logger logger = LoggerFactory.getLogger(CSVWriter.class);

    private static final String COLUMN_DELIMITER = ",";
    private static final String NEW_LINE_SEPARATOR = "\n";

    private final boolean enableCompression;
    private final boolean append;

    /**
     * @param enableCompression a boolean that is true if the files written using this CSVWriter should be compressed.
     *                          Otherwise false.
     * @param append            a boolean. If it is true then data will be written to the end of the file rather than the beginning.
     */
    public CSVWriter(boolean enableCompression, boolean append) {
        this.enableCompression = enableCompression;
        this.append = append;
    }

    /**
     * This function creates a temporary CSVWriter which then writes to the desired file using the supplier. This function
     * automatically writes data to the beginning of the file using the {@link #write(WritableToTable, String)} method.
     *
     * @param supplier          the supplier which contains both the headers and the individual row data to write to the CSV file
     * @param fileName          the file to write the CSV data to
     * @param enableCompression a boolean that is true if the file should be compressed. Otherwise false.
     */
    public static void write(WritableToTable supplier, String fileName, boolean enableCompression) {
        new CSVWriter(enableCompression, false).write(supplier, fileName);
    }

    /**
     * This function is used to write rows of data to a CSVFile.
     * This function first gets the headers of the supplier. If no headers exist then the function ends as no data can be
     * written to the file. Otherwise, the function writes rows of data to a CSVFile line by line adding a header if necessary.
     * The file is compressed if enableCompression is true. Additionally, the row data is appended to the end of the file
     * if append is true.
     * <p>
     * If any {@link IOException} is thrown the error will be logged.
     *
     * @param supplier which contains both the header data and the individual row data.
     * @param fileName the file name to write to.
     */
    public void write(WritableToTable supplier, String fileName) {
        ImmutableSet<String> headers = supplier.getHeaders();

        if (headers.isEmpty()) {
            return;
        }

        String newFileName = enableCompression ?
                fileName + ".gz" :
                fileName;

        boolean writeHeader = !pathExists(newFileName) || !append;

        try (BufferedWriter bufferedWriter = getWriter(newFileName)) {
            if (writeHeader) {
                writeLine(bufferedWriter, headers);
            }

            supplier.streamLines().forEach(tableLine ->
                    writeLine(bufferedWriter, getObjectsToWrite(headers, tableLine)));
            supplier.fileWritten();
        } catch (IOException e) {
            logger.error("Failed to write file " + fileName, e);
        }
    }

    private boolean pathExists(String fileName) {
        return new File(fileName).exists();
    }

    private ImmutableList<String> getObjectsToWrite(ImmutableSet<String> header, TableLine tableLine) {
        return header.stream()
                .map(columnHeader -> tableLine.getLineMap().get(columnHeader).replaceAll("[\",]", ""))
                .collect(ImmutableList.toImmutableList());
    }

    private void writeLine(BufferedWriter bufferedWriter, Collection<String> objects) {
        StringJoiner joiner = new StringJoiner(COLUMN_DELIMITER);
        objects.forEach(joiner::add);
        try {
            bufferedWriter.write(joiner.toString());
            bufferedWriter.write(NEW_LINE_SEPARATOR);
        } catch (IOException e) {
            logger.error("Failed to write file", e);
        }
    }

    private BufferedWriter getWriter(String fileName) throws IOException {
        return enableCompression ?
                createCompressedWriter(fileName) :
                createWriter(fileName);
    }

    private BufferedWriter createWriter(String fileName) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName, append), StandardCharsets.UTF_8));
    }

    private BufferedWriter createCompressedWriter(String fileName) throws IOException {
        return new BufferedWriter(
                new OutputStreamWriter(
                        new GZIPOutputStream(
                                new FileOutputStream(fileName, append)), StandardCharsets.UTF_8));
    }
}