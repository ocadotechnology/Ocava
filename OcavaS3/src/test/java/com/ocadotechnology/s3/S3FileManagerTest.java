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
package com.ocadotechnology.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.AmazonClientException;
import com.google.common.collect.ImmutableList;
import com.ocadotechnology.fileaccess.FileCache;
import com.ocadotechnology.fileaccess.FileCacheTestUtils;

@ExtendWith(MockitoExtension.class)
public class S3FileManagerTest {
    private static final String BUCKET_PREFIX = "PREFIX_";
    private static final String ENDPOINT = "ENDPOINT";
    private static final String BUCKET = "BUCKET";

    @Mock
    private FileCache fileCache;
    @Mock
    private S3Querier s3Querier;

    @Test
    void getEndpoint_returnsEndpoint() {
        S3FileManager fileManager = setupFileManager();
        assertThat(fileManager.getEndpoint()).isEqualTo(ENDPOINT);
    }

    @Test
    void getAllKeys_thenPassesThroughRequest() {
        ImmutableList<String> expectedKeys = ImmutableList.of(
                "file1",
                "file2"
        );

        S3FileManager fileManager = setupFileManager();
        setupListKeys(BUCKET, expectedKeys);
        assertThat(fileManager.getAllKeys(BUCKET)).isEqualTo(expectedKeys);
    }

    @Test
    void getAllKeysWithPrefix_thenPassesThroughRequest() {
        String prefix = "file_prefix_";

        ImmutableList<String> expectedKeys = ImmutableList.of(
                prefix + "file1",
                prefix + "file2"
        );

        S3FileManager fileManager = setupFileManager();
        setupListKeys(BUCKET, prefix, expectedKeys);
        assertThat(fileManager.getAllKeys(BUCKET, prefix)).isEqualTo(expectedKeys);
    }

    @Test
    void fileWithPrefixExists_whenNoFilesFound_thenReturnsFalse() {
        String prefix = "file_prefix_";

        S3FileManager fileManager = setupFileManager();
        setupListKeys(BUCKET, prefix, ImmutableList.of());
        assertThat(fileManager.fileWithPrefixExists(BUCKET, prefix)).isEqualTo(false);
    }

    @Test
    void fileWithPrefixExists_whenFilesFound_thenReturnsTrue() {
        String prefix = "file_prefix_";

        ImmutableList<String> returnedKeys = ImmutableList.of(
                prefix + "file1",
                prefix + "file2"
        );

        S3FileManager fileManager = setupFileManager();
        setupListKeys(BUCKET, prefix, returnedKeys);
        assertThat(fileManager.fileWithPrefixExists(BUCKET, prefix)).isEqualTo(true);
    }

    @Test
    void fileExists_whenNoFilesFound_thenReturnsFalse() {
        String fileName = "file_name";

        S3FileManager fileManager = setupFileManager();
        setupListKeys(BUCKET, fileName, ImmutableList.of());
        assertThat(fileManager.fileExists(BUCKET, fileName)).isEqualTo(false);
    }

    @Test
    void fileWithPrefixExists_whenWrongFilesFound_thenReturnsFalse() {
        String fileName = "file_name";

        ImmutableList<String> returnedKeys = ImmutableList.of(
                fileName + "_suffix_1",
                fileName + "_suffix_2"
        );

        S3FileManager fileManager = setupFileManager();
        setupListKeys(BUCKET, fileName, returnedKeys);
        assertThat(fileManager.fileExists(BUCKET, fileName)).isEqualTo(false);
    }

    @Test
    void fileWithPrefixExists_whenFileFound_thenReturnsTrue() {
        String fileName = "file_name";

        ImmutableList<String> returnedKeys = ImmutableList.of(
                fileName + "_suffix",
                fileName
        );

        S3FileManager fileManager = setupFileManager();
        setupListKeys(BUCKET, fileName, returnedKeys);
        assertThat(fileManager.fileExists(BUCKET, fileName)).isEqualTo(true);
    }

    @Test
    void getS3File_whenNoFileCaching_thenAttemptsToCreateTempFile() {
        Mockito.when(s3Querier.getEndpoint()).thenReturn(ENDPOINT);
        S3FileManager fileManager = new S3FileManager(null, s3Querier, BUCKET_PREFIX);

        String fileName = "file_name";

        assertThat(fileManager.getS3File(BUCKET, fileName, false)).isNotNull();

        Mockito.verify(s3Querier).writeObjectToFile(Mockito.eq(BUCKET_PREFIX + BUCKET), Mockito.eq(fileName), Mockito.any(File.class));
    }

