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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.CheckForNull;

import com.ocadotechnology.id.Identified;
import com.ocadotechnology.id.Identity;

/**
 * The default implementation of an {@link AbstractIdCachedPredicateIndex}.
 * <br>
 * A predicate index that does not fully cache its mapping to values in the backing cache - it caches a mapping to ids
 * that can be quickly looked up in the (hashmap-based) backing cache.
 * <br>
 * This class is best used for indexes where updates to the cached object rarely alter the outcome of the predicate
 * test (near-zero update cost), and where a common use-case involves streaming what is expected to be a small
 * percentage of the total cached objects (matching objects are always known, but require an indirect lookup).
 */
public class IdCachedPredicateIndex<C extends Identified<? extends I>, I> extends AbstractIdCachedPredicateIndex<C, I> {
    private final Set<Identity<? extends I>> objectIdsMatchingPredicate = new LinkedHashSet<>();
    private final Set<Identity<? extends I>> objectIdsNotMatchingPredicate = new LinkedHashSet<>();

    public IdCachedPredicateIndex(IndexedImmutableObjectCache<C, I> backingCache, Predicate<? super C> predicate) {
        super(null, backingCache, predicate);
    }

    public IdCachedPredicateIndex(@CheckForNull String name, IndexedImmutableObjectCache<C, I> backingCache, Predicate<? super C> predicate) {
        super(name, backingCache, predicate);
    }

    @Override
    protected final Set<Identity<? extends I>> getIdsMatchingPredicate() {
        return objectIdsMatchingPredicate;
    }

    @Override
    protected final Set<Identity<? extends I>> getIdsNotMatchingPredicate() {
        return objectIdsNotMatchingPredicate;
    }
}
