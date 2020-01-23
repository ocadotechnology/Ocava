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

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

class EnvironmentConfigLoader {
    private final Map<String, String> environmentVariables;

    private Set<String> accessedEnvVars = new HashSet<>();
    private Properties loadedProperties = new Properties();

    static Properties loadConfigFromEnvironmentVariables(
            Map<String, String> environmentVariables,
            ImmutableSet<Class<? extends Enum<?>>> configKeys
    ) {
        EnvironmentConfigLoader configLoader = new EnvironmentConfigLoader(environmentVariables);
        configKeys.forEach(clazz -> configLoader.loadConfigFromEnvironmentVariables(clazz, ""));
        return configLoader.loadedProperties;
    }

    private EnvironmentConfigLoader(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    private void loadConfigFromEnvironmentVariables(Class<? extends Enum<?>> clazz, String prefix) {
        String classPrefix = createClassPrefix(clazz, prefix);
        getConfigForEnumValuesCheckingForDuplicates(clazz, classPrefix);
        loadEnvironmentConfigFromDeclaredClasses(clazz, classPrefix);
    }

    private String createClassPrefix(Class<? extends Enum<?>> clazz, String prefix) {
        return prefix + clazz.getSimpleName() + ".";
    }

    private void getConfigForEnumValuesCheckingForDuplicates(Class<? extends Enum<?>> clazz, String classPrefix) {
        for (Enum<?> c : clazz.getEnumConstants()) {
            String key = c.name();
            String value = environmentVariables.get(key);
            if (value != null) {
                throwIfDuplicateEncountered(key);
                loadedProperties.put(classPrefix + key, value);
            }
        }
    }

    private void throwIfDuplicateEncountered(String key) {
        if (!accessedEnvVars.add(key)) {
            throw new DuplicateMatchingEnvironmentVariableException(key);
        }
    }

    private void loadEnvironmentConfigFromDeclaredClasses(Class<? extends Enum<?>> clazz, String classPrefix) {
        for (Class subEnum : clazz.getDeclaredClasses()) {
            if (subEnum.isEnum()) {
                loadConfigFromEnvironmentVariables(subEnum, classPrefix);
            }
        }
    }
}
