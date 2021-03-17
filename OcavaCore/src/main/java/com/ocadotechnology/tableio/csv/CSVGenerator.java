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

import java.io.BufferedReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.utils.BufferedReaderBuilder;
import com.ocadotechnology.utils.ImmutableMapFactory;

public class CSVGenerator<T extends CSVColumn> {
    private final Map<String, String> valuesByString = new LinkedHashMap<>();  // Preserve iteration order
    private final BufferedReaderBuilder bufferedReaderBuilder = BufferedReaderBuilder.create();

    private ImmutableList<String> expectedHeaderOverrides;

    public CSVGenerator(CSVRow<T> defaults) {
        valuesByString.putAll(defaults.lineByHeader);

        ImmutableList<String> header = valuesByString.keySet().stream()
                .collect(ImmutableList.toImmutableList());

        appendLine(header);
    }

    /**
     * Overwrites the default value of the specified Key with a new value. If this key is not in the CSVGenerator defaults
     * already this will throw an {@link IllegalStateException}.
     *
     * @param key   the key to represent which default to change.
     * @param value the value to change the default to.
     * @return this CSVGenerator with the modified value to allow for chaining.
     */
    public CSVGenerator<T> withValue(T key, Object value) {
        checkKey(key);

        valuesByString.put(key.name(), value.toString());
        return this;
    }

    /**
     * Overwrites the default value of the specified Key. If this key is not in the CSVGenerator defaults already this
     * will throw an {@link IllegalStateException}.
     *
     * @param key the key to be removed from the defaults.
     * @return this CSVGenerator with the modified value to allow for chaining.
     */
    public CSVGenerator<T> withValueRemoved(T key) {
        return withValue(key, "");
    }

    /**
     * Appends a line, which represents a row, to the {@link BufferedReader} with the default values provided when the
     * CSVGenerator was created.
     */
    public void generateLine() {
        appendLine(valuesByString.values().stream()
                .map(Object::toString)
                .collect(ImmutableList.toImmutableList()));
    }

    /**
     * Returns an {@link ImmutableMap}. The keys are the headers of the CSVRows provided when the CSVGenerator was created.
     * The values are the default values for each of those keys.
     *
     * @return the ImmutableMap.
     */
    public ImmutableMap<String, String> getLineMap() {
        return ImmutableMapFactory.createWithNewValues(
                valuesByString,
                Object::toString);
    }

    /**
     * Returns a {@link CSVRow} based on the default values given to the CSVGenerator.
     *
     * @return the CSVRow.
     */
    public CSVRow<T> getRow() {
        return new CSVRow<>(getLineMap());
    }

    /**
     * Returns a {@link BufferedReader} with all of the generated lines.
     *
     * @return the BufferedReader.
     */
    public BufferedReader getBufferedReader() {
        return bufferedReaderBuilder.build();
    }

    /**
     * Sets the default values allowed to be overridden. If any of these headers are not in the list of defaults provided
     * when the generator was created an {@link IllegalStateException} will be thrown.
     *
     * @param expectedHeaderOverrides the headers that are set to be overridden.
     */
    public void setExpectedHeaderOverrides(T... expectedHeaderOverrides) {
        Arrays.stream(expectedHeaderOverrides).forEach(this::checkKey);

        this.expectedHeaderOverrides = Arrays.stream(expectedHeaderOverrides)
                .map(CSVColumn::name)
                .collect(ImmutableList.toImmutableList());
    }

    /**
     *  {@link #setExpectedHeaderOverrides(CSVColumn[])} should be called before this method.
     * This function first checks that the number of overrides matches the number of expectedHeaderOverrides provided in
     * the setExpectedHeaderOverrides method. If they do not match an IllegalStateException will be thrown otherwise a new
     * line, which represents a CSVRow, will be added to the BufferedReaderBuilder. This new line will contain any both
     * any overridden values and any non-overridden default values.
     *
     * @param overrides the values to override the defaults.
     */
    public void generateLineWithOverrides(Object... overrides) {
        ImmutableMap<String, String> providedValuesByKey = getProvidedValues(overrides);

        ImmutableList<String> lineWithDefaults = valuesByString.keySet().stream()
                .map(key -> getValue(key, providedValuesByKey))
                .collect(ImmutableList.toImmutableList());

        appendLine(lineWithDefaults);
    }

    private void checkKey(T key) {
        Preconditions.checkState(
                valuesByString.containsKey(key.name()),
                "Should not be adding new key to CSV file: " + key);
    }

    private String getValue(
            String key,
            ImmutableMap<String, String> providedValuesByKey) {
        if (providedValuesByKey.containsKey(key)) {
            return providedValuesByKey.get(key);
        } else {
            return valuesByString.get(key);
        }
    }

    private ImmutableMap<String, String> getProvidedValues(Object... overrides) {
        Preconditions.checkState(
                overrides.length == expectedHeaderOverrides.size(),
                "The line provided is expected to match that passed to setExpectedHeaderOverrides");

        ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.builder();

        for (int i = 0; i < overrides.length; i++) {
            mapBuilder.put(expectedHeaderOverrides.get(i), overrides[i].toString());
        }

        return mapBuilder.build();
    }

    private void appendLine(ImmutableList<String> lineEntries) {
        String line = String.join(",", lineEntries);
        bufferedReaderBuilder.appendLine(line);
    }
}