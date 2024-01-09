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

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.ocadotechnology.config.ConfigManager.PrefixedProperty;

final class ConfigFactory {

    private ConfigFactory() {
        throw new UnsupportedOperationException("Static utility class that shouldn't be instantiated");
    }

    static <E extends Enum<E>> Config<E> read(Class<E> e, PropertiesAccessor props, ImmutableSet<PrefixedProperty> prefixedProperties) {
        return readInternal(e, props, e.getSimpleName(), prefixedProperties);
    }

    @SuppressWarnings("unchecked rawtypes") //It is actually checked (subEnum.isEnum()) and the types are only raw because Enum<E extends Enum<E>>
    private static <E extends Enum<E>> Config<E> readInternal(Class<E> cls, PropertiesAccessor props, String qualifier, ImmutableSet<PrefixedProperty> prefixedProperties) {
        Builder<E> builder = new Builder<>(cls, qualifier);
        for (E constant : cls.getEnumConstants()) {
            String val = props.getProperty(qualifier + "." + constant.name());
            if (val != null) {
                builder.put(constant, new ConfigValue(val, ImmutableMap.of()));
            }
        }
        for (Class<?> subEnum : cls.getDeclaredClasses()) {
            String subQualifier = qualifier + "." + subEnum.getSimpleName();
            Preconditions.checkState(subEnum.isEnum());
            builder.addSubConfig(readInternal((Class<Enum>)subEnum, props, subQualifier, prefixedProperties));
        }

        builder.managePrefixedProperties(cls, qualifier, prefixedProperties);

        return builder.build();
    }

    private static class Builder<E extends Enum<E>> {
        private final Class<E> cls;
        private final EnumMap<E, ConfigValue> configValues;
        private final Map<Object, Config<?>> subConfig;
        private final String qualifier;

        public Builder(Class<E> cls, String qualifier) {
            this.cls = cls;
            this.qualifier = qualifier;
            configValues = new EnumMap<>(cls);
            subConfig = new HashMap<>();
        }

        private Builder<E> put(E key, ConfigValue val) {
            configValues.put(key, val);
            return this;
        }

        private void addSubConfig(Config<?> config) {
            subConfig.put(config.cls, config);
        }

        private void managePrefixedProperties(
                Class<E> cls,
                String qualifier,
                ImmutableSet<PrefixedProperty> prefixedProperties) {

            for (PrefixedProperty prefixedProperty : prefixedProperties) {
                if (!qualifier.equals(prefixedProperty.qualifier)) {
                    continue;
                }

                Optional<E> enumConfigItem = Arrays.stream(cls.getEnumConstants())
                        .filter(configItem -> configItem.name().equals(prefixedProperty.constant))
                        .findFirst();

                enumConfigItem.ifPresent(e -> updatePrefixes(prefixedProperty, e));
            }
        }

        private void updatePrefixes(PrefixedProperty prefixedProperty, E enumConfigItem) {
            ImmutableMap.Builder<ImmutableSet<String>, String> prefixedValues = ImmutableMap.builder();

            ConfigValue configValue = configValues.get(enumConfigItem);

            prefixedValues.put(prefixedProperty.prefixes, prefixedProperty.propertyValue);

            String currentDefaultValue = null;

            if (configValue != null) {
                prefixedValues.putAll(configValue.prefixedValues);
                currentDefaultValue = configValue.currentValue;
            }

            configValues.put(enumConfigItem, new ConfigValue(currentDefaultValue, prefixedValues.build()));
        }

        private Config<E> build() {
            return new Config<>(cls, Maps.immutableEnumMap(configValues), ImmutableMap.copyOf(subConfig), qualifier);
        }

    }
}
