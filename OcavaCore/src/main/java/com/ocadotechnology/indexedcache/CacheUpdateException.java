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

import java.util.Optional;

import javax.annotation.CheckForNull;

/**
 * Indicates that an exception occurred while updating the IndexedImmutableObjectCache and that no changes should have
 * resulted to the cache or indices as a result of the bad method call.
 */
public class CacheUpdateException extends RuntimeException {
    @CheckForNull
    private final String failingIndexName;

    public CacheUpdateException(String message) {
        super(message);
        this.failingIndexName = null;
    }

    public CacheUpdateException(String message, IndexUpdateException cause) {
        super(message, cause);
        this.failingIndexName = cause.getIndexName();
    }

    public Optional<String> getFailingIndexName() {
        return Optional.ofNullable(failingIndexName);
    }
}
