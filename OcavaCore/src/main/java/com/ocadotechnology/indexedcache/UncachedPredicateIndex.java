/*
 * Copyright © 2017-2024 Ocado (Ocava)
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

import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import com.ocadotechnology.id.Identified;

/** An index that does not cache its content. */
public class UncachedPredicateIndex<C extends Identified<? extends I>, I> extends Index<C> implements PredicateIndex<C> {
    private final IndexedImmutableObjectCache<C, I> backingCache;
    private final Predicate<? super C> predicate;

    @CheckForNull
    private final String name;

    public UncachedPredicateIndex(@CheckForNull String name, IndexedImmutableObjectCache<C, I> backingCache, Predicate<? super C> predicate) {
        this.name = name;
        this.backingCache = backingCache;
        this.predicate = predicate;
    }

    @Override
    public Stream<C> stream() {
        return backingCache.stream().filter(predicate);
    }

    @Override
    public Stream<C> streamWhereNot() {
        return backingCache.stream().filter(predicate.negate());
    }

    @Override
    public boolean isEmpty() {
        return backingCache.stream().noneMatch(predicate);
    }

    @Override
    public int count() {
        return (int)stream().count();
    }

    @Override
    public int countWhereNot() {
        return (int)streamWhereNot().count();
    }

    @Override
    protected void update(@Nullable C c, @Nullable C c1) {
        // nop
    }

    @Override
    protected void updateAll(Iterable<Change<C>> iterable) {
        // nop
    }
}
