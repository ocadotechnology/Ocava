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

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.id.Identified;

public final class OptionalOneToOneIndex<R, C extends Identified<?>> extends AbstractIndex<C> {

    private final BiMap<R, C> indexValues = HashBiMap.create();
    public final Function<? super C, Optional<R>> indexingFunction;

    private ImmutableMap<R, C> snapshot; //Null if the previous snapshot has been invalidated by an update

    public OptionalOneToOneIndex(Function<? super C, Optional<R>> indexingFunction) {
        this.indexingFunction = indexingFunction;
    }

    public C getOrNull(R r) {
        return indexValues.get(r);
    }

    public Optional<C> get(R r) {
        return Optional.ofNullable(indexValues.get(r));
    }

    public boolean containsKey(R r) {
        return indexValues.containsKey(r);
    }

    public Stream<R> streamKeys() {
        return indexValues.keySet().stream();
    }

    public Stream<C> streamValues() {
        return indexValues.values().stream();
    }

    @Override
    protected void remove(C object) {
        indexingFunction.apply(object).ifPresent(val -> {
            indexValues.remove(val);
            snapshot = null;
        });
    }

    @Override
    protected void add(C object) {
        Optional<R> optionalR = indexingFunction.apply(object);
        optionalR.ifPresent(r -> {
            C oldValue = indexValues.put(r, object);
            Preconditions.checkState(oldValue == null, "Trying to add [%s] to OptionalOneToOneIndex, but oldValue [%s] already exists at index [%s]", object, oldValue, r);
            snapshot = null;
        });
    }

    public ImmutableMap<R, C> snapshot() {
        if (snapshot == null) {
            snapshot = ImmutableMap.copyOf(indexValues);
        }
        return snapshot;
    }
}
