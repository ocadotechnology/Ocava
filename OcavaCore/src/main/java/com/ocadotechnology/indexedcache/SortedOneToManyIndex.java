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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.ocadotechnology.id.Identified;

public class SortedOneToManyIndex<R, C extends Identified<?>> extends AbstractIndex<C> {
    private final NavigableSet<C> EMPTY_TREE_SET = ImmutableSortedSet.of();

    private final Map<R, NavigableSet<C>> indexValues = new LinkedHashMap<>();
    private final Function<? super C, R> function;
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
    public SortedOneToManyIndex(Function<? super C, R> function, Comparator<? super C> comparator) {
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
    public SortedOneToManyIndex(@CheckForNull String name, Function<? super C, R> function, Comparator<? super C> comparator) {
        super(name);
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

    public Optional<C> first(R r) {
        SortedSet<C> tmp = getMutable(r);

        return tmp.isEmpty() ? Optional.empty() : Optional.of(tmp.first());
    }

    public Optional<C> last(R r) {
        SortedSet<C> tmp = getMutable(r);

        return tmp.isEmpty() ? Optional.empty() : Optional.of(tmp.last());
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
        snapshot = null;
    }

    @Override
    protected void add(C object) throws IndexUpdateException {
        R r = function.apply(object);
        SortedSet<C> cs = indexValues.computeIfAbsent(r, this::newValues);
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

    private NavigableSet<C> getMutable(R r) {
        return indexValues.getOrDefault(r, EMPTY_TREE_SET);
    }

    private TreeSet<C> newValues(R ignore) {
        return new TreeSet<>(comparator);
    }

    public ImmutableListMultimap<R, C> snapshot() {
        if (snapshot == null) {
            ImmutableListMultimap.Builder<R, C> builder = ImmutableListMultimap.builder();
            indexValues.forEach(builder::putAll);
            snapshot = builder.build();
        }
        return snapshot;
    }

    public void forEach(R key, Consumer<C> consumer) {
        NavigableSet<C> set = indexValues.get(key);
        if (set == null) {
            return;
        }
        for (C c : set) {
            consumer.accept(c);
        }
    }

    public @CheckForNull C findFirstValueSatisfying(R key, Predicate<C> predicate) {
        NavigableSet<C> set = indexValues.get(key);
        if (set == null) {
            return null;
        }

        for (C c : set) {
            if (predicate.test(c)) {
                return c;
            }
        }

        return null;
    }
}
