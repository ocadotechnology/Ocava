/*
 * Copyright © 2017 Ocado (Ocava)
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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/** Provides a type-safe way of identifying something, backed by a long. */
@Immutable
public class Id<T> implements Serializable, Comparable<Id<T>>, Identity<T> {
    private static final long serialVersionUID = 1L;

    public final long id;

    @SuppressWarnings("rawtypes")
    private static final LoadingCache<Long, Id> objectCache = CacheBuilder.newBuilder()
            .maximumSize(5_000_000)
            .build(
                    new CacheLoader<Long, Id>() {
                        @Override
                        public Id load(Long id) {
                            return new Id(id);
                        }
                    });
    
    protected Id(long id) {
        this.id = id;
    }

    /**
     * Creates new Id.
     */
    public static <T> Id<T> create(long id) {
        return new Id<>(id);
    }

    /**
     * Returns Id from cache or create new one and cache it.
     * Cache can store up 5_000_000 Ids but unused ID can be evicted earlier if cache is close to max capacity.
     * The cache is backed by Guava {@link LoadingCache}.
     */
    @SuppressWarnings("unchecked")
    public static <T> Id<T> createCached(long id) {
        return objectCache.getUnchecked(id);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        
        return (obj instanceof Id) && ((Id) obj).id == id;
    }
    
    @Override
    public String toString() {
        return String.valueOf(id);
    }

    @Override
    public int compareTo(Id<T> o) {
        return Long.compare(id, o.id);
    }
}
