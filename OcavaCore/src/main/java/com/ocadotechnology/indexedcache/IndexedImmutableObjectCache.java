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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.UnmodifiableIterator;
import com.ocadotechnology.id.Identified;
import com.ocadotechnology.id.Identity;

/**
 * This implementation is <em>not</em> thread-safe.
 *
 * Calling any method which modifies the cache while another invocation is modifying the cache will cause a
 * ConcurrentModificationException.
 *
 * Calling any method which queries the cache while another invocation is modifying the cache will cause a
 * ConcurrentModificationException
 *
 * Calling a method which updates the cache while another invocation is querying the cache will not be detected.  This
 * is a limitation of the implementation, designed to be as performant as possible.  It is expected that thorough test
 * coverage of the calling code should detect this case as it seems unlikely that a multi-threaded approach could ensure
 * that the overlap only ever occurred in one direction
 *
 * Calling a method which queries the cache while another invocation is querying the cache is deliberately permitted.
 */
@ParametersAreNonnullByDefault
public class IndexedImmutableObjectCache<C extends Identified<? extends I>, I> implements StateChangeListenable<C> {
    /** There are several implementations of PredicateIndex and OptionalOneToOneIndex.<br>
     *  There are optional methods to allow callers to hint about the expected workload
     *  and for the cache to (possibly) take that into account.
     */
    public enum Hints {
        optimiseForUpdate,
        optimiseForQuery,
        optimiseForInfrequentChanges
    }

    private final ObjectStore<C, I> objectStore;
    private final List<Index<C>> indexes = new ArrayList<>();

    private final List<CacheStateChangeListener<C>> stateChangeListeners = new ArrayList<>();
    private final List<AtomicStateChangeListener<C>> atomicStateChangeListeners = new ArrayList<>();

    private final AtomicReference<String> updatingThread = new AtomicReference<>(null); // The thread name of the thread currently updating the cache, null if no update is ongoing

    public static <C extends Identified<? extends I>, I> IndexedImmutableObjectCache<C, I> createHashMapBackedCache() {
        return new IndexedImmutableObjectCache<>(new HashMapObjectStore<>(128, 0.99f));
    }

    public static <C extends Identified<? extends I>, I> IndexedImmutableObjectCache<C, I> createHashMapBackedCache(int initialSize, float fillFactor) {
        return new IndexedImmutableObjectCache<>(new HashMapObjectStore<>(initialSize, fillFactor));
    }

    public IndexedImmutableObjectCache(ObjectStore<C, I> objectStore) {
        this.objectStore = objectStore;
    }

    /**
     * Updates all of the provided values in the cache. Indexes will be updated and any appropriate cache update listeners will be run.
     * Accepts addint, updating or deleting objects in the cache.
     *
     * @param updates An collection of {@link Change} objects containing the value expected to be present in the cache and the value to be added
     * @throws CacheUpdateException if any expected values do not match the objects present in the cache
     */
    public void updateAll(ImmutableCollection<Change<C>> updates) throws CacheUpdateException {
        try {
            updateStarting();
            objectStore.updateAll(updates);
            updateIndexes(updates);
        } finally {
            updateComplete();
        }
    }

    /**
     * Adds all of the provided values to the cache. Indexes will be updated and any appropriate cache update listeners will be run.
     *
     * @param newObjects the collection of values to be added to the cache
     * @throws CacheUpdateException if any objects are already present in the cache with matching ids
     */
    public void addAll(ImmutableCollection<C> newObjects) throws CacheUpdateException {
        try {
            updateStarting();
            objectStore.addAll(newObjects);
            updateIndexes(newObjects.stream().map(Change::add).collect(ImmutableList.toImmutableList()));
        } finally {
            updateComplete();
        }
    }

    /**
     * Adds the provided value to the cache. Indexes will be updated and any appropriate cache update listeners will be run.
     *
     * @param newObject the value to be added to the cache
     * @throws CacheUpdateException if there is an object stored in the cache with the provided id
     */
    public void add(C newObject) throws CacheUpdateException {
        try {
            updateStarting();
            objectStore.add(newObject);
            updateIndexes(newObject, null);
        } finally {
            updateComplete();
        }
    }

