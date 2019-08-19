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
package com.ocadotechnology.s3;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Preconditions;
import com.ocadotechnology.config.Config;
import com.ocadotechnology.validation.Failer;

public class DataFileManager {
    public enum Mode { LOCAL, S3, S3_CACHE_ONLY, UNUSED }

    private final File rootDataDir;
    private final Config<S3Config> s3Config;
    private S3FileManager s3FileManager = null;

    private final boolean shouldAdjustWindowsFilePath;

    /**
     * Default constructor supporting any access mode.
     */
    public DataFileManager(File rootDataDir, Config<S3Config> s3Config) {
        this.rootDataDir = rootDataDir;
        this.s3Config = s3Config;
        shouldAdjustWindowsFilePath = System.getProperty("os.name").toLowerCase().contains("win") &&
                s3Config.containsKey(S3Config.PERMIT_EXTENDED_FILE_PATHS_IN_WINDOWS) &&
                s3Config.getBoolean(S3Config.PERMIT_EXTENDED_FILE_PATHS_IN_WINDOWS);
    }

    /**
     * Default constructor supporting only local access without a local root data directory.
     */
    public DataFileManager() {
        this(null, null);
    }

    /**
     * Constructor supporting only local access.
     */
    public DataFileManager(File rootDataDir) {
        this(rootDataDir, null);
    }

    /**
     * Constructor supporting only s3 file access.
     */
    public DataFileManager(Config<S3Config> s3Config) {
        this(null, s3Config);
    }

    public File getFileFromConfig(DataSourceDefinition<?> dataSource, Config<?> dataConfig, String defaultS3Bucket) {
        return getFileFromConfig(dataSource, dataConfig, defaultS3Bucket, true);
    }

    public File getOptionalFileFromConfig(DataSourceDefinition<?> dataSource, Config<?> dataConfig, String defaultS3Bucket) {
        return getFileFromConfig(dataSource, dataConfig, defaultS3Bucket, false);
    }

    private File getFileFromConfig(DataSourceDefinition<?> dataSource, Config<?> dataConfig, String defaultS3Bucket, boolean required) {
        Mode mode = Mode.valueOf(dataConfig.getString(dataSource.mode));
        File file;
        switch (mode) {
            case LOCAL:
                file = getLocalFile(dataSource, dataConfig);
                break;
            case S3:
                file = getS3File(dataSource, dataConfig, defaultS3Bucket, false);
                break;
            case S3_CACHE_ONLY:
                file = getS3File(dataSource, dataConfig, defaultS3Bucket, true);
                break;
            case UNUSED:
                if (required) {
                    throw Failer.fail("Required file has mode " + mode);
                } else {
                    return null;
                }
            default:
                throw Failer.fail("Unsupported Mode: " + mode);
        }
        return adjustFilePathIfWindows(file);
    }

    private File adjustFilePathIfWindows(File file) {
        if (shouldAdjustWindowsFilePath) {
            String rawPath;
            try {
                rawPath = file.getCanonicalPath();
            } catch (IOException e) {
                rawPath = file.getAbsolutePath();
            }
            return new File("\\\\?\\" + rawPath);
        }
        return file;
    }

    private File getLocalFile(DataSourceDefinition<?> dataSource, Config<?> dataConfig) {
        File absoluteFile = new File(dataConfig.getString(dataSource.localFile));
        if (rootDataDir == null) {
            return absoluteFile;
        }

        File relativeFile = new File(rootDataDir, dataConfig.getString(dataSource.localFile));
        if (!relativeFile.exists() && absoluteFile.exists()) {
            return absoluteFile;
        }
        return relativeFile; //If neither file exists, return the relative file path as a more likely default for writing the file not found error message.
    }

    private File getS3File(DataSourceDefinition<?> dataSource, Config<?> dataConfig, String defaultS3Bucket, boolean cacheOnly) {
        createS3FileManager();
        String s3BucketName = dataConfig.containsKey(dataSource.s3BucketOverride) ? dataConfig.getString(dataSource.s3BucketOverride) : defaultS3Bucket;
        return s3FileManager.getS3File(s3BucketName, dataConfig.getString(dataSource.s3Key), cacheOnly);
    }

    private void createS3FileManager() {
        if (s3FileManager != null) {
            return;
        }
        Preconditions.checkNotNull(s3Config, "Attempted to extract data from S3, but no S3 config provided.");
        s3FileManager = new S3FileManager(s3Config);
    }

    public static class DataSourceDefinition<E extends Enum<E>> {
        /**
         * Config key indicating the mode which should be used to find the file.  Values should correspond with {@link Mode}.
         */
        public final E mode;
        /**
         * Config key indicating the local file path, either absolute or relative to a root data directory.
         */
        public final E localFile;
        /**
         * Config key indicating an optional S3 bucket override.
         */
        public final E s3BucketOverride;
        /**
         * Config key indicating the s3 key of the desired file.
         */
        public final E s3Key;

        public DataSourceDefinition(E mode, E localFile, E s3BucketOverride, E s3Key) {
            this.mode = mode;
            this.localFile = localFile;
            this.s3BucketOverride = s3BucketOverride;
            this.s3Key = s3Key;
        }
    }
}
