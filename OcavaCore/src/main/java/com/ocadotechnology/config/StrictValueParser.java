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
package com.ocadotechnology.config;

import java.util.function.Function;

import com.google.common.base.MoreObjects;

/**
 * Parser class to convert a config value into a typed result.
 */
public class StrictValueParser {
    private final String value;

    StrictValueParser(String value) {
        this.value = value;
    }

    /**
     * @return the raw string config value.
     */
    public String asString() {
        return value;
    }

    /**
     * @return the string config value parsed to a boolean.
     * @throws IllegalStateException if the config value does not strictly equal "true" or "false", case insensitive.
     */
    public boolean asBoolean() {
        return ConfigParsers.parseBoolean(value);
    }

    /**
     * @return the string config value parsed to an integer. If the value is the String "max" or "min" (case
     *          insensitive) returns {@link Integer#MAX_VALUE} or {@link Integer#MIN_VALUE} respectively, otherwise
     *          defers to {@link Integer#parseInt(String)}.
     * @throws NumberFormatException if the config value cannot be parsed to an integer.
     */
    public int asInt() {
        return ConfigParsers.parseInt(value);
    }

    /**
     * @return the string config value parsed to a long. If the value is the String "max" or "min" (case insensitive)
     *          returns {@link Long#MAX_VALUE} or {@link Long#MIN_VALUE} respectively, otherwise defers to {@link
     *          Long#parseLong(String)}.
     * @throws NumberFormatException if the config value cannot be parsed to a long.
     */
    public long asLong() {
        return ConfigParsers.parseLong(value);
    }

    /**
     * @return the string config value parsed to a double via {@link Double#parseDouble(String)}.
     * @throws NumberFormatException if the config value cannot be parsed to a double.
     */
    public double asDouble() {
        return ConfigParsers.parseDouble(value);
    }

    /**
     * @return the String config value parsed using the provided custom parser.
     */
    public <T> T withCustomParser(Function<String, T> parser) {
        return parser.apply(value);
    }

    /**
     * @deprecated to help avoid calling this when {@link StrictValueParser#asString()} is desired
     */
    @Deprecated
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("value", value)
                .toString();
    }
}
