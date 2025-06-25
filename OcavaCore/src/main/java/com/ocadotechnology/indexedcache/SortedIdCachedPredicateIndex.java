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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import com.google.common.collect.UnmodifiableIterator;
import com.ocadotechnology.id.Identified;
import com.ocadotechnology.id.Identity;

/**
 * An implementation of {@link AbstractIdCachedPredicateIndex} that also sorts the elements that satisfy the predicate.
 */
public class SortedIdCachedPredicateIndex<C extends Identified<? extends I>, I> extends AbstractIdCachedPredicateIndex<C, I> implements SortedPredicateIndex<C> {

    /**
     * We require a NavigableSet of object ids, which needs to be sorted according to the objects themselves.
     * <br>
     * This creates difficulties when updating the index because the object in the backing cache may have been removed (or updated),
     * but we need to use the object to find the corresponding id in the Set (since {@link TreeSet} uses the comparator to lookup elements).
     * <br>
     * To solve this, the comparator in the NavigableSet instead looks up objects in a {@link BackingCacheWithLocalTemporaryChanges},
     * that we can use to temporarily add objects to the cache for the sake of a lookup.
     * */
    private final BackingCacheWithLocalTemporaryChanges<C, I> backingCacheWithLocalTemporaryChanges;
    private final NavigableSet<Identity<? extends I>> objectIdsMatchingPredicate;

    private final Set<Identity<? extends I>> objectIdsNotMatchingPredicate;
    private final Comparator<? super C> comparator;

    /**
     * @param name optional String parameter - the name of the index.
     * @param backingCache the cache that this index is indexing.
     * @param predicate the predicate used to filter values in the cache.
     * @param comparator the comparator used to sort the elements that satisfy the predicate.
     */
    public SortedIdCachedPredicateIndex(
            String name,
            IndexedImmutableObjectCache<C, I> backingCache,
            Predicate<? super C> predicate,
            Comparator<? super C> comparator) {
        super(name, backingCache, predicate);
        this.backingCacheWithLocalTemporaryChanges = new BackingCacheWithLocalTemporaryChanges<>(backingCache);
        this.objectIdsMatchingPredicate = new TreeSet<>(wrapComparator(comparator, backingCacheWithLocalTemporaryChanges));
        this.objectIdsNotMatchingPredicate = new LinkedHashSet<>();
        this.comparator = comparator;
    }

    @Override
    protected final NavigableSet<Identity<? extends I>> getIdsMatchingPredicate() {
        return objectIdsMatchingPredicate;
    }

    @Override
    protected final Set<Identity<? extends I>> getIdsNotMatchingPredicate() {
        return objectIdsNotMatchingPredicate;
    }

    @Override
    protected boolean indexUpdateRequired(@Nonnull C newObject, @Nonnull C oldObject) {
        return super.indexUpdateRequired(newObject, oldObject)
                || comparator.compare(newObject, oldObject) != 0;
    }

    @Override
    protected void remove(boolean predicateMatches, C toRemove) {
        if (predicateMatches) {
            // We need to be able to compare based on the removed object, which will not be in the backing cache. So we temporarily add it.
            backingCacheWithLocalTemporaryChanges.doWithTemporaryObjectInCache(toRemove, () -> objectIdsMatchingPredicate.remove(toRemove.getId()));
        } else {
            objectIdsNotMatchingPredicate.remove(toRemove.getId());
        }
    }

    @Override
    protected void updateAll(Iterable<Change<C>> changes) {
        Set<C> removed = new LinkedHashSet<>();
        for (Change<C> change : changes) {
            if (change.originalObject != null) {
                removed.add(change.originalObject);
            }
        }

        backingCacheWithLocalTemporaryChanges.doWithTemporaryObjectsInCache(
                removed,
                () -> {
                    for (Change<C> c : changes) {
                        if (c.originalObject != null) {
                            remove(c.originalObject);
                        }
                    }
                });

        for (Change<C> c : changes) {
            if (c.newObject != null) {
                add(c.newObject);
            }
        }
    }

