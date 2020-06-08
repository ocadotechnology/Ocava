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

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Function;

import com.google.common.base.MoreObjects;

/**
 * Parser class to convert a config value into a typed optional result. All parsing methods will return {@link
 * Optional#empty()} if the value is am empty String.
 */
public class OptionalValueParser {
    private final String value;

    OptionalValueParser(String value) {
        this.value = value;
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing the the config value.
     */
    public Optional<String> asString() {
        return withCustomParser(Function.identity());
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing the string config value parsed to a boolean.
     * @throws IllegalStateException if the config value does not strictly equal "true" or "false", case insensitive.
     */
    public Optional<Boolean> asBoolean() {
        return withCustomParser(ConfigParsers::parseBoolean);
    }

    /**
     * @return {@link OptionalInt#empty()} if the config value is an empty String, otherwise returns an {@link
     *          OptionalInt} containing the string config value parsed to an integer. If the value is the String "max"
     *          or "min" (case insensitive) parses the value to {@link Integer#MAX_VALUE} or {@link Integer#MIN_VALUE}
     *          respectively, otherwise defers to {@link Integer#parseInt(String)}.
     * @throws NumberFormatException if the config value cannot be parsed to an integer.
     */
    public OptionalInt asInt() {
        if (value.isEmpty()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(ConfigParsers.parseInt(value));
    }

    /**
     * @return {@link OptionalLong#empty()} if the config value is an empty String, otherwise returns an {@link
     *          OptionalLong} containing the string config value parsed to a long. If the value is the String "max" or
     *          "min" (case insensitive) parses the value to {@link Long#MAX_VALUE} or {@link Long#MIN_VALUE}
     *          respectively, otherwise defers to {@link Long#parseLong(String)}.
     * @throws NumberFormatException if the config value cannot be parsed to a long.
     */
    public OptionalLong asLong() {
        if (value.isEmpty()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(ConfigParsers.parseLong(value));
    }

    /**
     * @return {@link OptionalDouble#empty()} if the config value is an empty String, otherwise returns an {@link
     *          OptionalDouble} containing the string config value parsed to a double via {@link
     *          Double#parseDouble(String)}.
     * @throws NumberFormatException if the config value cannot be parsed to a double.
     */
    public OptionalDouble asDouble() {
        if (value.isEmpty()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(ConfigParsers.parseDouble(value));
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing the the result of the the provided custom parser applied to the config value.
     */
    public <T> Optional<T> withCustomParser(Function<String, T> parser) {
        if (value.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(parser.apply(value));
    }

    /**
     * @deprecated to help avoid calling this when {@link OptionalValueParser#asString()} is desired
     */
    @Deprecated
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("value", value)
                .toString();
    }
}
