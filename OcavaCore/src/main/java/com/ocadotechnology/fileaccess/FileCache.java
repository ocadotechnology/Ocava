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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * A file cache which looks for files in a specified cache directory. The directory structure within the cache should
 * follow the structure of the original file source.
 */
public class FileCache implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(FileCache.class);
    protected final File rootCacheDirectory;

    public FileCache(File rootCacheDirectory) {
        this.rootCacheDirectory = rootCacheDirectory;
    }

    /**
     * Retrieve a file from the cache using the bucket or directory and file name from the original source.
     * If the file is not in the cache then this returns empty.
     *
     * @param fullyQualifiedBucket The bucket or directory containing the file in the original source.
     * @param key The name of the file.
     * @return The locally cached file if it is in the cache, otherwise empty.
     */
    Optional<File> get(String fullyQualifiedBucket, String key) {
        File bucketCacheDirectory = new File(rootCacheDirectory, fullyQualifiedBucket);
        if (!bucketCacheDirectory.exists()) {
            return Optional.empty();
        }
        File cachedFile = new File(bucketCacheDirectory, key);
        return cachedFile.exists() ? Optional.of(cachedFile) : Optional.empty();
    }

    /**
     * Create a path for writing a file in the cache following the structure of the original source.
     * This will create the appropriate directories if they do not exist.
     * This will fail if a file at this path already exists.
     *
     * @param fullyQualifiedBucket The bucket or directory containing the file in the original source.
     * @param key The name of the file.
     * @return The path to the location in the cache to write the file to.
     */
    File createWritableFileHandle(String fullyQualifiedBucket, String key) {
        File f = createFileHandle(fullyQualifiedBucket, key);
        Preconditions.checkState(!f.exists(), "File %s exists", f);
        return f;
    }

    /**
     * Create a path for creating a lock file for a file in the cache following the structure of the original source of
     * that file. The intention is to use the lock file as a source of file locks for retrieving or accessing the file.
     * This will create the appropriate directories if they do not exist.
     *
     * @param fullyQualifiedBucket The bucket or directory containing the file in the original source.
     * @param key The name of the file.
     * @return The path to the location in the cache to write the lock file to.
     */
    File createLockFileHandle(String fullyQualifiedBucket, String key) {
        key = key + ".lock";
        return createFileHandle(fullyQualifiedBucket, key);
    }

    private File createFileHandle(String fullyQualifiedBucket, String key) {
        File f = new File(rootCacheDirectory, fullyQualifiedBucket);
        for (String subPath : key.split("/")) {
            f = new File(f, subPath);
        }
        if (f.getParentFile().mkdirs()) {
            logger.info("Created cache directory {}", f.getParent());
        }
        Preconditions.checkState(f.getParentFile().exists(), "Parent File %s does not exist", f.getParentFile());
        Preconditions.checkState(f.getParentFile().isDirectory(), "Parent File %s is not a directory", f.getParentFile());
        return f;
    }

    /**
     * Delete all files in the cache for a given bucket or directory from an original file source.
     *
     * @param fullyQualifiedBucket The directory in the cache to be cleared.
     * @throws IOException If the directory cannot be found
     */
    void purgeLocalCache(String fullyQualifiedBucket) throws IOException {
        logger.warn("Purging contents of {} in file cache", fullyQualifiedBucket);
        File bucketCacheDirectory = new File(rootCacheDirectory, fullyQualifiedBucket);
        if (bucketCacheDirectory.exists()) {
            Preconditions.checkState(bucketCacheDirectory.isDirectory());

            try (Stream<Path> files = Files.walk(bucketCacheDirectory.toPath())) {
                files.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }
}
