/*
 * Copyright © 2017-2020 Ocado (Ocava)
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
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.ocadotechnology.id.Identified;

public final class OptionalSortedManyToManyIndex<R, C extends Identified<?>> extends AbstractIndex<C> {

    private final Map<R, Set<C>> indexValues = new LinkedHashMap<>();
    private final Function<? super C, Optional<Set<R>>> function;
    private final Comparator<? super C> comparator;

    /**
     * @param function key extraction function
     * @param comparator A comparator on a set of elements C which is consistent with equals().
     *        More formally, a total-order comparator on a set of elements C where
     *        compare(c1, c2) == 0 implies that Objects.equals(c1, c2) == true.
     *        This requirement is strictly enforced. Violating it will produce an IllegalStateException
     *        and leave the cache in an inconsistent state.
     */
    OptionalSortedManyToManyIndex(Function<? super C, Optional<Set<R>>> function, Comparator<? super C> comparator) {
        this.function = function;
        this.comparator = comparator;
    }

    public Stream<C> stream(R r) {
        return getMutable(r).stream();
    }

    public Stream<R> streamKeySet() {
        return indexValues.keySet().stream();
    }

    public Stream<C> streamIncludingDuplicates(ImmutableCollection<R> keys) {
        return keys.stream().flatMap(this::stream);
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
        function.apply(object).ifPresent(setOfRs -> setOfRs.forEach(r -> {
            Set<C> cs = indexValues.get(r);
            cs.remove(object);
            if (cs.isEmpty()) {
                indexValues.remove(r);
            }
        }));
    }

    @Override
    protected void add(C object) {
        function.apply(object).ifPresent(setOfRs -> setOfRs.forEach(r -> {
            Set<C> cs = indexValues.computeIfAbsent(r, set -> new TreeSet<>(comparator));
            Preconditions.checkState(cs.add(object), "Trying to add [%s] to OptionalSortedManyToManyIndex, but an equal value already exists in the set. Does your comparator conform to the requirements?", object);
        }));
    }

    private Set<C> getMutable(R r) {
        Set<C> set = indexValues.get(r);
        return set == null ? Collections.emptySet() : set;
    }
}
