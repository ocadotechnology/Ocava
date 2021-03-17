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
package com.ocadotechnology.tableio;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * This interface marks a class which will be used to read data from a file.
 */
public interface TableReader {
    /**
     * This function consumes a set of headers. By default this method does nothing.
     *
     * @param header a set of the headers to consume.
     */
    default void consumeHeader(ImmutableList<String> header) {
    }

    /**
     * This function is used to consume an individual line of a file.
     *
     * @param lineByHeader the line data of the file. The keys represent the headers of the file and the values represent
     *                     the specific data for that row.
     */
    void consumeLine(ImmutableMap<String, String> lineByHeader);

    /**
     * This function is called when the file the TableReader is being used to read finishes.
     */
    default void fileFinished() {
    }
}