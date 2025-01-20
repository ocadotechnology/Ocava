/*
 * Copyright © 2017-2025 Ocado (Ocava)
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

import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.tableio.TableLine;
import com.ocadotechnology.tableio.WritableToTable;

public class TestWritableToTable implements WritableToTable {
    private final ImmutableList<ImmutableMap<String, String>> lines;
    private final ImmutableSet<String> stringColumns;

    public TestWritableToTable(
            ImmutableList<ImmutableMap<String, String>> lines,
            ImmutableSet<String> stringColumns) {
        this.lines = lines;
        this.stringColumns = stringColumns;
    }

    @Override
    public Stream<TableLine> streamLines() {
        return lines.stream()
                .map(line -> new TableLine(line, stringColumns));
    }

    public ImmutableList<ImmutableMap<String, String>> getLines() {
        return lines;
    }
}