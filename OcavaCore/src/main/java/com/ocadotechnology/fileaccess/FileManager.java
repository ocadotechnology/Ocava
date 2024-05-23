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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.google.common.base.Preconditions;
import com.ocadotechnology.validation.Failer;

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

    protected File getFile(String fullyQualifiedBucket, String key, boolean cacheOnly) {
        Preconditions.checkState(!cacheOnly || fileCache != null,
                "Attempted to fetch file Bucket=%s Key=%s using only S3 cache, but no cache configured.", fullyQualifiedBucket, key);
        if (fileCache == null) {
            return getFileAsLocalTemporaryFile(fullyQualifiedBucket, key);
        }

        File lockFileHandle = fileCache.createLockFileHandle(fullyQualifiedBucket, key);
        if (!lockFileHandle.exists()) {
            Optional<File> cachedFile = fileCache.get(fullyQualifiedBucket, key);
            if (cachedFile.isPresent()) {
                logger.info("{} loaded from FileCache", cachedFile.get().getAbsolutePath());
                return cachedFile.get();
            }
        }

        logger.info("Attempting to get lock on cache for file {}:{}", fullyQualifiedBucket, key);
        AsynchronousFileChannel channel = getFileChannel(lockFileHandle);
        FileLock lock = getFileLock(channel);

        // double checking to see if something which had the lock first has created the file.
        Optional<File> optionalCachedFile = fileCache.get(fullyQualifiedBucket, key);
        if (optionalCachedFile.isPresent()) {
            File cachedFile = optionalCachedFile.get();
            if (cacheOnly || verifyFileSize(cachedFile, fullyQualifiedBucket, key)) {
                logger.info("{} loaded from FileCache", cachedFile.getAbsolutePath());
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

    protected abstract File applyCallbackAndGetFile(File file);

}
