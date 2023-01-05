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
package com.ocadotechnology.utils;

import java.io.BufferedReader;
import java.io.StringReader;

public class BufferedReaderBuilder {
    private StringBuilder stringBuilder;

    private BufferedReaderBuilder() {
        this.stringBuilder = new StringBuilder();
    }

    /**
     * This creates and returns a BufferedReaderBuilder.
     *
     * @return the BufferedReaderBuilder
     */
    public static BufferedReaderBuilder create() {
        return new BufferedReaderBuilder();
    }

    /**
     * This function appends a string to a BufferedReaderBuilder.
     *
     * @param string the string to append.
     * @return this bufferedReaderBuilder to allow for chaining.
     */
    public BufferedReaderBuilder append(String string) {
        stringBuilder.append(string);
        return this;
    }

    /**
     * This function appends a string with a newline character.
     *
     * @param string the string to append
     * @return this bufferedReaderBuilder to allow for chaining.
     */
    public BufferedReaderBuilder appendLine(String string) {
        return append(string + "\n");
    }

    /**
     * This function builds the {@link BufferedReader} using the strings appended through either the {@link #append(String)}
     * or {@link #appendLine(String)} methods.
     *
     * @return the new BufferedReader.
     */
    public BufferedReader build() {
        StringReader stringReader = new StringReader(stringBuilder.toString());
        return new BufferedReader(stringReader);
    }
}