    /**
     * Updates the provided value in the cache. Indexes will be updated and any appropriate cache update listeners will be run.
     * Accepts adding, updating or deleting an object in the cache.
     *
     * @param original the value expected to be in the cache, or null if the object is new
     * @param newObject the value to be added to the cache, or null if the object is to be deleted
     * @throws CacheUpdateException if the object stored in the cache with the new id does not match the provided original
     * @throws IllegalArgumentException if both original and newObject are null
     */
    public void update(@CheckForNull C original, @CheckForNull C newObject) throws CacheUpdateException {
        try {
            updateStarting();
            if (original == newObject) {
                return;
            }
            objectStore.update(original, newObject);
            updateIndexes(newObject, original);
        } finally {
            updateComplete();
        }
    }

    /**
     * Removes all of the objects with the provided IDs from the cache.
     *
     * Removes from the cache all objects matching the identities in {@code ids}.
     * Indexes will be updated and any appropriate cache update listeners will be run.
     *
     * @param ids the collection of ids for values to be removed from the cache
     * @return a collection of the old values where any updates occurred, or else an empty collection if no updates were performed
     * @throws CacheUpdateException if any of the provided ids do not map to objects in the cache
     */
    public ImmutableCollection<C> deleteAll(ImmutableCollection<Identity<? extends I>> ids) throws CacheUpdateException {
        try {
            updateStarting();
            ImmutableCollection<C> oldObjects = objectStore.deleteAll(ids);
            updateIndexes(oldObjects.stream().map(Change::delete).collect(ImmutableList.toImmutableList()));
            return oldObjects;
        } finally {
            updateComplete();
        }
    }

    /**
     * Removes the object with the provided ID from the cache.
     *
     * Indexes will be updated and any appropriate cache update listeners will be run.
     *
     * @param id the id for value to be removed from the cache
     * @return the old value corresponding to the provided id
     * @throws CacheUpdateException if the provided id does not map to one in the cache
     */
    public C delete(Identity<? extends I> id) throws CacheUpdateException {
        try {
            updateStarting();
            C old = objectStore.delete(id);
            updateIndexes(null, old);
            return old;
        } finally {
            updateComplete();
        }
    }

    @Override
    public <T extends Index<? super C>> T registerCustomIndex(T index) {
        return addIndex(index);
    }

    @Override
    @Deprecated
    public void registerStateAddedOrRemovedListener(Consumer<? super C> consumer) {
        registerStateAddedListener(consumer::accept);
        registerStateRemovedListener(consumer::accept);
    }

