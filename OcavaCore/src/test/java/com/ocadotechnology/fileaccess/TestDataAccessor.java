/*
 * Copyright © 2017-2023 Ocado (Ocava)
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.ocadotechnology.config.Config;
import com.ocadotechnology.fileaccess.service.DataAccessor;
import com.ocadotechnology.validation.Failer;

public class TestDataAccessor implements DataAccessor {
    private static final String PREFIX = "testSpiFile";
    private static final String SUFFIX = "db";

    @Override
    public Path getFileFromConfig(DataSourceDefinition<?> dataSourceDefinition, Config<?> dataConfig, String defaultBucket)  {
        try {
            Path filePath = Files.createTempFile(PREFIX, SUFFIX);
            filePath.toFile().deleteOnExit();
            return filePath;
        } catch (IOException e) {
            throw Failer.fail("failed to create a temporary file for testing");
        }
    }
}
