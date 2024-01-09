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

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

class ConfigValueTest {

    private ConfigValue configValue = new ConfigValue("1", ImmutableMap.of(
            ImmutableSet.of("Prefix1"), "2",
            ImmutableSet.of("Prefix2", "Prefix3"), "3",
            ImmutableSet.of("Prefix4"), "4"));

    @Test
    void testCase_defaultCaseNoPrefixCalled() {
        assertThat(configValue.currentValue).isEqualTo("1");
        assertThat(configValue.prefixedValues).isNotEmpty();
    }

    @Test
    void testCase_onePrefix() {
        assertThat(configValue.getPrefix("Prefix1").prefixedValues).isEmpty();

        assertThat(configValue.getPrefix("Prefix1").currentValue).isEqualTo("2");

        assertThat(configValue.getPrefix("Prefix2").currentValue).isEqualTo("1");

        assertThat(configValue.getPrefix("Prefix3").currentValue).isEqualTo("1");
    }

    @Test
    void testCase_twoPrefixes_checkOnlyFirstPrefix() {
        assertThat(configValue.getPrefix("Prefix2").prefixedValues.keySet().stream()
                .filter(key -> key.contains("Prefix3"))
                .findAny()
                .orElse(null))
                .isNotEqualTo(null);
        assertThat(configValue.getPrefix("Prefix2").currentValue).isEqualTo("1");
    }

    @Test
    void testCase_twoPrefixes_checkOnlySecondPrefix() {
        assertThat(configValue.getPrefix("Prefix3").prefixedValues.keySet().stream()
                .filter(key -> key.contains("Prefix2"))
                .findAny()
                .orElse(null))
                .isNotEqualTo(null);
        assertThat(configValue.getPrefix("Prefix3").currentValue).isEqualTo("1");
    }

    @Test
    void testCase_twoPrefixes_checkBothPrefixes_beginWithPrefix2() {
        ConfigValue prefixedConfigValue = configValue.getPrefix("Prefix2").getPrefix("Prefix3");

        assertThat(prefixedConfigValue.prefixedValues).isEmpty();
        assertThat(prefixedConfigValue.currentValue).isEqualTo("3");
    }

    @Test
    void testCase_twoPrefixes_checkBothPrefixes_beginWithPrefix3() {
        ConfigValue prefixedConfigValue = configValue.getPrefix("Prefix3").getPrefix("Prefix2");

        assertThat(prefixedConfigValue.prefixedValues).isEmpty();
        assertThat(prefixedConfigValue.currentValue).isEqualTo("3");
    }

    @Test
    void testCase_prefixDoesntExist() {
        ConfigValue prefixedConfigValue = configValue.getPrefix("Prefix0");

        assertThat(prefixedConfigValue.prefixedValues).isEqualTo(ImmutableMap.of());
        assertThat(prefixedConfigValue.currentValue).isEqualTo("1");
    }

    @Test
    void testCase_getPrefixes() {
        assertThat(configValue.getPrefixes()).isEqualTo(ImmutableSet.of("Prefix1", "Prefix2", "Prefix3", "Prefix4"));
    }

    @Test
    void testCase_getValuesByPrefixedKeys() {
        ImmutableMap<String, String> expectedValuesByPrefixedKey = ImmutableMap.of(
                "Prefix1@VALUE", "2", "Prefix2@Prefix3@VALUE", "3", "Prefix4@VALUE", "4");

        assertThat(configValue.getValuesByPrefixedKeys("VALUE")).isEqualTo(expectedValuesByPrefixedKey);
    }

    @Test
    void testCase_biasNonExistentPrefix() {
        assertThat(configValue.getWithPrefixBias("Prefix0").currentValue).isEqualTo("1");
    }

    @Test
    void testCase_biasExistingPrefix() {
        assertThat(configValue.getWithPrefixBias("Prefix1").currentValue).isEqualTo("2");
    }

    @Test
    void testCase_biasOverwritePrefix() {
        assertThat(configValue.getWithPrefixBias("Prefix1").getWithPrefixBias("Prefix4").currentValue).isEqualTo("4");
    }

    @Test
    void testCase_biasKeepsPrefixes() {
        ConfigValue biasedConfigValue = configValue.getWithPrefixBias("Prefix1");
        assertThat(biasedConfigValue.getPrefix("Prefix2").getPrefix("Prefix3").currentValue).isEqualTo("3");
        assertThat(biasedConfigValue.getPrefix("Prefix4").currentValue).isEqualTo("4");
    }
}
