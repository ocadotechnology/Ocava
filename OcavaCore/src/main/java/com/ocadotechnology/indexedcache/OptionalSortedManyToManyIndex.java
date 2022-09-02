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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.ocadotechnology.id.Identified;

public final class OptionalSortedManyToManyIndex<R, C extends Identified<?>> extends AbstractIndex<C> {
    private final SortedSet<C> emptySet = ImmutableSortedSet.of();
    private final Map<R, SortedSet<C>> indexValues = new LinkedHashMap<>();
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
    OptionalSortedManyToManyIndex(@CheckForNull String name, Function<? super C, Optional<Set<R>>> function, Comparator<? super C> comparator) {
        super(name);
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

    public Optional<C> first(@CheckForNull R r) {
        SortedSet<C> tmp = getMutable(r);
        return tmp.isEmpty() ? Optional.empty() : Optional.of(tmp.first());
    }

    public Optional<C> last(@CheckForNull R r) {
        SortedSet<C> tmp = getMutable(r);
        return tmp.isEmpty() ? Optional.empty() : Optional.of(tmp.last());
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
            removeFromStore(object, r);
        }));
    }

    private void removeFromStore(C object, R r) {
        Set<C> cs = indexValues.get(r);
        cs.remove(object);
        if (cs.isEmpty()) {
            indexValues.remove(r);
        }
    }

    @Override
    protected void add(C object) throws IndexUpdateException {
        Optional<Set<R>> optionalSet = function.apply(object);
        List<R> addedTo = new ArrayList<>(optionalSet.map(Set::size).orElse(1));
        for (R r : optionalSet.orElse(ImmutableSet.of())) {
            Set<C> cs = indexValues.computeIfAbsent(r, set -> new TreeSet<>(comparator));
            if (cs.add(object)) {
                addedTo.add(r);
                continue;
            }
            addedTo.forEach(r1 -> removeFromStore(object, r1));
            throw new IndexUpdateException(
                    name != null ? name : function.getClass().getSimpleName(),
                    "Error updating %s: Trying to add [%s], but an equal value already exists in the set. Does your comparator conform to the requirements?",
                    formattedName,
                    object
            );
        }
    }

    private SortedSet<C> getMutable(R r) {
        return indexValues.getOrDefault(r, emptySet);
    }
}
