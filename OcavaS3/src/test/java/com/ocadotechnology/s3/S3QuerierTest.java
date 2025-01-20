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

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
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

        AmazonS3Client mockClient = Mockito.mock(AmazonS3Client.class);
        Mockito.when(s3Client.getS3Client()).thenReturn(mockClient);
        Mockito.when(mockClient.getObject(getObjectRequestCaptor.capture(), Mockito.eq(testFile))).thenReturn(null);

        s3Querier.writeObjectToFile(bucket, key, testFile);

        verifyGetObjectRequest(bucket, key);
    }

    @Test
    void getContentLength_thenPassesThroughCorrectRequest() {
        String bucket = "test_bucket";
        String key = "test_key";
        long fileSize = 10101;

        AmazonS3Client mockClient = Mockito.mock(AmazonS3Client.class);
        ObjectMetadata mockMetadata = Mockito.mock(ObjectMetadata.class);
        Mockito.when(s3Client.getS3Client()).thenReturn(mockClient);
        Mockito.when(mockClient.getObjectMetadata(Mockito.eq(bucket), Mockito.eq(key))).thenReturn(mockMetadata);
        Mockito.when(mockMetadata.getContentLength()).thenReturn(fileSize);

        assertThat(s3Querier.getContentLength(bucket, key)).isEqualTo(fileSize);
    }

    private void setupListFilesMocks(ImmutableList<String> keys) {
        AmazonS3Client mockClient = Mockito.mock(AmazonS3Client.class);
        ObjectListing mockObjectListing = Mockito.mock(ObjectListing.class);
        List<S3ObjectSummary> mockObjectSummaries = keys.stream()
                .map(key -> {
                    S3ObjectSummary summary = Mockito.mock(S3ObjectSummary.class);
                    Mockito.when(summary.getKey()).thenReturn(key);
                    return summary;
                })
                .collect(Collectors.toList());

        Mockito.when(s3Client.getS3Client()).thenReturn(mockClient);
        Mockito.when(mockClient.listObjects(listObjectsRequestCaptor.capture())).thenReturn(mockObjectListing);
        Mockito.when(mockObjectListing.getObjectSummaries()).thenReturn(mockObjectSummaries);
    }

    private void verifyListObjectsRequest(String bucket) {
        verifyListObjectsRequest(bucket, null);
    }

    private void verifyListObjectsRequest(String bucket, String keyPrefix) {
        ListObjectsRequest request = listObjectsRequestCaptor.getValue();
        assertThat(request.getBucketName()).isEqualTo(bucket);
        assertThat(request.getPrefix()).isEqualTo(keyPrefix);
        assertThat(request.getMaxKeys()).isEqualTo(Integer.MAX_VALUE);
    }

    private void verifyGetObjectRequest(String bucket, String fileName) {
        GetObjectRequest request = getObjectRequestCaptor.getValue();
        assertThat(request.getBucketName()).isEqualTo(bucket);
        assertThat(request.getKey()).isEqualTo(fileName);
    }
}
