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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.ocadotechnology.id.Identified;

/** A SortedOneToManyIndex where each 'Many' can have a different comparator. */
public class SeparatelySortedOneToManyIndex<R, C extends Identified<?>> extends AbstractIndex<C> {
    private final Map<R, TreeSet<C>> indexValues = new LinkedHashMap<>();
    private final Function<? super C, R> function;
    private final Function<R, Comparator<C>> comparatorGenerator;

    /**
     * @param function key extraction function.
     * @param comparatorGenerator generates a comparator for a given key
     *        A comparator on a set of elements C which is consistent with equals().
     *        More formally, a total-order comparator on a set of elements C where
     *        compare(c1, c2) == 0 implies that Objects.equals(c1, c2) == true.
     *        This requirement is strictly enforced. Violating it will produce an IllegalStateException
     *        and leave the cache in an inconsistent state.
     */
    public SeparatelySortedOneToManyIndex(Function<? super C, R> function, Function<R, Comparator<C>> comparatorGenerator) {
        this.function = function;
        this.comparatorGenerator = comparatorGenerator;
    }

    public Stream<C> stream(R r) {
        return getMutable(r).stream();
    }

    public TreeSet<C> getCopy(R r) {
        return new TreeSet<>(getMutable(r));
    }

    public int size(R r) {
        return getMutable(r).size();
    }

    public ImmutableSet<C> getCopyAsSet(R r) {
        return ImmutableSet.copyOf(getMutable(r));
    }

    public <Q> ImmutableSet<Q> getCopyAsSet(R r, Function<C, Q> mappingFunction) {
        return stream(r).map(mappingFunction).collect(ImmutableSet.toImmutableSet());
    }

    public ImmutableSet<R> keySet() {
        return ImmutableSet.copyOf(indexValues.keySet());
    }

    public Stream<R> streamKeys() {
        return indexValues.keySet().stream();
    }

    public UnmodifiableIterator<C> iterator(R r) {
        return Iterators.unmodifiableIterator(getMutable(r).iterator());
    }

    @Override
    protected void remove(C object) {
        R r = function.apply(object);
        Set<C> cs = indexValues.get(r);
        Preconditions.checkState(cs.remove(object));
        if (cs.isEmpty()) {
            indexValues.remove(r);
        }
    }

    @Override
    protected void add(C object) {
        R r = function.apply(object);
        TreeSet<C> cs = indexValues.computeIfAbsent(r, this::newValues);
        Preconditions.checkState(cs.add(object), "Trying to add [%s] to SeparatelySortedOneToManyIndex, but an equal value already exists in the set. Does your comparator conform to the requirements?", object);
    }

    private TreeSet<C> getMutable(R r) {
        return indexValues.computeIfAbsent(r, this::newValues);
    }

    private TreeSet<C> newValues(R r) {
        return new TreeSet<>(comparatorGenerator.apply(r));
    }

    public boolean isEmpty(R r) {
        return getMutable(r).isEmpty();
    }

    public Optional<C> first(R r) {
        TreeSet<C> cs = getMutable(r);
        return cs.isEmpty() ? Optional.empty() : Optional.of(cs.first());
    }

    /**
     *  For a given key 'r', return the least element from the sorted values greater than 'previous'
     *  (same as iteration order).<br>
     *  Note: previous does not have to exist in the set (the next element will still be returned).
     */
    public Optional<C> after(R r, C previous) {
        NavigableSet<C> cs = getMutable(r).tailSet(previous, false);
        return cs.isEmpty() ? Optional.empty() : Optional.of(cs.first());
    }

    public int count(R r) {
        return getMutable(r).size();
    }
}
