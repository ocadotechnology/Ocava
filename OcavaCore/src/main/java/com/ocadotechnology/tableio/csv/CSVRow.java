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

import java.math.BigDecimal;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

public class CSVRow<T extends CSVColumn> {
    private static final String EMPTY_STRING = "";

    public final ImmutableMap<String, String> lineByHeader;

    public CSVRow(ImmutableMap<String, String> lineByHeader) {
        this.lineByHeader = lineByHeader;
    }

    /**
     * Checks if a {@link CSVColumn}s header is in the lineByHeader map.
     *
     * @param csvColumn the csvColumn to check.
     * @return true if the column's header is in the list of headers. Otherwise false.
     */
    public boolean hasEntry(T csvColumn) {
        return lineByHeader.containsKey(csvColumn.name());
    }

    /**
     * This function parses a {@link CSVColumn}. The value is retrieved from the lineByHeader map if it exists.
     * This function will throw a {@link NullPointerException} if the value of the CSVColumn is null and the
     * {@link CSVColumn#allowMissingColumn()} function inherited from CSVColumn is false.
     * This function will throw an {@link IllegalStateException} if the value is equal to the empty string and
     * the {@link CSVColumn#isNullable()} ()} function inherited from CSVColumn is false.
     *
     * @param csvColumn the column of the CSVRow to parse.
     * @return the string value retrieved from the lineByHeader map.
     */
    public String parse(T csvColumn) {
        String header = csvColumn.name();
        String value = lineByHeader.get(header);

        if (!csvColumn.allowMissingColumn()) {
            Preconditions.checkNotNull(value, "Missing column " + header);
        }

        if (!csvColumn.isNullable()) {
            Preconditions.checkState(
                    !value.equals(""),
                    header + " should not be null");
        }

        return value;
    }

    /**
     * The method first parses the column using the {@link #parse(CSVColumn)}) method. If the value returned from that method is null or an empty string
     * this method returns null. Otherwise it will try and parse the value as a int.
     * This may throw a {@link NumberFormatException}() if the string is unable to be parsed.
     *
     * @param csvColumn the column of the CSVRow to parse.
     * @return the int value of the column or null.
     */
    public Integer parseInt(T csvColumn) {
        String value = parse(csvColumn);
        if (hasNullValue(value)) {
            return null;
        }

        return Integer.valueOf(value);
    }

    /**
     * The method first parses the column using the {@link #parse(CSVColumn)}) method. If the value returned from that method is null or an empty string
     * this method returns null. Otherwise it will try and parse the value as a double.
     * This may throw a {@link NumberFormatException}() if the string is unable to be parsed.
     *
     * @param csvColumn the column of the CSVRow to parse.
     * @return the double value of the column or null.
     */
    public Double parseDouble(T csvColumn) {
        String value = parse(csvColumn);
        if (hasNullValue(value)) {
            return null;
        }

        return Double.valueOf(value);
    }

    /**
     * The method first parses the column using the {@link #parse(CSVColumn)} method. If the value returned from that method is null or an empty string
     * this method returns null. Otherwise it will try and parse the value as a BigDecimal.
     * This may throw a {@link NumberFormatException} if the string is unable to be parsed.
     *
     * @param csvColumn the column of the CSVRow to parse.
     * @return the bigDecimal value of the column or null.
     */
    public BigDecimal parseBigDecimal(T csvColumn) {
        String value = parse(csvColumn);
        if (hasNullValue(value)) {
            return null;
        }

        return new BigDecimal(value);
    }

    /**
     * The method first parses the column using the {@link #parse(CSVColumn)} method. If the value returned from that method is null or an empty string
     * this method returns null. Otherwise it will try and parse the value as a decimal. The string will then be converted to lowercase.
     * If this is equal to "1" or "true" it will return true. Otherwise, if it is equal to "0" or "false" it will return false.
     * Otherwise, the boolean cannot be parsed.
     * If the boolean cannot be parsed it will throw a {@link NumberFormatException}
     *
     * @param csvColumn the column of the CSVRow to parse.
     * @return the boolean value of the column or null.
     */
    @Nullable
    public Boolean parseBoolean(T csvColumn) {
        String value = parse(csvColumn);
        if (hasNullValue(value)) {
            return null;
        }

        String lowerCase = value.toLowerCase();
        switch (lowerCase) {
            case "":
                return null;
            case "1":
            case "true":
                return true;
            case "0":
            case "false":
                return false;
            default:
                throw new NumberFormatException(lowerCase + " is neither true nor false");
        }
    }

    /**
     * The method first parses the column using the {@link #parse(CSVColumn)} method. If the value returned from that method is null or an empty string
     * this method returns null. Otherwise, this method parses the string using the {@link Enum#valueOf(Class, String)} method using the
     * supplied enumClass parameter. An {@link IllegalArgumentException} will be thrown if the value cannot be parsed
     *
     * @param csvColumn The column of the CSVRow to parse.
     * @param enumClass The type of Enum to parse the string into.
     * @param <E>       The type of Enum to parse the string into.
     * @return the enum value of the column or null.
     */
    public <E extends Enum<E>> E parseEnum(T csvColumn, Class<E> enumClass) {
        String value = parse(csvColumn);
        if (hasNullValue(value)) {
            return null;
        }

        return Enum.valueOf(enumClass, value);
    }

    /**
     * Checks if the given string is null or an empty string.
     *
     * @param value the string to check.
     * @return true if the string is null or the empty string otherwise false.
     */
    private boolean hasNullValue(String value) {
        return value == null || value.equals(EMPTY_STRING);
    }
}