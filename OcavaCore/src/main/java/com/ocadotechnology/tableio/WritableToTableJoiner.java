/*
 * Copyright © 2017-2024 Ocado (Ocava)
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
package com.ocadotechnology.tableio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

/**
 * This class is used to return a stream of each {@link TableLine} from a collection of {@link WritableToTable}s.
 */
public class WritableToTableJoiner implements WritableToTable {
    private final List<WritableToTable> writeables;

    /**
     * Creates a new WritableToTableJoiner with a collection of {@link WritableToTable}.
     * @param writables the collection of WritableToTables.
     */
    public WritableToTableJoiner(Collection<WritableToTable> writables) {
        this.writeables = new ArrayList<>(writables);
    }

    /**
     * Creates a new WritableToTableJoiner with an empty collection of {@link WritableToTable}.
     */
    public WritableToTableJoiner() {
        this(ImmutableList.of());
    }

    /**
     * Adds a new {@link WritableToTable} to the collection.
     * @param writeable the WritableToTable to add.
     */
    public void add(WritableToTable writeable) {
        writeables.add(writeable);
    }

    /**
     * Returns a stream of the {@link TableLine}s held in each {@link WritableToTable}.
     * @return the stream of TableLines.
     */
    @Override
    public Stream<TableLine> streamLines() {
        return writeables.stream()
                .flatMap(WritableToTable::streamLines);
    }
}