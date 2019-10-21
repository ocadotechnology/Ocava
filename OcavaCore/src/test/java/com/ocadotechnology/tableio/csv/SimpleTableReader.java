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

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import com.ocadotechnology.tableio.TableReader;

public class SimpleTableReader implements TableReader {
    private ImmutableList<String> header = ImmutableList.of();
    private Table<Integer, String, String> table;

    private int linesRead = 0;

    public void consumeHeader(ImmutableList<String> header) {
        this.header = header;
        this.table = HashBasedTable.create();
    }

    public void consumeLine(ImmutableMap<String, String> lineByHeader) {
        Preconditions.checkState(!header.isEmpty(), "Header not read yet or file empty");
        lineByHeader.forEach((colHeader, value) -> table.put(linesRead, colHeader, value));
        linesRead++;
    }

    public ImmutableMap<String, String> getRow(int rowIndex) {
        return ImmutableMap.copyOf(table.row(rowIndex));
    }
}