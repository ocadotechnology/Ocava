/*
 * Copyright © 2017-2021 Ocado (Ocava)
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
package com.ocadotechnology.fileaccess.local;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.base.Preconditions;
import com.ocadotechnology.config.Config;
import com.ocadotechnology.fileaccess.DataSourceDefinition;
import com.ocadotechnology.fileaccess.service.DataAccessor;

public class LocalFileFetcher implements DataAccessor {
    private final Config<LocalFileConfig> localConfig;

    public LocalFileFetcher(Config<?> initialConfig) {
        Preconditions.checkArgument((initialConfig != null && initialConfig.enumTypeMatches(LocalFileConfig.class)), "Invalid localConfig");
        this.localConfig = (Config<LocalFileConfig>) initialConfig;
    }

    @Override
    public Path getFileFromConfig(DataSourceDefinition<?> dataSourceDefinition, Config<?> dataConfig, String defaultBucket) {
        String filePath = dataConfig.getValue(dataSourceDefinition.localFile).asString();

        Path absoluteFile = Paths.get(filePath);
        if (!localConfig.areKeyAndValueDefined(LocalFileConfig.ROOT_DATA_DIR)) {
            return absoluteFile;
        }
        Path relativeFile = Paths.get(localConfig.getValue(LocalFileConfig.ROOT_DATA_DIR).asString(), filePath);
        if (!relativeFile.toFile().exists() && absoluteFile.toFile().exists()) {
            return absoluteFile;
        }
        return relativeFile;
    }
}
