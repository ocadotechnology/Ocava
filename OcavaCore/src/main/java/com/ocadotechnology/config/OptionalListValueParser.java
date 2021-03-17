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
package com.ocadotechnology.config;

import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.StringId;

/**
 * Class to convert string config values into {@link ImmutableList} by parsing the config value as a comma (",") or
 * colon (":") separated list. If both separators are present, elements will be separated on commas only.
 */
public class OptionalListValueParser {
    private final Optional<ListValueParser> parser;

    public OptionalListValueParser(Optional<ListValueParser> parser) {
        this.parser = parser;
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing an {@link ImmutableList} containing the String config value parsed as a comma (",") or
     *          colon (":") separated list. If both separators are present, elements will be separated on commas only.
     */
    public Optional<ImmutableList<String>> ofStrings() {
        return parser.map(ListValueParser::ofStrings);
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing an {@link ImmutableList} containing the String config value parsed as a comma (",") or colon
     *          (":") separated list, with each element converted to an integer. If any element is the String "max" or
     *          "min" (case insensitive), parses the element {@link Integer#MAX_VALUE} or {@link Integer#MIN_VALUE}
     *          respectively, otherwise defers to {@link Integer#parseInt(String)}. If both separators are present,
     *          elements will be separated on commas only, probably triggering a NumberFormatException.
     * @throws NumberFormatException if any elements cannot be parsed to integers.
     */
    public Optional<ImmutableList<Integer>> ofIntegers() {
        return parser.map(ListValueParser::ofIntegers);
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing an {@link ImmutableList} containing the String config value parsed as a comma (",") or colon
     *          (":") separated list, with each element converted to a long. If any element is the String "max" or "min"
     *          (case insensitive), parses the element {@link Long#MAX_VALUE} or {@link Long#MIN_VALUE}
     *          respectively, otherwise defers to {@link Long#parseLong(String)}. If both separators are present,
     *          elements will be separated on commas only, probably triggering a NumberFormatException.
     * @throws NumberFormatException if any elements cannot be parsed to longs.
     */
    public Optional<ImmutableList<Long>> ofLongs() {
        return parser.map(ListValueParser::ofLongs);
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing an {@link ImmutableList} containing the String config value parsed as a comma (",") or colon
     *          (":") separated list, with each element converted to a double via {@link Double#parseDouble(String)}. If
     *          both separators are present, elements will be separated on commas only, probably triggering a
     *          NumberFormatException.
     * @throws NumberFormatException if any elements cannot be parsed to doubles.
     */
    public Optional<ImmutableList<Double>> ofDoubles() {
        return parser.map(ListValueParser::ofDoubles);
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing an {@link ImmutableList} containing the String config value parsed as a comma (",") or colon
     *          (":") separated list, with each element  parsed to an enum value via
     *          {@link Enum#valueOf(Class, String)}. If both separators are present, elements will be separated on
     *          commas only, probably triggering an IllegalArgumentException.
     * @throws IllegalArgumentException if any element does not match a defined enum value.
     */
    public <T extends Enum<T>> Optional<ImmutableList<T>> ofEnums(Class<T> enumClass) {
        return parser.map(p -> p.ofEnums(enumClass));
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing an {@link ImmutableList} containing the String config value parsed as a comma (",") or colon
     *          (":") separated list, with each element converted to a long then wrapped in an {@link Id}. If both
     *          separators are present, elements will be separated on commas only, probably triggering a
     *          NumberFormatException.
     * @throws NumberFormatException if any elements cannot be parsed to longs.
     */
    public <T> Optional<ImmutableList<Id<T>>> ofIds() {
        return parser.map(ListValueParser::ofIds);
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing an {@link ImmutableList} containing the String config value parsed as a comma (",") or colon
     *          (":") separated list, with each element wrapped in a {@link StringId}. If both separators are present,
     *          elements will be separated on commas only.
     */
    public <T> Optional<ImmutableList<StringId<T>>> ofStringIds() {
        return parser.map(ListValueParser::ofStringIds);
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing an {@link ImmutableList} containing the String config value parsed as a comma (",") or colon
     *          (":") separated list, with each element converted using the supplied function. If both separators are
     *          present, elements will be separated on commas only.
     */
    public <T> Optional<ImmutableList<T>> withElementParser(Function<String, T> elementParser) {
        return parser.map(p -> p.withElementParser(elementParser));
    }
}
