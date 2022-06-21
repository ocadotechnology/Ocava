/*
 * Copyright © 2017-2022 Ocado (Ocava)
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

import java.util.Map.Entry;
import java.util.Properties;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;

/**
 * A set of tools to help parse modular config properties files.
 * These files are similar to ordinary properties files but may contain the
 * EXTENDS keyword, which gives a list of properties file names that should be
 * included as part of the main file.
 *
 * The file names should not include the extension and should be the full path
 * within the resources folder.
 */
@ParametersAreNonnullByDefault
public class ModularConfigUtils {
    public static final String EXTENDS = "EXTENDS";

    /**
     * Searches for the extension property, and splits the value into a list of strings,
     * adding the properties file extension to all the values.
     * @param properties    The properties to search through
     * @return              A collection of string file names
     */
    public static ImmutableCollection<String> getAllFilesExtended(Properties properties) {
        if (properties.containsKey(EXTENDS)) {
            return ConfigParsers.getSetOf(fileName -> fileName + ".properties")
                    .apply(properties.getProperty(EXTENDS));
        }

        return ImmutableSet.of();
    }

    /**
     * Compares two sets of properties to see if they have any conflicts.
     * Properties files are conflicting if they contain the same key but different values.
     * @param properties1   The first properties to compare
     * @param properties2   The second properties to compare
     */
    public static void checkForConflicts(Properties properties1, Properties properties2) {
        ImmutableSet<Object> clashingConfigKeys = properties1.entrySet().stream()
                .filter(entry -> !properties2.getOrDefault(entry.getKey(), entry.getValue()).equals(entry.getValue()))
                .map(Entry::getKey)
                .collect(ImmutableSet.toImmutableSet());

        if (!clashingConfigKeys.isEmpty()) {
            throw new ModularConfigException("Unresolved conflict in properties files for config keys: " + clashingConfigKeys);
        }
    }
}
