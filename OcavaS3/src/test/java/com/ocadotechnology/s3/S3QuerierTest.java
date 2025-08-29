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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class S3QuerierTest {
    @Mock
    private SerializableS3Client s3Client;
    private S3Querier s3Querier;

    @Captor
    private ArgumentCaptor<ListObjectsRequest> listObjectsRequestCaptor;
    @Captor
    private ArgumentCaptor<GetObjectRequest> getObjectRequestCaptor;

    @BeforeEach
    void setup() {
        s3Querier = new S3Querier(s3Client);
    }

    @Test
    void getEndpoint_returnsEndpoint() {
        String endpoint = "endpoint";
        Mockito.when(s3Client.getEndpoint()).thenReturn(endpoint);
        assertThat(s3Querier.getEndpoint()).isEqualTo(endpoint);
    }

    @Test
    void getAllKeys_whenNoKeysFound_thenReturnsNoKeys() {
        String bucket = "test_bucket";

        setupListFilesMocks(ImmutableList.of());
        assertThat(s3Querier.getAllKeys(bucket)).isEqualTo(ImmutableList.of());
        verifyListObjectsRequest(bucket);
    }

    @Test
    void getAllKeys_whenKeysFound_thenReturnsKeys() {
        ImmutableList<String> expectedKeys = ImmutableList.of(
                "file1",
                "file2"
        );
        String bucket = "test_bucket";

        setupListFilesMocks(expectedKeys);
        assertThat(s3Querier.getAllKeys(bucket)).isEqualTo(expectedKeys);
        verifyListObjectsRequest(bucket);
    }

    @Test
    void getAllKeysWithPrefix_whenNoKeysFound_thenReturnsNoKeys() {
        String prefix = "file_prefix_";
        String bucket = "test_bucket";

        setupListFilesMocks(ImmutableList.of());
        assertThat(s3Querier.getAllKeys(bucket, prefix)).isEqualTo(ImmutableList.of());
        verifyListObjectsRequest(bucket, prefix);
    }

    @Test
    void getAllKeysWithPrefix_whenKeysFound_thenReturnsKeys() {
        String prefix = "file_prefix_";
        ImmutableList<String> expectedKeys = ImmutableList.of(
                prefix + "file1",
                prefix + "file2"
        );
        String bucket = "test_bucket";

        setupListFilesMocks(expectedKeys);
        assertThat(s3Querier.getAllKeys(bucket, prefix)).isEqualTo(expectedKeys);
        verifyListObjectsRequest(bucket, prefix);
    }

    @Test
    void writeObjectToFile_thenPassesThroughCorrectRequest() {
        String bucket = "test_bucket";
        String key = "test_key";
        File testFile = new File("./test_file.csv");

        S3Client mockClient = Mockito.mock(S3Client.class);
        Mockito.when(s3Client.getS3Client()).thenReturn(mockClient);
        Mockito.when(mockClient.getObject(getObjectRequestCaptor.capture(), Mockito.eq(testFile.toPath()))).thenReturn(null);

        s3Querier.writeObjectToFile(bucket, key, testFile);

        verifyGetObjectRequest(bucket, key);
    }

    @Test
    void getContentLength_thenPassesThroughCorrectRequest() {
        String bucket = "test_bucket";
        String key = "test_key";
        long fileSize = 10101;

        S3Client mockClient = Mockito.mock(S3Client.class);
        HeadObjectResponse mockHeadObjectResponse = Mockito.mock(HeadObjectResponse.class);
        Mockito.when(s3Client.getS3Client()).thenReturn(mockClient);
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().bucket(bucket).key(key).build();
        Mockito.when(mockClient.headObject(Mockito.eq(headObjectRequest))).thenReturn(mockHeadObjectResponse);
        Mockito.when(mockHeadObjectResponse.contentLength()).thenReturn(fileSize);

        assertThat(s3Querier.getContentLength(bucket, key)).isEqualTo(fileSize);
    }

    private void setupListFilesMocks(ImmutableList<String> keys) {
        S3Client mockClient = Mockito.mock(S3Client.class);
        ListObjectsResponse mockObjectListing = Mockito.mock(ListObjectsResponse.class);
        List<S3Object> mockObjectSummaries = keys.stream()
                .map(key -> {
                    S3Object summary = Mockito.mock(S3Object.class);
                    Mockito.when(summary.key()).thenReturn(key);
                    return summary;
                })
                .collect(Collectors.toList());

        Mockito.when(s3Client.getS3Client()).thenReturn(mockClient);
        Mockito.when(mockClient.listObjects(listObjectsRequestCaptor.capture())).thenReturn(mockObjectListing);
        Mockito.when(mockObjectListing.contents()).thenReturn(mockObjectSummaries);
    }

    private void verifyListObjectsRequest(String bucket) {
        verifyListObjectsRequest(bucket, null);
    }

    private void verifyListObjectsRequest(String bucket, String keyPrefix) {
        ListObjectsRequest request = listObjectsRequestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo(bucket);
        assertThat(request.prefix()).isEqualTo(keyPrefix);
        assertThat(request.maxKeys()).isEqualTo(Integer.MAX_VALUE);
    }

    private void verifyGetObjectRequest(String bucket, String fileName) {
        GetObjectRequest request = getObjectRequestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo(bucket);
        assertThat(request.key()).isEqualTo(fileName);
    }
}
