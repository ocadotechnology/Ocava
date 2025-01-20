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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.config.Config;
import com.ocadotechnology.fileaccess.CredentialsProvider;

class S3Credentials {
    private final CredentialsProvider fileProvider;
    private static final String ACCESS_KEY = "s3_access_key";
    private static final String SECRET_KEY = "s3_secret_key";
    private static final String ENDPOINT = "s3_endpoint";

    private final String endpoint, accessKey, secretKey;

    S3Credentials(Config<S3Config> s3Config) {
        fileProvider = new CredentialsProvider();
        if (configDefinesAllValues(s3Config)){
            this.endpoint = s3Config.getValue(S3Config.S3_ENDPOINT).asString();
            this.accessKey = s3Config.getValue(S3Config.S3_ACCESS_KEY).asString();
            this.secretKey = s3Config.getValue(S3Config.S3_SECRET_KEY).asString();
        } else {
            ImmutableMap<String, String> credentialsMap = fileProvider.getCredentials();
            this.endpoint = Preconditions.checkNotNull(credentialsMap.get(ENDPOINT), "S3_ENDPOINT cannot be null");
            this.accessKey = Preconditions.checkNotNull(credentialsMap.get(ACCESS_KEY), "S3_ACCESS_KEY cannot be null");
            this.secretKey = Preconditions.checkNotNull(credentialsMap.get(SECRET_KEY), "S3_SECRET_KEY cannot be null");
        }
    }

    String getAccessKey() {
        return accessKey;
    }

    String getSecretKey() {
        return secretKey;
    }

    String getEndpoint() {
        return endpoint;
    }

    private boolean configDefinesAllValues(Config<S3Config> s3config) {
        return s3config.areKeyAndValueDefined(S3Config.S3_ACCESS_KEY) && s3config.areKeyAndValueDefined(S3Config.S3_SECRET_KEY) && s3config.areKeyAndValueDefined(S3Config.S3_ENDPOINT);
    }
}
