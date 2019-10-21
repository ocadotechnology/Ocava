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
package com.ocadotechnology.tableio;

import java.time.LocalDateTime;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.StringId;
import com.ocadotechnology.utils.TableUtils;

/**
 * This class is used to build a {@link TableLine} object with field data.
 */
public class TableLineBuilder {
    private static final String nullValue = "NULL";

    private final ImmutableMap.Builder<String, String> lineMapBuilder = ImmutableMap.builder();
    private final ImmutableSet.Builder<String> stringColumnsBuilder = ImmutableSet.builder();

    /**
     * This function adds a column with a specified int value to the builder. This will be added to the {@link TableLine}
     * when the {@link #build()} method is called.
     *
     * @param columnHeader the column to append the value to.
     * @param value        the int value to append.
     * @return This TableLineBuilder is returned to allow for chaining.
     */
    public TableLineBuilder withInt(String columnHeader, Integer value) {
        return setObject(columnHeader, value);
    }

    /**
     * This function adds a column with a specified Long value to the builder. This will be added to the {@link TableLine}
     * when the {@link #build()} method is called.
     *
     * @param columnHeader the column to append the value to.
     * @param value        the Long value to append.
     * @return This TableLineBuilder is returned to allow for chaining.
     */
    public TableLineBuilder withLong(String columnHeader, Long value) {
        return setObject(columnHeader, value);
    }

    /**
     * This function adds a column with a specified {@link Id} value to the builder. This will be added to the {@link TableLine}
     * when the {@link #build()} method is called.
     *
     * @param columnHeader the column to append the value to.
     * @param id           the Id value to append.
     * @return This TableLineBuilder is returned to allow for chaining.
     */
    public TableLineBuilder withId(String columnHeader, Id<?> id) {
        return setObject(columnHeader, id);
    }

    /**
     * This function adds a column with a specified {@link StringId} value to the builder. This will be added to the {@link TableLine}
     * when the {@link #build()} method is called.
     *
     * @param columnHeader the column to append the value to.
     * @param id           the StringId value to append.
     * @return This TableLineBuilder is returned to allow for chaining.
     */
    public TableLineBuilder withStringId(String columnHeader, StringId<?> id) {
        stringColumnsBuilder.add(columnHeader);
        return setObject(columnHeader, id);
    }

    /**
     * This function adds a column with a specified Double value to the builder. This will be added to the {@link TableLine}
     * when the {@link #build()} method is called.
     *
     * @param columnHeader the column to append the value to.
     * @param value        the double value to append.
     * @return This TableLineBuilder is returned to allow for chaining.
     */
    public TableLineBuilder withDouble(String columnHeader, Double value) {
        return setObject(columnHeader, value);
    }

    /**
     * This function adds a column with a specified boolean value to the builder. This will be added to the {@link TableLine}
     * when the {@link #build()} method is called.
     *
     * @param columnHeader the column to append the value to.
     * @param value        the boolean value to append.
     * @return This TableLineBuilder is returned to allow for chaining.
     */
    public TableLineBuilder withBoolean(String columnHeader, Boolean value) {
        return setObject(columnHeader, value);
    }

    /**
     * This function adds a column with a specified LocalDateTime value to the builder. This will be added to the {@link TableLine}
     * when the {@link #build()} method is called.
     *
     * @param columnHeader the column to append the value to.
     * @param value        the LocalDateTime value to append.
     * @return This TableLineBuilder is returned to allow for chaining.
     */
    public TableLineBuilder withTime(String columnHeader, LocalDateTime value) {
        return setObject(columnHeader, value);
    }

    /**
     * This function adds a column with a specified String value to the builder. This will be added to the {@link TableLine}
     * When the {@link #build()} method is called. This also adds the column to the stringColumns of the TableLine.
     *
     * @param columnHeader the column to append the value to.
     * @param value        the String value to append.
     * @return This TableLineBuilder is returned to allow for chaining.
     */
    public TableLineBuilder withString(String columnHeader, String value) {
        stringColumnsBuilder.add(columnHeader);
        return setObject(columnHeader, value);
    }

