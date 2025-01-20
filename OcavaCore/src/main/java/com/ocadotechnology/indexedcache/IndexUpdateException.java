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
package com.ocadotechnology.indexedcache;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Indicates that an exception occurred while updating an index. All changes caused by the throwing method should be
 * rolled back before propagating this exception.
 */
@ParametersAreNonnullByDefault
public class IndexUpdateException extends Exception {
    private final String indexName;

    public IndexUpdateException(String indexName, String message, Object... messageArgs) {
        super(String.format(message, messageArgs));
        this.indexName = indexName;
    }

    public String getIndexName() {
        return indexName;
    }
}
