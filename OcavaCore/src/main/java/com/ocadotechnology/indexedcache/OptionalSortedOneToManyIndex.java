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

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.ocadotechnology.id.Identified;

public class OptionalSortedOneToManyIndex<R, C extends Identified<?>> extends AbstractIndex<C> {

    private final Map<R, TreeSet<C>> indexValues = new LinkedHashMap<>();
    private final Function<? super C, Optional<R>> function;
    private final Comparator<? super C> comparator;
    private ImmutableListMultimap<R, C> snapshot;

    /**
     * @param function key extraction function
     * @param comparator A comparator on a set of elements C which is consistent with equals().
     *        More formally, a total-order comparator on a set of elements C where
     *        compare(c1, c2) == 0 implies that Objects.equals(c1, c2) == true.
     *        This requirement is strictly enforced. Violating it will produce an IllegalStateException
     *        and leave the cache in an inconsistent state.
     */
    public OptionalSortedOneToManyIndex(Function<? super C, Optional<R>> function, Comparator<? super C> comparator) {
        this(null, function, comparator);
    }

    /**
     * @param name optional String parameter - the name of the index.
     * @param function key extraction function
     * @param comparator A comparator on a set of elements C which is consistent with equals().
     *        More formally, a total-order comparator on a set of elements C where
     *        compare(c1, c2) == 0 implies that Objects.equals(c1, c2) == true.
     *        This requirement is strictly enforced. Violating it will produce an IllegalStateException
     *        and leave the cache in an inconsistent state.
     */
    public OptionalSortedOneToManyIndex(@CheckForNull String name, Function<? super C, Optional<R>> function, Comparator<? super C> comparator) {
        super(name);
        this.function = function;
        this.comparator = comparator;
    }

    public boolean isEmpty() {
        return indexValues.isEmpty();
    }

    public boolean isEmpty(R r) {
        return getMutable(r).isEmpty();
    }

    public Stream<C> stream(R r) {
        return getMutable(r).stream();
    }

    public Stream<R> streamKeySet() {
        return indexValues.keySet().stream();
    }

    public int size(R r) {
        return getMutable(r).size();
    }

    public ImmutableList<C> asList(R r) {
        return ImmutableList.copyOf(getMutable(r));
    }

    public UnmodifiableIterator<C> iterator(R r) {
        return Iterators.unmodifiableIterator(getMutable(r).iterator());
    }

    @Override
    protected void remove(C object) {
        function.apply(object).ifPresent(r -> {
            Set<C> cs = indexValues.get(r);
            Preconditions.checkState(cs.remove(object));
            if (cs.isEmpty()) {
                indexValues.remove(r);
            }

            snapshot = null;
        });
    }

    @Override
    protected void add(C object) throws IndexUpdateException {
        Optional<R> optionalR = function.apply(object);
        if (optionalR.isPresent()) {
            R r = optionalR.get();
            TreeSet<C> cs = indexValues.computeIfAbsent(r, list -> new TreeSet<>(comparator));
            if (!cs.add(object)) {
                throw new IndexUpdateException(
                        name != null ? name : function.getClass().getSimpleName(),
                        "Error updating %s: Trying to add [%s], but an equal value already exists in the set. Does your comparator conform to the requirements?",
                        formattedName,
                        object
                );
            }

            snapshot = null;
        }
    }

    private SortedSet<C> getMutable(R r) {
        SortedSet<C> list = indexValues.get(r);
        return list == null ? Collections.emptySortedSet() : list;
    }

    public Optional<C> getFirst(R r) {
        SortedSet<C> mutable = getMutable(r);
        return mutable.isEmpty() ? Optional.empty() : Optional.of(mutable.first());
    }

    public Optional<C> getLast(R r) {
        SortedSet<C> mutable = getMutable(r);
        return mutable.isEmpty() ? Optional.empty() : Optional.of(mutable.last());
    }

    public ImmutableListMultimap<R, C> snapshot() {
        if (snapshot == null) {
            ImmutableListMultimap.Builder<R, C> builder = ImmutableListMultimap.builder();
            indexValues.forEach(builder::putAll);
            snapshot = builder.build();
        }
        return snapshot;
    }
}
