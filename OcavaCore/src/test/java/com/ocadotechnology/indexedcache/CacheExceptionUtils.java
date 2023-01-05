/*
 * Copyright © 2017-2023 Ocado (Ocava)
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

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Condition;

class CacheExceptionUtils {
    static <T extends Throwable> Condition<T> validateCacheUpdateException(String indexName) {
        return new Condition<>(t -> validateCacheUpdateException(t, indexName), "correct error details");
    }

    private static boolean validateCacheUpdateException(Throwable t, String indexName) {
        CacheUpdateException cacheUpdateException = (CacheUpdateException) t;
        assertThat(cacheUpdateException.getFailingIndexName()).contains(indexName);
        assertThat(cacheUpdateException).hasCauseInstanceOf(IndexUpdateException.class);

        IndexUpdateException indexUpdateException = (IndexUpdateException) cacheUpdateException.getCause();
        assertThat(indexUpdateException).hasMessageContaining(indexName);
        assertThat(indexUpdateException.getIndexName()).isEqualTo(indexName);
        return true;
    }

}
