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

import com.ocadotechnology.config.SecretConfig;

public enum S3Config {
    S3_ENDPOINT,
    S3_ACCESS_KEY,
    @SecretConfig S3_SECRET_KEY,

    ENABLE_S3_FILE_CACHE,
    S3_FILE_CACHE_ROOT,
    BUCKET_PREFIX
}
