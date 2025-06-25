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
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;

import com.google.common.collect.UnmodifiableIterator;
import com.ocadotechnology.id.Identified;

/**
 * A sorted version of {@link DefaultPredicateIndex}.
 * <br>
 * Note: this implementation seperately sorts both the true and false values of the predicate,
 * though it is not strictly required by the {@link SortedPredicateIndex} interface.
 * If this is not required, consider using a {@link SortedIdCachedPredicateIndex} instead.
 */
public class DefaultSortedPredicateIndex<C extends Identified<?>> extends AbstractIndex<C> implements SortedPredicateIndex<C> {
    private final SortedOneToManyIndex<Boolean, C> index;

    /**
     * @param name optional String parameter - the name of the index.
     * @param predicate the predicate used to filter values in the cache.
     * @param comparator A comparator on a set of elements C which is consistent with equals().
     *                   More formally, a total-order comparator on a set of elements C where
     *                   compare(c1, c2) == 0 implies that Objects.equals(c1, c2) == true.
     *                   This requirement is strictly enforced. Violating it will produce an IllegalStateException
     *                   and leave the cache in an inconsistent state.
     */
    public DefaultSortedPredicateIndex(@CheckForNull String name, Predicate<? super C> predicate, Comparator<? super C> comparator) {
        super(name);
        this.index = new SortedOneToManyIndex<>(name, predicate::test, comparator);
    }

    @Override
    protected void remove(C object) throws IndexUpdateException {
        index.remove(object);
    }

    @Override
    protected void add(C object) throws IndexUpdateException {
        index.add(object);
    }

    @Override
    public Stream<C> stream() {
        return index.stream(true);
    }

    @Override
    public int count() {
        return index.size(true);
    }

    /**
     * @return a Stream of all elements that do not satisfy the predicate. Stream <strong>is</strong> sorted.
     */
    @Override
    public Stream<C> streamWhereNot() {
        return index.stream(false);
    }

    @Override
    public int countWhereNot() {
        return index.size(false);
    }

    @Override
    public boolean isEmpty() {
        return index.isEmpty(true);
    }

    @Override
    public Optional<C> getFirst() {
        return index.first(true);
    }

    @Override
    public Optional<C> getLast() {
        return index.last(true);
    }

    @Override
    public Optional<C> after(C previous) {
        return index.after(true, previous);
    }

    @Override
    public UnmodifiableIterator<C> iterator() {
        return index.iterator(true);
    }

    /**
     * Applies the given consumer to each index value matching the predicate.
     */
    @Override
    public void forEach(Consumer<C> consumer) {
        // Apply the consumer on the underlying SortedOneToManyIndex to each value matching true - i.e. matching the predicate.
        index.forEach(true, consumer);
    }
}
