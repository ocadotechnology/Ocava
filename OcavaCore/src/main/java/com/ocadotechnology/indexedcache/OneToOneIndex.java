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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.id.Identified;
import com.ocadotechnology.indexedcache.IndexedImmutableObjectCache.Hints;

public final class OneToOneIndex<R, C extends Identified<?>> extends AbstractIndex<C> {
    private final AbstractOptionalOneToOneIndex<R, C> optionalOneToOneIndex;

    /**
     * @param indexingFunction mapping C -&gt; R where nulls are not permitted
     */
    public OneToOneIndex(Function<? super C, R> indexingFunction) {
        this(null, indexingFunction, Hints.optimiseForQuery);
    }

    OneToOneIndex(@CheckForNull String name, Function<? super C, R> indexingFunction, Hints hint) {
        super(name);
        this.optionalOneToOneIndex = OptionalOneToOneIndexFactory.newOptionalOneToOneIndex(
                name,
                new WrappedIndexingFunction<>(indexingFunction, formattedName),
                hint);
    }

    public C get(R r) {
        return optionalOneToOneIndex.getOrNull(r);
    }

    @Override
    protected void add(C newObject) throws IndexUpdateException {
        optionalOneToOneIndex.add(newObject);
    }

    @Override
    protected void remove(C object) throws IndexUpdateException {
        optionalOneToOneIndex.remove(object);
    }

    public ImmutableMap<R, C> snapshot() {
        return optionalOneToOneIndex.snapshot();
    }

    public boolean containsKey(R r) {
        return optionalOneToOneIndex.containsKey(r);
    }

    public Stream<R> streamKeySet() {
        return optionalOneToOneIndex.streamKeys();
    }

    static class WrappedIndexingFunction<R, C extends Identified<?>> implements Function<C, Optional<R>> {
        private final Function<? super C, R> indexingFunction;
        private final String formattedName;

        WrappedIndexingFunction(Function<? super C, R> indexingFunction, String formattedName) {
            this.indexingFunction = indexingFunction;
            this.formattedName = formattedName;
        }

        @Override
        public Optional<R> apply(C object) {
            R result = Preconditions.checkNotNull(
                    indexingFunction.apply(object),
                    "Error updating %s: Mapping function returned null for object [%s]",
                    formattedName,
                    object);
            return Optional.of(result);
        }
    }
}