    @Override
    public void registerStateAddedListener(CacheStateAddedListener<? super C> listener) {
        this.stateChangeListeners.add((oldState, updatedState) -> {
            if (oldState == null) {
                listener.stateAdded(updatedState);
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerStateChangeListener(CacheStateChangeListener<? super C> listener) {
        // The cast is safe as the objects in the cache should be immutable
        this.stateChangeListeners.add((CacheStateChangeListener<C>)listener);
    }

    /** Applies <code>mapAndFilter</code> to all updates, then calls listener (if either is non-null).<br>
     *  Allows listeners to be written that are only notified when an object's type or property changes.
     */
    public <D extends Identified<?>> void registerStateChangeListener(Function<? super C, D> mapAndFilter, CacheStateChangeListener<? super D> listener) {
        // The cast is safe as the objects in the cache should be immutable
        asFilteringListenable(mapAndFilter).registerStateChangeListener(listener);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerAtomicStateChangeListener(AtomicStateChangeListener<? super C> listener) {
        // The cast is safe as the objects in the cache should be immutable and the interface uses only immutable collections
        this.atomicStateChangeListeners.add((AtomicStateChangeListener<C>)listener);
    }

    @Override
    public void registerStateRemovedListener(CacheStateRemovedListener<? super C> listener) {
        this.stateChangeListeners.add(((oldState, updatedState) -> {
            if (updatedState == null) {
                listener.stateRemoved(oldState);
            }
        }));
    }

    @Override
    public void removeStateChangeListener(CacheStateChangeListener<? super C> listener) {
        this.stateChangeListeners.remove(listener);
    }

    /**
     * @param mapAndFilter all arguments will be non-null and present in the cache.
     * The return from the function can be null (the null will either be passed to any listeners, or
     * the whole update will be ignored (eg, an addition, removal or null-to-null state change).
     * @return a listenable that will notify any registered listeners for a subset of the cache's contents (based on <code>mapAndFilter</code>)
     */
    public <D extends Identified<?>> StateChangeListenable<D> asFilteringListenable(Function<? super C, D> mapAndFilter) {
        return new FilteringStateChangeListenable<>(this, mapAndFilter);
    }

    public PredicateIndex<C> addPredicateIndex(Predicate<? super C> predicate) {
        return addPredicateIndex(null, predicate);
    }

    public PredicateIndex<C> addPredicateIndex(@CheckForNull String name, Predicate<? super C> predicate) {
        return addPredicateIndex(name, predicate, Hints.optimiseForQuery);
    }

    /**
     * An optimiseForUpdate index has zero update overhead and streaming queries are performed by applying the predicate
     * function to every object in the parent cache.<br>
     *
     * An optimiseForQuery index caches a mapping from predicate result to object during update, so that streaming
     * queries can directly stream the pre-cached result.<br>
     *
     * An optimiseForInfrequentChanges index caches a mapping from predicate result to object id during update, so that
     * streaming queries can stream the pre-cached result and lookup the actual object in the parent cache. This makes
     * querying faster than an optimiseForUpdate index (especially when there are few matches), and updates faster
     * than an optimiseForQuery index (when the outcome of the predicate function does not change, the index cache
     * requires no changes).<br>
     */
    public PredicateIndex<C> addPredicateIndex(Predicate<? super C> predicate, Hints hint) {
        return addPredicateIndex(null, predicate, hint);
    }

    /**
     * An optimiseForUpdate index has zero update overhead and streaming queries are performed by applying the predicate
     * function to every object in the parent cache.<br>
     *
     * An optimiseForQuery index caches a mapping from predicate result to object during update, so that streaming
     * queries can directly stream the pre-cached result.<br>
     *
     * An optimiseForInfrequentChanges index caches a mapping from predicate result to object id during update, so that
     * streaming queries can stream the pre-cached result and lookup the actual object in the parent cache. This makes
     * querying faster than an optimiseForUpdate index (especially when there are few matches), and updates faster
     * than an optimiseForQuery index (when the outcome of the predicate function does not change, the index cache
     * requires no changes).<br>
     */
    public PredicateIndex<C> addPredicateIndex(@CheckForNull String name, Predicate<? super C> predicate, Hints hint) {
        switch (hint) {
            case optimiseForUpdate:
                // We could consider a lazy index (build on first query), but our currently use case doesn't justify that
                return addIndex(new UncachedPredicateIndex<>(name, this, predicate));
            case optimiseForQuery:
                return addIndex(new DefaultPredicateIndex<>(name, predicate));
            case optimiseForInfrequentChanges:
                return addIndex(new IdCachedPredicateIndex<>(name, this, predicate));
            default:
                throw new UnsupportedOperationException("Missing case:" + hint);
        }
    }

    public <R, T> PredicateValue<C, R, T> addPredicateValue(
            Predicate<? super C> predicate,
            Function<? super C, R> extract,
            BiFunction<T, R, T> leftAccumulate,
            BiFunction<T, R, T> rightDecumulate,
            T initial) {
        return addPredicateValue(null, predicate, extract, leftAccumulate, rightDecumulate, initial);
    }

    public <R, T> PredicateValue<C, R, T> addPredicateValue(
            @CheckForNull String name,
            Predicate<? super C> predicate,
            Function<? super C, R> extract,
            BiFunction<T, R, T> leftAccumulate,
            BiFunction<T, R, T> rightDecumulate,
            T initial) {
        PredicateValue<C, R, T> value = new PredicateValue<>(name, predicate, extract, leftAccumulate, rightDecumulate, initial);
        return addIndex(value);
    }

    public PredicateCountValue<C> addPredicateCount(Predicate<? super C> predicate) {
        return addPredicateCount(null, predicate);
    }

    public PredicateCountValue<C> addPredicateCount(@CheckForNull String name, Predicate<? super C> predicate) {
        PredicateCountValue<C> counter = new PredicateCountValue<>(name, predicate);
        return addIndex(counter);
    }

    public <R> ManyToManyIndex<R, C> addManyToManyIndex(Function<? super C, Set<R>> function) {
        return addManyToManyIndex(null, function);
    }

    public <R> ManyToManyIndex<R, C> addManyToManyIndex(@CheckForNull String name, Function<? super C, Set<R>> function) {
        ManyToManyIndex<R, C> index = new ManyToManyIndex<>(name, function);
        return addIndex(index);
    }

    /**
     * @param function key extraction function
     * @param comparator A comparator on a set of elements C which is consistent with equals().
     *        More formally, a total-order comparator on a set of elements C where
     *        compare(c1, c2) == 0 implies that Objects.equals(c1, c2) == true.
     *        This requirement is strictly enforced. Violating it will produce an IllegalStateException
     *        and leave the cache in an inconsistent state.
     */
    public <R> OptionalSortedManyToManyIndex<R, C> addOptionalSortedManyToManyIndex(
            Function<? super C, Optional<Set<R>>> function,
            Comparator<? super C> comparator) {
        return addOptionalSortedManyToManyIndex(null, function, comparator);
    }

    /**
     * @param name optional String parameter - the name of the index.
     * @param function key extraction function
     * @param comparator A comparator on a set of elements C which is consistent with equals().
     *        More formally, a total-order comparator on a set of elements C where
     *        compare(c1, c2) == 0 implies that Objects.equals(c1, c2) == true.
     *        This requirement is strictly enforced. Violating it will produce an IllegalStateException
     *        and leave the cache in an inconsistent state.
     */
    public <R> OptionalSortedManyToManyIndex<R, C> addOptionalSortedManyToManyIndex(
            @CheckForNull String name,
            Function<? super C, Optional<Set<R>>> function,
            Comparator<? super C> comparator) {
        OptionalSortedManyToManyIndex<R, C> index = new OptionalSortedManyToManyIndex<>(name, function, comparator);
        return addIndex(index);
    }

    public <R> OneToOneIndex<R, C> addOneToOneIndex(Function<? super C, R> function) {
        return addOneToOneIndex(null, function);
    }

    public <R> OneToOneIndex<R, C> addOneToOneIndex(@CheckForNull String name, Function<? super C, R> function) {
        return addOneToOneIndex(name, function, Hints.optimiseForQuery);
    }

    public <R> OneToOneIndex<R, C> addOneToOneIndex(Function<? super C, R> function, Hints hint) {
        return addOneToOneIndex(null, function, hint);
    }

    public <R> OneToOneIndex<R, C> addOneToOneIndex(@CheckForNull String name, Function<? super C, R> function, Hints hint) {
        return addIndex(new OneToOneIndex<>(name, function, hint));
    }

    public <R> OneToManyIndex<R, C> addOneToManyIndex(Function<? super C, R> function) {
        return addOneToManyIndex(null, function);
    }

    public <R> OneToManyIndex<R, C> addOneToManyIndex(@CheckForNull String name, Function<? super C, R> function) {
        return addIndex(OneToManyIndex.create(name, function));
    }

    public <R> ManyToOneIndex<R, C> addManyToOneIndex(Function<? super C, Collection<R>> function) {
        return addManyToOneIndex(null, function);
    }

    public <R> ManyToOneIndex<R, C> addManyToOneIndex(@CheckForNull String name, Function<? super C, Collection<R>> function) {
        ManyToOneIndex<R, C> index = new ManyToOneIndex<>(name, function);
        return addIndex(index);
    }

    public <R> OptionalOneToManyIndex<R, C> addOptionalOneToManyIndex(Function<? super C, Optional<R>> function) {
        return addOptionalOneToManyIndex(null, function);
    }

    public <R> OptionalOneToManyIndex<R, C> addOptionalOneToManyIndex(@CheckForNull String name, Function<? super C, Optional<R>> function) {
        OptionalOneToManyIndex<R, C> index = new OptionalOneToManyIndex<>(name, function);
        return addIndex(index);
    }

    public <R> OptionalOneToOneIndex<R, C> addOptionalOneToOneIndex(Function<? super C, Optional<R>> function) {
        return addOptionalOneToOneIndex(null, function);
    }

    public <R> OptionalOneToOneIndex<R, C> addOptionalOneToOneIndex(@CheckForNull String name, Function<? super C, Optional<R>> function) {
        return addOptionalOneToOneIndex(name, function, Hints.optimiseForQuery);
    }

    /** An Index optimised for update is a little faster.  Query times are equal.<br>
     *  The update-optimised implementation does not separately validate uniqueness of value
     *  (which is what makes a faster update possible).
     */
    public <R> OptionalOneToOneIndex<R, C> addOptionalOneToOneIndex(Function<? super C, Optional<R>> function, Hints hint) {
        return addOptionalOneToOneIndex(null, function, hint);
    }

    /** An Index optimised for update is a little faster.  Query times are equal.<br>
     *  The update-optimised implementation does not separately validate uniqueness of value
     *  (which is what makes a faster update possible).
     */
    public <R> OptionalOneToOneIndex<R, C> addOptionalOneToOneIndex(@CheckForNull String name, Function<? super C, Optional<R>> function, Hints hint) {
        return addIndex(OptionalOneToOneIndexFactory.newOptionalOneToOneIndex(name, function, hint));
    }

    /**
     * @param function key extraction function.
     * @param comparator A comparator on a set of elements C which is consistent with equals().
     *        More formally, a total-order comparator on a set of elements C where
     *        compare(c1, c2) == 0 implies that Objects.equals(c1, c2) == true.
     *        This requirement is strictly enforced. Violating it will produce an IllegalStateException
     *        and leave the cache in an inconsistent state.
     */
    public <R> SortedOneToManyIndex<R, C> addSortedOneToManyIndex(
            Function<? super C, R> function,
            Comparator<? super C> comparator) {
        return addSortedOneToManyIndex(null, function, comparator);
    }

    /**
     * @param name optional String parameter - the name of the index.
     * @param function key extraction function.
     * @param comparator A comparator on a set of elements C which is consistent with equals().
     *        More formally, a total-order comparator on a set of elements C where
     *        compare(c1, c2) == 0 implies that Objects.equals(c1, c2) == true.
     *        This requirement is strictly enforced. Violating it will produce an IllegalStateException
     *        and leave the cache in an inconsistent state.
     */
    public <R> SortedOneToManyIndex<R, C> addSortedOneToManyIndex(
            @CheckForNull String name,
            Function<? super C, R> function,
            Comparator<? super C> comparator) {
        SortedOneToManyIndex<R, C> index = new SortedOneToManyIndex<>(name, function, comparator);
        return addIndex(index);
    }

    /**
     * @param function key extraction function.
     * @param comparatorGenerator generates a comparator for a given key
     *        A comparator on a set of elements C which is consistent with equals().
     *        More formally, a total-order comparator on a set of elements C where
     *        compare(c1, c2) == 0 implies that Objects.equals(c1, c2) == true.
     *        This requirement is strictly enforced. Violating it will produce an IllegalStateException
     *        and leave the cache in an inconsistent state.
     */
    public <R> SeparatelySortedOneToManyIndex<R, C> addSeparatelySortedOneToManyIndex(
            Function<? super C, R> function,
            Function<R, Comparator<? super C>> comparatorGenerator) {
        return addSeparatelySortedOneToManyIndex(null, function, comparatorGenerator);
    }

    /**
     * @param name optional String parameter - the name of the index.
     * @param function key extraction function.
     * @param comparatorGenerator generates a comparator for a given key
     *        A comparator on a set of elements C which is consistent with equals().
     *        More formally, a total-order comparator on a set of elements C where
     *        compare(c1, c2) == 0 implies that Objects.equals(c1, c2) == true.
     *        This requirement is strictly enforced. Violating it will produce an IllegalStateException
     *        and leave the cache in an inconsistent state.
     */
    public <R> SeparatelySortedOneToManyIndex<R, C> addSeparatelySortedOneToManyIndex(
            @CheckForNull String name,
            Function<? super C, R> function,
            Function<R, ? extends Comparator<? super C>> comparatorGenerator) {
        SeparatelySortedOneToManyIndex<R, C> index = new SeparatelySortedOneToManyIndex<>(name, function, comparatorGenerator);
        return addIndex(index);
    }

    /**
     * @param function key extraction function
     * @param comparator A comparator on a set of elements C which is consistent with equals().
     *        More formally, a total-order comparator on a set of elements C where
     *        compare(c1, c2) == 0 implies that Objects.equals(c1, c2) == true.
     *        This requirement is strictly enforced. Violating it will produce an IllegalStateException
     *        and leave the cache in an inconsistent state.
     */
    public <R> OptionalSortedOneToManyIndex<R, C> addOptionalSortedOneToManyIndex(
            Function<? super C, Optional<R>> function,
            Comparator<? super C> comparator) {
        return addOptionalSortedOneToManyIndex(null, function, comparator);
    }

    /**
     * @param name optional String parameter - the name of the index.
     * @param function key extraction function
     * @param comparator A comparator on a set of elements C which is consistent with equals().
     *        More formally, a total-order comparator on a set of elements C where
     *        compare(c1, c2) == 0 implies that Objects.equals(c1, c2) == true.
     *        This requirement is strictly enforced. Violating it will produce an IllegalStateException
     *        and leave the cache in an inconsistent state.
     */
    public <R> OptionalSortedOneToManyIndex<R, C> addOptionalSortedOneToManyIndex(
            @CheckForNull String name,
            Function<? super C, Optional<R>> function,
            Comparator<? super C> comparator) {
        OptionalSortedOneToManyIndex<R, C> index = new OptionalSortedOneToManyIndex<>(name, function, comparator);
        return addIndex(index);
    }

    public <G, T> CachedGroupBy<C, G, T> cacheGroupBy(Function<? super C, G> groupByExtractor, Collector<? super C, ?, T> collector) {
        return cacheGroupBy(null, groupByExtractor, collector);
    }

    public <G, T> CachedGroupBy<C, G, T> cacheGroupBy(@CheckForNull String name, Function<? super C, G> groupByExtractor, Collector<? super C, ?, T> collector) {
        CachedGroupBy<C, G, T> cachedGroupByAggregation = new CachedGroupBy<>(name, groupByExtractor, collector);
        return addIndex(cachedGroupByAggregation);
    }

    public <R> MappedPredicateIndex<C, R> addMappedPredicateIndex(Predicate<? super C> predicate, Function<? super C, R> mappingFunction) {
        return addMappedPredicateIndex(null, predicate, mappingFunction);
    }

    public <R> MappedPredicateIndex<C, R> addMappedPredicateIndex(@CheckForNull String name, Predicate<? super C> predicate, Function<? super C, R> mappingFunction) {
        MappedPredicateIndex<C, R> index = new MappedPredicateIndex<>(name, predicate, mappingFunction);
        return addIndex(index);
    }

    /**
     * @param comparator - A comparator on a set of elements C which is consistent with equals().
     *        More formally, a total-order comparator on a set of elements C where
     *        compare(c1, c2) == 0 implies that Objects.equals(c1, c2) == true.
     *        This requirement is strictly enforced. Violating it will produce an IllegalStateException
     *        and leave the cache in an inconsistent state.
     */
    public CachedSort<C> addCacheSort(Comparator<? super C> comparator) {
        return addCacheSort(null, comparator);
    }

    /**
     * @param name optional String parameter - the name of the index.
     * @param comparator - A comparator on a set of elements C which is consistent with equals().
     *        More formally, a total-order comparator on a set of elements C where
     *        compare(c1, c2) == 0 implies that Objects.equals(c1, c2) == true.
     *        This requirement is strictly enforced. Violating it will produce an IllegalStateException
     *        and leave the cache in an inconsistent state.
     */
    public CachedSort<C> addCacheSort(@CheckForNull String name, Comparator<? super C> comparator) {
        CachedSort<C> cacheSort = new CachedSort<>(name, comparator);
        return addIndex(cacheSort);
    }

    public C get(@CheckForNull Identity<I> id) {
        return objectStore.get(id);
    }

    public boolean containsId(@CheckForNull Identity<I> id) {
        return objectStore.containsId(id);
    }

    public int size() {
        return objectStore.size();
    }

    public boolean isEmpty() {
        return objectStore.size() == 0;
    }

    @Override
    public Stream<C> stream() {
        return objectStore.stream();
    }

    @Override
    public UnmodifiableIterator<C> iterator() {
        return objectStore.iterator();
    }

    @Override
    public void forEach(Consumer<C> action) {
        objectStore.forEach(action);
    }

    @SuppressWarnings("unchecked")
    private <T extends Index<? super C>> T addIndex(T index) {
        try {
            updateStarting();
            // The cast is safe as the objects in the cache should be immutable and the interface uses only immutable collections
            Index<C> castedIndex = (Index<C>) index;
            indexes.add(castedIndex);

            //Do not collect to an ImmutableSet - Guava's default collector does not infer the stream size
            //which results in a lot of collisions and array extensions.
            ImmutableList<Change<C>> allStates = objectStore.stream().map(Change::add).collect(ImmutableList.toImmutableList());
            try {
                castedIndex.updateAll(allStates);
            } catch (IndexUpdateException e) {
                throw new IllegalStateException("Failed to add new index", e);
            }
            return index;
        } finally {
            updateComplete();
        }
    }

    private void updateIndexes(@CheckForNull C newValue, @CheckForNull C oldValue) {
        for (int i = 0; i < indexes.size(); ++i) {
            try {
                indexes.get(i).update(newValue, oldValue);
            } catch (IndexUpdateException e) {
                rollbackSingleUpdate(newValue, oldValue, i, e);
                throw new CacheUpdateException("Failed to update indices", e);
            }
        }
        updateStateChangeListeners(oldValue, newValue);
        updateAtomicStateChangeListeners(oldValue, newValue);
    }

    private void updateAtomicStateChangeListeners(@CheckForNull C oldValue, @CheckForNull C newValue) {
        if (atomicStateChangeListeners.isEmpty()) {
            return;
        }

        ImmutableList<Change<C>> changes = ImmutableList.of(Change.change(oldValue, newValue));
        atomicStateChangeListeners.forEach(l -> l.stateChanged(changes));
    }

    private void updateIndexes(ImmutableCollection<Change<C>> changes) {
        for (int i = 0; i < indexes.size(); ++i) {
            try {
                indexes.get(i).updateAll(changes);
            } catch (IndexUpdateException e) {
                rollbackBatchUpdate(changes, i, e);
                throw new CacheUpdateException("Failed to update indices", e);
            }
        }
        changes.forEach(update -> updateStateChangeListeners(update.originalObject, update.newObject));
        if (!atomicStateChangeListeners.isEmpty()) {
            atomicStateChangeListeners.forEach(l -> l.stateChanged(changes));
        }
    }

    private void updateStateChangeListeners(@CheckForNull C oldState, @CheckForNull C newState) {
        stateChangeListeners.forEach(s -> s.stateChanged(oldState, newState));
    }

    private void rollbackSingleUpdate(@CheckForNull C newValue, @CheckForNull C oldValue, int failedIndexNumber, IndexUpdateException cause) {
        try {
            for (int i = 0; i < failedIndexNumber; ++i) {
                indexes.get(i).update(oldValue, newValue);
            }
            objectStore.update(newValue, oldValue);
        } catch (IndexUpdateException | CacheUpdateException e) {
            throw new IllegalStateException("Failed to rollback changes after index failure: " + cause.getMessage(), e);
        }
    }

    private void rollbackBatchUpdate(ImmutableCollection<Change<C>> changes, int failedIndexNumber, IndexUpdateException cause) {
        ImmutableList<Change<C>> reverseChanges = changes.stream()
                .map(Change::inverse)
                .collect(ImmutableList.toImmutableList());

        try {
            for (int i = 0; i < failedIndexNumber; ++i) {
                indexes.get(i).updateAll(reverseChanges);
            }
            objectStore.updateAll(reverseChanges);
        } catch (IndexUpdateException | CacheUpdateException e) {
            throw new IllegalStateException("Failed to rollback changes after index failure: " + cause.getMessage(), e);
        }
    }

    public void clear() {
        try {
            updateStarting();
            ImmutableCollection<Change<C>> clearedObjects = objectStore.stream().map(Change::delete).collect(ImmutableList.toImmutableList());
            objectStore.clear();
            updateIndexes(clearedObjects);
        } finally {
            updateComplete();
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("objects", objectStore.size())
                .toString();
    }

    public ImmutableMap<Identity<? extends I>, C> snapshotObjects() {
        return objectStore.snapshot();
    }

    private void updateStarting() {
        if (!updatingThread.compareAndSet(null, Thread.currentThread().getName())) {
            failUpdate();
        }
    }

    /**
     * Method separated out to make it easier for the JVM to inline the updateStarting method for performance
     */
    private void failUpdate() {
        throw new ConcurrentModificationException(
                String.format("Attempting to update cache while another update is ongoing. currentThread=[%s] otherThread=[%s]",
                        Thread.currentThread().getName(),
                        updatingThread));
    }

    private void updateComplete() {
        updatingThread.compareAndSet(Thread.currentThread().getName(), null);
    }

}
