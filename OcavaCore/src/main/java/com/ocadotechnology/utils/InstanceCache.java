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
package com.ocadotechnology.utils;

import com.google.common.base.Functions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * This class is useful when you want to avoid creating large numbers of
 * instances of the same reference type in memory (e.g. date times) that
 * are otherwise reference equal and need to live for the lifetime of the
 * simulation (and thus can't get garbage collected).
 * @param <T> The type of the object to cache.
 */
public class InstanceCache<T> {
    private final LoadingCache<T, T> cache = CacheBuilder.newBuilder()
            .build(CacheLoader.from(Functions.identity()));

    public T getEqual(T instance) {
        return cache.getUnchecked(instance);
    }
}
