/*
 * Copyright © 2017-2024 Ocado (Ocava)
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.config.ConfigManager.ConfigDataSource;

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
    public static final String PROPERTIES_EXTENSION = ".properties";

    /**
     * Searches for the extension property, and splits the value into a list of strings,
     * adding the properties file extension to all the values.
     * @param properties    The properties to search through
     * @return              A collection of string file names
     */
    public static ImmutableCollection<String> getAllFilesExtended(Properties properties) {
        if (properties.containsKey(EXTENDS)) {
            return ConfigParsers.getSetOf(fileName -> fileName + PROPERTIES_EXTENSION)
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

    /**
     * Loads all the properties from the file including the extended configs and returns an ordered set of entries (key,value).
     * This can be used for the modular config to get the end config after processing all the "extends".
     * @param file - if file is not a modular config it will return the sorted properties inside the file.
     *
     */
    public static Set<Entry<String, String>> getSortedMergedConfig(File file) throws IOException {
        ConfigDataSource configDataSource = ConfigDataSource.fromFile(file);
        Properties sourceProps = configDataSource.readAsProperties(new ConfigSettingCollector());
        return sourceProps.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey().toString(), entry.getValue().toString()))
                .sorted(Entry.comparingByKey())
                .collect(ImmutableSet.toImmutableSet());
    }

    /**
     * This combines all the configs from every <i>"extended"</i> config in one file.<br>
     * The entries are sorted by the first key and would be grouped by it (with an empty line as separator).<br>
     * Ex "a.b.c=1; b.a.a=2; a.e.d=3; a.a.z=4" would result in the following format <br>
     * ----------------<br>
     * a.a.z=4 <br>
     * a.b.c=1 <br>
     * a.e.d=3<br>
     * <br>
     * b.a.a=2<br>
     * ----------------<br>
     *
     * @param configFile     the entry point of the config
     * @param outputFilePath the output file. This can be in a different directory, adding the <i>.properties</i> suffix if it is missing.
     * @throws IOException
     * @throws IllegalArgumentException if the output file is the same as input file or if the output file already exists.
     */
    public static void writeToFileMergedConfig(File configFile, String outputFilePath) throws IOException {
        if (!outputFilePath.endsWith(PROPERTIES_EXTENSION)) {
            outputFilePath += PROPERTIES_EXTENSION;
        }
        File newFile = new File(outputFilePath);
        Preconditions.checkArgument(!configFile.equals(newFile), "Wrong outputFilePath: %s as this will overwrite the original file.", outputFilePath);

        Set<Entry<String, String>> configList = getSortedMergedConfig(configFile);

        Optional.ofNullable(newFile.getParentFile()).ifPresent(File::mkdirs);
        Preconditions.checkArgument (newFile.createNewFile(), "OutputFilePath %s already exists.", outputFilePath);

        String currentKey = null;
        try (FileWriter fileWriter = new FileWriter(newFile, StandardCharsets.UTF_8)) {
            for (Entry<String, String> entry : configList) {
                if (!substringBefore(entry.getKey(), ".").equals(currentKey)) {
                    if (currentKey != null) {
                        fileWriter.write(System.lineSeparator());
                    }
                    currentKey = substringBefore(entry.getKey(), ".");
                }
                fileWriter.write(entry + System.lineSeparator());
            }
        }
    }

    private static String substringBefore(String str, String separator) {
        if (!(str == null || str.length() == 0) && separator != null) {
            if (separator.isEmpty()) {
                return "";
            } else {
                int pos = str.indexOf(separator);
                return pos == -1 ? str : str.substring(0, pos);
            }
        } else {
            return str;
        }
    }
}
