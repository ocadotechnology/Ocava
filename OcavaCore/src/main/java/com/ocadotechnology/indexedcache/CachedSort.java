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

import java.util.Comparator;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;

import javax.annotation.CheckForNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.ocadotechnology.id.Identified;

public class CachedSort<C extends Identified<?>> extends AbstractIndex<C> {
    private final TreeSet<C> values;

    /**
     * @param comparator - A comparator on a set of elements C which is consistent with equals().
     *        More formally, a total-order comparator on a set of elements C where
     *        compare(c1, c2) == 0 implies that Objects.equals(c1, c2) == true.
     *        This requirement is strictly enforced. Violating it will produce an IllegalStateException
     *        and leave the cache in an inconsistent state.
     */
    public CachedSort(Comparator<? super C> comparator) {
        this(null, comparator);
    }

    /**
     * @param name optional String parameter - the name of the index.
     * @param comparator - A comparator on a set of elements C which is consistent with equals().
     *        More formally, a total-order comparator on a set of elements C where
     *        compare(c1, c2) == 0 implies that Objects.equals(c1, c2) == true.
     *        This requirement is strictly enforced. Violating it will produce an IllegalStateException
     *        and leave the cache in an inconsistent state.
     */
    public CachedSort(@CheckForNull String name, Comparator<? super C> comparator) {
        super(name);
        this.values = new TreeSet<>(comparator);
    }

    @Override
    protected void remove(C object) {
        values.remove(object);
    }

    @Override
    protected void add(C object) throws IndexUpdateException {
        if (!values.add(object)) {
            throw new IndexUpdateException(
                    getNameOrDefault(),
                    "Error updating %s: Trying to add [%s], but an equal value already exists in the set. Does your comparator conform to the requirements?",
                    formattedName,
                    object
            );
        }
    }

    private String getNameOrDefault() {
        if (name != null) {
            return name;
        }
        Comparator<? super C> comparator = values.comparator();
        return comparator != null ? comparator.getClass().getSimpleName() : getClass().getSimpleName();
    }

    public Optional<C> peek() {
        return values.isEmpty() ? Optional.empty() : Optional.of(values.first());
    }

    public C peekOrNull() {
        return values.isEmpty() ? null : values.first();
    }

    public <R> R peekOrNull(Function<C, R> mappingFunction) {
        return values.isEmpty() ? null : mappingFunction.apply(values.first());
    }

    public Optional<C> peekLast() {
        return values.isEmpty() ? Optional.empty() : Optional.of(values.last());
    }

    public <R> R peekLastOrNull(Function<C, R> mappingFunction) {
        return  values.isEmpty() ? null : mappingFunction.apply(values.last());
    }

    public ImmutableList<C> asList() {
        return ImmutableList.copyOf(values);
    }

    public <R> ImmutableList<R> asList(Function<C, R> mappingFunction) {
        return values.stream()
                .map(mappingFunction)
                .collect(ImmutableList.toImmutableList());
    }

    public UnmodifiableIterator<C> iterator() {
        return Iterators.unmodifiableIterator(values.iterator());
    }
}
