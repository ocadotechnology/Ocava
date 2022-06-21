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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.testing.InternalClassTest;

class ConfigUsageCheckerTest implements InternalClassTest {

    @Override
    public Class<?> getTestSubject() {
        return ConfigUsageChecker.class;
    }

    @Test
    void whenExtraConfigIsPresent_thenCheckerReturnsUnrecognisedProperties() {
        Properties props = fullySpecifiedProperties();
        props.setProperty("TestConfig.Unknown.BottomLevel.VAL_5", "7");

        ConfigUsageChecker checker = new ConfigUsageChecker();
        ConfigFactory.read(TestConfig.class, checker.checkAccessTo(props), ImmutableSet.of());

        assertThat(checker.getUnrecognisedProperties()).isNotEmpty();
        assertThat(checker.getUnrecognisedProperties()).contains("TestConfig.Unknown.BottomLevel.VAL_5");
    }

    @Test
    void whenExtensionIsPresent_thenCheckerReturnsOk() {
        Properties props = fullySpecifiedProperties();
        props.setProperty(ModularConfigUtils.EXTENDS, "some-file");

        ConfigUsageChecker checker = new ConfigUsageChecker();
        ConfigFactory.read(TestConfig.class, checker.checkAccessTo(props), ImmutableSet.of());

        assertThat(checker.getUnrecognisedProperties()).isEmpty();
    }

    @Test
    void whenExtraPrefixedConfigIsPresent_thenCheckerReturnsUnrecognisedProperties() {
        Properties props = fullySpecifiedPropertiesWithPrefixedProperties();
        props.setProperty("Prefix1@TestConfig.Unknown.ITEM", "2");

        ConfigUsageChecker checker = new ConfigUsageChecker();
        ConfigFactory.read(TestConfig.class, checker.checkAccessTo(props), ImmutableSet.of());

        assertThat(checker.getUnrecognisedProperties()).isNotEmpty();
        assertThat(checker.getUnrecognisedProperties()).contains("Prefix1@TestConfig.Unknown.ITEM");
    }

    private static Properties fullySpecifiedProperties() {
        Properties props = new Properties();
        props.setProperty("TestConfig.FOO", "1");
        props.setProperty("TestConfig.BAR", "2");
        props.setProperty("TestConfig.FirstSubConfig.WOO", "4");
        props.setProperty("TestConfig.FirstSubConfig.HOO", "5");
        props.setProperty("TestConfig.FirstSubConfig.SubSubConfig.X", "6");
        props.setProperty("TestConfig.FirstSubConfig.SubSubConfig.Y", "7");
        return props;
    }

    private static Properties fullySpecifiedPropertiesWithPrefixedProperties() {
        Properties props = new Properties();
        props.setProperty("TestConfig.FOO", "1");
        props.setProperty("Prefix1@TestConfig.FOO", "2");
        return props;
    }

}