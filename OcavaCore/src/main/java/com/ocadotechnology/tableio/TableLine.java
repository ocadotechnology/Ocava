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
package com.ocadotechnology.tableio;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.utils.ImmutableMapFactory;

public class TableLine {
    private final ImmutableMap<String, String> lineMap;
    private final ImmutableSet<String> stringColumns;

    public TableLine(ImmutableMap<String, String> lineMap, ImmutableSet<String> stringColumns) {
        this.lineMap = lineMap;
        this.stringColumns = stringColumns;
    }

    /**
     * Returns a {@link ImmutableMap} of columns and their values.
     *
     * @return the map.
     */
    public ImmutableMap<String, String> getLineMap() {
        return lineMap;
    }

    /**
     * Create a new {@link ImmutableMap}. In this ImmutableMap the elements of each Column whose values are
     * meant to represent a String have quotation marks added.
     *
     * @return the new ImmutableMap.
     */
    public ImmutableMap<String, String> getLineMapWithStringsQuoted() {
        return ImmutableMapFactory.createWithNewValues(
                getLineMap(),
                this::quoteStringIfNecessary);
    }

    private String quoteStringIfNecessary(String columnHeader, String value) {
        if (stringColumns.contains(columnHeader)) {
            return "\"" + value + "\"";
        } else {
            return value;
        }
    }
}