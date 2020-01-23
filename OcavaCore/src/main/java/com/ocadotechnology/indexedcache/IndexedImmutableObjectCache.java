/*
 * Copyright © 2017-2020 Ocado (Ocava)
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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.UnmodifiableIterator;
import com.ocadotechnology.id.Identified;
import com.ocadotechnology.id.Identity;

/** <p>This implementation is <em>not</em> thread-safe.</p>
 *  <p>Calling any method which modifies the cache from within a listener is <em>not</em> permitted and will throw
 *  a ConcurrentModificationException.
 *  This is to prevent out-of-order updates, where some listeners may be notified of the second cache
 *  update before notification of the first.</p>
 */
public class IndexedImmutableObjectCache<C extends Identified<? extends I>, I> implements StateChangeListenable<C> {
    private final ObjectStore<C, I> objectStore;
    private final List<Index<C>> indexes = new ArrayList<>();

    private final List<CacheStateChangeListener<C>> stateChangeListeners = new ArrayList<>();
    private final List<AtomicStateChangeListener<C>> atomicStateChangeListeners = new ArrayList<>();

    private transient boolean updateIndexesInProgressReEntryLatch;  // true if we're already updating the indexes (notifying stateChangeListeners)

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
        objectStore.updateAll(updates);
        updateIndexes(updates);
    }

    /**
     * Adds all of the provided values to the cache. Indexes will be updated and any appropriate cache update listeners will be run.
     *
     * @param newObjects the collection of values to be added to the cache
     * @throws CacheUpdateException if any objects are already present in the cache with matching ids
     */
    public void addAll(ImmutableCollection<C> newObjects) throws CacheUpdateException {
        objectStore.addAll(newObjects);
        updateIndexes(newObjects.stream().map(Change::add).collect(ImmutableList.toImmutableList()));
    }

    /**
     * Adds the provided value to the cache. Indexes will be updated and any appropriate cache update listeners will be run.
     *
     * @param newObject the value to be added to the cache
     * @throws CacheUpdateException if there is an object stored in the cache with the provided id
     */
    public void add(C newObject) throws CacheUpdateException {
        objectStore.add(newObject);
        updateIndexes(newObject, null);
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
    public void update(@Nullable C original, @Nullable C newObject) throws CacheUpdateException {
        objectStore.update(original, newObject);
        updateIndexes(newObject, original);
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
        ImmutableCollection<C> oldObjects = objectStore.deleteAll(ids);
        updateIndexes(oldObjects.stream().map(Change::delete).collect(ImmutableList.toImmutableList()));
        return oldObjects;
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
        C old = objectStore.delete(id);
        updateIndexes(null, old);
        return old;
    }

    @Override
    public void registerCustomIndex(Index<? super C> index) {
        addIndex(index);
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

    public PredicateIndex<C> addPredicateIndex(Predicate<? super C> predicate) {
        PredicateIndex<C> index = new PredicateIndex<>(predicate);
        addIndex(index);
        return index;
    }

    public <R, T> PredicateValue<C, R, T> addPredicateValue(
            Predicate<? super C> predicate,
            Function<? super C, R> extract,
            BiFunction<T, R, T> leftAccumulate,
            BiFunction<T, R, T> rightDecumulate,
            T initial) {
        PredicateValue<C, R, T> value = new PredicateValue<>(predicate, extract, leftAccumulate, rightDecumulate, initial);
        addIndex(value);
        return value;
    }

    public PredicateCountValue<C> addPredicateCount(Predicate<? super C> predicate) {
        PredicateCountValue<C> counter = new PredicateCountValue<>(predicate);
        addIndex(counter);
        return counter;
    }

    public <R> ManyToManyIndex<R, C> addManyToManyIndex(Function<? super C, Set<R>> function) {
        ManyToManyIndex<R, C> index = new ManyToManyIndex<>(function);
        addIndex(index);
        return index;
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
        OptionalSortedManyToManyIndex<R, C> index = new OptionalSortedManyToManyIndex<>(function, comparator);
        addIndex(index);
        return index;
    }

    public <R> OneToOneIndex<R, C> addOneToOneIndex(Function<? super C, R> function) {
        OneToOneIndex<R, C> index = new OneToOneIndex<>(function);
        addIndex(index);
        return index;
    }

    public <R> OneToManyIndex<R, C> addOneToManyIndex(Function<? super C, R> function) {
        OneToManyIndex<R, C> index = new OneToManyIndex<>(function);
        addIndex(index);
        return index;
    }

    public <R> ManyToOneIndex<R, C> addManyToOneIndex(Function<? super C, Collection<R>> function) {
        ManyToOneIndex<R, C> index = new ManyToOneIndex<>(function);
        addIndex(index);
        return index;
    }

    public <R> OptionalOneToManyIndex<R, C> addOptionalOneToManyIndex(Function<? super C, Optional<R>> function) {
        OptionalOneToManyIndex<R, C> index = new OptionalOneToManyIndex<>(function);
        addIndex(index);
        return index;
    }

    public <R> OptionalOneToOneIndex<R, C> addOptionalOneToOneIndex(Function<? super C, Optional<R>> function) {
        OptionalOneToOneIndex<R, C> index = new OptionalOneToOneIndex<>(function);
        addIndex(index);
        return index;
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
        SortedOneToManyIndex<R, C> index = new SortedOneToManyIndex<>(function, comparator);
        addIndex(index);
        return index;
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
            Function<R, Comparator<C>> comparatorGenerator) {
        SeparatelySortedOneToManyIndex<R, C> index = new SeparatelySortedOneToManyIndex<>(function, comparatorGenerator);
        addIndex(index);
        return index;
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
        OptionalSortedOneToManyIndex<R, C> index = new OptionalSortedOneToManyIndex<>(function, comparator);
        addIndex(index);
        return index;
    }

    public <G, T> CachedGroupBy<C, G, T> cacheGroupBy(Function<? super C, G> groupByExtractor, Collector<? super C, ?, T> collector) {
        CachedGroupBy<C, G, T> cachedGroupByAggregation = new CachedGroupBy<>(groupByExtractor, collector);
        addIndex(cachedGroupByAggregation);
        return cachedGroupByAggregation;
    }

    public <R> MappedPredicateIndex<C, R> addMappedPredicateIndex(Predicate<? super C> predicate, Function<? super C, R> mappingFunction) {
        MappedPredicateIndex<C, R> index = new MappedPredicateIndex<>(predicate, mappingFunction);
        addIndex(index);
        return index;
    }

    /**
     * @param comparator - A comparator on a set of elements C which is consistent with equals().
     *        More formally, a total-order comparator on a set of elements C where
     *        compare(c1, c2) == 0 implies that Objects.equals(c1, c2) == true.
     *        This requirement is strictly enforced. Violating it will produce an IllegalStateException
     *        and leave the cache in an inconsistent state.
     */
    public CachedSort<C> addCacheSort(Comparator<? super C> comparator) {
        CachedSort<C> cacheSort = new CachedSort<>(comparator);
        addIndex(cacheSort);
        return cacheSort;
    }

    public C get(Identity<I> id) {
        return objectStore.get(id);
    }

    public boolean containsId(Identity<I> id) {
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
    private void addIndex(Index<? super C> index) {
        // The cast is safe as the objects in the cache should be immutable and the interface uses only immutable collections
        Index<C> castedIndex = (Index<C>)index;
        indexes.add(castedIndex);
        castedIndex.updateAll(objectStore.stream().map(Change::add).collect(ImmutableSet.toImmutableSet()));
    }

    private void updateIndexes(C newValue, C oldValue) {
        if (updateIndexesInProgressReEntryLatch) {
            throw new ConcurrentModificationException(this.toString());
        }
        try {
            updateIndexesInProgressReEntryLatch = true;

            indexes.forEach(i -> i.update(newValue, oldValue));
            updateStateChangeListeners(oldValue, newValue);
            if (!atomicStateChangeListeners.isEmpty()) {
                ImmutableList<Change<C>> changes = ImmutableList.of(Change.change(oldValue, newValue));
                atomicStateChangeListeners.forEach(l -> l.stateChanged(changes));
            }
        }
        finally {
            updateIndexesInProgressReEntryLatch = false;
        }
    }

    private void updateIndexes(ImmutableCollection<Change<C>> changes) {
        if (updateIndexesInProgressReEntryLatch) {
            throw new ConcurrentModificationException(this.toString());
        }
        try {
            updateIndexesInProgressReEntryLatch = true;

            indexes.forEach(i -> i.updateAll(changes));
            changes.forEach(update -> updateStateChangeListeners(update.originalObject, update.newObject));
            if (!atomicStateChangeListeners.isEmpty()) {
                atomicStateChangeListeners.forEach(l -> l.stateChanged(changes));
            }
        }
        finally {
            updateIndexesInProgressReEntryLatch = false;
        }
    }
    
    private void updateStateChangeListeners(@Nullable C oldState, @Nullable C newState) {
        stateChangeListeners.forEach(s -> s.stateChanged(oldState, newState));
    }

    public void clear() {
        ImmutableCollection<Change<C>> clearedObjects = objectStore.stream().map(Change::delete).collect(ImmutableSet.toImmutableSet());
        objectStore.clear();
        updateIndexes(clearedObjects);
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

}
