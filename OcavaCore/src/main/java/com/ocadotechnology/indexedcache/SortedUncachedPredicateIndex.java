/*
 * Copyright © 2017-2025 Ocado (Ocava)
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

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.ocadotechnology.id.Identified;

/**
 * An index that does not cache its content.
 */
public class SortedUncachedPredicateIndex<C extends Identified<? extends I>, I> extends UncachedPredicateIndex<C, I> implements SortedPredicateIndex<C> {
    private final Comparator<? super C> comparator;

    /**
     * @param name optional String parameter - the name of the index.
     * @param backingCache the cache to index
     * @param predicate the predicate used to filter values in the cache. Only values for which the predicate returns true will be sorted.
     * @param comparator the comparator used to sort values that pass the predicate.
     */
    public SortedUncachedPredicateIndex(
            @CheckForNull String name,
            IndexedImmutableObjectCache<C, I> backingCache,
            Predicate<? super C> predicate,
            Comparator<? super C> comparator) {
        super(name, backingCache, predicate);
        this.comparator = comparator;
    }

    @Override
    public Stream<C> stream() {
        return super.stream().sorted(comparator);
    }

    @Override
    public Optional<C> getFirst() {
        return streamUnsorted().min(comparator);
    }

    @Override
    public Optional<C> getLast() {
        return streamUnsorted().max(comparator);
    }

    @Override
    public Optional<C> after(C previous) {
        return stream().filter(c -> comparator.compare(c, previous) > 0).findFirst();
    }

    @Override
    public UnmodifiableIterator<C> iterator() {
        return Iterators.unmodifiableIterator(stream().iterator());
    }

    private Stream<C> streamUnsorted() {
        return super.stream();
    }
}
