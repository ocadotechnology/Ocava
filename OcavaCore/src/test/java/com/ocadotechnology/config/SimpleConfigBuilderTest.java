/*
 * Copyright © 2017 Ocado (Ocava)
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
                .put("TestConfig.SubConfig.WOO", "Second")
                .put("TestConfig.SubConfig.HOO", "Third")
                .buildWrapped();

        assertThat(config.getInt(TestConfig.FOO)).isEqualTo(42);
        assertThat(config.getString(TestConfig.BAR)).isEqualTo("First");
        assertThat(config.getString(TestConfig.SubConfig.WOO)).isEqualTo("Second");
        assertThat(config.getString(TestConfig.SubConfig.HOO)).isEqualTo("Third");
    }

    @Test
    @DisplayName("can be built using Enum keys")
    void testSimpleConfig_canBeBuiltWithEnumKeys() {
        Config<TestConfig> config = configBuilder.put(TestConfig.FOO, "42")
                .buildWrapped();

        assertThat(config.getInt(TestConfig.FOO)).isEqualTo(42);
    }

    @Test
    @DisplayName("can be built using Object values")
    void testSimpleConfig_canBeBuiltWithObjectValues() {
        Config<TestConfig> config = configBuilder.put(TestConfig.FOO, Id.create(42))
                .buildWrapped();

        assertThat(config.getInt(TestConfig.FOO)).isEqualTo(42);
    }

    @Test
    @DisplayName("can be built using Object values")
    void testSimpleConfig_canBeBuiltWithCollectionValues() {
        Config<TestConfig> config = configBuilder.put(TestConfig.FOO, ImmutableList.of(Id.create(42), Id.create(43), Id.create(44)))
                .buildWrapped();

        assertThat(config.getString(TestConfig.FOO)).isEqualTo("42,43,44");
    }

    @Test
    @DisplayName("can be built with time unit")
    void testSimpleConfig_canBeBuiltWithTimeUnit() {
        Config<TestConfig> config = configBuilder.put(TestConfig.FOO, 2.3, TimeUnit.SECONDS)
                .buildWrapped();

        assertThat(config.getString(TestConfig.FOO)).isEqualTo("2.3,SECONDS");
    }

    @Test
    @DisplayName("can be built with length values")
    void testSimpleConfig_canBeBuiltWithLengthUnit() {
        Config<TestConfig> config = configBuilder.put(TestConfig.FOO, 2.3, LengthUnit.METERS)
                .buildWrapped();

        assertThat(config.getString(TestConfig.FOO)).isEqualTo("2.3,METERS");
    }

    @Test
    @DisplayName("can be built with time and length units")
    void testSimpleConfig_canBeBuiltWithTimeAndLengthUnits() {
        Config<TestConfig> config = configBuilder.put(TestConfig.FOO, 2.3, LengthUnit.METERS, TimeUnit.SECONDS)
                .buildWrapped();

        assertThat(config.getString(TestConfig.FOO)).isEqualTo("2.3,METERS,SECONDS");
    }

    @Test
    @DisplayName("can have keys overwritten")
    void testSimpleConfig_overwritesKeys() {
        Config<TestConfig> config = configBuilder.put("TestConfig.BAR", "First")
                .put("TestConfig.BAR", "Second")
                .put("TestConfig.BAR", "Final")
                .buildWrapped();

        assertThat(config.getString(TestConfig.BAR)).isEqualTo("Final");
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
}