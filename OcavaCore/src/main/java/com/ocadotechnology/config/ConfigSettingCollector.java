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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.wrappers.Pair;

/**
 * Collects config settings against the source of those settings.
 */
class ConfigSettingCollector {
    private final Map<String, Pair<String, String>> valueAndFileByKey = new LinkedHashMap<>();
    private final Set<String> secretKeys = new LinkedHashSet<>();

    /**
     * Adds enumerations that should be checked to determine whether settings should be collected.
     *
     * @param configEnums   Enumerations that setting keys will be checked against before collecting them; any annotated
     *                      with @SecretConfig will be excluded.
     */
    void addSecrets(ImmutableSet<Class<? extends Enum<?>>> configEnums) {
        configEnums.forEach(clazz -> secretKeys.addAll(resolveSecretKeysForClass(clazz)));

        // Defensively remove any existing settings that we now know to be secrets.
        valueAndFileByKey.keySet().removeAll(secretKeys);
    }

    private Set<String> resolveSecretKeysForClass(Class<? extends Enum<?>> inputClazz) {
        Set<String> result = new LinkedHashSet<>();
        Queue<Pair<String, Class<? extends Enum<?>>>> qualifierAndClassQueue = new LinkedList<>();
        qualifierAndClassQueue.offer(Pair.of(inputClazz.getSimpleName(), inputClazz));

        while (!qualifierAndClassQueue.isEmpty()) {
            Pair<String, Class<? extends Enum<?>>> qualifierAndClass = qualifierAndClassQueue.poll();
            String qualifier = qualifierAndClass.a();
            Class<? extends Enum<?>> clazz = qualifierAndClass.b();

            for (Enum<?> enumeration : qualifierAndClass.b().getEnumConstants()) {
                try {
                    String settingNameFormat = "%s.%s";

                    if (clazz.getField(enumeration.name()).isAnnotationPresent(SecretConfig.class)) {
                        result.add(String.format(settingNameFormat, qualifier, enumeration.name()));
                    }

                    for (Class<?> subEnumeration : clazz.getDeclaredClasses()) {
                        String subQualifier = String.format(settingNameFormat, qualifier, subEnumeration.getSimpleName());
                        Preconditions.checkState(subEnumeration.isEnum());

                        qualifierAndClassQueue.offer(Pair.of(subQualifier, (Class<? extends Enum<?>>) subEnumeration));
                    }
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return result;
    }

    /**
     * Accept the properties associated with the given file name, collecting those settings whose keys are not annotated
     * with @SecretConfig.
     */
    public void accept(String fileName, Properties properties) {
        properties.forEach((objKey, objValue) -> {
            String key = (String)objKey;
            String value = (String) objValue;

            if (!secretKeys.contains(key)) {
                valueAndFileByKey.put(key, Pair.of(value, fileName));
            }
        });
    }

    @Override
    public String toString() {
        Map<String, Properties> propertiesByFile = new LinkedHashMap<>();

        for (Map.Entry<String, Pair<String, String>> entry : valueAndFileByKey.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().a();
            String fileName = entry.getValue().b();
            propertiesByFile.computeIfAbsent(fileName, k -> new Properties()).put(key, value);
        }

        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, Properties> entry: propertiesByFile.entrySet()) {
            builder.append(entry.getKey());
            builder.append("\n");
            builder.append(propertiesToString(entry.getValue()));
            builder.append("\n");
        }

        return builder.toString();
    }

    private String propertiesToString(Properties properties) {
        StringBuilder builder = new StringBuilder();

        for (Entry<Object, Object> valueByKey: properties.entrySet()) {
            builder.append(valueByKey);
            builder.append("\n");
        }

        return builder.toString();
    }
}
