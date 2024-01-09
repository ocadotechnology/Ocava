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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.ocadotechnology.config.TestConfig.FirstSubConfig;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.physics.units.LengthUnit;

@DisplayName("A SimpleConfigBuilder")
class SimpleConfigBuilderTest {

    private SimpleConfigBuilder<TestConfig> configBuilder;

    @BeforeEach
    void setup() {
        configBuilder = SimpleConfigBuilder.create(TestConfig.class);
    }

    @Test
    @DisplayName("can be built with no values")
    void testSimpleConfig_canBeBuiltWithNoValues() {
        assertThatCode(configBuilder::build).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("can be built using String keys")
    void testSimpleConfig_canBeBuiltWithStringKeys() {
        Config<TestConfig> config = configBuilder.put("TestConfig.FOO", "42")
                .put("TestConfig.BAR", "First")
                .put("TestConfig.FirstSubConfig.WOO", "Second")
                .put("TestConfig.FirstSubConfig.HOO", "Third")
                .buildWrapped();

        assertThat(config.getValue(TestConfig.FOO).asInt()).isEqualTo(42);
        assertThat(config.getValue(TestConfig.BAR).asString()).isEqualTo("First");
        assertThat(config.getValue(FirstSubConfig.WOO).asString()).isEqualTo("Second");
        assertThat(config.getValue(FirstSubConfig.HOO).asString()).isEqualTo("Third");
    }

    @Test
    @DisplayName("can be built using prefixes")
    void testSimpleConfig_canBeBuiltWithPrefixes() {
        Config<TestConfig> config = configBuilder
                .put("TestConfig.FOO", "10", "Prefix1")
                .put("TestConfig.FOO", "20", "Prefix2")
                .buildWrapped();

        assertThat(config.getKeyValueStringMapWithPrefixesWithoutSecrets().get("Prefix1@TestConfig.FOO")).isEqualTo("10");
        assertThat(config.getKeyValueStringMapWithPrefixesWithoutSecrets().get("Prefix2@TestConfig.FOO")).isEqualTo("20");
    }

    @Test
    @DisplayName("can be built using Enum keys")
    void testSimpleConfig_canBeBuiltWithEnumKeys() {
        Config<TestConfig> config = configBuilder.put(TestConfig.FOO, "42")
                .buildWrapped();

        assertThat(config.getValue(TestConfig.FOO).asInt()).isEqualTo(42);
    }

    @Test
    @DisplayName("can be built using Object values")
    void testSimpleConfig_canBeBuiltWithObjectValues() {
        Config<TestConfig> config = configBuilder.put(TestConfig.FOO, Id.create(42))
                .buildWrapped();

        assertThat(config.getValue(TestConfig.FOO).asInt()).isEqualTo(42);
    }

    @Test
    @DisplayName("can be built using Collection values")
    void testSimpleConfig_canBeBuiltWithCollectionValues() {
        ImmutableList<Id<Object>> of = ImmutableList.of(Id.create(42), Id.create(43), Id.create(44));
        Config<TestConfig> config = configBuilder.put(TestConfig.FOO, of)
                .buildWrapped();

        assertThat(config.getValue(TestConfig.FOO).asString()).isEqualTo("42,43,44");
    }

    @Test
    @DisplayName("can be built with time unit")
    void testSimpleConfig_canBeBuiltWithTimeUnit() {
        Config<TestConfig> config = configBuilder.put(TestConfig.FOO, 2.3, TimeUnit.SECONDS)
                .buildWrapped();

        assertThat(config.getValue(TestConfig.FOO).asString()).isEqualTo("2.3,SECONDS");
    }

    @Test
    @DisplayName("can be built with length values")
    void testSimpleConfig_canBeBuiltWithLengthUnit() {
        Config<TestConfig> config = configBuilder.put(TestConfig.FOO, 2.3, LengthUnit.METERS)
                .buildWrapped();

        assertThat(config.getValue(TestConfig.FOO).asString()).isEqualTo("2.3,METERS");
    }

    @Test
    @DisplayName("can be built with time and length units")
    void testSimpleConfig_canBeBuiltWithTimeAndLengthUnits() {
        Config<TestConfig> config = configBuilder.put(TestConfig.FOO, 2.3, LengthUnit.METERS, TimeUnit.SECONDS)
                .buildWrapped();

        assertThat(config.getValue(TestConfig.FOO).asString()).isEqualTo("2.3,METERS,SECONDS");
    }

    @Test
    @DisplayName("can have keys overwritten")
    void testSimpleConfig_overwritesKeys() {
        Config<TestConfig> config = configBuilder.put("TestConfig.BAR", "First")
                .put("TestConfig.BAR", "Second")
                .put("TestConfig.BAR", "Final")
                .buildWrapped();

        assertThat(config.getValue(TestConfig.BAR).asString()).isEqualTo("Final");
    }

    @Test
    @DisplayName("throws an exception if a key is not recognised")
    void testSimpleConfig_throwsExceptionForUnknownKey() {
        configBuilder.put("TestConfig.CHICKEN", "cluck");
        assertThatThrownBy(() -> configBuilder.build())
                .isInstanceOf(ConfigKeysNotRecognisedException.class)
                .hasMessageContaining("The following config keys were not recognised:[TestConfig.CHICKEN]");
    }

    @Test
    @DisplayName("throws an unchecked exception if a key is not recognised")
    void testSimpleConfig_throwsRuntimeExceptionForUnknownKey() {
        configBuilder.put("TestConfig.CHICKEN", "cluck");
        assertThatThrownBy(() -> configBuilder.buildWrapped())
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(ConfigKeysNotRecognisedException.class)
                .hasMessageContaining("Error caught building Config object.");
    }

    @Test
    @DisplayName("can be built using an existing config")
    void testSimpleConfig_canBeBuiltWithExistingConfig() {
        Config<TestConfig> originalConfig = configBuilder
                .put(TestConfig.FOO, "42")
                .put(TestConfig.BAR, "43")
                .buildWrapped();

        Config<TestConfig> fromConfig = SimpleConfigBuilder.createFromConfig(originalConfig)
                .put(TestConfig.BAR, "24") // overridden default
                .put(TestConfig.BAZ, "44") // added new value
                .buildWrapped();

        assertThat(fromConfig.getValue(TestConfig.FOO).asInt()).isEqualTo(42); // default
        assertThat(fromConfig.getValue(TestConfig.BAR).asInt()).isEqualTo(24); // overridden default
        assertThat(fromConfig.getValue(TestConfig.BAZ).asInt()).isEqualTo(44); // added
    }

    @Test
    @DisplayName("can be built using an existing config and prefixes")
    void testSimpleConfig_canBeBuiltWithExistingConfigAndPrefixes() {
        final String prefix1 = "Prefix1";
        final String prefix2 = "Prefix2";

        Config<TestConfig> originalConfig = configBuilder
                .put(TestConfig.FOO, "42")
                .put(TestConfig.FOO, "123", prefix1, prefix2)
                .put(TestConfig.FOO, "122", prefix1)
                .put(TestConfig.BAR, "22")
                .put(TestConfig.BAR, "43", prefix1, prefix2)
                .buildWrapped();

        Config<TestConfig> fromConfig = SimpleConfigBuilder.createFromConfig(originalConfig)
                .put(TestConfig.BAR, "24") // added
                .put(TestConfig.BAR, "12", prefix1, prefix2) // overridden
                .put(TestConfig.BAZ, "44") // added
                .put(TestConfig.BAZ, "90", prefix1)
                .buildWrapped();

        assertThat(fromConfig.getValue(TestConfig.FOO).asInt()).isEqualTo(42); // default
        assertThat(fromConfig.getValue(TestConfig.BAR).asInt()).isEqualTo(24); // overridden
        assertThat(fromConfig.getValue(TestConfig.BAZ).asInt()).isEqualTo(44); // added

        Config<TestConfig> configForPrefix1 = fromConfig.getPrefixedConfigItems(prefix1);
        assertThat(configForPrefix1.getValue(TestConfig.FOO).asInt()).isEqualTo(122); // default prefixed
        assertThat(configForPrefix1.getValue(TestConfig.BAZ).asInt()).isEqualTo(90); // added prefixed
        Config<TestConfig> configForPrefix2 = configForPrefix1.getPrefixedConfigItems(prefix2);
        assertThat(configForPrefix2.getValue(TestConfig.BAR).asInt()).isEqualTo(12); // overridden prefixed
    }
}