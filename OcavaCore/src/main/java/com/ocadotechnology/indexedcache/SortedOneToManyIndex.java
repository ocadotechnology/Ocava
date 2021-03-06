/*
 * Copyright © 2017-2021 Ocado (Ocava)
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.ocadotechnology.id.Identified;

public class SortedOneToManyIndex<R, C extends Identified<?>> extends AbstractIndex<C> {
    private final SortedSet<C> EMPTY_TREE_SET = ImmutableSortedSet.of();

    private final Map<R, SortedSet<C>> indexValues = new LinkedHashMap<>();
    private final Function<? super C, R> function;
    private final Comparator<? super C> comparator;

    /**
     * @param function key extraction function
     * @param comparator A comparator on a set of elements C which is consistent with equals().
     *        More formally, a total-order comparator on a set of elements C where
     *        compare(c1, c2) == 0 implies that Objects.equals(c1, c2) == true.
     *        This requirement is strictly enforced. Violating it will produce an IllegalStateException
     *        and leave the cache in an inconsistent state.
     */
    public SortedOneToManyIndex(Function<? super C, R> function, Comparator<? super C> comparator) {
        this.function = function;
        this.comparator = comparator;
    }

    public Stream<C> stream(R r) {
        return getMutable(r).stream();
    }

    public Stream<R> streamKeys() {
        return indexValues.keySet().stream();
    }

    public ImmutableSet<R> keySet() {
        return ImmutableSet.copyOf(indexValues.keySet());
    }

    public TreeSet<C> getCopy(R r) {
        return new TreeSet<>(getMutable(r));
    }

    public ImmutableSet<C> getCopyAsSet(R r) {
        return ImmutableSet.copyOf(getMutable(r));
    }

    public <Q> ImmutableSet<Q> getCopyAsSet(R r, Function<C, Q> mappingFunction) {
        return stream(r).map(mappingFunction).collect(ImmutableSet.toImmutableSet());
    }

    public boolean isEmpty(R r) {
        return getMutable(r).isEmpty();
    }

    public int size(R r) {
        return getMutable(r).size();
    }

    public C first(R r) {
        return getMutable(r).first();
    }

    public UnmodifiableIterator<C> iterator(R r) {
        return Iterators.unmodifiableIterator(getMutable(r).iterator());
    }

    @Override
    protected void remove(C object) {
        R r = function.apply(object);
        Set<C> rs = indexValues.get(r);
        Preconditions.checkState(rs.remove(object));
        if (rs.isEmpty()) {
            indexValues.remove(r);
        }
    }

    @Override
    protected void add(C object) {
        R r = function.apply(object);
        SortedSet<C> cs = indexValues.computeIfAbsent(r, this::newValues);
        Preconditions.checkState(cs.add(object), "Trying to add [%s] to SortedOneToManyIndex, but an equal value already exists in the set. Does your comparator conform to the requirements?", object);
    }

    private SortedSet<C> getMutable(R r) {
        return indexValues.getOrDefault(r, EMPTY_TREE_SET);
    }

    private TreeSet<C> newValues(R ignore) {
        return new TreeSet<>(comparator);
    }

}
