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
package com.ocadotechnology.utils;

import java.util.function.Function;

import com.google.common.collect.Table;
import com.ocadotechnology.function.TriConsumer;

public class TableUtils {
    /**
     * Copies all cells from the first {@link Table} to the second {@link Table},
     * applying the valueMapper to the values.
     * Used for optimisation reasons where the outputTable is of a known size.
     */
    public static <R, C, V, W> void putAll(
            Table<? extends R, ? extends C, V> baseTable,
            Table<R, C, W> outputTable,
            Function<? super V, ? extends W> valueMapper) {
        forEach(baseTable, (r, c, v) -> outputTable.put(r, c, valueMapper.apply(v)));
    }

    /**
     * This function checks if the {@link Table} is full.
     *
     * @param table the table to check
     * @return a boolean - the result is true if each row/column pair has a value and false if they do not.
     */
    public static <R, C, W> boolean isFull(Table<R, C, W> table) {
        for (R row : table.rowKeySet()) {
            for (C column : table.columnKeySet()) {
                if (table.get(row, column) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * This function performs an action on each value in a {@link Table} using a {@link TriConsumer}.
     *
     * @param table  the table
     * @param action the action to perform on each value.
     */
    public static <R, C, V> void forEach(
            Table<R, C, V> table,
            TriConsumer<? super R, ? super C, ? super V> action) {
        table.cellSet().forEach(
                cell -> action.accept(cell.getRowKey(), cell.getColumnKey(), cell.getValue())
        );
    }

    /**
     * This function updates each value in a {@link Table} using a {@link Function}.
     *
     * @param table   the table to update
     * @param updater the function to update each value present in the table.
     */
    public static <R, C, V> void update(
            Table<R, C, V> table,
            Function<V, V> updater) {
        forEach(
                table,
                (rowKey, columnKey, oldValue) -> {
                    V newValue = updater.apply(oldValue);
                    table.put(rowKey, columnKey, newValue);
                }
        );
    }

    /**
     * This function tries to return a value from a table. If the value does not exist it returns a specified default value.
     *
     * @param table        the table to return the value from.
     * @param row          the row of the table to check for the value.
     * @param column       the column of the table to check for the value.
     * @param defaultValue the default value to return if the table does not contain a value for the row/column pair.
     * @return either the value found in the table or the default value.
     */
    public static <R, C, V> V getOrDefault(
            Table<R, C, V> table,
            R row,
            C column,
            V defaultValue) {
        V value = table.get(row, column);
        return value != null ? value : defaultValue;
    }
}