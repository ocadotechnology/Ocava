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
package com.ocadotechnology.utils;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

public class ImmutableTableFactory {
    /**
     * Create an {@link ImmutableTable} from a base {@link Table}.
     * The new table rows, columns and values are obtained from the old table rows, columns and values respectively.
     * <br> If multiple rows (or columns) are mapped to a same row (column) in the new table a
     * {@link IllegalArgumentException} is thrown.
     *
     * @param baseTable    the base Table used to generate the new ImmutableTable.
     * @param rowMapper    the function to generate the new rows from the old rows.
     * @param columnMapper the function to generate the new columns from the old columns.
     * @param valueMapper  the function to generate the new values from the old values.
     * @param <R>          the type of the rows of the base table.
     * @param <C>          the type of the columns of the new table.
     * @param <V>          The type of the values of the base table.
     * @param <M>          the type of the rows of the new table.
     * @param <N>          the type of the columns of the new table.
     * @param <W>          the type of the values of the new table.
     * @return a new ImmutableTable with modified rows, columns and values.
     */
    public static <R, C, V, M, N, W> ImmutableTable<M, N, W> create(
            Table<R, C, V> baseTable,
            Function<? super R, ? extends M> rowMapper,
            Function<? super C, ? extends N> columnMapper,
            Function<? super V, ? extends W> valueMapper) {
        ImmutableTable.Builder<M, N, W> mappedTableBuilder = ImmutableTable.builder();

        TableUtils.forEach(baseTable, (r, c, v) -> mappedTableBuilder.put(
                rowMapper.apply(r),
                columnMapper.apply(c),
                valueMapper.apply(v)));

        try {
            return mappedTableBuilder.build();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Multiple input cells mapped to the same ouput cell", e);
        }
    }

    /**
     * Create an {@link ImmutableTable} from a base {@link Table}, maintaining the same rows and columns and
     * changing the values. The new value is calculated from the old one.
     *
     * @param baseTable    the base Table used to generate the new ImmutableTable.
     * @param valueMapper  the function to generate the new values from the old values.
     * @param <R>          the type of the rows of both the base and new table.
     * @param <C>          the type of the columns of both the base and the new table.
     * @param <V>          The type of the values of the base table.
     * @param <W>          the type of the values of the new table.
     * @return a new ImmutableTable with modified values.
     */
    public static <R, C, V, W> ImmutableTable<R, C, W> create(
            Table<? extends R, ? extends C, V> baseTable,
            Function<? super V, ? extends W> valueMapper) {
        return create(
                baseTable,
                Function.identity(),
                Function.identity(),
                valueMapper);
    }

    /**
     * Create an {@link ImmutableTable} from a base {@link Table}, maintaining the same rows and columns and
     * recalculating the values. The new value is recalculated from the rows and columns.
     *
     * @param baseTable             the base Table used to generate the new ImmutableTable.
     * @param newValueCalculator    the function to recalculates the new values from the row and column.
     * @param <R>                   the type of the rows of both the base and new table.
     * @param <C>                   the type of the columns of both the base and the new table.
     * @param <V>                   the type of the values of the new table.
     * @return a new ImmutableTable with recalculated values.
     */
    public static <R, C, V> ImmutableTable<R, C, V> create(
            Table<? extends R, ? extends C, ?> baseTable,
            BiFunction<? super R, ? super C, ? extends V> newValueCalculator) {
        ImmutableTable.Builder<R, C, V> mappedTableBuilder = ImmutableTable.builder();

        TableUtils.forEach(baseTable, (r, c, v) -> {
            V newTableValue = newValueCalculator.apply(r, c);
            mappedTableBuilder.put(r, c, newTableValue);
        });
        return mappedTableBuilder.build();
    }

