/*
 * Copyright © 2017-2020 Ocado (Ocava)
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

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.ocadotechnology.config.Config;

/**
 * Wrapper class to effectively make the AmazonS3Client serializable by storing its construction data.
 */
public class SerializableS3Client implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String signerType = "S3SignerType";

    private transient AmazonS3Client amazonS3Client;

    private final String endpoint, accessKey, secretKey;

    public SerializableS3Client(Config<S3Config> applicationConfig) {
        this.endpoint = applicationConfig.getValue(S3Config.S3_ENDPOINT).asString();
        this.accessKey = applicationConfig.getValue(S3Config.S3_ACCESS_KEY).asString();
        this.secretKey = applicationConfig.getValue(S3Config.S3_SECRET_KEY).asString();
    }

    public synchronized AmazonS3Client getS3Client() {
        if (amazonS3Client == null) {
            ClientConfiguration clientConfiguration = new ClientConfiguration();
            clientConfiguration.setSignerOverride(signerType);
            AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            amazonS3Client = new AmazonS3Client(credentials, clientConfiguration);
            amazonS3Client.setEndpoint(endpoint);
            amazonS3Client.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build());
        }
        return amazonS3Client;
    }
}
