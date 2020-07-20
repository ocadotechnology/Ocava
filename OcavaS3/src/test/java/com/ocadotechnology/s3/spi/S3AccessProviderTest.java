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
package com.ocadotechnology.s3.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.config.Config;
import com.ocadotechnology.config.ConfigKeysNotRecognisedException;
import com.ocadotechnology.config.ConfigManager;
import com.ocadotechnology.s3.S3Config;

public class S3AccessProviderTest {
    private static final String INFO = "INFO";
    private static final String RESULTS_DIR = "results_dir";
    private static final String ENDPOINT = "ENDPOINT";
    private static final String TRUE = "true";
    private S3AccessProvider provider = new S3AccessProvider();

    @Test
    public void testNullInitialConfig() {
        Throwable exception = assertThrows(
                IllegalArgumentException.class,
                () -> provider.createAccessor(null));
        assertEquals("Invalid S3Config", exception.getMessage());
    }

    @Test
    public void testCreateFileFetcherWithInvalidConfig() {
        Config<?> invalidConfig = createAppConfig();

        Throwable exception = assertThrows(
                IllegalArgumentException.class,
                () -> provider.createAccessor(invalidConfig)
        );
        assertEquals("Invalid S3Config", exception.getMessage());
    }

    @Test
    public void testCreateFileFetcherWithValidConfig() {
        Config<?> validConfig = createS3Config();
        S3FileFetcher s3FileFetcher = (S3FileFetcher) provider.createAccessor(validConfig);
        assertNotNull(s3FileFetcher);
    }

    private Config<?> createAppConfig() {
        ImmutableMap<String, String> invalidConfigMap = new ImmutableMap.Builder<String, String>()
                .put("TestApplicationConfig.LOG_LEVEL", INFO)
                .put("TestApplicationConfig.RESULTS_DIRECTORY", RESULTS_DIR)
                .build();
        ConfigManager.Builder builder = new ConfigManager.Builder(new String[]{});
        builder.loadConfigFromMap(invalidConfigMap, ImmutableSet.of(TestApplicationConfig.class));

        try {
            return builder.build().getConfig(TestApplicationConfig.class);
        } catch (ConfigKeysNotRecognisedException e) {
            throw new AssertionError("Invalid config");
        }
    }

    private Config<?> createS3Config() {
        ImmutableMap<String, String> validConfigMap = new ImmutableMap.Builder<String, String>()
                .put("S3Config.S3_ENDPOINT", ENDPOINT)
                .put("S3Config.ENABLE_S3_FILE_CACHE", TRUE)
                .build();

        ConfigManager.Builder builder = new ConfigManager.Builder(new String[]{});
        builder.loadConfigFromMap(validConfigMap, ImmutableSet.of(S3Config.class));

        try {
            return builder.build().getConfig(S3Config.class);
        } catch (ConfigKeysNotRecognisedException e) {
            throw new AssertionError("Invalid Config");
        }
    }

    public enum TestApplicationConfig {
        LOG_LEVEL,
        RESULTS_DIRECTORY
    }
}