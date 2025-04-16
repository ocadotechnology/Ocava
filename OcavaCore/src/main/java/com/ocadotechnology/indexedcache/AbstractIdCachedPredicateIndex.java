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

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import com.ocadotechnology.id.Identified;
import com.ocadotechnology.id.Identity;

/**
 * A predicate index that does not fully cache its mapping to values in the backing cache - it caches a mapping to ids
 * that can be quickly looked up in the (hashmap-based) backing cache.
 * <br>
 * This class is best used for indexes where updates to the cached object rarely alter the outcome of the predicate
 * test (near-zero update cost), and where a common use-case involves streaming what is expected to be a small
 * percentage of the total cached objects (matching objects are always known, but require an indirect lookup).
 */
abstract class AbstractIdCachedPredicateIndex<C extends Identified<? extends I>, I> extends AbstractIndex<C> implements PredicateIndex<C> {
    private final IndexedImmutableObjectCache<C, I> backingCache;
    private final Predicate<? super C> predicate;

    /**
     * @param name optional String parameter - the name of the index.
     * @param backingCache the cache that this index is indexing.
     * @param predicate the predicate used to filter values in the cache.
     */
    protected AbstractIdCachedPredicateIndex(@CheckForNull String name, IndexedImmutableObjectCache<C, I> backingCache, Predicate<? super C> predicate) {
        super(name);
        this.predicate = predicate;
        this.backingCache = backingCache;
    }

    /**
     * @return the mutable set of ids that match the predicate
     */
    protected abstract Set<Identity<? extends I>> getIdsMatchingPredicate();

    /**
     * @return the mutable set of ids that do not match the predicate
     */
    protected abstract Set<Identity<? extends I>> getIdsNotMatchingPredicate();

    /**
     * Check whether the index needs to be updated based on the new and old objects.
     * <br>
     * Overriding methods should only add updates to the base case, not remove them. For example,
     * {@code return super.indexUpdateRequired(newObject, oldObject) || newObject.getFoo() != oldObject.getFoo();} is fine,
     * but {@code return super.indexUpdateRequired(newObject, oldObject) && newObject.getFoo() != oldObject.getFoo();} may lead to the cache
     * becoming inconsistent.
     **/
    @OverridingMethodsMustInvokeSuper
    protected boolean indexUpdateRequired(@Nonnull C newObject, @Nonnull C oldObject) {
        return predicate.test(newObject) != predicate.test(oldObject);
    }

    /**
     * Remove an object from this index if it is present.<br>
     * This method is exposed to inheritors in case they need access to the actual object, since it will no longer be in the backing cache.
     * @param predicateMatches whether the removed object matches the predicate
     * @param toRemove the object to remove from the index
     */
    protected void remove(boolean predicateMatches, C toRemove) {
        if (predicateMatches) {
            getIdsMatchingPredicate().remove(toRemove.getId());
        } else {
            getIdsNotMatchingPredicate().remove(toRemove.getId());
        }
    }

    @Override
    public Stream<C> stream() {
        return getIdsMatchingPredicate().stream()
                .map(this::get);
    }

    @Override
    public int count() {
        return getIdsMatchingPredicate().size();
    }

    @Override
    public Stream<C> streamWhereNot() {
        return getIdsNotMatchingPredicate().stream()
                .map(this::get);
    }

    @Override
    public int countWhereNot() {
        return getIdsNotMatchingPredicate().size();
    }

    @Override
    protected final void update(C newObject, C oldObject) throws IndexUpdateException {
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
    protected final void add(C object) {
        add(predicate.test(object), object);
    }

    @Override
    protected final void remove(C object) {
        remove(predicate.test(object), object);
    }

    @Override
    public boolean isEmpty() {
        return getIdsMatchingPredicate().isEmpty();
    }

    @SuppressWarnings("unchecked")
    protected final C get(Identity<? extends I> id) {
        return backingCache.get((Identity<I>) id);
    }

    private void add(boolean predicateMatches, C object) {
        if (predicateMatches) {
            getIdsMatchingPredicate().add(object.getId());
        } else {
            getIdsNotMatchingPredicate().add(object.getId());
        }
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
