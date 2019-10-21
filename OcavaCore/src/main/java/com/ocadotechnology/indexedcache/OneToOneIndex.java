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
import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.id.Identified;

public final class OneToOneIndex<R, C extends Identified<?>> extends AbstractIndex<C> {
    private final OptionalOneToOneIndex<R, C> optionalOneToOneIndex;
    public final Function<? super C, R> indexingFunction;

    /**
     *
     * @param indexingFunction mapping C -> R where nulls are not permitted
     */
    OneToOneIndex(Function<? super C, R> indexingFunction) {
        this.indexingFunction = indexingFunction;
        Function<? super C, Optional<R>> optionalIndexingFunction = indexingFunction.andThen(Preconditions::checkNotNull).andThen(Optional::of);
        this.optionalOneToOneIndex = new OptionalOneToOneIndex<>(optionalIndexingFunction);
    }

    public C get(R r) {
        return optionalOneToOneIndex.getOrNull(r);
    }

    @Override
    protected void add(C newObject) {
        optionalOneToOneIndex.add(newObject);
    }

    @Override
    protected void remove(C object) {
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
}
