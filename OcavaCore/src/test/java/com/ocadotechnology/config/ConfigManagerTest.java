/*
 * Copyright © 2017-2021 Ocado (Ocava)
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.config.ConfigManager.Builder;
import com.ocadotechnology.config.ConfigManager.ConfigDataSource;
import com.ocadotechnology.config.ConfigManager.PrefixedProperty;
import com.ocadotechnology.config.TestConfig.Colours;
import com.ocadotechnology.config.TestConfig.FirstSubConfig;
import com.ocadotechnology.config.TestConfig.SecondSubConfig;
import com.ocadotechnology.id.Id;

class ConfigManagerTest {

    @SuppressWarnings("unused") // 'FOO' deliberately clashes with TestConfig
    private enum TestConfigTwo {
        FOO,
        MOO
    }

    private enum TestConfigThree {
        QUACK
    }

    @Test
    void loadConfigFromResourceOrFile_testGenericIdsParsing() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder();

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("test-config-resource.properties"), ImmutableSet.of(TestConfig.class))
                .build();

        Config<TestConfig> config = configManager.getConfig(TestConfig.class);
        ImmutableList<Id<Double>> listOfIds = config.getValue(FirstSubConfig.WOO).asList().ofIds();

        assertThat(listOfIds.get(0)).isEqualTo(Id.create(1));
        assertThat(listOfIds.get(1)).isEqualTo(Id.create(2));
        assertThat(listOfIds.get(2)).isEqualTo(Id.create(3));
        assertThat(listOfIds.get(3)).isEqualTo(Id.create(4));
    }

    @Test
    void loadConfigFromResourceOrFile_whenGenericIdsListIsEmpty() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder();

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("test-config-resource.properties"), ImmutableSet.of(TestConfig.class))
                .build();

        Config<TestConfig> config = configManager.getConfig(TestConfig.class);
        ImmutableList<Id<Double>> listOfIds = config.getValue(FirstSubConfig.HOO).asList().ofIds();

        assertThat(listOfIds).isEmpty();
    }

    @Test
    void loadConfigFromResourceOrFile_whenResourceOnClassPath_usesResource() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder();

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("test-config-resource.properties"), ImmutableSet.of(TestConfig.class))
                .build();

        Config<TestConfig> config = configManager.getConfig(TestConfig.class);
        assertThat(config.getValue(TestConfig.FOO).asInt()).isEqualTo(1);
        assertThat(config.getValue(TestConfig.BAR).asInt()).isEqualTo(2);
    }

    @Test
    void loadConfigFromResourceOrFile_whenResourceNotOnClassPath_usesFile() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder();

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("src/test/test-config-file.properties"), ImmutableSet.of(TestConfig.class))
                .build();

        Config<TestConfig> config = configManager.getConfig(TestConfig.class);
        assertThat(config.getValue(TestConfig.FOO).asInt()).isEqualTo(2);
        assertThat(config.getValue(TestConfig.BAR).asInt()).isEqualTo(1);
    }

    @Test
    void loadConfigFromInputStream_testGenericIdsParsing() throws IOException, ConfigKeysNotRecognisedException {
        InputStream inputStream = new FileInputStream("src/test/resources/test-config-resource.properties");
        Builder builder = new Builder();

        ConfigManager configManager = builder.loadConfig(ImmutableList.of(ConfigDataSource.fromInputStream(inputStream)), ImmutableSet.of(TestConfig.class))
                .build();

        Config<TestConfig> config = configManager.getConfig(TestConfig.class);
        ImmutableList<Id<Double>> listOfIds = config.getValue(FirstSubConfig.WOO).asList().ofIds();

        assertThat(listOfIds.get(0)).isEqualTo(Id.create(1));
        assertThat(listOfIds.get(1)).isEqualTo(Id.create(2));
        assertThat(listOfIds.get(2)).isEqualTo(Id.create(3));
        assertThat(listOfIds.get(3)).isEqualTo(Id.create(4));
    }

    @Test
    void whenLoadConfigTwice_thenKeepsOldDataWhereNewDataDoesNotOverride() throws ConfigKeysNotRecognisedException {
        ImmutableMap<String, String> map1 = ImmutableMap.of(
                "TestConfig.FOO", "0",
                "TestConfig.BAR", "1");
        ImmutableMap<String, String> map2 = ImmutableMap.of(
                "TestConfig.BAR", "2",
                "TestConfig.BAZ", "1");

        Builder builder = new Builder();
        builder.loadConfigFromMap(map1, ImmutableSet.of(TestConfig.class));
        Config<TestConfig> config = builder.build().getConfig(TestConfig.class);
        assertThat(config.getValue(TestConfig.FOO).asInt()).isEqualTo(0);
        assertThat(config.getValue(TestConfig.BAR).asInt()).isEqualTo(1);

        builder.loadConfigFromMap(map2, ImmutableSet.of(TestConfig.class));
        config = builder.build().getConfig(TestConfig.class);
        assertThat(config.getValue(TestConfig.FOO).asInt()).isEqualTo(0);
        assertThat(config.getValue(TestConfig.BAR).asInt()).isEqualTo(2);
        assertThat(config.getValue(TestConfig.BAZ).asInt()).isEqualTo(1);
    }

    @Test
    void builderGetConfigUnchecked_whenKeyIsTopLevel_thenReturnsValue() throws IOException {
        Builder builder = new Builder().loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/test-config-file.properties"),
                ImmutableSet.of(TestConfig.class));

        assertThat(builder.getConfigUnchecked(TestConfig.FOO)).isEqualTo("2");
    }

    @Test
    void builderGetConfigUnchecked_whenKeyIsSubLevel_thenReturnsValue() throws IOException {
        Builder builder = new Builder().loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/test-config-file.properties"),
                ImmutableSet.of(TestConfig.class));

        assertThat(builder.getConfigUnchecked(Colours.BLUE)).isEqualTo("7");
    }

    @Test
    void builderGetConfigUnchecked_whenKeyIsSubSubLevel_thenReturnsValue() throws IOException {
        Builder builder = new Builder().loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/test-config-file.properties"),
                ImmutableSet.of(TestConfig.class));

        assertThat(builder.getConfigUnchecked(TestConfig.FirstSubConfig.SubSubConfig.X)).isEqualTo("3");
    }

    @Test
    void builderGetConfigUnchecked_whenKeyIsExplicitlySetToEmpty_thenReturnsEmptyString() throws IOException {
        Builder builder = new Builder().loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/test-config-file.properties"),
                ImmutableSet.of(TestConfig.class));

        assertThat(builder.getConfigUnchecked(TestConfig.EMPTY)).isEqualTo("");
    }

    @Test
    void builderGetConfigUnchecked_whenKeyIsNotExplicitlySet_thenThrowsConfigKeyNotFoundException() throws IOException {
        Builder builder = new Builder().loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/test-config-file.properties"),
                ImmutableSet.of(TestConfig.class));

        Assertions.assertThrows(ConfigKeyNotFoundException.class, () -> builder.getConfigUnchecked(TestConfig.BAZ));
    }

    @Test
    void builderGetConfigUnchecked_whenKeyIsFromWrongConfig_thenThrowsIllegalArgumentException() throws IOException {
        Builder builder = new Builder().loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/test-config-file.properties"),
                ImmutableSet.of(TestConfig.class));

        Assertions.assertThrows(IllegalArgumentException.class, () -> builder.getConfigUnchecked(TestConfigDummy.FOO));
    }

    @Test
    void builderGetConfigIfKeyAndValueDefinedUnchecked_whenKeyIsTopLevel_thenReturnsValue() throws IOException {
        Builder builder = new Builder().loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/test-config-file.properties"),
                ImmutableSet.of(TestConfig.class));

        assertThat(builder.getConfigIfKeyAndValueDefinedUnchecked(TestConfig.FOO)).isEqualTo(Optional.of("2"));
    }

    @Test
    void builderGetConfigIfKeyAndValueDefinedUnchecked_whenKeyIsSubLevel_thenReturnsValue() throws IOException {
        Builder builder = new Builder().loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/test-config-file.properties"),
                ImmutableSet.of(TestConfig.class));

        assertThat(builder.getConfigIfKeyAndValueDefinedUnchecked(Colours.BLUE)).isEqualTo(Optional.of("7"));
    }

    @Test
    void builderGetConfigIfKeyAndValueDefinedUnchecked_whenKeyIsSubSubLevel_thenReturnsValue() throws IOException {
        Builder builder = new Builder().loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/test-config-file.properties"),
                ImmutableSet.of(TestConfig.class));

        assertThat(builder.getConfigIfKeyAndValueDefinedUnchecked(TestConfig.FirstSubConfig.SubSubConfig.X)).isEqualTo(Optional.of("3"));
    }

    @Test
    void builderGetConfigIfKeyAndValueDefinedUnchecked_whenKeyIsExplicitlySetToEmpty_thenReturnsOptionalEmpty() throws IOException {
        Builder builder = new Builder().loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/test-config-file.properties"),
                ImmutableSet.of(TestConfig.class));

        assertThat(builder.getConfigIfKeyAndValueDefinedUnchecked(TestConfig.EMPTY)).isEqualTo(Optional.empty());
    }

    @Test
    void builderGetConfigIfKeyAndValueDefinedUnchecked_whenKeyIsNotExplicitlySet_thenReturnsOptionalEmpty() throws IOException {
        Builder builder = new Builder().loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/test-config-file.properties"),
                ImmutableSet.of(TestConfig.class));

        assertThat(builder.getConfigIfKeyAndValueDefinedUnchecked(TestConfig.EMPTY)).isEqualTo(Optional.empty());
    }

    @Test
    void builderGetConfigIfKeyAndValueDefinedUnchecked_whenKeyIsFromWrongConfig_thenThrowsIllegalArgumentException() throws IOException {
        Builder builder = new Builder().loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/test-config-file.properties"),
                ImmutableSet.of(TestConfig.class));

        Assertions.assertThrows(IllegalArgumentException.class, () -> builder.getConfigUnchecked(TestConfigDummy.FOO));
    }

    @Test
    void builderAreKeyAndValueDefinedUnchecked_whenValueIsNonEmpty_thenReturnsTrue() throws IOException {
        Builder builder = new Builder().loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/test-config-file.properties"),
                ImmutableSet.of(TestConfig.class));

        assertThat(builder.areKeyAndValueDefinedUnchecked(TestConfig.FOO)).isTrue();
    }

    @Test
    void builderAreKeyAndValueDefinedUnchecked_whenValueIsExplicitlySetEmpty_thenReturnsFalse() throws IOException {
        Builder builder = new Builder().loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/test-config-file.properties"),
                ImmutableSet.of(TestConfig.class));

        assertThat(builder.areKeyAndValueDefinedUnchecked(TestConfig.EMPTY)).isFalse();
    }

    @Test
    void builderAreKeyAndValueDefinedUnchecked_whenValueIsNotExplicitlySet_thenReturnsFalse() throws IOException {
        Builder builder = new Builder().loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/test-config-file.properties"),
                ImmutableSet.of(TestConfig.class));

        assertThat(builder.areKeyAndValueDefinedUnchecked(TestConfig.BAZ)).isFalse();
    }

    @Test
    void builderAreKeyAndValueDefinedUnchecked_whenKeyIsFromWrongConfig_thenReturnsFalse() throws IOException {
        Builder builder = new Builder().loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/test-config-file.properties"),
                ImmutableSet.of(TestConfig.class));

        assertThat(builder.areKeyAndValueDefinedUnchecked(TestConfigDummy.FOO)).isFalse();
    }

    @Test
    void getPrefixes() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder();

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/prefixes-test-config-file.properties"),
                ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        ImmutableSet<String> actual = configManager.getConfig(TestConfig.class).getPrefixes();

        ImmutableSet<String> expected = ImmutableSet.of(
                "Prefix1",
                "Prefix2",
                "Prefix3",
                "Prefix5",
                "Prefix6");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void loadPrefixedConfigItems_singlePrefix() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder();

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/prefixes-test-config-file.properties"),
                ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfig> testConfig = configManager.getConfig(TestConfig.class);

        // Default (initial) config values.
        assertThat(testConfig.getValue(TestConfig.FOO).asInt()).isEqualTo(1);
        assertThat(testConfig.getValue(TestConfig.BAR).asInt()).isEqualTo(2);

        Config<TestConfig> prefixedConfig = testConfig.getPrefixedConfigItems("Prefix1");

        // Enum keys for prefixed config items. Note that keys with a different Prefix (e.g. BAZ) will still appear in getValues.
        assertThat(prefixedConfig.getValues().keySet()).isEqualTo(ImmutableSet.of(
                TestConfig.FOO,
                TestConfig.BAR,
                TestConfig.BAZ));

        // Get values for prefixed config files.
        assertThat(prefixedConfig.getValue(TestConfig.FOO).asInt()).isEqualTo(3);
        assertThat(prefixedConfig.getValue(TestConfig.BAR).asInt()).isEqualTo(4);
        assertThat(prefixedConfig.areKeyAndValueDefined(TestConfig.BAZ)).isFalse();
    }

    @Test
    void loadPrefixedConfigItems_clashingNames() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder();

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/prefixes-test-config-file.properties"),
                ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfig> testConfig = configManager.getConfig(TestConfig.class);

        Config<TestConfig> prefixedConfig = testConfig.getPrefixedConfigItems("Prefix6");

        // Get values for prefixed config files.
        assertThat(prefixedConfig.getValue(FirstSubConfig.WOO).asInt()).isEqualTo(1);
        assertThat(prefixedConfig.getValue(SecondSubConfig.WOO).asInt()).isEqualTo(2);
    }

    @Test
    void loadPrefixedConfigItems_noUnprefixedEquivalent() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder();

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/prefixes-test-config-file.properties"),
                ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfig> testConfig = configManager.getConfig(TestConfig.class);

        assertThat(testConfig.getPrefixedConfigItems("Prefix5").getValue(TestConfig.BAZ).asInt()).isEqualTo(13);
    }

    @Test
    void loadPrefixedConfigItems_prefixNotFound() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder();

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/prefixes-test-config-file.properties"),
                ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfig> testConfig = configManager.getConfig(TestConfig.class);

        // Default (initial) config values.
        assertThat(testConfig.getValue(TestConfig.FOO).asInt()).isEqualTo(1);
        assertThat(testConfig.getValue(TestConfig.BAR).asInt()).isEqualTo(2);

        // Assertions with the default values
        assertThat(testConfig.getPrefixedConfigItems("InvalidPrefix").getValue(TestConfig.FOO).asInt()).isEqualTo(1);
        assertThat(testConfig.getPrefixedConfigItems("InvalidPrefix").getValue(TestConfig.BAR).asInt()).isEqualTo(2);
    }

    @Test
    void loadPrefixedConfigItems_multiplePrefixes_callFirstPrefix() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder();

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("src/test/prefixes-test-config-file.properties"), ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfig> testConfig = configManager.getConfig(TestConfig.class);

        // Default (initial) config values.
        assertThat(testConfig.getValue(TestConfig.FOO).asInt()).isEqualTo(1);
        assertThat(testConfig.getValue(TestConfig.BAR).asInt()).isEqualTo(2);

        assertThat(testConfig.getPrefixedConfigItems("Prefix2").getValues().values().stream()
                .allMatch(configValue -> configValue.currentValue == null)).isFalse();
        assertThat(testConfig.getPrefixedConfigItems("Prefix2").getValues().values().stream()
                .allMatch(configValue -> configValue.prefixedValues == null)).isFalse();
    }

    @Test
    void loadPrefixedConfigItems_multiplePrefixes_callSecondPrefix() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder();

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("src/test/prefixes-test-config-file.properties"), ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfig> testConfig = configManager.getConfig(TestConfig.class);

        // Default (initial) config values.
        assertThat(testConfig.getValue(TestConfig.FOO).asInt()).isEqualTo(1);
        assertThat(testConfig.getValue(TestConfig.BAR).asInt()).isEqualTo(2);

        assertThat(testConfig.getPrefixedConfigItems("Prefix3").getValues().values().stream()
                .allMatch(configValue -> configValue.currentValue == null)).isFalse();
        assertThat(testConfig.getPrefixedConfigItems("Prefix3").getValues().values().stream()
                .allMatch(configValue -> configValue.prefixedValues == null)).isFalse();
    }

    @Test
    void loadPrefixedConfigItems_multiplePrefixes_callBothPrefixes() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder();

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("src/test/prefixes-test-config-file.properties"), ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfig> testConfig = configManager.getConfig(TestConfig.class);

        // Default (initial) config values.
        assertThat(testConfig.getValue(TestConfig.FOO).asInt()).isEqualTo(1);
        assertThat(testConfig.getValue(TestConfig.BAR).asInt()).isEqualTo(2);

        assertThat(testConfig.getPrefixedConfigItems("Prefix2").getPrefixedConfigItems("Prefix3").getValue(TestConfig.BAR).asInt()).isEqualTo(5);
        assertThat(testConfig.getPrefixedConfigItems("Prefix2").getPrefixedConfigItems("Prefix3").getValue(TestConfig.FOO).asInt()).isEqualTo(6);
    }

    @Test
    void loadPrefixedConfigItems_testCallOrder() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder();

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("src/test/prefixes-test-config-file.properties"), ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfig> testConfig = configManager.getConfig(TestConfig.class);

        Config<TestConfig.Colours> testConfigPrefixSubConfig = testConfig.getPrefixedConfigItems("Prefix1")
                .getSubConfig(Colours.class);
        Config<TestConfig.Colours> testConfigSubConfigPrefix = testConfig.getSubConfig(Colours.class)
                .getPrefixedConfigItems("Prefix1");

        assertThat(testConfigPrefixSubConfig.getValues().keySet()).isEqualTo(testConfigSubConfigPrefix.getValues().keySet());
    }

    @Test
    void loadPrefixedConfigItems_testDefaultCurrentValue() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder();

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("src/test/prefixes-test-config-file.properties"), ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfigDummy> testConfig = configManager.getConfig(TestConfigDummy.class);

        // Default (initial) config values.
        assertThat(testConfig.getValue(TestConfigDummy.BAR).asInt()).isEqualTo(10);
        assertThat(testConfig.getValue(TestConfigDummy.FOO).asInt()).isEqualTo(12);

        assertThat(testConfig.getPrefixedConfigItems("Prefix4").getValue(TestConfigDummy.BAR).asInt()).isEqualTo(11);
        assertThat(testConfig.getPrefixedConfigItems("Prefix4").getValue(TestConfigDummy.FOO).asInt()).isEqualTo(12);
    }

    @Test
    void loadPrefixedConfigItems_singlePrefix_testSubConfigs() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder();

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("src/test/prefixes-test-config-file.properties"), ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfig.Colours> subTestConfig = configManager.getConfig(TestConfig.class).getSubConfig(TestConfig.Colours.class);

        assertThat(subTestConfig.getValue(Colours.BLUE).asInt()).isEqualTo(7);

        assertThat(subTestConfig.getPrefixedConfigItems("Prefix1").getValue(TestConfig.Colours.RED).asInt()).isEqualTo(8);
        assertThat(subTestConfig.getPrefixedConfigItems("Prefix1").getValue(TestConfig.Colours.GREEN).asInt()).isEqualTo(9);
    }

    @Test
    void loadPrefixedConfigItems_testSubConfigPenetration() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder();

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("src/test/prefixes-test-config-file.properties"), ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfig> testConfig = configManager.getConfig(TestConfig.class);

        // Default values
        assertThat(testConfig.getValue(Colours.RED).asInt()).isEqualTo(80);
        assertThat(testConfig.getValue(Colours.GREEN).asInt()).isEqualTo(90);

        // Prefixed values
        assertThat(testConfig.getPrefixedConfigItems("Prefix1").getValue(Colours.RED).asInt()).isEqualTo(8);
        assertThat(testConfig.getPrefixedConfigItems("Prefix1").getValue(Colours.GREEN).asInt()).isEqualTo(9);
    }

    @Test
    void loadPrefixedConfigItems_testCommandLineOverrides() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder("-OPrefix1@TestConfig.FOO=100");

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("src/test/prefixes-test-config-file.properties"), ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfig> testConfig = configManager.getConfig(TestConfig.class);
        assertThat(testConfig.getPrefixedConfigItems("Prefix1").getValue(TestConfig.FOO).asInt()).isEqualTo(100);
        assertThat(testConfig.getValue(TestConfig.FOO).asInt()).isEqualTo(1);
    }

    @Test
    void loadPrefixedConfigItems_whenCommandLineOverrideHasNonExistingPrefixedConfig_thenThrowException() {
        Builder builder = new Builder("-OPrefix1@TestConfig.NON_EXISTING_CONFIG=100");

        ConfigKeysNotRecognisedException configKeysNotRecognisedException = Assertions.assertThrows(ConfigKeysNotRecognisedException.class, () ->
                builder
                        .loadConfigFromResourceOrFile(ImmutableList.of("src/test/prefixes-test-config-file.properties"), ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                        .build());

        assertThat(configKeysNotRecognisedException.getMessage()).isEqualTo("The following config keys were not recognised:[Prefix1@TestConfig.NON_EXISTING_CONFIG]");
    }

    @Test
    void loadConfigFromResourceOrFile_whenFileInBothLocations_thenDefaultToTheClasspath() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder();

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("src/test/potentially-overridden-file.properties"), ImmutableSet.of(TestConfig.class))
                .build();

        Config<TestConfig> config = configManager.getConfig(TestConfig.class);
        assertThat(config.getValue(TestConfig.FOO).asInt()).isEqualTo(1);
        assertThat(config.getValue(TestConfig.BAR).asInt()).isEqualTo(2);
    }

    @Test
    void loadConfigFromResourceOrFile_whenFileDoesNotExist_thenThrow() {
        Builder builder = new Builder();
        assertThatThrownBy(() -> builder.loadConfigFromResourceOrFile(ImmutableList.of("not-a-file.properties"), ImmutableSet.of(TestConfig.class)))
                .isInstanceOf(IOException.class);
    }

    @Test
    void loadConfigFromEnvVarsSingleClass() throws Exception {
        Builder builder = new Builder();

        ConfigManager configManager = builder.loadConfigFromEnvironmentVariables(
                ImmutableMap.of("FOO", "1", "BAR", "2"), ImmutableSet.of(TestConfig.class)
        ).build();

        Config<TestConfig> config = configManager.getConfig(TestConfig.class);
        assertThat(config.getValue(TestConfig.FOO).asInt()).isEqualTo(1);
        assertThat(config.getValue(TestConfig.BAR).asInt()).isEqualTo(2);
    }

    @Test
    void loadConfigFromEnvVarsMultipleClassesNoOverlappingEnums() throws Exception {
        Builder builder = new Builder();

        ConfigManager configManager = builder.loadConfigFromEnvironmentVariables(
                ImmutableMap.of("MOO", "5", "QUACK", "4"),
                ImmutableSet.of(TestConfigTwo.class, TestConfigThree.class)
        ).build();

        assertThat(configManager.getConfig(TestConfigTwo.class).getValue(TestConfigTwo.MOO).asInt()).isEqualTo(5);
        assertThat(configManager.getConfig(TestConfigThree.class).getValue(TestConfigThree.QUACK).asInt()).isEqualTo(4);
    }

    @Test
    void loadConfigFromEnvVarsMultipleClassesOverlappingEnumsNotSet() throws Exception {
        Builder builder = new Builder();

        ConfigManager configManager = builder.loadConfigFromEnvironmentVariables(
                ImmutableMap.of("BAR", "4", "MOO", "5"),
                ImmutableSet.of(TestConfig.class, TestConfigTwo.class)
        ).build();

        assertThat(configManager.getConfig(TestConfig.class).getValue(TestConfig.BAR).asInt()).isEqualTo(4);
        assertThat(configManager.getConfig(TestConfigTwo.class).getValue(TestConfigTwo.MOO).asInt()).isEqualTo(5);
    }

    @Test
    void loadConfigFromEnvVarsUnrecognisedValuesNoException() throws Exception {
        Builder builder = new Builder();
        ConfigManager configManager = builder.loadConfigFromEnvironmentVariables(
                ImmutableMap.of("UNKNOWN-KEY", "5"), ImmutableSet.of(TestConfig.class)
        ).build();
        configManager.getConfig(TestConfig.class);
    }

    @Test
    void commandLineArgsTakePrecedenceOverEnvVars() throws Exception {
        Builder builder = new Builder("-OTestConfig.FOO=20");

        ConfigManager configManager = builder.loadConfigFromEnvironmentVariables(
                ImmutableMap.of("FOO", "1", "BAR", "2"), ImmutableSet.of(TestConfig.class)
        ).build();

        Config<TestConfig> config = configManager.getConfig(TestConfig.class);
        assertThat(config.getValue(TestConfig.FOO).asInt()).isEqualTo(20);
        assertThat(config.getValue(TestConfig.BAR).asInt()).isEqualTo(2);
    }

    @Test
    void environmentVariableMatchesEnumInSubclass() throws Exception {
        Builder builder = new Builder();
        ConfigManager configManager = builder.loadConfigFromEnvironmentVariables(
                ImmutableMap.of("HOO", "1"), ImmutableSet.of(TestConfig.class)
        ).build();

        Config<TestConfig> config = configManager.getConfig(TestConfig.class);
        assertThat(config.getValue(FirstSubConfig.HOO).asInt()).isEqualTo(1);
    }

    @Test
    void environmentVariableMatchesEnumInMultipleSubclassFails() {
        Builder builder = new Builder();
        ImmutableMap<String, String> envVars = ImmutableMap.of("FOO", "1", "DUPLICATED_KEY", "2");

        assertThatThrownBy(() -> builder.loadConfigFromEnvironmentVariables(envVars, ImmutableSet.of(TestConfig.class)))
                .isInstanceOf(DuplicateMatchingEnvironmentVariableException.class);
    }

    @Test
    void environmentVariableMatchesEnumAcrossMultipleClassesFails() {
        Builder builder = new Builder();

        ImmutableMap<String, String> envVars = ImmutableMap.of("FOO", "1", "BAR", "2");
        assertThatThrownBy(() -> builder.loadConfigFromEnvironmentVariables(envVars, ImmutableSet.of(TestConfig.class, TestConfigTwo.class)).build())
                .isInstanceOf(DuplicateMatchingEnvironmentVariableException.class);
    }

    @Test
    void junkCommandLineArgsStillThrowExceptionWhenLoadingEnvironmentVariables() {
        Builder builder = new Builder("-OTestConfig.INVALID=1");
        ImmutableMap<String, String> envVars = ImmutableMap.of("FOO", "1");

        assertThatThrownBy(() -> builder.loadConfigFromEnvironmentVariables(envVars, ImmutableSet.of(TestConfig.class)).build())
                .isInstanceOf(ConfigKeysNotRecognisedException.class);
    }

    @Test
    void loadConfigFromResource_whenUsingAlternateArg_deprecated() throws ConfigKeysNotRecognisedException {
        ConfigManager cm = new Builder("-a=test-config-resource.properties").withConfig(TestConfig.class).build();
        assertThat(cm.getConfig(TestConfig.class).areKeyAndValueDefined(TestConfig.FOO)).isTrue();
    }
    
    @Test
    void loadConfigFromResource_whenUsingAlternateArg() throws ConfigKeysNotRecognisedException {
        ConfigManager cm = new Builder("-a=test-config-resource.properties").withConfigFromCommandLine(TestConfig.class).build();
        assertThat(cm.getConfig(TestConfig.class).areKeyAndValueDefined(TestConfig.FOO)).isTrue();
    }

    @Test
    void loadConfigFromResource_whenUsingAlternateArgThenActualCommandLineTakesPriority_deprecated() throws ConfigKeysNotRecognisedException {
        {
            ConfigManager cm1 = new Builder("-a:test-config-resource.properties").withConfig(TestConfig.class).build();
            assertThat(cm1.getConfig(TestConfig.class).getValue(TestConfig.FOO).asInt()).isEqualTo(1);
        }
        {
            ConfigManager cm2 = new Builder("-a:test-config-resource.properties", "-OTestConfig.FOO=2").withConfig(TestConfig.class).build();
            assertThat(cm2.getConfig(TestConfig.class).getValue(TestConfig.FOO).asInt()).isEqualTo(2);
        }
    }
    
    @Test
    void loadConfigFromResource_whenUsingAlternateArgThenActualCommandLineTakesPriority() throws ConfigKeysNotRecognisedException {
        {
            ConfigManager cm1 = new Builder("-a:test-config-resource.properties").withConfigFromCommandLine(TestConfig.class).build();
            assertThat(cm1.getConfig(TestConfig.class).getValue(TestConfig.FOO).asInt()).isEqualTo(1);
        }
        {
            ConfigManager cm2 = new Builder("-a:test-config-resource.properties", "-OTestConfig.FOO=2").withConfigFromCommandLine(TestConfig.class).build();
            assertThat(cm2.getConfig(TestConfig.class).getValue(TestConfig.FOO).asInt()).isEqualTo(2);
        }
    }

    @Test
    void buildWithInitialConfig_whenInitialValueExists_deprecated() throws ConfigKeysNotRecognisedException {
        Config<TestConfig> initialConfig = new Config<>(
                TestConfig.class,
                ImmutableMap.of(TestConfig.FOO, new ConfigValue("1", ImmutableMap.of()), TestConfig.BAR, new ConfigValue("2", ImmutableMap.of())),
                ImmutableMap.of(),
                "TestConfig");
        ConfigManager configManager = new Builder(initialConfig)
                .build();
        Config<TestConfig> result = configManager.getConfig(TestConfig.class);
        assertThat(result.getValue(TestConfig.FOO).asInt()).isEqualTo(1);
        assertThat(result.getValue(TestConfig.BAR).asInt()).isEqualTo(2);
    }

    @Test
    void buildWithInitialConfig_whenInitialValueExists() throws ConfigKeysNotRecognisedException {
        Config<TestConfig> initialConfig = new Config<>(
                TestConfig.class,
                ImmutableMap.of(TestConfig.FOO, new ConfigValue("1", ImmutableMap.of()), TestConfig.BAR, new ConfigValue("2", ImmutableMap.of())),
                ImmutableMap.of(),
                "TestConfig");
        ConfigManager configManager = new Builder()
                .withConfigFromExisting(initialConfig)
                .build();
        Config<TestConfig> result = configManager.getConfig(TestConfig.class);
        assertThat(result.getValue(TestConfig.FOO).asInt()).isEqualTo(1);
        assertThat(result.getValue(TestConfig.BAR).asInt()).isEqualTo(2);
    }

    @Test
    void overrideInitialConfig_whenOverrideExistsThenOverrideTakesPriority_deprecated() throws ConfigKeysNotRecognisedException {
        Config<TestConfig> initialConfig = new Config<>(
                TestConfig.class,
                ImmutableMap.of(TestConfig.FOO, new ConfigValue("1", ImmutableMap.of()), TestConfig.BAR, new ConfigValue("2", ImmutableMap.of())),
                ImmutableMap.of(),
                "TestConfig");
        ConfigManager configManager = new Builder(initialConfig)
                .loadConfigFromMap(ImmutableMap.of("TestConfig.BAR", "3", "TestConfig.BAZ", "4"), ImmutableSet.of(TestConfig.class))
                .build();
        Config<TestConfig> result = configManager.getConfig(TestConfig.class);
        assertThat(result.getValue(TestConfig.FOO).asInt()).isEqualTo(1);//no override
        assertThat(result.getValue(TestConfig.BAR).asInt()).isEqualTo(3);//override exists
        assertThat(result.getValue(TestConfig.BAZ).asInt()).isEqualTo(4);//no initial value
    }

    @Test
    void overrideInitialConfig_whenOverrideExistsThenOverrideTakesPriority() throws ConfigKeysNotRecognisedException {
        Config<TestConfig> initialConfig = new Config<>(
                TestConfig.class,
                ImmutableMap.of(TestConfig.FOO, new ConfigValue("1", ImmutableMap.of()), TestConfig.BAR, new ConfigValue("2", ImmutableMap.of())),
                ImmutableMap.of(),
                "TestConfig");
        ConfigManager configManager = new Builder()
                .withConfigFromExisting(initialConfig)
                .loadConfigFromMap(ImmutableMap.of("TestConfig.BAR", "3", "TestConfig.BAZ", "4"), ImmutableSet.of(TestConfig.class))
                .build();
        Config<TestConfig> result = configManager.getConfig(TestConfig.class);
        assertThat(result.getValue(TestConfig.FOO).asInt()).isEqualTo(1);//no override
        assertThat(result.getValue(TestConfig.BAR).asInt()).isEqualTo(3);//override exists
        assertThat(result.getValue(TestConfig.BAZ).asInt()).isEqualTo(4);//no initial value
    }

    static class PrefixedPropertyTest {

        private PrefixedProperty prefixedProperty = new PrefixedProperty("", "");

        @Test
        void testPrefixedProperty_emptyPrefixedProperty() {
            checkPrefixedProperty("", "", "", "", ImmutableSet.of());
        }

        @Test
        void testPrefixedProperty_nonEmptyPrefixedProperty() {
            prefixedProperty = new PrefixedProperty("Prefix1@ConfigItem.FOO", "1");
            checkPrefixedProperty(
                    "Prefix1@ConfigItem.FOO",
                    "ConfigItem",
                    "FOO",
                    "1",
                    ImmutableSet.of("Prefix1"));
        }

        @Test
        void testPrefixedProperty_multiplePrefixes() {
            prefixedProperty = new PrefixedProperty("Prefix1@Prefix2@ConfigItem.FOO", "1");
            checkPrefixedProperty(
                    "Prefix1@Prefix2@ConfigItem.FOO",
                    "ConfigItem",
                    "FOO",
                    "1",
                    ImmutableSet.of("Prefix1", "Prefix2"));
        }

        void checkPrefixedProperty(
                String prefixedConfigItem,
                String qualifier,
                String constant,
                String propertyValue,
                ImmutableSet<String> prefixes) {

            assertThat(prefixedProperty.prefixedConfigItem).isEqualTo(prefixedConfigItem);
            assertThat(prefixedProperty.qualifier).isEqualTo(qualifier);
            assertThat(prefixedProperty.constant).isEqualTo(constant);
            assertThat(prefixedProperty.propertyValue).isEqualTo(propertyValue);
            assertThat(prefixedProperty.prefixes).isEqualTo(prefixes);

        }

    }
}
