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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;

class ModularConfigUtilsTest {
    @Test
    void whenPropertiesContainsNoExtends_thenNoFilesExtended() {
        Properties properties = new Properties();
        properties.put("prop1", "foo");

        assertEquals(
                ImmutableSet.of(),
                ModularConfigUtils.getAllFilesExtended(properties));
    }

    @Test
    void whenPropertiesExtendsOneFile_thenSingleFileExtended() {
        Properties properties = new Properties();
        properties.put(ModularConfigUtils.EXTENDS, "bar");
        properties.put("prop1", "foo");

        assertEquals(
                ImmutableSet.of("bar.properties"),
                ModularConfigUtils.getAllFilesExtended(properties));
    }

    @Test
    void whenPropertiesExtendsMultipleFiles_thenAllFilesExtended() {
        Properties properties = new Properties();
        properties.put(ModularConfigUtils.EXTENDS, "bar, foo,nested/woo");
        properties.put("prop1", "hoo");

        assertEquals(
                ImmutableSet.of("foo.properties", "bar.properties", "nested/woo.properties"),
                ModularConfigUtils.getAllFilesExtended(properties));
    }

    @Test
    void whenPropertiesDoNotConflict_thenDoesNotThrow() {
        Properties properties1 = new Properties();
        properties1.put("foo", "1");
        Properties properties2 = new Properties();
        properties2.put("bar", "2");

        assertDoesNotThrow(() -> ModularConfigUtils.checkForConflicts(properties1, properties2));
    }

    @Test
    void whenPropertiesContainSameKeyAndValue_thenDoesNotThrow() {
        Properties properties1 = new Properties();
        properties1.put("foo", "1");
        properties1.put("bar", "2");
        Properties properties2 = new Properties();
        properties2.put("bar", "2");
        properties2.put("woo", "3");

        assertDoesNotThrow(() -> ModularConfigUtils.checkForConflicts(properties1, properties2));
    }

    @Test
    void whenPropertiesContainSameKeyAndDifferentValue_thenThrows() {
        Properties properties1 = new Properties();
        properties1.put("foo", "1");
        properties1.put("bar", "2");
        Properties properties2 = new Properties();
        properties2.put("bar", "5");
        properties2.put("woo", "3");

        assertThrows(ModularConfigException.class, () -> ModularConfigUtils.checkForConflicts(properties1, properties2));
    }
}