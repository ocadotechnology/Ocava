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
package com.ocadotechnology.id;

import java.io.Serializable;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/** Provides a type-safe way of identifying something, backed by a String. */
@Immutable
public class StringId<T> implements Serializable, Comparable<StringId<T>>, Identity<T> {
    private static final long serialVersionUID = 1L;

    public final String id;
    private final int hashCode;

    @SuppressWarnings("rawtypes")
    private static final LoadingCache<String, StringId> objectCache = CacheBuilder.newBuilder()
            .maximumSize(5_000_000)
            .build(CacheLoader.from(StringId::new));

    /**
     * Returns StringId from cache or create new one and cache it.
     * Cache can store up 5_000_000 Ids but unused ID can be evicted earlier if cache is close to max capacity.
     * The cache is backed by Guava {@link LoadingCache}.
     */
    @SuppressWarnings("unchecked")
    public static <T> StringId<T> createCached(String id) {
        return objectCache.getUnchecked(id);
    }

    /**
     * Creates new StringId
     */
    public static <T> StringId<T> create(String id) {
        return new StringId<>(id);
    }

    private StringId(String id) {
        Preconditions.checkNotNull(id);
        Preconditions.checkArgument(!id.isEmpty());
        this.id = id;
        this.hashCode = id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof StringId
                && this.hashCode() == obj.hashCode()
                && this.id.equals(((StringId) obj).id);
    }

    @Override
    public int compareTo(StringId<T> o) {
        return id.compareTo(o.id);
    }
}
