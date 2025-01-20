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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Test
    void whenOutputPathWouldOverrideOriginalFile_thenThrows() {
        File config = new File("test.properties");
        assertThrows(IllegalArgumentException.class, () -> ModularConfigUtils.writeToFileMergedConfig(config, "test"));
    }

    @Test
    void whenPropertiesExtends_thenFullFileCreatedFormatted() throws IOException {
        File entryFile = new File("entry.properties");
        File wooFile = new File("woo.properties");
        File outputFile = new File("output.properties");

        entryFile.deleteOnExit();
        wooFile.deleteOnExit();
        outputFile.deleteOnExit();

        Properties entryProp = new Properties();
        entryProp.put(ModularConfigUtils.EXTENDS, "woo");
        entryProp.put("foo.1_one", "1");
        entryProp.put("bar.1_one", "2");
        entryProp.put("foo.2_two", "1");
        entryProp.put("bar.2_two", "2");
        entryProp.put("foo.3_three", "1");
        entryProp.put("bar.3_three", "2");

        Properties wooProp = new Properties();
        wooProp.put("woo.1_one", "1");
        wooProp.put("bar.2_two", "4");
        wooProp.put("foo.1_one", "3");

        try (FileWriter entryWriter = new FileWriter(entryFile); FileWriter wooWriter = new FileWriter(wooFile)) {
            entryProp.store(entryWriter, "entry config");
            wooProp.store(wooWriter, "");
        }

        ModularConfigUtils.writeToFileMergedConfig(entryFile, outputFile.getPath());

        String fileContent = Files.readString(Path.of(outputFile.getPath()));
        // %n inside a String.format will use System.lineSeparator
        String expectedContent = String.format(
                "bar.1_one=2%n" +
                "bar.2_two=2%n" +
                "bar.3_three=2%n" +
                "%n" +
                "foo.1_one=1%n" +
                "foo.2_two=1%n" +
                "foo.3_three=1%n" +
                "%n" +
                "woo.1_one=1%n"
        );
        assertEquals(fileContent, expectedContent);
    }

}