/*
 * Copyright © 2017-2025 Ocado (Ocava)
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

import com.google.common.collect.ImmutableSetMultimap;

/**
 * Class to convert String config values into {@link ImmutableSetMultimap} by parsing the config value as a semicolon
 * (";") separated list of equals ("=") separated key-value pairs.
 */
public class OptionalSetMultimapValueParser {
    private final Optional<SetMultimapValueParser> parser;

    public OptionalSetMultimapValueParser(Optional<SetMultimapValueParser> parser) {
        this.parser = parser;
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing an {@link ImmutableSetMultimap} containing the String config value parsed as a semicolon
     *          (";") separated list of equals ("=") separated key-value pairs.  For example:
     *
     * <pre>"key1=value1;key1=value2;key2=value3"</pre>
     *
     * Keys and values will have leading and trailing whitespace trimmed.
     * Any pair which does not contain the character '=' or which starts with '=' will be ignored.
     */
    public Optional<ImmutableSetMultimap<String, String>> ofStrings() {
        return withKeyAndValueParsers(Function.identity(), Function.identity());
    }

    /**
     * @return {@link Optional#empty()} if the config value is an empty String, otherwise returns an {@link Optional}
     *          containing an {@link ImmutableSetMultimap} containing the String config value parsed as a semicolon
     *          (";") separated list of equals ("=") separated key-value pairs, with each key and value converted using
     *          the supplied function.
     *
     * For example:
     *
     * <pre>"key1=value1;key1=value2;key2=value3"</pre>
     *
     * Keys and values will have leading and trailing whitespace trimmed.
     * Any pair which does not contain the character '=' or which starts with '=' will be ignored.
     *
     * @throws IllegalArgumentException   if duplicate keys are specified
     */
    public <K, V> Optional<ImmutableSetMultimap<K, V>> withKeyAndValueParsers(Function<String, K> keyParser, Function<String, V> valueParser) {
        return parser.map(p -> p.withKeyAndValueParsers(keyParser, valueParser));
    }
}
