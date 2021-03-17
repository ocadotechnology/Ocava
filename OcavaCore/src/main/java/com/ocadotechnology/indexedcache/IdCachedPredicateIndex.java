/*
 * Copyright © 2017-2021 Ocado (Ocava)
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

import java.util.HashSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.ocadotechnology.id.Identified;
import com.ocadotechnology.id.Identity;

/**
 * A predicate index that does not fully cache its mapping to values in the backing cache - it caches a mapping to ids
 * that can be quickly looked up in the (hashmap-based) backing cache.
 *
 * This class is best used for indexes where updates to the cached object rarely alter the outcome of the predicate
 * test (near-zero update cost), and where a common use-case involves streaming what is expected to be a small
 * percentage of the total cached objects (matching objects are always known, but require an indirect lookup).
 */
public class IdCachedPredicateIndex<C extends Identified<? extends I>, I> extends AbstractIndex<C> implements PredicateIndex<C> {
    private final IndexedImmutableObjectCache<C, I> backingCache;
    private final Predicate<? super C> predicate;

    private final HashSet<Identity<? extends I>> objectIdsMatchingPredicate = new HashSet<>();
    private final HashSet<Identity<? extends I>> objectIdsNotMatchingPredicate = new HashSet<>();

    public IdCachedPredicateIndex(IndexedImmutableObjectCache<C, I> backingCache, Predicate<? super C> predicate) {
        this.predicate = predicate;
        this.backingCache = backingCache;
    }

    @Override
    protected void update(C newObject, C oldObject) {
        if (newObject == null) {
            remove(oldObject);
            return;
        }
        if (oldObject == null) {
            add(newObject);
            return;
        }

        boolean newTest = predicate.test(newObject);
        if (newTest != predicate.test(oldObject)) {
            remove(!newTest, newObject.getId());
            add(newTest, newObject.getId());
        }
        this.afterUpdate();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<C> stream() {
        return objectIdsMatchingPredicate.stream()
                .map(id -> backingCache.get((Identity<I>) id));
    }

    @Override
    public int count() {
        return objectIdsMatchingPredicate.size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<C> streamWhereNot() {
        return objectIdsNotMatchingPredicate.stream()
                .map(id -> backingCache.get((Identity<I>) id));
    }

    @Override
    public int countWhereNot() {
        return objectIdsNotMatchingPredicate.size();
    }

    @Override
    protected void add(C object) {
        add(predicate.test(object), object.getId());
    }

    @Override
    protected void remove(C object) {
        remove(predicate.test(object), object.getId());
    }

    @Override
    public boolean isEmpty() {
        return objectIdsMatchingPredicate.isEmpty();
    }

    private void add(boolean predicateMatches, Identity<? extends I> id) {
        if (predicateMatches) {
            objectIdsMatchingPredicate.add(id);
        } else {
            objectIdsNotMatchingPredicate.add(id);
        }
    }

    private void remove(boolean predicateMatches, Identity<? extends I> id) {
        if (predicateMatches) {
            objectIdsMatchingPredicate.remove(id);
        } else {
            objectIdsNotMatchingPredicate.remove(id);
        }
    }
}
