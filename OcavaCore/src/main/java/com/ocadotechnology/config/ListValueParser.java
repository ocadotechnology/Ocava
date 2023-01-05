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
package com.ocadotechnology.config;

import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.StringId;

/**
 * Class to convert string config values into {@link ImmutableList} by parsing the config value as a comma (",") or
 * colon (":") separated list. If both separators are present, elements will be separated on commas only.
 */
public class ListValueParser {
    private final Enum<?> key;
    private final String value;

    public ListValueParser(Enum<?> key, String value) {
        this.value = value;
        this.key = key;
    }

    /**
     * @return an {@link ImmutableList} containing the String config value parsed as a comma (",") or colon (":")
     *          separated list. If both separators are present, elements will be separated on commas only.
     */
    public ImmutableList<String> ofStrings() {
        return withElementParser(Function.identity());
    }

    /**
     * @return an {@link ImmutableList} containing the String config value parsed as a comma (",") or colon (":")
     *          separated list, with each element converted to an integer. If any element is the String "max" or "min"
     *          (case insensitive), parses the element {@link Integer#MAX_VALUE} or {@link Integer#MIN_VALUE}
     *          respectively, otherwise defers to {@link Integer#parseInt(String)}. If both separators are present,
     *          elements will be separated on commas only, probably triggering a NumberFormatException.
     * @throws NumberFormatException if any elements cannot be parsed to integers.
     */
    public ImmutableList<Integer> ofIntegers() {
        return withElementParser(ConfigParsers::parseInt);
    }

    /**
     * @return an {@link ImmutableList} containing the String config value parsed as a comma (",") or colon (":")
     *          separated list, with each element converted to a long. If any element is the String "max" or "min"
     *          (case insensitive), parses the element {@link Long#MAX_VALUE} or {@link Long#MIN_VALUE}
     *          respectively, otherwise defers to {@link Long#parseLong(String)}. If both separators are present,
     *          elements will be separated on commas only, probably triggering a NumberFormatException.
     * @throws NumberFormatException if any elements cannot be parsed to longs.
     */
    public ImmutableList<Long> ofLongs() {
        return withElementParser(ConfigParsers::parseLong);
    }

    /**
     * @return an {@link ImmutableList} containing the String config value parsed as a comma (",") or colon (":")
     *          separated list, with each element converted to a double via {@link Double#parseDouble(String)}. If both
     *          separators are present, elements will be separated on commas only, probably triggering a
     *          NumberFormatException.
     * @throws NumberFormatException if any elements cannot be parsed to doubles.
     */
    public ImmutableList<Double> ofDoubles() {
        return withElementParser(ConfigParsers::parseDouble);
    }

    /**
     * @return an {@link ImmutableList} containing the String config value parsed as a comma (",") or colon (":")
     *          separated list, with each element  parsed to an enum value via {@link Enum#valueOf(Class, String)}. If
     *          both separators are present, elements will be separated on commas only, probably triggering an
     *          IllegalArgumentException.
     * @throws IllegalArgumentException if any element does not match a defined enum value.
     */
    public <T extends Enum<T>> ImmutableList<T> ofEnums(Class<T> enumClass) {
        return withElementParser(s -> Enum.valueOf(enumClass, s));
    }

    /**
     * @return an {@link ImmutableList} containing the String config value parsed as a comma (",") or colon (":")
     *          separated list, with each element converted to a long then wrapped in an {@link Id}. If both separators
     *          are present, elements will be separated on commas only, probably triggering a NumberFormatException.
     * @throws NumberFormatException if any elements cannot be parsed to longs.
     */
    public <T> ImmutableList<Id<T>> ofIds() {
        return withElementParser(s -> Id.create(ConfigParsers.parseLong(s)));
    }

    /**
     * @return an {@link ImmutableList} containing the String config value parsed as a comma (",") or colon (":")
     *          separated list, with each element wrapped in a {@link StringId}. If both separators are present,
     *          elements will be separated on commas only.
     */
    public <T> ImmutableList<StringId<T>> ofStringIds() {
        return withElementParser(StringId::create);
    }

    /**
     * @return an {@link ImmutableList} containing the String config value parsed as a comma (",") or colon (":")
     *          separated list, with each element converted using the supplied function. If both separators are present,
     *          elements will be separated on commas only.
     */
    public <T> ImmutableList<T> withElementParser(Function<String, T> elementParser) {
        try {
            return ConfigParsers.getListOf(elementParser).apply(value);
        } catch (Throwable t) {
            throw new IllegalStateException("Error parsing " + ConfigKeyUtils.getKeyName(key), t);
        }
    }
}
