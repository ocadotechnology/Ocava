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
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.testing.InternalClassTest;
import com.ocadotechnology.testing.UtilityClassTest;

@DisplayName("The ConfigFactory utility class")
class ConfigFactoryTest implements UtilityClassTest, InternalClassTest {

    @Override
    public Class<?> getTestSubject() {
        return ConfigFactory.class;
    }

    @Nested
    @DisplayName("read() method")
    class ReadMethodTests {

        protected Config<TestConfig> readConfigFromProperties(Properties props) throws ConfigKeysNotRecognisedException {
            return ConfigFactory.read(TestConfig.class, props::getProperty, ImmutableSet.of());
        }

        @Test
        @DisplayName("with all properties provided")
        void allPropertiesProvided() throws ConfigKeysNotRecognisedException {
            Properties props = fullySpecifiedProperties();
            Config<TestConfig> config = readConfigFromProperties(props);

            assertSoftly(softly -> {
                softly.assertThat(config.getInt(TestConfig.FOO)).isEqualTo(1);
                softly.assertThat(config.getInt(TestConfig.BAR)).isEqualTo(2);
                softly.assertThat(config.getInt(TestConfig.SubConfig.WOO)).isEqualTo(3);
                softly.assertThat(config.getInt(TestConfig.SubConfig.HOO)).isEqualTo(4);
                softly.assertThat(config.getInt(TestConfig.SubConfig.SubSubConfig.X)).isEqualTo(5);
                softly.assertThat(config.getInt(TestConfig.SubConfig.SubSubConfig.Y)).isEqualTo(6);
            });
        }

        @Test
        @DisplayName("does not include config for missing properties")
        void notAllPropertiesProvided() throws ConfigKeysNotRecognisedException {
                Config<TestConfig> config = readConfigFromProperties(new Properties());
                assertThat(config.containsKey(TestConfig.FOO)).isFalse();
        }
    }

    private static Properties fullySpecifiedProperties() {
        Properties props = new Properties();
        props.setProperty("TestConfig.FOO", "1");
        props.setProperty("TestConfig.BAR", "2");
        props.setProperty("TestConfig.SubConfig.WOO", "3");
        props.setProperty("TestConfig.SubConfig.HOO", "4");
        props.setProperty("TestConfig.SubConfig.SubSubConfig.X", "5");
        props.setProperty("TestConfig.SubConfig.SubSubConfig.Y", "6");
        return props;
    }
}
