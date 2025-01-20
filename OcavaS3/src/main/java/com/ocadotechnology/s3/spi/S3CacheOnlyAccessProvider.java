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
package com.ocadotechnology.s3.spi;

import com.ocadotechnology.config.Config;
import com.ocadotechnology.fileaccess.service.DataAccessor;
import com.ocadotechnology.fileaccess.serviceprovider.DataAccessServiceProvider;

public class S3CacheOnlyAccessProvider implements DataAccessServiceProvider {
    public static final String MODE = "S3_CACHE_ONLY";

    @Override
    public DataAccessor createAccessor(Config<?> initialConfig) {
        return new S3FileFetcher(initialConfig, true);
    }

    @Override
    public String getMode() {
        return MODE;
    }
}
