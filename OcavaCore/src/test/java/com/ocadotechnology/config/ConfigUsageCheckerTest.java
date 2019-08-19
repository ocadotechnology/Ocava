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
    void whenExtraConfigIsPresent_thenCheckerReturnsUnrecognisedProperties() throws ConfigKeysNotRecognisedException {
        Properties props = fullySpecifiedProperties();
        props.setProperty("TestConfig.Unknown.BottomLevel.VAL_5", "7");

        ConfigUsageChecker checker = new ConfigUsageChecker();
        ConfigFactory.read(TestConfig.class, checker.checkAccessTo(props), ImmutableSet.of());

        assertThat(checker.getUnrecognisedProperties()).isNotEmpty();
    }

    private static Properties fullySpecifiedProperties() {
        Properties props = new Properties();
        props.setProperty("TestConfig.FOO", "1");
        props.setProperty("TestConfig.BAR", "2");
        props.setProperty("TestConfig.VAL_3", "3");
        props.setProperty("TestConfig.SubConfig.WOO", "4");
        props.setProperty("TestConfig.SubConfig.HOO", "5");
        props.setProperty("TestConfig.SubConfig.SubSubConfig.X", "6");
        props.setProperty("TestConfig.SubConfig.SubSubConfig.Y", "7");
        return props;
    }

}