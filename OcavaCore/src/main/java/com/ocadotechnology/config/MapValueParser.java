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

import java.util.function.Function;

import com.google.common.collect.ImmutableMap;

/**
 * Class to convert String config values into {@link ImmutableMap} by parsing the config value as a semicolon (";")
 * separated list of equals ("=") separated key-value pairs.
 */
public class MapValueParser {
    private final Enum<?> key;
    private final String value;

    public MapValueParser(Enum<?> key, String value) {
        this.value = value;
        this.key = key;
    }

    /**
     * @return an {@link ImmutableMap} containing the String config value parsed as a semicolon (";") separated list of
     *          equals ("=") separated key-value pairs.  For example:
     *
     * <pre>"key1=value1;key2=value2"</pre>
     *
     * Keys and values will have leading and trailing whitespace trimmed.
     * Any pair which does not contain the character '=' or which starts with '=' will be ignored.
     *
     * @throws IllegalArgumentException   if duplicate keys are specified
     */
    public ImmutableMap<String, String> ofStrings() {
        return withKeyAndValueParsers(Function.identity(), Function.identity());
    }

    /**
     * @return an {@link ImmutableMap} containing the String config value parsed as a semicolon (";") separated list of
     *          equals ("=") separated key-value pairs, with each key and value converted using the supplied function.
     *
     * For example:
     *
     * <pre>"key1=value1;key2=value2"</pre>
     *
     * Keys and values will have leading and trailing whitespace trimmed.
     * Any pair which does not contain the character '=' or which starts with '=' will be ignored.
     *
     * @throws IllegalArgumentException   if duplicate keys are specified
     */
    public <K, V> ImmutableMap<K, V> withKeyAndValueParsers(Function<String, K> keyParser, Function<String, V> valueParser) {
        try {
            return ConfigParsers.parseMap(value, keyParser, valueParser);
        } catch (Throwable t) {
            throw new IllegalStateException("Error parsing " + ConfigKeyUtils.getKeyName(key), t);
        }
    }
}
