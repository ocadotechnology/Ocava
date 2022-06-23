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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;

import com.google.common.collect.ImmutableCollection;
import com.ocadotechnology.id.Identified;

public final class ManyToManyIndex<R, C extends Identified<?>> extends AbstractIndex<C> {

    private final Map<R, Set<C>> indexValues = new LinkedHashMap<>();
    private final Function<? super C, Set<R>> function;

    ManyToManyIndex(Function<? super C, Set<R>> function) {
        this(null, function);
    }

    ManyToManyIndex(@CheckForNull String name, Function<? super C, Set<R>> function) {
        super(name);
        this.function = function;
    }

    public boolean containsKey(R r) {
        return indexValues.keySet().contains(r);
    }

    public Stream<C> stream(R r) {
        return getMutable(r).stream();
    }

    public Stream<R> streamKeySet() {
        return indexValues.keySet().stream();
    }

    private Set<C> getMutable(R r) {
        Set<C> set = indexValues.get(r);
        return set == null ? Collections.emptySet() : set;
    }

    public int count(R r) {
        return getMutable(r).size();
    }

    @Override
    protected void remove(C object) {
        function.apply(object).forEach(r -> {
            Set<C> rs = indexValues.get(r);
            rs.remove(object);
            if (rs.isEmpty()) {
                indexValues.remove(r);
            }
        });
    }

    @Override
    protected void add(C object) {
        function.apply(object).forEach(value -> indexValues.computeIfAbsent(value, set -> new LinkedHashSet<>()).add(object));
    }

    public Stream<C> streamIncludingDuplicates(ImmutableCollection<R> keys) {
        return keys.stream().flatMap(this::stream);
    }

}
