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
package com.ocadotechnology.tableio.csv;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.tableio.TableLine;

class CSVWriterTest {
    private static final String FIRST_LINE = "first line";
    private static final String SECOND_LINE = "second line";
    private final Path path;
    private final Path gzippedPath;

    CSVWriterTest() throws IOException {
        this.path = getOriginalPath();
        this.gzippedPath = path.resolveSibling(path.getFileName() + ".gz");
    }

    private static class FakeTableLine extends TableLine {
        private static final String FAKE = "FAKE";
        public FakeTableLine(String value) {
            super(ImmutableMap.of(FAKE, value), ImmutableSet.of(FAKE));
        }
    }

    private Stream<String> streamLines(Path path) throws IOException {
        InputStream inputStream = new GZIPInputStream(Files.newInputStream(path));
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charsets.UTF_8);
        return new BufferedReader(inputStreamReader).lines();
    }

    private Path getOriginalPath() throws IOException {
        Path testDir = Files.createTempDirectory("ocava-csv-test-");
        Path path = testDir.resolve("example.csv");
        return path;
    }

    @Test
    public void itShouldBePossibleToAppendToACSVFileThatIsGzipped() throws IOException {
        CSVWriter writer = new CSVWriter(true, true);
        writer.write(path, () -> Stream.of(new FakeTableLine(FIRST_LINE)));
        writer.write(path, () -> Stream.of(new FakeTableLine(SECOND_LINE)));
        ImmutableList<String> actual = this.streamLines(gzippedPath).collect(ImmutableList.toImmutableList());
        ImmutableList<String> expected = ImmutableList.of(
                FakeTableLine.FAKE,
                FIRST_LINE,
                SECOND_LINE
        );
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void itShouldOverwriteTheExistingFileWhenAppendIsSetToFalse() throws IOException {
        CSVWriter writer = new CSVWriter(false, false);
        writer.write(path, () -> Stream.of(new FakeTableLine(FIRST_LINE)));
        writer.write(path, () -> Stream.of(new FakeTableLine(SECOND_LINE)));
        List<String> actual = Files.readAllLines(path);
        List<String> expected = ImmutableList.of(FakeTableLine.FAKE, SECOND_LINE);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void itShouldAppendToTheExistingFileWhenAppendIsSetToTrue() throws IOException {
        CSVWriter writer = new CSVWriter(false, true);
        writer.write(path, () -> Stream.of(new FakeTableLine(FIRST_LINE)));
        writer.write(path, () -> Stream.of(new FakeTableLine(SECOND_LINE)));
        List<String> actual = Files.readAllLines(path);
        List<String> expected = ImmutableList.of(
                FakeTableLine.FAKE,
                FIRST_LINE,
                SECOND_LINE
        );
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void itShouldNotAddAnExtensionWhenTheFileAlreadyHasOne() throws IOException {
        CSVWriter writer = new CSVWriter(true, true);
        writer.write(gzippedPath, () -> Stream.of(new FakeTableLine(FIRST_LINE)));
        ImmutableList<String> actual = this.streamLines(gzippedPath).collect(ImmutableList.toImmutableList());
        ImmutableList<String> expected = ImmutableList.of(FakeTableLine.FAKE, FIRST_LINE);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void appendsToExistingFileWhenAddingGzExtension() throws IOException {
        CSVWriter writer = new CSVWriter(true, true);
        try (
            FileOutputStream fileOutputStream = new FileOutputStream(gzippedPath.toFile(), true);
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(gzipOutputStream, StandardCharsets.UTF_8)) {

            outputStreamWriter.write(FakeTableLine.FAKE + "\n" + FIRST_LINE + "\n");
        }
        writer.write(path, () -> Stream.of(new FakeTableLine(SECOND_LINE)));
        List<String> actual = this.streamLines(gzippedPath).collect(ImmutableList.toImmutableList());
        assertThat(actual).isEqualTo(ImmutableList.of(
                FakeTableLine.FAKE,
                FIRST_LINE,
                SECOND_LINE
        ));
    }
}