    /**
     * Create an {@link ImmutableTable} adding rows to a base {@link Map} representing columns and values.
     * All the rows in the resulting {@link ImmutableTable} will have the same values.
     *
     * @param baseMap      the base Map used as columns and values of the new ImmutableTable.
     * @param rows         the Collection used as rows of the new ImmutableTable.
     * @param <R>          the type of the rows of the new table.
     * @param <C>          the type of the columns the new table.
     * @param <V>          The type of the values of the new table.
     * @return a new ImmutableTable with rows, columns and values as given.
     */
    public static <R, C, V> ImmutableTable<R, C, V> createWithRows(
            Map<? extends C, ? extends V> baseMap,
            Collection<? extends R> rows) {
        ImmutableTable.Builder<R, C, V> tableBuilder = ImmutableTable.builder();

        rows.forEach(row ->
                baseMap.forEach((c, v) -> tableBuilder.put(row, c, v)));

        return tableBuilder.build();
    }

    /**
     * Create an {@link ImmutableTable} adding columns to a base {@link Map} representing rows and values.
     * All the columns in the resulting {@link ImmutableTable} will have the same values.
     *
     * @param baseMap      the base Map used as rows and values of the new ImmutableTable.
     * @param columns      the Collection used as columns of the new ImmutableTable.
     * @param <R>          the type of the rows of the new table.
     * @param <C>          the type of the columns the new table.
     * @param <V>          The type of the values of the new table.
     * @return a new ImmutableTable with rows, columns and values as given.
     */
    public static <R, C, V> ImmutableTable<R, C, V> createWithColumns(
            Map<? extends R, ? extends V> baseMap,
            Collection<? extends C> columns) {
        ImmutableTable.Builder<R, C, V> tableBuilder = ImmutableTable.builder();

        columns.forEach(column ->
                baseMap.forEach((r, v) -> tableBuilder.put(r, column, v)));

        return tableBuilder.build();
    }

    /**
     * Create an {@link ImmutableTable} from a {@link Collection} of rows, one of columns and a
     * function to generate the values from the rows and the columns.
     *
     * @param rowKeys      the Collection used as rows of the new ImmutableTable.
     * @param columnKeys   the Collection used as columns of the new ImmutableTable.
     * @param <R>          the type of the rows of the new table.
     * @param <C>          the type of the columns the new table.
     * @param <V>          The type of the values of the new table.
     * @return a new ImmutableTable with rows and columns as given and calculated values.
     */
    public static <R, C, V> ImmutableTable<R, C, V> create(
            Collection<? extends R> rowKeys,
            Collection<? extends C> columnKeys,
            BiFunction<? super R, ? super C, ? extends V> valueCreator) {
        ImmutableTable.Builder<R, C, V> tableBuilder = ImmutableTable.builder();

        rowKeys.forEach(rowKey ->
                columnKeys.forEach(columnKey -> tableBuilder.put(
                        rowKey,
                        columnKey,
                        valueCreator.apply(rowKey, columnKey)))
        );

        return tableBuilder.build();
    }

    /**
     * Create an {@link ImmutableTable} from a {@link Collection} of rows, one of columns and a
     * value supplier. The table is created by looping through the rows first and then the columns
     * (the value supplier should take that into account).
     *
     * @param rowKeys           the Collection used as rows of the new ImmutableTable.
     * @param columnKeys        the Collection used as columns of the new ImmutableTable.
     * @param valueSupplier     the Supplier used to create the values.
     * @param <R>               the type of the rows of the new table.
     * @param <C>               the type of the columns the new table.
     * @param <V>               the type of the values of the new table.
     * @return a new ImmutableTable with rows, columns and values as given.
     */
    public static <R, C, V> ImmutableTable<R, C, V> create(
            Collection<? extends R> rowKeys,
            Collection<? extends C> columnKeys,
            Supplier<? extends V> valueSupplier) {
        ImmutableTable.Builder<R, C, V> tableBuilder = ImmutableTable.builder();

        rowKeys.forEach(rowKey ->
                columnKeys.forEach(columnKey -> tableBuilder.put(
                        rowKey,
                        columnKey,
                        valueSupplier.get()))
        );

        return tableBuilder.build();
    }
}
