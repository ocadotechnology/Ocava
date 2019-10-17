/*
 * Copyright © 2017 Ocado (Ocava)
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

/**
 * Interface to represent a generic CSVColumn
 */
public interface CSVColumn {
    /**
     * @return the header of the CsvColumn
     */
    String name();

    /**
     * Represents if the csvColumn is allowed to be nullable. By default the value is false.
     *
     * @return true if it is allowed to be nullable otherwise false.
     */
    default boolean isNullable() {
        return false;
    }

    /**
     * Represents if the csvColumn is allowed to be missing. By default the value is false.
     *
     * @return true if it is allowed to be missing otherwise false.
     */
    default boolean allowMissingColumn() {
        return false;
    }
}