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
import com.ocadotechnology.id.Identified;

public class SortedOneToManyIndex<R, C extends Identified<?>> extends AbstractIndex<C> {
    private final SortedSet<C> EMPTY_TREE_SET = ImmutableSortedSet.of();

    private final Map<R, SortedSet<C>> indexValues = new LinkedHashMap<>();
    private final Function<? super C, R> function;
    private final Comparator<C> comparator;

    public SortedOneToManyIndex(Function<? super C, R> function, Comparator<C> comparator) {
        this.function = function;
        this.comparator = comparator;
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
        indexValues.computeIfAbsent(r, this::newValues).add(object);
    }

    private SortedSet<C> getMutable(R r) {
        return indexValues.getOrDefault(r, EMPTY_TREE_SET);
    }

    private TreeSet<C> newValues(R ignore) {
        return new TreeSet<>(comparator);
    }

    public boolean isEmpty(R r) {
        return getMutable(r).isEmpty();
    }

    public C first(R r) {
        return getMutable(r).first();
    }

}
