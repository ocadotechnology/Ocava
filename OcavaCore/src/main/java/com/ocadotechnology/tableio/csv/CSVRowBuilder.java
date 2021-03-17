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

import com.google.common.collect.ImmutableMap;

public class CSVRowBuilder<T extends CSVColumn> {
    private ImmutableMap.Builder<String, String> lineByHeaderBuilder = ImmutableMap.builder();

    /**
     * @param clazz The class of the {@link CSVRow} the builder will make.
     * @return a CSVRowBuilder that can make CSVRow's of class T
     */
    public static <T extends CSVColumn> CSVRowBuilder<T> create(Class<T> clazz) {
        return new CSVRowBuilder<T>();
    }

    /**
     * Adds a column with a specific value to be created when the builder is built. This value represents one field of a {@link CSVRow}
     * @param columnHeader the column to add the value to
     * @param value the value for the specified column.
     * @return returns this CSVRowBuilder to allow method calls to be chained.
     */
    public CSVRowBuilder<T> withValue(T columnHeader, Object value) {
        lineByHeaderBuilder.put(columnHeader.name(), value.toString());
        return this;
    }

    /**
     * Builds and returns a {@link CSVRow} with the values supplied by the {@link #withValue(CSVColumn, Object)} method.
     * @return the CSVRow
     */
    public CSVRow<T> build() {
        return new CSVRow<>(lineByHeaderBuilder.build());
    }
}