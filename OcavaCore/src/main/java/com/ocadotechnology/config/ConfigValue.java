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
package com.ocadotechnology.config;

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

class ConfigValue implements Serializable {

    final ImmutableMap<ImmutableSet<String>, String> prefixedValues;
    final String currentValue;

    ConfigValue(String currentValue, ImmutableMap<ImmutableSet<String>, String> prefixedValues) {
        this.prefixedValues = prefixedValues;
        this.currentValue = currentValue;
    }

    ConfigValue getPrefix(String prefix) {
        ImmutableMap<ImmutableSet<String>, String> filteredPrefixedValues = prefixedValues.entrySet()
                .stream()
                .filter(setStringEntry -> setStringEntry.getKey().contains(prefix))
                .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

        String currentValue = filteredPrefixedValues.entrySet()
                .stream()
                .filter(e -> hasSinglePrefix(e.getKey()))
                .findFirst()
                .map(Entry::getValue)
                .orElse(this.currentValue);

        ImmutableMap.Builder<ImmutableSet<String>, String> prefixedValues = ImmutableMap.builder();

        filteredPrefixedValues.forEach((prefixes, value) -> {
            ImmutableSet<String> updatedPrefixes = removePrefix(prefix, prefixes);
            if (!updatedPrefixes.isEmpty()) {
                prefixedValues.put(updatedPrefixes, value);
            }
        });

        return new ConfigValue(currentValue, prefixedValues.build());
    }

    private ImmutableSet<String> removePrefix(String prefix, ImmutableSet<String> prefixes) {
        return prefixes.stream()
                .filter(s -> !prefix.equals(s))
                .collect(ImmutableSet.toImmutableSet());
    }

    private boolean hasSinglePrefix(Set<String> prefixes) {
        return prefixes.size() == 1;
    }
}