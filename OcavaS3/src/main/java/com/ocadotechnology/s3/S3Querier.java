/*
 * Copyright © 2017-2022 Ocado (Ocava)
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
import java.io.Serializable;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.collect.ImmutableList;

class S3Querier implements Serializable {
    private final SerializableS3Client s3Client;

    S3Querier(SerializableS3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * @return the S3 endpoint for this connection
     */
    public String getEndpoint() {
        return s3Client.getEndpoint();
    }

    /**
     * @param fullyQualifiedBucket the bucket to search
     * @return All (up to Integer.MAX_VALUE) keys in the bucket
     */
    ImmutableList<String> getAllKeys(String fullyQualifiedBucket) {
        ListObjectsRequest request = new ListObjectsRequest().withBucketName(fullyQualifiedBucket).withMaxKeys(Integer.MAX_VALUE);
        return getAllKeys(request);
    }

    /**
     * @param fullyQualifiedBucket the bucket to search
     * @param keyPrefix the key prefix to filter on
     * @return All (up to Integer.MAX_VALUE) keys in the bucket which match the given prefix
     */
    ImmutableList<String> getAllKeys(String fullyQualifiedBucket, String keyPrefix) {
        ListObjectsRequest request = new ListObjectsRequest().withBucketName(fullyQualifiedBucket).withPrefix(keyPrefix).withMaxKeys(Integer.MAX_VALUE);
        return getAllKeys(request);
    }

    private ImmutableList<String> getAllKeys(ListObjectsRequest request) {
        return s3Client.getS3Client().listObjects(request)
                .getObjectSummaries()
                .stream()
                .map(S3ObjectSummary::getKey)
                .collect(ImmutableList.toImmutableList());
    }

    /**
     * @param fullyQualifiedBucket the bucket to query
     * @param key the key to locate
     * @return the {@link ObjectMetadata#getContentLength()} result for this key
     */
    long getContentLength(String fullyQualifiedBucket, String key) throws AmazonClientException {
        return s3Client.getS3Client().getObjectMetadata(fullyQualifiedBucket, key).getContentLength();
    }

    /**
     * @param fullyQualifiedBucket the bucket to query
     * @param key the key to locate
     * @param destinationFile the file location to write the contents of this object to
     */
    void writeObjectToFile(String fullyQualifiedBucket, String key, File destinationFile) throws AmazonClientException {
        s3Client.getS3Client().getObject(new GetObjectRequest(fullyQualifiedBucket, key), destinationFile);
    }
}
