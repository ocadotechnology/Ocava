/*
 * Copyright © 2017-2019 Ocado (Ocava)
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

class S3FileCache implements Serializable {
    private static final long serialVersionUID = 1L;

    static final String DEFAULT_S3_CACHE_DIR = System.getProperty("user.home") + File.separatorChar + ".s3cache";
    private static final Logger logger = LoggerFactory.getLogger(S3FileCache.DEFAULT_S3_CACHE_DIR);

    private final File rootCacheDirectory;

    S3FileCache(File rootCacheDirectory) {
        this.rootCacheDirectory = rootCacheDirectory;
    }

    Optional<File> get(String fullyQualifiedBucket, String key) {
        File bucketCacheDirectory = new File(rootCacheDirectory, fullyQualifiedBucket);
        if (!bucketCacheDirectory.exists()) {
            return Optional.empty();
        }
        File cachedFile = new File(bucketCacheDirectory, key);
        return cachedFile.exists() ? Optional.of(cachedFile) : Optional.empty();
    }

    File createWritableFileHandle(String fullyQualifiedBucket, String key) {
        File f = new File(rootCacheDirectory, fullyQualifiedBucket);
        for (String subPath : key.split("/")) {
            f = new File(f, subPath);
        }
        if (f.getParentFile().mkdirs()) {
            logger.info("Created cache directory {}", f.getParent());
        }
        Preconditions.checkState(f.getParentFile().exists(), "Parent File %s does not exist", f.getParentFile());
        Preconditions.checkState(f.getParentFile().isDirectory(), "Parent File %s is not a directory", f.getParentFile());
        Preconditions.checkState(!f.exists(), "File %s exists", f);
        return f;
    }

    File createLockFileHandle(String fullyQualifiedBucket, String key) {
        key = key + ".lock";
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

    void purgeLocalCache(String fullyQualifiedBucket) throws IOException {
        logger.warn("Purging S3 Cache");
        File bucketCacheDirectory = new File(rootCacheDirectory, fullyQualifiedBucket);
        if (bucketCacheDirectory.exists()) {
            Preconditions.checkState(bucketCacheDirectory.isDirectory());
            Files.walk(bucketCacheDirectory.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}
