/*
 * Copyright © 2017-2019 Ocado (Ocava)
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

import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.UnmodifiableIterator;
import com.ocadotechnology.id.Identified;
import com.ocadotechnology.id.Identity;

interface ObjectStore<C extends Identified<? extends I>, I> {

    /**
     * Add multiple objects in an atomic operation.
     *
     * @param newObjects the objects to add
     * @throws CacheUpdateException if any of the provided objects have Ids already found in the object store.
     */
    void addAll(ImmutableCollection<C> newObjects) throws CacheUpdateException;

    /**
     * Updates multiple objects in an atomic operation.
     *
     * @param changes collection of {@link Change} objects to apply in one atomic action.
     * @throws CacheUpdateException if any of the updates have originalObjects not found in the cache.
     */
    void updateAll(ImmutableCollection<Change<C>> changes) throws CacheUpdateException;

    /**
     * Add a single object
     *
     * @param newObject the object to add
     * @throws CacheUpdateException if the new object matches the id of one already in the cache.
     */
    void add(C newObject) throws CacheUpdateException;

    /**
     * Update a single object.  May add, update or delete an object in the cache.
     *
     * @param expectedObject the expected current version of the object in the cache, or null if it is expected to be new
     * @param newObject the object to add or update, or null if the object is to be deleted
     * @throws CacheUpdateException if the expectedObject does not match the one in the cache.
     * @throws IllegalArgumentException if both expectedObject and newObject are null
     */
    void update(@Nullable C expectedObject, @Nullable C newObject) throws CacheUpdateException;

    /**
     * Remove multiple objects from the cache in an atomic operation
     * @param ids the IDs of the objects to remove
     * @return a list of the previous states of all removed objects
     * @throws CacheUpdateException if any of the objects doe not exist in the cache
     */
    ImmutableCollection<C> deleteAll(ImmutableCollection<Identity<? extends I>> ids) throws CacheUpdateException;

    /**
     * Remove a single object based on its ID
     * @param id the ID of the object to remove
     * @return the previous state of the object in the cache
     * @throws CacheUpdateException if the object does not exist in the cache
     */
    C delete(Identity<? extends I> id) throws CacheUpdateException;

    /**
     * Retrieve an object based on its ID
     * @param id the ID of the object to retrieve
     * @return the object with the given ID or null if not found
     */
    @CheckForNull C get(@Nullable Identity<? extends I> id);

    /**
     * Check if an object with the given ID is in the store
     * @param id the ID of the object to check for
     * @return true if the object with the given ID is in the store, otherwise false
     */
    boolean containsId(Identity<? extends I> id);

    /**
     * @return a stream of the stored objects
     */
    Stream<C> stream();

    /**
     * @return an unmodifiable iterator over the stored objects
     */
    UnmodifiableIterator<C> iterator();

    /**
     * Apply the action to each stored object
     * @param action to apply to each object
     */
    default void forEach(Consumer<C> action) {
        stream().forEach(action);
    }

    /**
     * Clears all objects from the store
     */
    void clear();

    /**
     * @return the number of objects in the store
     */
    int size();

    /**
     * @return an immutable copy of the stored objects keyed by ID
     */
    ImmutableMap<Identity<? extends I>, C> snapshot();
}
