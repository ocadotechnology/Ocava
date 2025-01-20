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

import static com.ocadotechnology.config.ModularConfigUtils.EXTENDS;

import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

class ConfigUsageChecker {
    private static final Predicate<String> IS_STANDARD_PROPERTY = property -> !property.contains("@") && !property.contains(EXTENDS);

    private final HashSet<String> accessedProperties = new HashSet<>();
    private final HashSet<String> definedProperties = new HashSet<>();

    PropertiesAccessor checkAccessTo(Properties properties) {
        definedProperties.addAll(properties.stringPropertyNames());
        return key -> {
            accessedProperties.add(key);
            return properties.getProperty(key);
        };
    }

    Set<String> getUnrecognisedProperties() {
        return Sets.union(getUnrecognisedNonPrefixedProperties(), getUnrecognisedPrefixedProperties());
    }

    ImmutableSet<String> getDeprecatedConfigs(Collection<Config<?>> configList) {
        return populateAllDeprecatedConfigsRecursively(configList);
    }

    private ImmutableSet<String> populateAllDeprecatedConfigsRecursively(Collection<Config<?>> configList) {
        Set<String> deprecatedConfigKeys = new HashSet<>();

        configList.stream()
                .flatMap(ConfigUsageChecker::streamDeprecatedConfigKeys)
                .forEach(deprecatedConfigKeys::add);

        configList.stream()
                .map(config -> populateAllDeprecatedConfigsRecursively(config.getSubConfigValues()))
                .forEach(deprecatedConfigKeys::addAll);

        return ImmutableSet.copyOf(deprecatedConfigKeys);
    }

    private static <E extends Enum<E>> Stream<String> streamDeprecatedConfigKeys(Config<E> conf) {
        return conf.streamConfigKeys()
                .filter(configKey -> {
                    try {
                        return conf.cls.getField(configKey.name()).isAnnotationPresent(Deprecated.class);
                    } catch (NoSuchFieldException | SecurityException e) {
                        throw new IllegalArgumentException("Config doesn't exist: " + conf.getQualifiedKeyName(configKey), e);
                    }
                })
                .map(conf::getQualifiedKeyName);
    }

    private Set<String> getUnrecognisedNonPrefixedProperties() {
        ImmutableSet<String> definedNonPrefixedProperties = definedProperties.stream()
                .filter(IS_STANDARD_PROPERTY)
                .collect(ImmutableSet.toImmutableSet());

        ImmutableSet<String> accessedNonPrefixedProperties = accessedProperties.stream()
                .filter(IS_STANDARD_PROPERTY)
                .collect(ImmutableSet.toImmutableSet());

        return Sets.difference(definedNonPrefixedProperties, accessedNonPrefixedProperties);
    }

    private Set<String> getUnrecognisedPrefixedProperties() {
        ImmutableSet<String> definedNonPrefixedProperties = accessedProperties.stream()
                .filter(IS_STANDARD_PROPERTY)
                .collect(ImmutableSet.toImmutableSet());
        return definedProperties.stream()
                .filter(p -> unrecognisedPrefixedProperty(p, definedNonPrefixedProperties))
                .collect(ImmutableSet.toImmutableSet());
    }

    private boolean unrecognisedPrefixedProperty(String property, Set<String> definedNonPrefixedProperties) {
        if (!property.contains("@")) {
            return false;
        }
        String propertyFromPrefixedProperty = extractPropertyFromPrefixedProperty(property);
        return !definedNonPrefixedProperties.contains(propertyFromPrefixedProperty);
    }

    private String extractPropertyFromPrefixedProperty(String property) {
        return property.contains("@") ?
                property.substring(property.lastIndexOf("@") + 1) :
                property;
    }
}
