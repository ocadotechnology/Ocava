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
package com.ocadotechnology.fileaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.config.Config;
import com.ocadotechnology.config.ConfigKeysNotRecognisedException;
import com.ocadotechnology.config.ConfigManager;
import com.ocadotechnology.fileaccess.serviceloader.DataAccessManager;
import com.ocadotechnology.fileaccess.serviceloader.DataAccessServiceProviderNotAvailableException;

public class DataAccessManagerTest {
    private static final String TEST_MODE = "TEST_MODE";
    private static final String UNSUPPORTED_MODE = "UNSUPPORTED_MODE";
    private static final String DEFAULT_BUCKET = "defaultBucket";
    private static final String BUCKET = "bucket";
    private static final String KEY = "key";

    @Test
    public void testNullConfigMap() {
        Throwable exception = assertThrows(
                NullPointerException.class,
                () -> new DataAccessManager(null)
        );
        assertEquals("initial config cannot be null", exception.getMessage());
    }

    @Test
    public void testFileAccessThroughModeWithoutProvider() {
        ImmutableMap<String, Config<?>> initialConfigMap = ImmutableMap.of(TEST_MODE, Config.empty(TestAccessorConfig.class));
        DataAccessManager manager = new DataAccessManager(initialConfigMap);
        assertThrows(
                DataAccessServiceProviderNotAvailableException.class,
                () -> manager.getFileFromConfig(TestFileType.getLocalDataSourceDefinition(), createDataConfigForMode(UNSUPPORTED_MODE), DEFAULT_BUCKET)
        );
    }

    @Test
    public void testFileAccessThroughModeWithoutInitialisation() {
        ImmutableMap<String, Config<?>> initialConfigMap = ImmutableMap.of(UNSUPPORTED_MODE, Config.empty(UnsupportedModeConfig.class));
        DataAccessManager manager = new DataAccessManager(initialConfigMap);
        assertThrows(
                IllegalStateException.class,
                () -> manager.getFileFromConfig(TestFileType.getLocalDataSourceDefinition(), createDataConfigForMode(TEST_MODE), DEFAULT_BUCKET)
        );
    }

    @Test
    public void testFileAccessOnInitialisation() {
        ImmutableMap<String, Config<?>> initialConfigMap = ImmutableMap.of(TEST_MODE, Config.empty(TestAccessorConfig.class));
        DataAccessManager manager = new DataAccessManager(initialConfigMap);
        Path path = manager.getFileFromConfig(TestFileType.getLocalDataSourceDefinition(), createDataConfigForMode(TEST_MODE), DEFAULT_BUCKET);
        assertNotNull(path.toFile());
    }

    private Config<?> createDataConfigForMode(String mode) {
        ImmutableMap<String, String> map = new ImmutableMap.Builder<String, String>()
                .put("TestFileType.MODE", mode)
                .put("TestFileType.BUCKET", BUCKET)
                .put("TestFileType.KEY", KEY)
                .build();

        ConfigManager.Builder builder = new ConfigManager.Builder(new String[]{});
        builder.loadConfigFromMap(map, ImmutableSet.of(TestFileType.class));

        try {
            return builder.build().getConfig(TestFileType.class);
        } catch (ConfigKeysNotRecognisedException e) {
            throw new AssertionError("Invalid Config");
        }
    }

    public enum TestAccessorConfig {
    }

    public enum UnsupportedModeConfig {
    }

    public enum TestFileType {
        MODE,
        LOCAL,
        BUCKET,
        KEY;

        public static DataSourceDefinition<TestFileType> getLocalDataSourceDefinition() {
            return new DataSourceDefinition<>(MODE, LOCAL, BUCKET, KEY);

        }
    }
}
