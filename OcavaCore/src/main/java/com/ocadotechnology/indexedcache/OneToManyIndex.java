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

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Identified;

public final class OneToManyIndex<R, C extends Identified<?>> extends AbstractIndex<C> {

    private final OptionalOneToManyIndex<R, C> optionalOneToManyIndex;

    public OneToManyIndex(Function<? super C, R> function) {
        Function<? super C, Optional<R>> optionalFunction = function.andThen(Preconditions::checkNotNull).andThen(Optional::of);
        optionalOneToManyIndex = new OptionalOneToManyIndex<>(optionalFunction);
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
