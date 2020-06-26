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
import java.util.function.Function;

import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.StringId;

/**
 * Class to convert string config values into {@link ImmutableSet} by parsing the config value as a comma (",") or
 * colon (":") separated set. If both separators are present, elements will be separated on commas only.
 */
public class OptionalSetValueParser {
    private final Optional<SetValueParser> parser;

    public OptionalSetValueParser(Optional<SetValueParser> parser) {
        this.parser = parser;
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing an {@link ImmutableSet} containing the String config value parsed as a comma (",") or
     *          colon (":") separated set. If both separators are present, elements will be separated on commas only.
     */
    public Optional<ImmutableSet<String>> ofStrings() {
        return parser.map(SetValueParser::ofStrings);
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing an {@link ImmutableSet} containing the String config value parsed as a comma (",") or colon
     *          (":") separated set, with each element converted to an integer. If any element is the String "max" or
     *          "min" (case insensitive), parses the element {@link Integer#MAX_VALUE} or {@link Integer#MIN_VALUE}
     *          respectively, otherwise defers to {@link Integer#parseInt(String)}. If both separators are present,
     *          elements will be separated on commas only, probably triggering a NumberFormatException.
     * @throws NumberFormatException if any elements cannot be parsed to integers.
     */
    public Optional<ImmutableSet<Integer>> ofIntegers() {
        return parser.map(SetValueParser::ofIntegers);
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing an {@link ImmutableSet} containing the String config value parsed as a comma (",") or colon
     *          (":") separated set, with each element converted to a long. If any element is the String "max" or "min"
     *          (case insensitive), parses the element {@link Long#MAX_VALUE} or {@link Long#MIN_VALUE}
     *          respectively, otherwise defers to {@link Long#parseLong(String)}. If both separators are present,
     *          elements will be separated on commas only, probably triggering a NumberFormatException.
     * @throws NumberFormatException if any elements cannot be parsed to longs.
     */
    public Optional<ImmutableSet<Long>> ofLongs() {
        return parser.map(SetValueParser::ofLongs);
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing an {@link ImmutableSet} containing the String config value parsed as a comma (",") or colon
     *          (":") separated set, with each element converted to a double via {@link Double#parseDouble(String)}. If
     *          both separators are present, elements will be separated on commas only, probably triggering a
     *          NumberFormatException.
     * @throws NumberFormatException if any elements cannot be parsed to doubles.
     */
    public Optional<ImmutableSet<Double>> ofDoubles() {
        return parser.map(SetValueParser::ofDoubles);
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing an {@link ImmutableSet} containing the String config value parsed as a comma (",") or colon
     *          (":") separated set, with each element  parsed to an enum value via
     *          {@link Enum#valueOf(Class, String)}. If both separators are present, elements will be separated on
     *          commas only, probably triggering an IllegalArgumentException.
     * @throws IllegalArgumentException if any element does not match a defined enum value.
     */
    public <T extends Enum<T>> Optional<ImmutableSet<T>> ofEnums(Class<T> enumClass) {
        return parser.map(p -> p.ofEnums(enumClass));
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing an {@link ImmutableSet} containing the String config value parsed as a comma (",") or colon
     *          (":") separated set, with each element converted to a long then wrapped in an {@link Id}. If both
     *          separators are present, elements will be separated on commas only, probably triggering a
     *          NumberFormatException.
     * @throws NumberFormatException if any elements cannot be parsed to longs.
     */
    public <T> Optional<ImmutableSet<Id<T>>> ofIds() {
        return parser.map(SetValueParser::ofIds);
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing an {@link ImmutableSet} containing the String config value parsed as a comma (",") or colon
     *          (":") separated set, with each element wrapped in a {@link StringId}. If both separators are present,
     *          elements will be separated on commas only.
     */
    public <T> Optional<ImmutableSet<StringId<T>>> ofStringIds() {
        return parser.map(SetValueParser::ofStringIds);
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing an {@link ImmutableSet} containing the String config value parsed as a comma (",") or colon
     *          (":") separated set, with each element converted using the supplied function. If both separators are
     *          present, elements will be separated on commas only.
     */
    public <T> Optional<ImmutableSet<T>> withElementParser(Function<String, T> elementParser) {
        return parser.map(p -> p.withElementParser(elementParser));
    }
}