    @Test
    void getS3File_whenFileCachingEnabled_thenAttemptsToCreateFile() {
        File testFile = new File("./test_file.txt");
        File lockFile = new File("./lock_file.txt");
        String testKey = "test_key";

        S3FileManager fileManager = setupFileManager();

        FileCacheTestUtils.expectCreateLockFileHandle(fileCache, BUCKET_PREFIX + BUCKET, testKey, lockFile);
        FileCacheTestUtils.expectGet(fileCache, BUCKET_PREFIX + BUCKET, testKey, Optional.empty());
        FileCacheTestUtils.expectCreateWriteableFileHandle(fileCache, BUCKET_PREFIX + BUCKET, testKey, testFile);

        assertThat(fileManager.getS3File(BUCKET, testKey, false)).isEqualTo(testFile);

        Mockito.verify(s3Querier).writeObjectToFile(Mockito.eq(BUCKET_PREFIX + BUCKET), Mockito.eq(testKey), Mockito.eq(testFile));
    }

    @Test
    void getS3File_whenCachedFilePresent_thenDoesNotAttemptToCreateFile() {
        File testFile = new File("./test_file.txt");
        File lockFile = new File("./lock_file.txt");
        String testKey = "test_key";

        S3FileManager fileManager = setupFileManager();

        FileCacheTestUtils.expectCreateLockFileHandle(fileCache, BUCKET_PREFIX + BUCKET, testKey, lockFile);
        FileCacheTestUtils.expectGet(fileCache, BUCKET_PREFIX + BUCKET, testKey, Optional.of(testFile));

        assertThat(fileManager.getS3File(BUCKET, testKey, false)).isEqualTo(testFile);

        //Shouldn't be called
        FileCacheTestUtils.neverExpectCreateWriteableFileHandle(fileCache);
        Mockito.verify(s3Querier, Mockito.never()).getContentLength(Mockito.any(), Mockito.any());
        Mockito.verify(s3Querier, Mockito.never()).writeObjectToFile(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void getS3File_whenDownloadFailsOnce_thenRetriesUntilSuccessful() {
        Mockito.when(s3Querier.getEndpoint()).thenReturn(ENDPOINT);
        S3FileManager fileManager = new S3FileManager(null, s3Querier, BUCKET_PREFIX);

        String fileName = "file_name";

        Mockito.doThrow(new AmazonClientException("Test Exception")) //First call
                .doNothing() // Second call
                .when(s3Querier).writeObjectToFile(Mockito.eq(BUCKET_PREFIX + BUCKET), Mockito.eq(fileName), Mockito.any(File.class)); //Method definition

        fileManager.getS3File(BUCKET, fileName, false);

        Mockito.verify(s3Querier, Mockito.times(2))
                .writeObjectToFile(Mockito.eq(BUCKET_PREFIX + BUCKET), Mockito.eq(fileName), Mockito.any(File.class));
    }

    @Test
    void getS3File_whenDownloadFailsRepeatedly_thenRetriesUntilTermination() {
        Mockito.when(s3Querier.getEndpoint()).thenReturn(ENDPOINT);
        S3FileManager fileManager = new S3FileManager(null, s3Querier, BUCKET_PREFIX);

        String fileName = "file_name";

        Mockito.doThrow(new AmazonClientException("Test Exception"))
                .when(s3Querier).writeObjectToFile(Mockito.eq(BUCKET_PREFIX + BUCKET), Mockito.eq(fileName), Mockito.any(File.class));

        assertThatThrownBy(() -> fileManager.getS3File(BUCKET, fileName, false)).isInstanceOf(IllegalStateException.class);

        Mockito.verify(s3Querier, Mockito.times(S3FileManager.MAX_RETRY_ATTEMPTS))
                .writeObjectToFile(Mockito.eq(BUCKET_PREFIX + BUCKET), Mockito.eq(fileName), Mockito.any(File.class));
    }

    private S3FileManager setupFileManager() {
        Mockito.when(s3Querier.getEndpoint()).thenReturn(ENDPOINT);
        return new S3FileManager(fileCache, s3Querier, BUCKET_PREFIX);
    }

    private void setupListKeys(String bucket, ImmutableList<String> keys) {
        Mockito.when(s3Querier.getAllKeys(Mockito.eq(BUCKET_PREFIX + bucket))).thenReturn(keys);
    }

    private void setupListKeys(String bucket, String keyPrefix, ImmutableList<String> keys) {
        Mockito.when(s3Querier.getAllKeys(Mockito.eq(BUCKET_PREFIX + bucket), Mockito.eq(keyPrefix))).thenReturn(keys);
    }
}
