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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.config.ConfigManager.Builder;
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
        Builder builder = new Builder(new String[]{});

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("test-config-resource.properties"), ImmutableSet.of(TestConfig.class))
                .build();

        Config<TestConfig> config = configManager.getConfig(TestConfig.class);
        ImmutableList<Id<Double>> listOfIds = config.getListOfIds(FirstSubConfig.WOO);

        assertThat(listOfIds.get(0)).isEqualTo(Id.create(1));
        assertThat(listOfIds.get(1)).isEqualTo(Id.create(2));
        assertThat(listOfIds.get(2)).isEqualTo(Id.create(3));
        assertThat(listOfIds.get(3)).isEqualTo(Id.create(4));
    }

    @Test
    void loadConfigFromResourceOrFile_whenGenericIdsListIsEmpty() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder(new String[]{});

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("test-config-resource.properties"), ImmutableSet.of(TestConfig.class))
                .build();

        Config<TestConfig> config = configManager.getConfig(TestConfig.class);
        ImmutableList<Id<Double>> listOfIds = config.getListOfIds(FirstSubConfig.HOO);

        assertThat(listOfIds).isEmpty();
    }

    @Test
    void loadConfigFromResourceOrFile_whenResourceOnClassPath_usesResource() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder(new String[]{});

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("test-config-resource.properties"), ImmutableSet.of(TestConfig.class))
                .build();

        Config<TestConfig> config = configManager.getConfig(TestConfig.class);
        assertThat(config.getInt(TestConfig.FOO)).isEqualTo(1);
        assertThat(config.getInt(TestConfig.BAR)).isEqualTo(2);
    }

    @Test
    void loadConfigFromResourceOrFile_whenResourceNotOnClassPath_usesFile() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder(new String[]{});

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("src/test/test-config-file.properties"), ImmutableSet.of(TestConfig.class))
                .build();

        Config<TestConfig> config = configManager.getConfig(TestConfig.class);
        assertThat(config.getInt(TestConfig.FOO)).isEqualTo(2);
        assertThat(config.getInt(TestConfig.BAR)).isEqualTo(1);
    }

    @Test
    void getPrefixes() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder(new String[]{});

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
        Builder builder = new Builder(new String[]{});

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/prefixes-test-config-file.properties"),
                ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfig> testConfig = configManager.getConfig(TestConfig.class);

        // Default (initial) config values.
        assertThat(testConfig.getInt(TestConfig.FOO)).isEqualTo(1);
        assertThat(testConfig.getInt(TestConfig.BAR)).isEqualTo(2);

        Config<TestConfig> prefixedConfig = testConfig.getPrefixedConfigItems("Prefix1");

        // Enum keys for prefixed config items. Note that keys with a different Prefix (e.g. BAZ) will still appear in getValues.
        assertThat(prefixedConfig.getValues().keySet()).isEqualTo(ImmutableSet.of(
                TestConfig.FOO,
                TestConfig.BAR,
                TestConfig.BAZ));

        // Get values for prefixed config files.
        assertThat(prefixedConfig.getInt(TestConfig.FOO)).isEqualTo(3);
        assertThat(prefixedConfig.getInt(TestConfig.BAR)).isEqualTo(4);
        assertThat(prefixedConfig.containsKey(TestConfig.BAZ)).isFalse();
    }

    @Test
    void loadPrefixedConfigItems_clashingNames() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder(new String[]{});

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/prefixes-test-config-file.properties"),
                ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfig> testConfig = configManager.getConfig(TestConfig.class);

        Config<TestConfig> prefixedConfig = testConfig.getPrefixedConfigItems("Prefix6");

        // Get values for prefixed config files.
        assertThat(prefixedConfig.getInt(FirstSubConfig.WOO)).isEqualTo(1);
        assertThat(prefixedConfig.getInt(SecondSubConfig.WOO)).isEqualTo(2);
    }

    @Test
    void loadPrefixedConfigItems_noUnprefixedEquivalent() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder(new String[]{});

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/prefixes-test-config-file.properties"),
                ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfig> testConfig = configManager.getConfig(TestConfig.class);

        assertThat(testConfig.getPrefixedConfigItems("Prefix5").getInt(TestConfig.BAZ)).isEqualTo(13);
    }

    @Test
    void loadPrefixedConfigItems_prefixNotFound() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder(new String[]{});

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(
                ImmutableList.of("src/test/prefixes-test-config-file.properties"),
                ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfig> testConfig = configManager.getConfig(TestConfig.class);

        // Default (initial) config values.
        assertThat(testConfig.getInt(TestConfig.FOO)).isEqualTo(1);
        assertThat(testConfig.getInt(TestConfig.BAR)).isEqualTo(2);

        // Assertions with the default values
        assertThat(testConfig.getPrefixedConfigItems("InvalidPrefix").getInt(TestConfig.FOO)).isEqualTo(1);
        assertThat(testConfig.getPrefixedConfigItems("InvalidPrefix").getInt(TestConfig.BAR)).isEqualTo(2);
    }

    @Test
    void loadPrefixedConfigItems_multiplePrefixes_callFirstPrefix() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder(new String[]{});

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("src/test/prefixes-test-config-file.properties"), ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfig> testConfig = configManager.getConfig(TestConfig.class);

        // Default (initial) config values.
        assertThat(testConfig.getInt(TestConfig.FOO)).isEqualTo(1);
        assertThat(testConfig.getInt(TestConfig.BAR)).isEqualTo(2);

        assertThat(testConfig.getPrefixedConfigItems("Prefix2").getValues().values().stream()
                .allMatch(configValue -> configValue.currentValue == null)).isFalse();
        assertThat(testConfig.getPrefixedConfigItems("Prefix2").getValues().values().stream()
                .allMatch(configValue -> configValue.prefixedValues == null)).isFalse();
    }

    @Test
    void loadPrefixedConfigItems_multiplePrefixes_callSecondPrefix() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder(new String[]{});

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("src/test/prefixes-test-config-file.properties"), ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfig> testConfig = configManager.getConfig(TestConfig.class);

        // Default (initial) config values.
        assertThat(testConfig.getInt(TestConfig.FOO)).isEqualTo(1);
        assertThat(testConfig.getInt(TestConfig.BAR)).isEqualTo(2);

        assertThat(testConfig.getPrefixedConfigItems("Prefix3").getValues().values().stream()
                .allMatch(configValue -> configValue.currentValue == null)).isFalse();
        assertThat(testConfig.getPrefixedConfigItems("Prefix3").getValues().values().stream()
                .allMatch(configValue -> configValue.prefixedValues == null)).isFalse();
    }

    @Test
    void loadPrefixedConfigItems_multiplePrefixes_callBothPrefixes() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder(new String[]{});

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("src/test/prefixes-test-config-file.properties"), ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfig> testConfig = configManager.getConfig(TestConfig.class);

        // Default (initial) config values.
        assertThat(testConfig.getInt(TestConfig.FOO)).isEqualTo(1);
        assertThat(testConfig.getInt(TestConfig.BAR)).isEqualTo(2);

        assertThat(testConfig.getPrefixedConfigItems("Prefix2").getPrefixedConfigItems("Prefix3").getInt(TestConfig.BAR)).isEqualTo(5);
        assertThat(testConfig.getPrefixedConfigItems("Prefix2").getPrefixedConfigItems("Prefix3").getInt(TestConfig.FOO)).isEqualTo(6);
    }

    @Test
    void loadPrefixedConfigItems_testCallOrder() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder(new String[]{});

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
        Builder builder = new Builder(new String[]{});

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("src/test/prefixes-test-config-file.properties"), ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfigDummy> testConfig = configManager.getConfig(TestConfigDummy.class);

        // Default (initial) config values.
        assertThat(testConfig.getInt(TestConfigDummy.BAR)).isEqualTo(10);
        assertThat(testConfig.getInt(TestConfigDummy.FOO)).isEqualTo(12);

        assertThat(testConfig.getPrefixedConfigItems("Prefix4").getInt(TestConfigDummy.BAR)).isEqualTo(11);
        assertThat(testConfig.getPrefixedConfigItems("Prefix4").getInt(TestConfigDummy.FOO)).isEqualTo(12);
    }

    @Test
    void loadPrefixedConfigItems_singlePrefix_testSubConfigs() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder(new String[]{});

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("src/test/prefixes-test-config-file.properties"), ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfig.Colours> subTestConfig = configManager.getConfig(TestConfig.class).getSubConfig(TestConfig.Colours.class);

        assertThat(subTestConfig.getInt(Colours.BLUE)).isEqualTo(7);

        assertThat(subTestConfig.getPrefixedConfigItems("Prefix1").getInt(TestConfig.Colours.RED)).isEqualTo(8);
        assertThat(subTestConfig.getPrefixedConfigItems("Prefix1").getInt(TestConfig.Colours.GREEN)).isEqualTo(9);
    }

    @Test
    void loadPrefixedConfigItems_testSubConfigPenetration() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder(new String[]{});

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("src/test/prefixes-test-config-file.properties"), ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfig> testConfig = configManager.getConfig(TestConfig.class);

        // Default values
        assertThat(testConfig.getInt(Colours.RED)).isEqualTo(80);
        assertThat(testConfig.getInt(Colours.GREEN)).isEqualTo(90);

        // Prefixed values
        assertThat(testConfig.getPrefixedConfigItems("Prefix1").getInt(Colours.RED)).isEqualTo(8);
        assertThat(testConfig.getPrefixedConfigItems("Prefix1").getInt(Colours.GREEN)).isEqualTo(9);
    }

    @Test
    void loadPrefixedConfigItems_testCommandLineOverrides() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder(new String[]{"-OPrefix1@TestConfig.FOO=100"});

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("src/test/prefixes-test-config-file.properties"), ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                .build();

        Config<TestConfig> testConfig = configManager.getConfig(TestConfig.class);
        assertThat(testConfig.getPrefixedConfigItems("Prefix1").getInt(TestConfig.FOO)).isEqualTo(100);
        assertThat(testConfig.getInt(TestConfig.FOO)).isEqualTo(1);
    }

    @Test
    void loadPrefixedConfigItems_whenCommandLineOverrideHasNonExistingPrefixedConfig_thenThrowException() {
        Builder builder = new Builder(new String[]{ "-OPrefix1@TestConfig.NON_EXISTING_CONFIG=100" });

        ConfigKeysNotRecognisedException configKeysNotRecognisedException = Assertions.assertThrows(ConfigKeysNotRecognisedException.class, () ->
                builder
                        .loadConfigFromResourceOrFile(ImmutableList.of("src/test/prefixes-test-config-file.properties"), ImmutableSet.of(TestConfig.class, TestConfigDummy.class))
                        .build());

        assertThat(configKeysNotRecognisedException.getMessage()).isEqualTo("The following config keys were not recognised:[Prefix1@TestConfig.NON_EXISTING_CONFIG]");
    }

    @Test
    void loadConfigFromResourceOrFile_whenFileInBothLocations_thenDefaultToTheClasspath() throws IOException, ConfigKeysNotRecognisedException {
        Builder builder = new Builder(new String[]{});

        ConfigManager configManager = builder.loadConfigFromResourceOrFile(ImmutableList.of("src/test/potentially-overridden-file.properties"), ImmutableSet.of(TestConfig.class))
                .build();

        Config<TestConfig> config = configManager.getConfig(TestConfig.class);
        assertThat(config.getInt(TestConfig.FOO)).isEqualTo(1);
        assertThat(config.getInt(TestConfig.BAR)).isEqualTo(2);
    }

    @Test
    void loadConfigFromResourceOrFile_whenFileDoesNotExist_thenThrow() {
        Builder builder = new Builder(new String[]{});
        assertThatThrownBy(() -> builder.loadConfigFromResourceOrFile(ImmutableList.of("not-a-file.properties"), ImmutableSet.of(TestConfig.class)))
                .isInstanceOf(IOException.class);
    }

    @Test
    void loadConfigFromEnvVarsSingleClass() throws Exception {
        ConfigManager.Builder builder = new ConfigManager.Builder(new String[]{});

        ConfigManager configManager = builder.loadConfigFromEnvironmentVariables(
                ImmutableMap.of("FOO", "1", "BAR", "2"), ImmutableSet.of(TestConfig.class)
        ).build();

        Config<TestConfig> config = configManager.getConfig(TestConfig.class);
        assertThat(config.getInt(TestConfig.FOO)).isEqualTo(1);
        assertThat(config.getInt(TestConfig.BAR)).isEqualTo(2);
    }

    @Test
    void loadConfigFromEnvVarsMultipleClassesNoOverlappingEnums() throws Exception {
        ConfigManager.Builder builder = new ConfigManager.Builder(new String[]{});

        ConfigManager configManager = builder.loadConfigFromEnvironmentVariables(
                ImmutableMap.of("MOO", "5", "QUACK", "4"),
                ImmutableSet.of(TestConfigTwo.class, TestConfigThree.class)
        ).build();

        assertThat(configManager.getConfig(TestConfigTwo.class).getInt(TestConfigTwo.MOO)).isEqualTo(5);
        assertThat(configManager.getConfig(TestConfigThree.class).getInt(TestConfigThree.QUACK)).isEqualTo(4);
    }

    @Test
    void loadConfigFromEnvVarsMultipleClassesOverlappingEnumsNotSet() throws Exception {
        ConfigManager.Builder builder = new ConfigManager.Builder(new String[]{});

        ConfigManager configManager = builder.loadConfigFromEnvironmentVariables(
                ImmutableMap.of("BAR", "4", "MOO", "5"),
                ImmutableSet.of(TestConfig.class, TestConfigTwo.class)
        ).build();

        assertThat(configManager.getConfig(TestConfig.class).getInt(TestConfig.BAR)).isEqualTo(4);
        assertThat(configManager.getConfig(TestConfigTwo.class).getInt(TestConfigTwo.MOO)).isEqualTo(5);
    }

    @Test
    void loadConfigFromEnvVarsUnrecognisedValuesNoException() throws Exception {
        ConfigManager.Builder builder = new ConfigManager.Builder(new String[]{});
        ConfigManager configManager = builder.loadConfigFromEnvironmentVariables(
                ImmutableMap.of("UNKNOWN-KEY", "5"), ImmutableSet.of(TestConfig.class)
        ).build();
        configManager.getConfig(TestConfig.class);
    }

    @Test
    void commandLineArgsTakePrecedenceOverEnvVars() throws Exception {
        ConfigManager.Builder builder = new ConfigManager.Builder(new String[]{"-OTestConfig.FOO=20"});

        ConfigManager configManager = builder.loadConfigFromEnvironmentVariables(
                ImmutableMap.of("FOO", "1", "BAR", "2"), ImmutableSet.of(TestConfig.class)
        ).build();

        Config<TestConfig> config = configManager.getConfig(TestConfig.class);
        assertThat(config.getInt(TestConfig.FOO)).isEqualTo(20);
        assertThat(config.getInt(TestConfig.BAR)).isEqualTo(2);
    }

    @Test
    void environmentVariableMatchesEnumInSubclass() throws Exception {
        ConfigManager.Builder builder = new ConfigManager.Builder(new String[]{});
        ConfigManager configManager = builder.loadConfigFromEnvironmentVariables(
                ImmutableMap.of("HOO", "1"), ImmutableSet.of(TestConfig.class)
        ).build();

        Config<TestConfig> config = configManager.getConfig(TestConfig.class);
        assertThat(config.getInt(FirstSubConfig.HOO)).isEqualTo(1);
    }

    @Test
    void environmentVariableMatchesEnumInMultipleSubclassFails() {
        ConfigManager.Builder builder = new ConfigManager.Builder(new String[]{});
        ImmutableMap<String, String> envVars = ImmutableMap.of("FOO", "1", "DUPLICATED_KEY", "2");

        assertThatThrownBy(() -> builder.loadConfigFromEnvironmentVariables(envVars, ImmutableSet.of(TestConfig.class)))
                .isInstanceOf(DuplicateMatchingEnvironmentVariableException.class);
    }

    @Test
    void environmentVariableMatchesEnumAcrossMultipleClassesFails() {
        ConfigManager.Builder builder = new ConfigManager.Builder(new String[]{});

        ImmutableMap<String, String> envVars = ImmutableMap.of("FOO", "1", "BAR", "2");
        assertThatThrownBy(() -> builder.loadConfigFromEnvironmentVariables(envVars, ImmutableSet.of(TestConfig.class, TestConfigTwo.class)).build())
                .isInstanceOf(DuplicateMatchingEnvironmentVariableException.class);
    }

    @Test
    void junkCommandLineArgsStillThrowExceptionWhenLoadingEnvironmentVariables() {
        ConfigManager.Builder builder = new ConfigManager.Builder(new String[]{"-OTestConfig.INVALID=1"});
        ImmutableMap<String, String> envVars = ImmutableMap.of("FOO", "1");

        assertThatThrownBy(() -> builder.loadConfigFromEnvironmentVariables(envVars, ImmutableSet.of(TestConfig.class)).build())
                .isInstanceOf(ConfigKeysNotRecognisedException.class);
    }

    @Test
    void loadConfigFromResource_whenUsingAlternateArg() throws ConfigKeysNotRecognisedException {
        ConfigManager cm = new ConfigManager.Builder(new String[] { "-a=test-config-resource.properties" }).withConfig(TestConfig.class).build();
        assertThat(cm.getConfig(TestConfig.class).containsKey(TestConfig.FOO)).isEqualTo(true);
    }

    @Test
    void loadConfigFromResource_whenUsingAlternateArgThenActualCommandLineTakesPriority() throws ConfigKeysNotRecognisedException {
        {
            ConfigManager cm1 = new ConfigManager.Builder(new String[]{"-a:test-config-resource.properties"}).withConfig(TestConfig.class).build();
            assertThat(cm1.getConfig(TestConfig.class).getInt(TestConfig.FOO)).isEqualTo(1);
        }
        {
            ConfigManager cm2 = new ConfigManager.Builder(new String[]{"-a:test-config-resource.properties", "-OTestConfig.FOO=2"}).withConfig(TestConfig.class).build();
            assertThat(cm2.getConfig(TestConfig.class).getInt(TestConfig.FOO)).isEqualTo(2);
        }
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
