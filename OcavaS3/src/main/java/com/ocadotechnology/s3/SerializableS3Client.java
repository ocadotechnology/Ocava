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

import java.io.Serializable;
import java.net.URI;

import javax.annotation.CheckForNull;

import joptsimple.internal.Strings;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import com.ocadotechnology.config.Config;

/**
 * Wrapper class to effectively make the AmazonS3Client serializable by storing its construction data.
 */
public class SerializableS3Client implements Serializable {
    private static final long serialVersionUID = 1L;

    @CheckForNull
    private static final String AWS_REGION = System.getenv("aws_region");
    private transient S3Client amazonS3Client;

    private final String endpoint, accessKey, secretKey;

    public SerializableS3Client(Config<S3Config> applicationConfig) {
        S3Credentials s3Credentials = new S3Credentials(applicationConfig);
        this.endpoint = s3Credentials.getEndpoint();
        this.accessKey = s3Credentials.getAccessKey();
        this.secretKey = s3Credentials.getSecretKey();
    }

    public synchronized S3Client getS3Client() {
        if (amazonS3Client == null) {
            var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
            var builder = S3Client.builder();
            if (!Strings.isNullOrEmpty(AWS_REGION)) {
                builder = builder.region(Region.of(AWS_REGION));
            }
            amazonS3Client = builder
                    .credentialsProvider(credentials)
                    .endpointOverride(URI.create(endpoint))
                    .forcePathStyle(true)
                    .build();
        }
        return amazonS3Client;
    }

    public String getEndpoint() {
        return this.endpoint;
    }
}