    /**
     * This function adds a column with a specified enum value to the builder. This will be added to the {@link TableLine}
     * When the {@link #build()} method is called.
     *
     * @param columnHeader the column to append the value to.
     * @param enumValue    the value of the enum value to append.
     * @return This TableLineBuilder is returned to allow for chaining.
     */
    public <T extends Enum<T>> TableLineBuilder withEnumValue(String columnHeader, T enumValue) {
        return setObject(columnHeader, enumValue.name());
    }

    /**
     * This function iterates through each enum value of a specific type. For each enum value it retrieves an int
     * from the countByEnum map representing the number of occurrences of that enum, and appends it to a
     * unique column of the TableLineBuilder. This unique column's header is calculated by appending the statName to
     * the specific enum values name. If the enum does not occur in the map the value of 0 is appended.
     *
     * @param statName    the stat name to append to the header.
     * @param countByEnum the map to count the occurrences of each enum.
     * @param clazz       the type of the Enum which is being counted.
     * @return This TableLineBuilder is returned to allow for chaining.
     */
    public <T extends Enum<T>> TableLineBuilder withEnumCount(
            String statName,
            Map<T, Integer> countByEnum,
            Class<T> clazz) {
        for (T enumValue : clazz.getEnumConstants()) {
            withInt(
                    enumValue.toString() + "_" + statName,
                    countByEnum.getOrDefault(enumValue, 0));
        }
        return this;
    }

    /**
     * This function iterates through each enum of the rowClass. For each of the rowClass it then iterates through each
     * enum of the columnClass. For each possible enum pair it retrieves an int from the Table, representing the number
     * of occurrences of that enum pair, and appends it to a unique column of the TableLineBuilder.
     * This unique column's header is calculated by appending the statName to the specific enum values name.
     * If the enum pair does not occur in the map the value of 0 is appended.
     *
     * @param statName    the stat name to append to the header.
     * @param countTable  the table to count the occurrences of each enum pair .
     * @param rowClass    the enum class of the row of the pair being counted.
     * @param columnClass The enum class of the column of the pair being counted.
     * @return This TableLineBuilder is returned to allow for chaining.
     */
    public <C extends Enum<C>, R extends Enum<R>> TableLineBuilder withTableEnumCount(
            String statName,
            Table<R, C, Integer> countTable,
            Class<R> rowClass,
            Class<C> columnClass) {
        for (R enumRowValue : rowClass.getEnumConstants()) {
            for (C enumColumnValue : columnClass.getEnumConstants()) {
                withInt(
                        String.format("%s_%s_%s", enumColumnValue, enumRowValue, statName),
                        TableUtils.getOrDefault(countTable, enumRowValue, enumColumnValue, 0));
            }
        }
        return this;
    }

    /**
     * This function takes a {@link TableLineExtender} and calls it's addData function on this builder.
     * This should be used to add data to the TableLineBuilder which, when the {@link #build()} function is invoked, will create
     * a {@link TableLine} with the added data.
     *
     * @param extender the extender to add data to the tableLineBuilder
     * @return This TableLineBuilder is returned to allow for chaining.
     */
    public TableLineBuilder withExtension(TableLineExtender extender) {
        extender.addData(this);
        return this;
    }

    /**
     * This function creates a new {@link TableLine} object with the provided headers and row data.
     *
     * @return the TableLine object.
     */
    public TableLine build() {
        return new TableLine(lineMapBuilder.build(), stringColumnsBuilder.build());
    }

    private TableLineBuilder setObject(String columnHeader, Object value) {
        if (value == null) {
            value = nullValue;
        }

        lineMapBuilder.put(columnHeader, value.toString());
        return this;
    }
}