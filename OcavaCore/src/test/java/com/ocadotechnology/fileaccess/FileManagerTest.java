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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class FileManagerTest {
    public static final String BUCKET = "/bucket/path";
    public static final String FILE_NAME = "fileName.txt";
    private static final File CACHE_DIRECTORY = new File("/tmp/FileManagerTestCache");

    private FileCache fileCache;

    @BeforeEach
    public void setUp() {
        fileCache = new FileCache(CACHE_DIRECTORY);
    }

    @Test
    public void getFile_withoutCache_shouldRetrieveFile() {
        TestFileManager fileManager = new TestFileManager(null);

        File file = fileManager.getFile(BUCKET, FILE_NAME, false);
        assertThat(file.exists()).isTrue();
        assertThat(fileManager.hasFileBeenRetrievedAndWritten()).isTrue();
    }

    @Test
    public void getFile_withoutCache_shouldApplyCallback() {
        AtomicBoolean hasCallbackRun = new AtomicBoolean(false);
        TestFileManager fileManager = new TestFileManager(null, () -> hasCallbackRun.set(true));

        fileManager.getFile(BUCKET, FILE_NAME, false);
        assertThat(hasCallbackRun.get()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void getFile_withCache_shouldRetrieveFileForFileNotInCache(boolean lockFileExists) {
        if (lockFileExists) {
            createLockFile();
        }

        TestFileManager fileManager = new TestFileManager(fileCache);

        File expected = new File(CACHE_DIRECTORY, BUCKET + "/" + FILE_NAME);
        assertThat(expected.exists()).isFalse();

        File file = fileManager.getFile(BUCKET, FILE_NAME, false);
        assertThat(file.exists()).isTrue();
        assertThat(fileManager.hasFileBeenRetrievedAndWritten()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void getFile_withCache_shouldApplyCallbackForFileNotInCache(boolean lockFileExists) {
        if (lockFileExists) {
            createLockFile();
        }

        AtomicBoolean hasCallbackRun = new AtomicBoolean(false);
        TestFileManager fileManager = new TestFileManager(fileCache, () -> hasCallbackRun.set(true));

        File expected = new File(CACHE_DIRECTORY, BUCKET + "/" + FILE_NAME);
        assertThat(expected.exists()).isFalse();

        fileManager.getFile(BUCKET, FILE_NAME, false);
        assertThat(hasCallbackRun.get()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void getFile_withCache_shouldGetFileFromCacheForFileInCache(boolean lockFileExists) {
        if (lockFileExists) {
            createLockFile();
        }

        TestFileManager fileManager = new TestFileManager(fileCache);

        createCachedFile();

        File file = fileManager.getFile(BUCKET, FILE_NAME, false);
        assertThat(file.exists()).isTrue();
        assertThat(fileManager.hasFileBeenRetrievedAndWritten()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void getFile_withCache_shouldApplyCallbackForFileInCache(boolean lockFileExists) {
        if (lockFileExists) {
            createLockFile();
        }

        AtomicBoolean hasCallbackRun = new AtomicBoolean(false);
        TestFileManager fileManager = new TestFileManager(fileCache, () -> hasCallbackRun.set(true));

        createCachedFile();

        fileManager.getFile(BUCKET, FILE_NAME, false);
        assertThat(hasCallbackRun.get()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void getFile_withCache_shouldRetrieveFileIfFileInCacheButVerificationFails(boolean lockFileExists) {
        if (lockFileExists) {
            createLockFile();
        }

        TestFileManager fileManager = new TestFileManager(fileCache, false);

        createCachedFile();

        File file = fileManager.getFile(BUCKET, FILE_NAME, false);
        assertThat(file.exists()).isTrue();
        assertThat(fileManager.hasFileBeenRetrievedAndWritten()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void getFile_withCache_shouldGetFileFromCacheIfFileInCacheAndVerificationFailsButCacheOnlyFlagEnabled(boolean lockFileExists) {
        if (lockFileExists) {
            createLockFile();
        }

        TestFileManager fileManager = new TestFileManager(fileCache, false);

        createCachedFile();

        File file = fileManager.getFile(BUCKET, FILE_NAME, true);
        assertThat(file.exists()).isTrue();
        assertThat(fileManager.hasFileBeenRetrievedAndWritten()).isFalse();
    }

    @AfterEach
    public void tearDown() {
        deleteFileRecursively(CACHE_DIRECTORY);
    }

    private static void createFileAndDirectories(File expected) {
        try {
            expected.getParentFile().mkdirs();
            expected.createNewFile();
        } catch (IOException e) {
            // Ignore
        }
    }

    private void deleteFileRecursively(File file) {
        if (file.isDirectory()) {
            for (File innerFile : file.listFiles()) {
                deleteFileRecursively(innerFile);
            }
        }
        file.delete();
    }

    private void createLockFile() {
        File lockFile = fileCache.createLockFileHandle(BUCKET, FILE_NAME);
        createFileAndDirectories(lockFile);
        assertThat(lockFile.exists()).isTrue();
    }

    private static void createCachedFile() {
        File expected = new File(CACHE_DIRECTORY, BUCKET + "/" + FILE_NAME);
        createFileAndDirectories(expected);
        assertThat(expected.exists()).isTrue();
    }
}
