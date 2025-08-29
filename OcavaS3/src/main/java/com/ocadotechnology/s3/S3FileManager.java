/*
 * Copyright © 2017-2025 Ocado (Ocava)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.core.exception.SdkException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.ocadotechnology.config.Config;
import com.ocadotechnology.fileaccess.FileCache;
import com.ocadotechnology.fileaccess.FileManager;
import com.ocadotechnology.validation.Failer;

/**
 * An implementation of {@link FileManager} to retrieve files from an S3 bucket.
 */
public class S3FileManager extends FileManager {
    @VisibleForTesting
    protected static final int MAX_RETRY_ATTEMPTS = 5;
    private static final int RETRY_WAIT_PERIOD = 5;
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_S3_CACHE_DIR = System.getProperty("user.home") + File.separatorChar + ".s3cache";

    private static final Logger logger = LoggerFactory.getLogger(S3FileManager.class);

    private final S3Querier s3Querier;
    private final String endpoint;
    private final String bucketPrefix;

    public S3FileManager(Config<S3Config> s3Config) {
        this(
                setupFileCache(s3Config),
                new S3Querier(new SerializableS3Client(s3Config)),
                s3Config.getIfKeyAndValueDefined(S3Config.BUCKET_PREFIX).asString().orElse(""));
        logger.info("S3 client initialised for endpoint {}", endpoint);
    }

    @VisibleForTesting
    protected S3FileManager(FileCache fileCache, S3Querier s3Querier, String bucketPrefix) {
        super(fileCache);
        this.s3Querier = s3Querier;
        this.endpoint = s3Querier.getEndpoint();
        this.bucketPrefix = bucketPrefix;
    }

    private static FileCache setupFileCache(Config<S3Config> s3Config) {
        if (!s3Config.getValue(S3Config.ENABLE_S3_FILE_CACHE).asBoolean()) {
            return null;
        }
        File cacheDirectory = s3Config.areKeyAndValueDefined(S3Config.S3_FILE_CACHE_ROOT)
                ? new File(s3Config.getValue(S3Config.S3_FILE_CACHE_ROOT).asString())
                : new File(DEFAULT_S3_CACHE_DIR);
        logger.info("Using S3FileCache with root directory {}", cacheDirectory.getAbsolutePath());
        return new FileCache(cacheDirectory);
    }

    public String getEndpoint() {
        return endpoint;
    }

    private String getFullyQualifiedBucketName(String bucket) {
        return bucketPrefix + bucket;
    }

    /**
     * @param bucket the bucket to search
     * @return All (up to Integer.MAX_VALUE) keys in the bucket
     */
    public ImmutableList<String> getAllKeys(String bucket) {
        return s3Querier.getAllKeys(getFullyQualifiedBucketName(bucket));
    }

    /**
     * @param bucket the bucket to search
     * @param keyPrefix the key prefix to filter on
     * @return All (up to Integer.MAX_VALUE) keys in the bucket which match the given prefix
     */
    public ImmutableList<String> getAllKeys(String bucket, String keyPrefix) {
        return s3Querier.getAllKeys(getFullyQualifiedBucketName(bucket), keyPrefix);
    }

    /**
     * @param bucket the bucket to search
     * @param keyPrefix the key prefix to filter on
     * @return true if any key is found in the given bucket matching the given prefix
     */
    public boolean fileWithPrefixExists(String bucket, String keyPrefix) {
        return !getAllKeys(bucket, keyPrefix).isEmpty();
    }

    /**
     * @param bucket the bucket to search
     * @param name the key to search for
     * @return true if the exact key specified is found in the bucket
     */
    public boolean fileExists(String bucket, String name) {
        return getAllKeys(bucket, name).contains(name);
    }

    public File getS3File(String bucket, String key) {
        return getS3File(bucket, key, false);
    }

    public File getS3File(String bucket, String key, boolean cacheOnly) {
        return getFile(getFullyQualifiedBucketName(bucket), key, cacheOnly);
    }

    protected boolean verifyFileSize(File cachedFileHandle, String fullyQualifiedBucket, String key) {
        logger.info("Verifying size of {}", cachedFileHandle.getAbsolutePath());
        for (int retry = 0; retry < MAX_RETRY_ATTEMPTS; retry++) {
            try {
                long remoteSizeInBytes = s3Querier.getContentLength(fullyQualifiedBucket, key);
                long localSizeInBytes = cachedFileHandle.length();
                return remoteSizeInBytes == localSizeInBytes;
            } catch (SdkException e) {
                logger.warn("S3 File Fetch attempt {} failed: {}", retry, e.getMessage());
                try {
                    Thread.sleep(RETRY_WAIT_PERIOD * 1000);
                } catch (InterruptedException e2) {
                    logger.warn("S3 wait between retries interrupted: {}", e.getMessage());
                }
            }
        }
        throw Failer.fail("Failed to download %s:%s from S3 after %s attempts", fullyQualifiedBucket, key, MAX_RETRY_ATTEMPTS);
    }

    @Override
    protected void getFileAndWriteToDestination(String fullyQualifiedBucket, String key, File writableFileHandle) {
        for (int retry = 0; retry < MAX_RETRY_ATTEMPTS; retry++) {
            try {
                s3Querier.writeObjectToFile(fullyQualifiedBucket, key, writableFileHandle);
                return;
            } catch (SdkException e) {
                logger.warn("S3 File Fetch attempt {} failed: {}", retry, e.getMessage());
                try {
                    Thread.sleep(RETRY_WAIT_PERIOD * 1000);
                } catch (InterruptedException e2) {
                    logger.warn("S3 wait between retries interrupted: {}", e.getMessage());
                }
            }
        }
        throw Failer.fail("Failed to download %s:%s from S3 after %s attempts", fullyQualifiedBucket, key, MAX_RETRY_ATTEMPTS);
    }
}