    @Override
    public Optional<C> getFirst() {
        if (objectIdsMatchingPredicate.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(get(objectIdsMatchingPredicate.first()));
    }

    @Override
    public Optional<C> getLast() {
        if (objectIdsMatchingPredicate.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(get(objectIdsMatchingPredicate.last()));
    }

    @Override
    public Optional<C> after(C previous) {
        Identity<? extends I> previousId = previous.getId();
        Identity<? extends I> nextId = backingCacheWithLocalTemporaryChanges.getWithTemporaryObjectInCache(previous, () -> objectIdsMatchingPredicate.higher(previousId));
        return nextId == null ? Optional.empty() : Optional.of(get(nextId));
    }

    @Override
    public UnmodifiableIterator<C> iterator() {
        return new UnmodifiableIterator<>() {
            private final Iterator<Identity<? extends I>> idIterator = objectIdsMatchingPredicate.iterator();

            @Override
            public boolean hasNext() {
                return idIterator.hasNext();
            }

            @Override
            public C next() {
                return get(idIterator.next());
            }
        };
    }

    /**
     * Applies the given consumer to each index value matching the predicate.
     */
    @Override
    public void forEach(Consumer<C> consumer) {
        for (Identity<? extends I> id : objectIdsMatchingPredicate) {
            consumer.accept(get(id));
        }
    }

    private Comparator<? super Identity<? extends I>> wrapComparator(Comparator<? super C> comparator, BackingCacheWithLocalTemporaryChanges<C, I> backingCache) {
        return (id1, id2) -> comparator.compare(backingCache.get(id1), backingCache.get(id2));
    }

    /**
     *  A wrapper around the backing cache that allows for temporary changes to be made to the cache.
     *  <br>
     *  Note that methods such as {@link BackingCacheWithLocalTemporaryChanges#add} have purposefully been made private to enforce that changes are always cleared after use.
     */
    private static class BackingCacheWithLocalTemporaryChanges<C extends Identified<? extends I>, I> {
        private final IndexedImmutableObjectCache<C, I> backingCache;
        private final Map<Identity<? extends I>, C> addedObjects = new HashMap<>();

        /**
         * We may perform multiple operations on the cache at once,
         * for instance if one action adds an object, and calls another action which adds further objects.
         * When the sub-action is complete, we don't want to clear the changes made by the first action.
         * This flag prevents that.
         */
        private boolean transactionInProgress = false;

        public BackingCacheWithLocalTemporaryChanges(IndexedImmutableObjectCache<C, I> backingCache) {
            this.backingCache = backingCache;
        }

        public void doWithTemporaryObjectInCache(C tempObject, Runnable action) {
            add(tempObject);
            doAction(action);
            clearChanges();
        }

        public void doWithTemporaryObjectsInCache(Iterable<C> tempObjects, Runnable action) {
            addAll(tempObjects);
            doAction(action);
            clearChanges();
        }

        public <T> T getWithTemporaryObjectInCache(C tempObject, Supplier<T> action) {
            add(tempObject);
            boolean wasTransactionInProgress = transactionInProgress;
            T result = action.get();
            transactionInProgress = wasTransactionInProgress;
            clearChanges();
            return result;
        }

        @SuppressWarnings("unchecked")
        public C get(Identity<? extends I> id) {
            if (addedObjects.containsKey(id)) {
                return addedObjects.get(id);
            }

            return backingCache.get((Identity<I>) id);
        }

        private void doAction(Runnable action) {
            boolean wasTransactionInProgress = transactionInProgress;
            transactionInProgress = true;
            action.run();
            transactionInProgress = wasTransactionInProgress;
        }

        private void add(C object) {
            addedObjects.put(object.getId(), object);
        }

        private void addAll(Iterable<C> objects) {
            objects.forEach(this::add);
        }

        private void clearChanges() {
            if (!transactionInProgress) {
                addedObjects.clear();
            }
        }
    }
}
