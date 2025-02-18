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
package com.ocadotechnology.fileaccess;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.annotation.CheckForNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.ocadotechnology.validation.Failer;

/**
 * Class to handle file retrieval given a bucket or directory for a file and a file key.
 *
 * A {@link FileCache} can be passed in to cache any retrieved files. {@link FileManager} will attempt to retrieve files
 * from the cache before retrieving them from the original source.
 * Cached file sizes will be verified before retrival. If they fail verification then the cached file will be deleted
 * and re-acquired.
 *
 * A callback can also be defined to be applied once a file is retrieved. For example, a method to decompress compressed
 * file formats.
 *
 * Implementers must define how to retrieve files from their original source and then store the file by overriding
 * {@link #getFileAndWriteToDestination}
 * They must also define how to verify the size of a cached file against the original source by overriding {@link #verifyFileSize}
 * Implementers can also optionally define a callback to be applied to the file once it is retrieved by overriding {@link #applyCallbackAndGetFile}
 */
public abstract class FileManager implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(FileManager.class);
    private static final String DIR_SEPARATOR_FORWARD_SLASH = "/";
    private static final String DIR_SEPARATOR_BACK_SLASH = "\\";
    private static final String REPLACEMENT_CHARACTER = "_";
    private static final String FILE_PREFIX = "Temp";
    @CheckForNull
    protected final FileCache fileCache;

    protected FileManager(@CheckForNull FileCache fileCache) {
        this.fileCache = fileCache;
    }

    /**
     * Retrieves a file from the cache if it exists, otherwise retrieves it from the original source.
     * If the file is in the cache then the file size will be verified before it is retrieved. If it fails verification
     * then the cached file will be deleted and re-acquired.
     * A callback can be applied to the file once it is retrieved.
     *
     * File locking will be used to ensure that only one process or thread at a time can retrieve the file.
     *
     * @param fullyQualifiedBucket The bucket or directory in which the file is stored.
     * @param key The name of the file.
     * @param cacheOnly If true, the file will only be retrieved from the cache and not the original source.
     * @return The retrieved file after any callback has been applied.
     */
    protected File getFile(String fullyQualifiedBucket, String key, boolean cacheOnly) {
        Preconditions.checkState(!cacheOnly || fileCache != null,
                "Attempted to fetch file Bucket=%s Key=%s using only file cache, but no cache configured.", fullyQualifiedBucket, key);
        if (fileCache == null) {
            return getFileAsLocalTemporaryFile(fullyQualifiedBucket, key);
        }

        // Try to retrieve the file from the cache if nothing is currently retrieving or otherwise handling the file
        File lockFileHandle = fileCache.createLockFileHandle(fullyQualifiedBucket, key);
        if (!lockFileHandle.exists()) {
            Optional<File> maybeCachedFile = getVerifiedFileFromCacheOrDelete(fullyQualifiedBucket, key, cacheOnly);
            if (maybeCachedFile.isPresent()) {
                return maybeCachedFile.get();
            }
        }

        logger.info("Attempting to get lock on cache for file {}:{}", fullyQualifiedBucket, key);
        AsynchronousFileChannel channel = getFileChannel(lockFileHandle);
        FileLock lock = getFileLock(channel);

        // double checking to see if something which had the lock first has created the file.
        Optional<File> maybeCachedFile = getVerifiedFileFromCacheOrDelete(fullyQualifiedBucket, key, cacheOnly);
        if (maybeCachedFile.isPresent()) {
            releaseLock(channel, lock, lockFileHandle);
            return maybeCachedFile.get();
        }
        Preconditions.checkState(!cacheOnly, "File Bucket=%s Key=%s not found in cache.", fullyQualifiedBucket, key);

        File writableFileHandle = fileCache.createWritableFileHandle(fullyQualifiedBucket, key);
        logger.info("Writing file {}:{} to cache at {}", fullyQualifiedBucket, key, writableFileHandle.getAbsolutePath());
        getFileAndWriteToDestination(fullyQualifiedBucket, key, writableFileHandle);

        logger.info("Applying callback to {}", writableFileHandle.getAbsolutePath());
        writableFileHandle = applyCallbackAndGetFile(writableFileHandle);
        releaseLock(channel, lock, lockFileHandle);
        return writableFileHandle;
    }

    private File getFileAsLocalTemporaryFile(String fullyQualifiedBucket, String key) {
        try {
            //Sanitise the key to not contain any possible directory separators
            String sanitisedFileSuffix = key.replace(DIR_SEPARATOR_FORWARD_SLASH, REPLACEMENT_CHARACTER)
                    .replace(DIR_SEPARATOR_BACK_SLASH, REPLACEMENT_CHARACTER);

            File tempFile = File.createTempFile(FILE_PREFIX, sanitisedFileSuffix);
            tempFile.deleteOnExit();
            logger.info("Cache disabled, writing file {}:{} to temp file at {} (will delete on JVM termination)",
                    fullyQualifiedBucket, key, tempFile.getAbsolutePath());
            getFileAndWriteToDestination(fullyQualifiedBucket, key, tempFile);
            tempFile = applyCallbackAndGetFile(tempFile);
            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            throw Failer.fail("Could not write file %s from bucket %s to local temp file", key, fullyQualifiedBucket);
        }
    }

    private AsynchronousFileChannel getFileChannel(File lockFileHandle) {
        try {
            AsynchronousFileChannel channel;
            channel = AsynchronousFileChannel.open(
                    Paths.get(lockFileHandle.getAbsolutePath()),
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE);
            return channel;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    // Attempts to retrieve a file from the cache. If a file is found it is verified before being returned. If
    // verification fails then the file is deleted so it can separately be re-acquired
    private Optional<File> getVerifiedFileFromCacheOrDelete(String fullyQualifiedBucket, String key, boolean getWithoutVerification) {
        Preconditions.checkState(fileCache != null,
                "Attempted to fetch file Bucket=%s Key=%s using only file cache, but no cache configured.", fullyQualifiedBucket, key);

        Optional<File> maybeCachedFile = fileCache.get(fullyQualifiedBucket, key);

        if (maybeCachedFile.isPresent()) {
            File cachedFile = maybeCachedFile.get();

            if (getWithoutVerification || verifyFileSize(cachedFile, fullyQualifiedBucket, key)) {
                logger.info("{} loaded from FileCache", cachedFile.getAbsolutePath());
                File cachedFileAfterCallback = applyCallbackAndGetFile(cachedFile);
                return Optional.of(cachedFileAfterCallback);
            }

            logger.warn("{} failed verification. Deleting.", cachedFile.getAbsolutePath());
            Preconditions.checkState(
                    cachedFile.delete(),
                    "Failed to delete cached file which is of the wrong file size. File: %s",
                    cachedFile.toString());
        }

        return Optional.empty();
    }

    private FileLock getFileLock(AsynchronousFileChannel channel) {
        try {
            return channel.lock().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    private void releaseLock(AsynchronousFileChannel channel, FileLock lock, File lockFileHandle) {
        try {
            lock.release();
            channel.close();
            Preconditions.checkState(
                    lockFileHandle.delete() || !lockFileHandle.exists(),
                    "Failed to delete lock file %s when trying to release lock. This may remain locked forever",
                    lockFileHandle);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected abstract boolean verifyFileSize(File cachedFile, String bucket, String key);

    protected abstract void getFileAndWriteToDestination(String bucket, String key, File writeableFileHandle);

    protected File applyCallbackAndGetFile(File file) {
        // Default implementation if no callback is required. This method should be overridden if a callback is required.
        return file;
    }

}
