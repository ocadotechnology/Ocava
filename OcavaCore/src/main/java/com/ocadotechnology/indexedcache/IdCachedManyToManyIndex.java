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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.ocadotechnology.id.Identified;
import com.ocadotechnology.id.Identity;

/**
 * A many-to-many index that does not fully cache its mapping to values in the backing cache - it caches a mapping to ids
 * that can be quickly looked up in the (hashmap-based) backing cache.
 * <br>
 * This class is best used for indexes where updates to the cached object rarely alter the outcome of the mapping value (near-zero update cost),
 * and where a common use-case involves streaming what is expected to be a small
 * percentage of the total cached objects (matching objects are always known, but require an indirect lookup).
 */
final class IdCachedManyToManyIndex<R, C extends Identified<? extends I>, I> extends ManyToManyIndex<R, C> {
    private final IndexedImmutableObjectCache<C, I> backingCache;
    private final Function<? super C, Set<R>> function;
    private final Map<R, Set<Identity<? extends I>>> indexValues = new HashMap<>();

    /**
     * @param name optional String parameter - the name of the index.
     * @param backingCache the cache that this index is indexing.
     * @param function the function used to map values in the cache.
     */
    public IdCachedManyToManyIndex(@CheckForNull String name, IndexedImmutableObjectCache<C, I> backingCache, Function<? super C, Set<R>> function) {
        super(name);
        this.function = function;
        this.backingCache = backingCache;
    }

    @Override
    public boolean containsKey(R r) {
        return indexValues.containsKey(r);
    }

    @Override
    public Stream<C> stream(R r) {
        return getMutable(r).stream().map(this::get);
    }

    @Override
    public Stream<R> streamKeySet() {
        return indexValues.keySet().stream();
    }

    @Override
    public int count(R r) {
        return getMutable(r).size();
    }

    @Override
    public int countKeys() {
        return indexValues.size();
    }

    @SuppressWarnings("unchecked")
    private C get(Identity<? extends I> id) {
        return backingCache.get((Identity<I>) id);
    }

    private Set<Identity<? extends I>> getMutable(R r) {
        Set<Identity<? extends I>> set = indexValues.get(r);
        return set == null ? Collections.emptySet() : set;
    }

    /**
     * Check whether the index needs to be updated based on the new and old objects.
     */
    private boolean indexUpdateRequired(@Nonnull C newObject, @Nonnull C oldObject) {
        return !function.apply(newObject).equals(function.apply(oldObject));
    }

    @Override
    protected void update(C newObject, C oldObject) throws IndexUpdateException {
        if (newObject == null) {
            remove(oldObject);
            return;
        }
        if (oldObject == null) {
            add(newObject);
            return;
        }
        if (indexUpdateRequired(newObject, oldObject)) {
            remove(oldObject);
            add(newObject);
        }

        try {
            this.afterUpdate();
        } catch (IndexUpdateException e) {
            rollbackAndThrow(newObject, oldObject, e);
        }
    }

    @Override
    protected void add(C object) {
        function.apply(object).forEach(value -> indexValues.computeIfAbsent(value, set -> new LinkedHashSet<>()).add(object.getId()));
    }

    @Override
    protected void remove(C object) {
        function.apply(object).forEach(r -> {
            Set<Identity<? extends I>> rs = indexValues.get(r);
            rs.remove(object.getId());
            if (rs.isEmpty()) {
                indexValues.remove(r);
            }
        });
    }

    private void rollbackAndThrow(C newObject, C oldObject, IndexUpdateException cause) throws IndexUpdateException {
        try {
            update(oldObject, newObject);
        } catch (IndexUpdateException rollbackFailure) {
            throw new IllegalStateException("Failed to rollback after error: " + cause.getMessage(), rollbackFailure);
        }
        throw cause;
    }
}
