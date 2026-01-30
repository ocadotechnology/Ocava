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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.ocadotechnology.id.Identified;

/**
 * An index that groups elements of type {@code C} by a key of type {@code G} and aggregates them into a result of type {@code T}.
 *
 * <p>
 * This class maintains a mapping from group keys ({@code G}) to aggregated results ({@code T}), updating the aggregation
 * as elements are added or removed.
 * </p>
 *
 * @param <C> - the type of elements being indexed and grouped.
 * @param <G> - the type of the group key, as extracted from each element using the provided groupByExtractor function.
 * @param <T> - the type of the aggregation result for each group, as produced by the provided collector. The expectation
 *           is that T is immutable, as otherwise there can be unexpected behavior from returning cached objects directly.
 */
public class CachedGroupBy<C extends Identified<?>, G, T> extends AbstractIndex<C> {

    private final Multimap<G, C> cachedGroupValues = LinkedHashMultimap.create();
    private final Map<G, T> cachedAggregation = new LinkedHashMap<>();
    private transient ImmutableMap<G, T> snapshot; //Null if the previous snapshot has been invalidated by an update

    private final Function<? super C, G> groupByExtractor;
    private final Collector<? super C, ?, T> collector;

    private final T emptyAggregation;

    private final Set<G> invalidatedGroups = new LinkedHashSet<>();

    CachedGroupBy(Function<? super C, G> groupByExtractor, Collector<? super C, ?, T> collector) {
        this(null, groupByExtractor, collector);
    }

    CachedGroupBy(@CheckForNull String name, Function<? super C, G> groupByExtractor, Collector<? super C, ?, T> collector) {
        super(name);
        this.groupByExtractor = groupByExtractor;
        this.collector = collector;
        this.emptyAggregation = Stream.<C>of().collect(collector);
    }

    public T get(G g) {
        return cachedAggregation.getOrDefault(g, emptyAggregation);
    }

    public ImmutableMap<G, T> snapshot() {
        if (snapshot == null) {
            snapshot = ImmutableMap.copyOf(cachedAggregation);
        }
        return snapshot;
    }

    @Override
    protected void remove(C object) {
        G group = groupByExtractor.apply(object);
        cachedGroupValues.remove(group, object);
        invalidatedGroups.add(group);
    }

    @Override
    protected void add(C object) {
        G group = groupByExtractor.apply(object);
        invalidatedGroups.add(group);
        cachedGroupValues.put(group, object);
    }

    @Override
    protected void afterUpdate() {
        invalidatedGroups.forEach(this::updateGroup);
        invalidatedGroups.clear();
    }

    private void updateGroup(G g) {
        snapshot = null;
        Collection<C> groupValues = cachedGroupValues.get(g);

        // Clear up empty groups to avoid a memory leak
        if (groupValues.isEmpty()) {
            cachedAggregation.remove(g);
            return;
        }

        T collect = groupValues
                .stream()
                .collect(collector);
        cachedAggregation.put(g, collect);
    }
}
