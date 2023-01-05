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

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.id.Identified;

/** BiMap-based partial injection index.
 *  <br>
 *  Enforces both one-to-one mappings:
 *  if a key for a value exists, it is present at most once, and
 *  if a value exists, it is present at most once.
 *  <br>
 *
 *  Implementation Note:
 *  <br>
 *  This class implements update as a remove followed by an add.<br>
 *  That avoids using class "C" equals method to compare old against new
 *  (to decide if a value has changed),
 *  (but still uses it to enforce that a value is associated with one key).
 */
public final class DefaultOptionalOneToOneIndex<R, C extends Identified<?>> extends AbstractOptionalOneToOneIndex<R, C> {

    private final BiMap<R, C> indexValues = HashBiMap.create();
    private final Function<? super C, Optional<R>> indexingFunction;

    private transient ImmutableMap<R, C> snapshot; //Null if the previous snapshot has been invalidated by an update

    public DefaultOptionalOneToOneIndex(Function<? super C, Optional<R>> indexingFunction) {
        this(null, indexingFunction);
    }

    public DefaultOptionalOneToOneIndex(@CheckForNull String name, Function<? super C, Optional<R>> indexingFunction) {
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
    protected void remove(C object) {
        indexingFunction.apply(object).ifPresent(val -> {
            indexValues.remove(val);
            snapshot = null;
        });
    }

    @Override
    protected void add(C object) throws IndexUpdateException {
        Optional<R> optionalR = indexingFunction.apply(object);
        if (optionalR.isPresent()) {
            R r = optionalR.get();
            C oldValue = indexValues.put(r, object);
            if (oldValue != null) {
                indexValues.put(r, oldValue);
                throw new IndexUpdateException(
                        name != null ? name : indexingFunction.getClass().getSimpleName(),
                        "Error updating %s: Trying to add [%s] to OptionalOneToOneIndex, but oldValue [%s] already exists at index [%s]",
                        formattedName,
                        object,
                        oldValue,
                        r
                );
            }
            snapshot = null;
        }
    }

    @Override
    public ImmutableMap<R, C> snapshot() {
        if (snapshot == null) {
            snapshot = ImmutableMap.copyOf(indexValues);
        }
        return snapshot;
    }
}
