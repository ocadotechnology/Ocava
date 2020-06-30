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
package com.ocadotechnology.s3;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.config.Config;
import com.ocadotechnology.config.ConfigKeysNotRecognisedException;
import com.ocadotechnology.config.ConfigManager.Builder;

public class S3ConfigTest {

    private static final String TRUE = "TRUE";
    private static final String FALSE = "FALSE";
    private static final String ACCESS_KEY = "ACCESS_KEY";
    private static final String ENDPOINT = "ENDPOINT";

    @Test
    void whenLoadConfigTwice_thenKeepsOldDataWhereNewDataDoesNotOverride() throws ConfigKeysNotRecognisedException {
        ImmutableMap<String, String> map1 = ImmutableMap.of(
                "S3Config.ENABLE_S3_FILE_CACHE", FALSE,
                "S3Config.S3_ACCESS_KEY", ACCESS_KEY);
        ImmutableMap<String, String> map2 = ImmutableMap.of(
                "S3Config.ENABLE_S3_FILE_CACHE", TRUE,
                "S3Config.S3_ENDPOINT", ENDPOINT);

        Builder builder = new Builder(new String[]{});
        builder.loadConfigFromMap(map1, ImmutableSet.of(S3Config.class));
        Config<S3Config> s3Config = builder.build().getConfig(S3Config.class);
        testConfig(s3Config, S3Config.ENABLE_S3_FILE_CACHE, FALSE);
        testConfig(s3Config, S3Config.S3_ACCESS_KEY, ACCESS_KEY);

        builder.loadConfigFromMap(map2, ImmutableSet.of(S3Config.class));
        s3Config = builder.build().getConfig(S3Config.class);
        testConfig(s3Config, S3Config.ENABLE_S3_FILE_CACHE, TRUE);
        testConfig(s3Config, S3Config.S3_ACCESS_KEY, ACCESS_KEY);
        testConfig(s3Config, S3Config.S3_ENDPOINT, ENDPOINT);
    }

    private void testConfig(Config<S3Config> config, S3Config testKey, String expectedValue) {
        assertThat(config.getValue(testKey).asString()).as("Incorrect: %s", testKey).isEqualTo(expectedValue);
    }
}
