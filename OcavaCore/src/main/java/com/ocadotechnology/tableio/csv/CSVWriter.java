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
package com.ocadotechnology.tableio.csv;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.StringJoiner;
import java.util.zip.GZIPOutputStream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.tableio.TableLine;
import com.ocadotechnology.tableio.WritableToTable;

public class CSVWriter {
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
     * automatically writes data to the beginning of the file using the {@link #write(Path, WritableToTable)} method.
     *
     * @param pathToFile        the path to the file to write to.
     * @param supplier          the supplier which contains both the headers and the individual row data to write to the CSV file
     * @param enableCompression a boolean that is true if the file should be compressed. Otherwise false.
     */
    public static void write(Path pathToFile, WritableToTable supplier, boolean enableCompression) {
        new CSVWriter(enableCompression, false).write(pathToFile, supplier);
    }

    /**
     * This function is used to write rows of data to a file represented by a {@link Path}.
     * This function first gets the headers of the supplier. If no headers exist then the function ends as no data can be
     * written to the file. Otherwise, the function writes rows of data to a CSVFile line by line adding a header if necessary.
     * The file is compressed if enableCompression is true. Additionally, the row data is appended to the end of the file
     * if append is true.
     * <p>
     * A {@link RuntimeException} is thrown if any {@link IOException} is caught writing to the file.
     *
     * @param supplier   which contains both the header data and the individual row data.
     * @param filePath the path to the file to write to.
     */
    public void write(Path filePath, WritableToTable supplier) {
        ImmutableSet<String> headers = supplier.getHeaders();

        if (headers.isEmpty()) {
            return;
        }

        if (shouldAppendExtension(filePath)) {
            filePath = filePath.resolveSibling(filePath.getFileName() + ".gz");
        }

        boolean writeHeader = !Files.exists(filePath) || !append;

        try (BufferedWriter bufferedWriter = getWriter(filePath)) {
            if (writeHeader) {
                writeLine(bufferedWriter, headers);
            }

            supplier.streamLines().forEach(tableLine ->
                    writeLine(bufferedWriter, getObjectsToWrite(headers, tableLine)));
            supplier.fileWritten();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file " + filePath, e);
        }
    }

    private boolean shouldAppendExtension(Path pathToFile) {
        return enableCompression && !pathToFile.getFileSystem().getPathMatcher("glob:*.gz").matches(pathToFile.getFileName());
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
            throw new RuntimeException("Failed to write file ", e);
        }
    }

    private BufferedWriter getWriter(Path fileName) throws IOException {
        return enableCompression ?
                createCompressedWriter(fileName) :
                createWriter(fileName);
    }

    private BufferedWriter createWriter(Path fileName) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName.toFile(), append), StandardCharsets.UTF_8));
    }

    private BufferedWriter createCompressedWriter(Path fileName) throws IOException {
        return new BufferedWriter(
                new OutputStreamWriter(
                        new GZIPOutputStream(
                                new FileOutputStream(fileName.toFile(), append)), StandardCharsets.UTF_8));
    }
}