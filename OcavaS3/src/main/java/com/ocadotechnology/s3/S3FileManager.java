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

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.http.IdleConnectionReaper;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.base.Preconditions;
import com.ocadotechnology.config.Config;
import com.ocadotechnology.fileaccess.FileCache;
import com.ocadotechnology.fileaccess.FileManager;
import com.ocadotechnology.validation.Failer;

public class S3FileManager extends FileManager {
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final int RETRY_WAIT_PERIOD = 5;
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_S3_CACHE_DIR = System.getProperty("user.home") + File.separatorChar + ".s3cache";

    private static final Logger logger = LoggerFactory.getLogger(S3FileManager.class);

    protected final SerializableS3Client s3;
    private final String endpoint;
    private final String bucketPrefix;

    public S3FileManager(Config<S3Config> s3Config) {
        super(s3Config);
        s3 = new SerializableS3Client(s3Config);
        endpoint = s3.getEndpoint();
        logger.info("S3 client initialised for endpoint {} with signer override {}", endpoint, SerializableS3Client.signerType);
        bucketPrefix = s3Config.getIfKeyAndValueDefined(S3Config.BUCKET_PREFIX).asString().orElse("");
    }

    protected FileCache setupFileCache(Config<?> s3Config) {
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

    /** Will return up to Integer.MAX_VALUE objects in bucket. */
    public List<S3ObjectSummary> getFullObjectList(String bucket) {
        ListObjectsRequest fileCheck = new ListObjectsRequest().withBucketName(getFullyQualifiedBucketName(bucket)).withMaxKeys(Integer.MAX_VALUE);
        return s3.getS3Client().listObjects(fileCheck).getObjectSummaries();
    }

    /** Will return up to Integer.MAX_VALUE objects in bucket. */
    public List<S3ObjectSummary> getFullObjectList(String bucket, String keyPrefix) {
        ListObjectsRequest fileCheck = new ListObjectsRequest().withBucketName(getFullyQualifiedBucketName(bucket)).withPrefix(keyPrefix).withMaxKeys(Integer.MAX_VALUE);
        return s3.getS3Client().listObjects(fileCheck).getObjectSummaries();
    }

    /** Will only return the first 1,000 objects in bucket. */
    public List<S3ObjectSummary> getObjectList(String bucket) {
        return getFirstObjectListing(bucket).getObjectSummaries();
    }

    /** The first object listing contains the first 1,000 object.<br>
     *  Use getMoreObjects to get the next batch (and so on).
     */
    public ObjectListing getFirstObjectListing(String bucket) {
        ListObjectsRequest fileCheck = new ListObjectsRequest().withBucketName(getFullyQualifiedBucketName(bucket));
        return s3.getS3Client().listObjects(fileCheck);
    }

    public ObjectListing getMoreObjects(ObjectListing previousListing) {
        return s3.getS3Client().listNextBatchOfObjects(previousListing);
    }

    public void summarise(List<S3ObjectSummary> objectListing) {
        objectListing.forEach(summary -> System.out.println(summary.getKey() + " (" + summary.getSize() + ")"));
    }

    public boolean fileWithPrefixExists(String bucket, String prefix) {
        return getFullObjectList(bucket, prefix).isEmpty();
    }

    public boolean fileExists(String bucket, String name) {
        return getFullObjectList(bucket, name).stream().anyMatch(o -> o.getKey().equals(name));
    }

    public File getS3File(String bucket, String key) {
        return getS3File(bucket, key, false);
    }

    public File getS3File(String bucket, String key, boolean cacheOnly) {
        Preconditions.checkState(!cacheOnly || fileCache != null, "Attempted to fetch file Bucket=%s Key=%s using only S3 cache, but no cache configured.", bucket, key);
        return fileCache != null ?
                getFileUsingFileCache(getFullyQualifiedBucketName(bucket), key, cacheOnly) :
                getFileAsLocalTemporaryFile(getFullyQualifiedBucketName(bucket), key);
    }

    protected boolean verifyFileSize(File cachedFileHandle, String fullyQualifiedBucket, String key) {
        logger.info("Verifying size of {}", cachedFileHandle.getAbsolutePath());
        for (int retry = 0; retry < MAX_RETRY_ATTEMPTS; retry++) {
            try {
                long remoteSizeInBytes = s3.getS3Client().getObjectMetadata(fullyQualifiedBucket, key).getContentLength();
                long localSizeInBytes = cachedFileHandle.length();
                return remoteSizeInBytes == localSizeInBytes;
            } catch (AmazonClientException e) {
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
                s3.getS3Client().getObject(new GetObjectRequest(fullyQualifiedBucket, key), writableFileHandle);
                return;
            } catch (AmazonClientException e) {
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

    public void close() {
        IdleConnectionReaper.shutdown();
    }
}
