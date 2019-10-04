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
package com.ocadotechnology.indexedcache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.id.Identified;
import com.ocadotechnology.id.Identity;

public class HashMapObjectStore<C extends Identified<? extends I>, I> implements ObjectStore<C, I> {

    private final Map<Identity<? extends I>, C> objects;

    private @CheckForNull ImmutableMap<Identity<? extends I>, C> snapshot; //Null if the previous snapshot has been invalidated by an update

    public HashMapObjectStore() {
        objects = new LinkedHashMap<>();
    }

    public HashMapObjectStore(int initialSize, float fillFactor) {
        objects = new LinkedHashMap<>(initialSize, fillFactor);
    }

    @Override
    public void addAll(ImmutableCollection<C> newObjects) throws CacheUpdateException {
        ImmutableList<CacheMismatch<C>> clashingObjects = newObjects.stream()
                .map(o -> applyChange(null, o))
                .filter(Objects::nonNull)
                .collect(ImmutableList.toImmutableList());
        if (!clashingObjects.isEmpty()) {
            newObjects.forEach(o -> rollback(null, o)); //rollback successful changes
            clashingObjects.forEach(m -> rollback(m.presentObject, m.expectedObject)); //rollback clashes
            throw new CacheUpdateException("The following objects with clashing IDs were already found in the map: " + clashingObjects.stream().map(c -> c.presentObject).collect(Collectors.toList()));
        }
        if (!newObjects.isEmpty()) {
            snapshot = null;
        }
    }

    @Override
    public void updateAll(ImmutableCollection<Change<C>> changes) throws CacheUpdateException {
        ImmutableList<CacheMismatch<C>> clashingObjects = changes.stream()
                .map(c -> applyChange(c.originalObject, c.newObject))
                .filter(Objects::nonNull)
                .collect(ImmutableList.toImmutableList());
        if (!clashingObjects.isEmpty()) {
            changes.forEach(u -> rollback(u.originalObject, u.newObject)); //rollback successful changes
            clashingObjects.forEach(m -> rollback(m.presentObject, m.expectedObject)); //rollback clashes
            throw new CacheUpdateException("The following objects in the cache did not match the expectation given in the updates: " + clashingObjects);
        }
        if (!changes.isEmpty()) {
            snapshot = null;
        }
    }

    @Override
    public void update(@Nullable C expectedObject, @Nullable C newObject) throws CacheUpdateException {
        CacheMismatch<C> mismatch = applyChange(expectedObject, newObject);
        if (mismatch != null) {
            rollback(mismatch.presentObject, mismatch.expectedObject);
            throw new CacheUpdateException("expectedObject was " + mismatch.expectedObject + ", but found different object in cache: " + mismatch.presentObject);
        }
        snapshot = null;
    }

    @Override
    public void add(C newObject) throws CacheUpdateException {
        CacheMismatch<C> mismatch = applyChange(null, newObject);
        if (mismatch != null) {
            rollback(mismatch.presentObject, mismatch.expectedObject);
            throw new CacheUpdateException("Object " + mismatch.presentObject + " with clashing ID was already found in the map.");
        }
        snapshot = null;
    }

    private @CheckForNull CacheMismatch<C> applyChange(C expectedObject, @Nullable C newObject) {
        if (newObject == null) {
            C originalObject = objects.remove(expectedObject.getId());
            if (originalObject == expectedObject) {
                return null;
            }
            return new CacheMismatch<>(originalObject, expectedObject);
        }

        C originalObject = objects.put(newObject.getId(), newObject);
        if (originalObject == expectedObject) {
            return null;
        }
        return new CacheMismatch<>(originalObject, expectedObject);
    }

    private void rollback(@Nullable C originalObject, C referenceObject) {
        if (originalObject == null) {
            objects.remove(referenceObject.getId());
        } else {
            objects.put(originalObject.getId(), originalObject);
        }
    }

    @Override
    public ImmutableCollection<C> deleteAll(ImmutableCollection<Identity<? extends I>> ids) throws CacheUpdateException {
        ImmutableMap<Identity<? extends I>, C> oldObjects = ids.stream().map(objects::remove)
                .filter(Objects::nonNull)
                .collect(ImmutableMap.toImmutableMap(Identified::getId, o -> o));
        if (!oldObjects.keySet().containsAll(ids)) {
            oldObjects.forEach(objects::put); //rollback
            throw new CacheUpdateException("No object(s) in cache with ID(s) " + ids.stream().filter(id -> !oldObjects.containsKey(id)).collect(Collectors.toList()));
        }
        if (!ids.isEmpty()) {
            snapshot = null;
        }
        return oldObjects.values();
    }

    @Override
    public C delete(Identity<? extends I> id) throws CacheUpdateException {
        C oldObject = objects.remove(id);
        if (oldObject == null) {
            throw new CacheUpdateException("No object in cache with ID " + id);
        }
        snapshot = null;
        return oldObject;
    }

    @Override
    public C get(Identity<? extends I> id) {
        return objects.get(id);
    }

    @Override
    public boolean containsId(Identity<? extends I> id) {
        return objects.containsKey(id);
    }

    @Override
    public Stream<C> stream() {
        return objects.values().stream();
    }

    @Override
    public void forEach(Consumer<C> action) {
        objects.values().forEach(action);
    }

    @Override
    public void clear() {
        snapshot = null;
        objects.clear();
    }

    @Override
    public int size() {
        return objects.size();
    }

    @Override
    public ImmutableMap<Identity<? extends I>, C> snapshot() {
        if (snapshot == null) {
            snapshot = ImmutableMap.copyOf(objects);
        }
        return snapshot;
    }
}
