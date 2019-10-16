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
import java.io.Serializable;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.amazonaws.AmazonClientException;
import com.amazonaws.http.IdleConnectionReaper;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.base.Preconditions;
import com.ocadotechnology.config.Config;
import com.ocadotechnology.validation.Failer;

public class S3FileManager implements Serializable {
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final int RETRY_WAIT_PERIOD = 5;
    private static final long serialVersionUID = 1L;

    private final S3FileCache fileCache;

    protected final SerializableS3Client s3;
    private final String endpoint;
    private final String bucketPrefix;

    private static final Logger logger = LoggerFactory.getLogger(S3FileManager.class);

    public S3FileManager(Config<S3Config> s3Config) {
        s3 = new SerializableS3Client(s3Config);
        endpoint = s3Config.getString(S3Config.S3_ENDPOINT);
        logger.info("S3 client initialised for endpoint {} with signer override {}", endpoint, SerializableS3Client.signerType);
        bucketPrefix = s3Config.containsKey(S3Config.BUCKET_PREFIX) ? s3Config.getString(S3Config.BUCKET_PREFIX) : "";

        if (s3Config.getBoolean(S3Config.ENABLE_S3_FILE_CACHE)) {
            File cacheDirectory = s3Config.containsKey(S3Config.S3_FILE_CACHE_ROOT) ?
                    new File(s3Config.getString(S3Config.S3_FILE_CACHE_ROOT)) :
                    new File(S3FileCache.DEFAULT_S3_CACHE_DIR);
            logger.info("Using S3FileCache with root directory {}", cacheDirectory.getAbsolutePath());
            fileCache = new S3FileCache(cacheDirectory);
        } else {
            fileCache = null;
        }
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
                getS3FileUsingFileCache(getFullyQualifiedBucketName(bucket), key, cacheOnly) :
                getS3FileAsLocalTemporaryFile(getFullyQualifiedBucketName(bucket), key);
    }

    private File getS3FileUsingFileCache(String fullyQualifiedBucket, String key, boolean cacheOnly) {
        File lockFileHandle = fileCache.createLockFileHandle(fullyQualifiedBucket, key);
        if (!lockFileHandle.exists()) {
            Optional<File> cachedFile = fileCache.get(fullyQualifiedBucket, key);
            if (cachedFile.isPresent()) {
                logger.info("{} loaded from S3FileCache", cachedFile.get().getAbsolutePath());
                return cachedFile.get();
            }
        }

        logger.info("Attempting to get lock on cache for S3 file {}:{}", fullyQualifiedBucket, key);
        AsynchronousFileChannel channel = getFileChannel(lockFileHandle);
        FileLock lock = getFileLock(channel);

        // double checking to see if something which had the lock first has created the file.
        Optional<File> optionalCachedFile = fileCache.get(fullyQualifiedBucket, key);
        if (optionalCachedFile.isPresent()) {
            File cachedFile = optionalCachedFile.get();
            if (cacheOnly || verifyFileSize(cachedFile, fullyQualifiedBucket, key)) {
                logger.info("{} loaded from S3FileCache", cachedFile.getAbsolutePath());
                releaseLock(channel, lock, lockFileHandle);
                return cachedFile;
            } else {
                logger.warn("{} failed verification. Deleting.", cachedFile.getAbsolutePath());
                Preconditions.checkState(
                        cachedFile.delete(),
                        "Failed to delete cached file which is of the wrong file size. File: %s",
                        cachedFile.toString());
            }
        }
        Preconditions.checkState(!cacheOnly, "File Bucket=%s Key=%s not found in S3 cache.", fullyQualifiedBucket, key);

        File writableFileHandle = fileCache.createWritableFileHandle(fullyQualifiedBucket, key);
        logger.info("Writing S3 file {}:{} to cache at {}", fullyQualifiedBucket, key, writableFileHandle.getAbsolutePath());
        writeS3FileContentsToLocalFile(fullyQualifiedBucket, key, writableFileHandle);
        releaseLock(channel, lock, lockFileHandle);
        return writableFileHandle;
    }

    @SuppressFBWarnings(
            value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE",
            justification = "Don't care if the lockFileHandle exists, we just want to make sure there is one")
    private AsynchronousFileChannel getFileChannel(File lockFileHandle) {
        try {
            AsynchronousFileChannel channel;
            lockFileHandle.createNewFile();
            channel = AsynchronousFileChannel.open(
                    Paths.get(lockFileHandle.getAbsolutePath()),
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE);
            return channel;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private FileLock getFileLock(AsynchronousFileChannel channel) {
        try {
            return channel.lock().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void releaseLock(AsynchronousFileChannel channel, FileLock lock, File lockFileHandle) {
        try {
            lock.release();
            channel.close();
            Preconditions.checkState(
                    lockFileHandle.delete(),
                    "Failed to delete lock file %s when trying to release lock. This may remain locked forever",
                    lockFileHandle);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private File getS3FileAsLocalTemporaryFile(String fullyQualifiedBucket, String key) {
        try {
            File tempFile = File.createTempFile(key, "db");
            tempFile.deleteOnExit();
            logger.info("Cache disabled, writing S3 file {}:{} to temp file at {} (will delete on JVM termination)",
                    fullyQualifiedBucket, key, tempFile.getAbsolutePath());
            writeS3FileContentsToLocalFile(fullyQualifiedBucket, key, tempFile);
            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            throw Failer.fail("Could not write S3 file %s from bucket %s to local temp file", key, fullyQualifiedBucket);
        }
    }

    private void writeS3FileContentsToLocalFile(String fullyQualifiedBucket, String key, File writableFileHandle) {
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

    private boolean verifyFileSize(File cachedFileHandle, String fullyQualifiedBucket, String key) {
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

    public void close() {
        IdleConnectionReaper.shutdown();
    }
}
