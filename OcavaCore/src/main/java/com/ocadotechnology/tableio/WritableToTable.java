/*
 * Copyright © 2017-2023 Ocado (Ocava)
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

import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.utils.ImmutableMapFactory;

@FunctionalInterface
public interface WritableToTable {
    Stream<TableLine> streamLines();

    /**
     * This function returns a stream of {@link ImmutableMap}'s which each represent a single row of data. By default
     * The keys of these ImmutableMaps are the headers of the row and
     * the values are the individual field values corresponding to those headers.
     *
     * @return the stream of the ImmutableMap.
     */
    default Stream<ImmutableMap<String, String>> streamLinesEntriesByHeader() {
        return streamLines()
                .map(line -> ImmutableMapFactory.createWithNewValues(line.getLineMap(), Object::toString));
    }

    /**
     * This function returns an {@link ImmutableList} of {@link ImmutableMap}s which each represent a single row of data.
     * By default the keys of these ImmutableMaps are the headers of the row and
     * the values are the individual field values corresponding to those headers.
     *
     * @return an ImmutableList of ImmutableMaps.
     */
    default ImmutableList<ImmutableMap<String, String>> linesEntriesByHeader() {
        return streamLinesEntriesByHeader()
                .collect(ImmutableList.toImmutableList());
    }

    /**
     * This function is used to perform an action when a file is finished being written.
     */
    default void fileWritten() {
    }

    /**
     * This function returns a set of the headers of each {@link TableLine}.
     *
     * @return the set of the headers.
     */
    default ImmutableSet<String> getHeaders() {
        return streamLines()
                .map(TableLine::getLineMap)
                .map(ImmutableMap::keySet)
                .findFirst()
                .orElse(ImmutableSet.of());
    }
}