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
package com.ocadotechnology.s3.spi;

import java.nio.file.Path;

import com.google.common.base.Preconditions;
import com.ocadotechnology.config.Config;
import com.ocadotechnology.fileaccess.DataSourceDefinition;
import com.ocadotechnology.fileaccess.service.DataAccessor;
import com.ocadotechnology.s3.S3Config;
import com.ocadotechnology.s3.S3FileManager;

public class S3FileFetcher implements DataAccessor {
    private final Config<S3Config> s3Config;
    private S3FileManager s3FileManager = null;
    private final boolean cacheOnly;

    public S3FileFetcher(Config<?> config, boolean cacheOnly) {
        Preconditions.checkArgument((config != null && config.enumTypeMatches(S3Config.class)), "Invalid S3Config");
        s3Config = (Config<S3Config>) config;
        this.cacheOnly = cacheOnly;
    }

    @Override
    public Path getFileFromConfig(DataSourceDefinition<?> dataSourceDefinition, Config<?> dataConfig, String defaultBucket) {
        createS3FileManager();
        String s3BucketName = dataConfig.getIfKeyAndValueDefined(dataSourceDefinition.bucket).asString().orElse(defaultBucket);
        return s3FileManager.getS3File(s3BucketName, dataConfig.getValue(dataSourceDefinition.key).asString(), this.cacheOnly).toPath();
    }

    private void createS3FileManager() {
        if (s3FileManager != null) {
            return;
        }
        Preconditions.checkNotNull(s3Config, "Attempted to extract data from S3, but no S3 config provided.");
        s3FileManager = new S3FileManager(s3Config);
    }
}
