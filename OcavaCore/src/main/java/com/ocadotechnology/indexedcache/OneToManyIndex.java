/*
 * Copyright © 2017-2022 Ocado (Ocava)
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Identified;
import com.ocadotechnology.indexedcache.OneToOneIndex.WrappedIndexingFunction;

public final class OneToManyIndex<R, C extends Identified<?>> extends AbstractIndex<C> {

    private final OptionalOneToManyIndex<R, C> optionalOneToManyIndex;

    /**
     * @deprecated use OptionalOneToManyIndex or pass in a non-optional function
     */
    @Deprecated
    public OneToManyIndex(Function<? super C, Optional<R>> function) {
        super(null);
        optionalOneToManyIndex = new OptionalOneToManyIndex<>(function);
    }

    private OneToManyIndex(@CheckForNull String name, Function<? super C, R> function) {
        super(name);
        optionalOneToManyIndex = new OptionalOneToManyIndex<>(name, new WrappedIndexingFunction<>(function, formattedName));
    }

    public static <R, C extends Identified<?>> OneToManyIndex<R, C> create(Function<? super C, R> function) {
        return create(null, function);
    }

    public static <R, C extends Identified<?>> OneToManyIndex<R, C> create(@CheckForNull String name, Function<? super C, R> function) {
        return new OneToManyIndex<>(name, function);
    }

    public ImmutableList<C> getCopy(R r) {
        return optionalOneToManyIndex.getCopy(r);
    }

    public ImmutableSet<C> getCopyAsSet(R r) {
        return optionalOneToManyIndex.getCopyAsSet(r);
    }

    public boolean isEmpty(R r) {
        return optionalOneToManyIndex.isEmpty(r);
    }

    public int count(R r) {
        return optionalOneToManyIndex.count(r);
    }

    public Stream<C> stream(R r) {
        return optionalOneToManyIndex.stream(r);
    }

    @Override
    protected void add(C object) {
        optionalOneToManyIndex.add(object);
    }

    @Override
    protected void remove(C object) {
        optionalOneToManyIndex.remove(object);
    }

    public Stream<R> streamKeys() {
        return optionalOneToManyIndex.streamKeys();
    }

    public ImmutableMultimap<R, C> snapshot() {
        return optionalOneToManyIndex.snapshot();
    }
}
