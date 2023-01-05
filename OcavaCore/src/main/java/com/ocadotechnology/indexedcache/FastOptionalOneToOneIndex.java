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
package com.ocadotechnology.indexedcache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.id.Identified;

/** Alternative implementation of OptionalOneToOneIndex, using a simple (linked)HashMap to provide a faster update
 *  at the expense of being unable to detect duplicate values.
 *  <br>
 *  This implementation is fine if used with IndexedImmutableObjectCache
 *  and is around 10% faster for an update-biased workload.
 */
@ParametersAreNonnullByDefault
public class FastOptionalOneToOneIndex<R, C extends Identified<?>> extends AbstractOptionalOneToOneIndex<R, C> {

    private final Function<? super C, Optional<R>> indexingFunction;

    // Use linked hashmap to ensure stream order is deterministic between runs
    private final Map<R, C> indexValues = new LinkedHashMap<>();

    private transient ImmutableMap<R, C> snapshot; //Null if the previous snapshot has been invalidated by an update

    public FastOptionalOneToOneIndex(Function<? super C, Optional<R>> indexingFunction) {
        this(null, indexingFunction);
    }

    public FastOptionalOneToOneIndex(@CheckForNull String name, Function<? super C, Optional<R>> indexingFunction) {
        super(name);
        this.indexingFunction = indexingFunction;
    }

    @Override
    public C getOrNull(R r) {
        return indexValues.get(r);
    }

    @Override
    public Optional<C> get(R r) {
        return Optional.ofNullable(indexValues.get(r));
    }

    @Override
    public Optional<R> getKeyFor(C c) {
        return indexingFunction.apply(c);
    }

    @Override
    public boolean containsKey(R r) {
        return indexValues.containsKey(r);
    }

    @Override
    public Stream<R> streamKeys() {
        return indexValues.keySet().stream();
    }

    @Override
    public Stream<C> streamValues() {
        return indexValues.values().stream();
    }

    @Override
    public boolean isEmpty() {
        return indexValues.isEmpty();
    }

    @Override
    public ImmutableMap<R, C> snapshot() {
        if (snapshot == null) {
            snapshot = ImmutableMap.copyOf(indexValues);
        }
        return snapshot;
    }

    // Implemenation Notes for update:
    // Using "put" instead of "remove" + "add" is around 10% faster
    // (but we can't do this in the Default class because it uses BiMap which has an equals check
    // and many of our "C"-type objects don't have useful equals methods)
    // Breaking this method down into 4 or 5 smaller methods is 2% slower.
    // This code assumes updates are far more common than adds or removes, so most "else if", "else" are never touched.

    // Care!
    // Performance-critical code.
    // Do not change without validating against both OptionalOneToOneIndexTest
    //   AND production-like system tests.
    // Baseline timing: 1130ms (for remove then add -- just comment this update method to repeat)
    // Current code timing: 980ms

    @Override
    protected void update(@CheckForNull C newObject, @CheckForNull C oldObject) throws IndexUpdateException {
        Optional<R> maybeOldKey = null;
        Optional<R> maybeNewKey = null;
        C oldValue = oldObject;
        C replacedValue = null;

        if (oldObject != null && (maybeOldKey = indexingFunction.apply(oldObject)).isPresent()) {
            R oldKey = maybeOldKey.get();

            if (newObject != null && (maybeNewKey = indexingFunction.apply(newObject)).isPresent()) {
                R newKey = maybeNewKey.get();

                if (oldKey.equals(newKey)) {
                    oldValue = indexValues.put(newKey, newObject);
                } else {
                    oldValue = indexValues.remove(oldKey);
                    replacedValue = indexValues.put(newKey, newObject);
                }
            } else {
                oldValue = indexValues.remove(oldKey);
            }
        } else {
            if (newObject != null && (maybeNewKey = indexingFunction.apply(newObject)).isPresent()) {
                R newKey = maybeNewKey.get();
                replacedValue = indexValues.put(newKey, newObject);
            } else {
                return;  // neither oldKey nor newKey are present: for us, this is the most common codepath
            }
        }

        snapshot = null;
        try {
            validateUpdate(oldObject, oldValue, replacedValue, maybeOldKey, maybeNewKey);
            afterUpdate();
        } catch (IndexUpdateException e) {
            rollbackAndThrow(maybeOldKey, maybeNewKey, oldValue, replacedValue, e);
        }
    }

    private void validateUpdate(
            @CheckForNull C oldObject,
            @CheckForNull C oldValue,
            @CheckForNull C replacedValue,
            @CheckForNull Optional<R> maybeOldKey,
            @CheckForNull Optional<R> maybeNewKey) throws IndexUpdateException {
        if (oldValue != oldObject) {
            throw new IndexUpdateException(
                    getNameOrDefault(),
                    "Error updating %s: Expected %s at old index %s, but found %s.  New index is %s",
                    formattedName,
                    oldObject,
                    maybeOldKey,
                    oldValue,
                    maybeNewKey);
        }
        if (replacedValue != null) {
            throw new IndexUpdateException(
                    getNameOrDefault(),
                    "Error updating %s: Unexpected value %s at new index %s.  Old index is %s",
                    formattedName,
                    replacedValue,
                    maybeNewKey,
                    maybeOldKey);
        }
    }

    private void rollbackAndThrow(
            @CheckForNull Optional<R> maybeOldKey,
            @CheckForNull Optional<R> maybeNewKey,
            @CheckForNull C oldValue,
            @CheckForNull C replacedValue,
            IndexUpdateException cause) throws IndexUpdateException {
        if (maybeOldKey != null && maybeOldKey.isPresent()) {
            indexValues.put(maybeOldKey.get(), oldValue);
        }
        if (maybeNewKey != null && maybeNewKey.isPresent() && !maybeNewKey.equals(maybeOldKey)) {
            indexValues.put(maybeNewKey.get(), replacedValue);
        }
        try {
            afterUpdate();
        } catch (IndexUpdateException rollbackFailure) {
            throw new IllegalStateException("Failed to rollback after error: " + cause.getMessage(), rollbackFailure);
        }
        throw cause;
    }

    @Override
    protected void remove(C object) throws IndexUpdateException {
        Optional<R> optionalKey = indexingFunction.apply(object);
        if (optionalKey.isPresent()) {
            R key = optionalKey.get();
            C oldValue = indexValues.remove(key);
            if (oldValue != object) {
                indexValues.put(key, oldValue);
                throw new IndexUpdateException(
                        getNameOrDefault(),
                        "Error updating %s: Trying to remove [%s], but oldValue [%s] not found at index [%s]",
                        formattedName,
                        object,
                        oldValue,
                        key);
            }
            snapshot = null;
        }
    }

    @Override
    protected void add(C object) throws IndexUpdateException {
        Optional<R> optionalKey = indexingFunction.apply(object);
        if (optionalKey.isPresent()) {
            R key = optionalKey.get();
            C oldValue = indexValues.put(key, object);
            if (oldValue != null) {
                indexValues.put(key, oldValue);
                throw new IndexUpdateException(
                        getNameOrDefault(),
                        "Error updating %s: Trying to add [%s] to OptionalOneToOneIndex, but oldValue [%s] already exists at index [%s]",
                        formattedName,
                        object,
                        oldValue,
                        key);
            }
            snapshot = null;
        }
    }

    private String getNameOrDefault() {
        return name != null ? name : indexingFunction.getClass().getSimpleName();
    }
}
