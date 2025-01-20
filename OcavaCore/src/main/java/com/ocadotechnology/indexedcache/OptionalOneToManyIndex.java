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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Identified;

public final class OptionalOneToManyIndex<R, C extends Identified<?>> extends AbstractIndex<C> {

    private final Map<R, Set<C>> indexValues = new LinkedHashMap<>();
    private final Function<? super C, Optional<R>> function;
    private ImmutableMultimap<R, C> snapshot;

    OptionalOneToManyIndex(Function<? super C, Optional<R>> function) {
        this(null, function);
    }

    OptionalOneToManyIndex(@CheckForNull String name, Function<? super C, Optional<R>> function) {
        super(name);
        this.function = function;
    }

    public boolean hasStatesForKey(R r) {
        return indexValues.containsKey(r) && !indexValues.get(r).isEmpty();
    }

    public boolean isEmpty(R r) {
        return getMutable(r).isEmpty();
    }

    public int count(R r) {
        return getMutable(r).size();
    }

    public Stream<C> stream(R r) {
        return getMutable(r).stream();
    }

    /**
     * @return all C for which function returns a present R
     */
    public Stream<C> flatStream() {
        return indexValues.values().stream().flatMap(Collection::stream);
    }

    /** Order is arbitrary (but deterministic). */
    @Deprecated //FIXME: leaks pointer to list in map
    public Stream<Map.Entry<R, Set<C>>> streamEntries() {
        return indexValues.entrySet().stream();
    }

    /** Order is arbitrary (but deterministic). */
    public Set<R> keySet() {
        return indexValues.keySet();
    }

    private Set<C> getMutable(R r) {
        return indexValues.getOrDefault(r, Collections.emptySet());
    }

    @Override
    protected void remove(C object) {
        function.apply(object).ifPresent(r -> {
            Set<C> rs = indexValues.get(r);
            Preconditions.checkState(rs.remove(object));
            if (rs.isEmpty()) {
                indexValues.remove(r);
            }
            snapshot = null;
        });
    }

    protected void add(C object) {
        function.apply(object).ifPresent(r -> {
            indexValues.computeIfAbsent(r, list -> new LinkedHashSet<>()).add(object);
            snapshot = null;
        });
    }

    public ImmutableList<C> getCopy(R r) {
        return ImmutableList.copyOf(getMutable(r));
    }

    public ImmutableSet<C> getCopyAsSet(R r) {
        return ImmutableSet.copyOf(getMutable(r));
    }

    public Stream<R> streamKeys() {
        return indexValues.keySet().stream();
    }

    public ImmutableMultimap<R, C> snapshot() {
        if (snapshot == null) {
            Builder<R, C> builder = ImmutableMultimap.builder();
            indexValues.forEach(builder::putAll);
            snapshot = builder.build();
        }
        return snapshot;
    }
}
