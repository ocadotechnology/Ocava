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

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.ocadotechnology.id.Identified;

public class MappedPredicateIndex<C extends Identified<?>, R> extends AbstractIndex<C> {

    private final Predicate<? super C> predicate;
    private final Function<? super C, R> mappingFunction;

    private final Multiset<R> where = LinkedHashMultiset.create();
    private final Multiset<R> whereNot = LinkedHashMultiset.create();

    private ImmutableSet<R> distinctWhereSnapshot;
    private ImmutableSet<R> distinctWhereNotSnapshot;

    public MappedPredicateIndex(Predicate<? super C> predicate, Function<? super C, R> mappingFunction) {
        this.predicate = predicate;
        this.mappingFunction = mappingFunction;
    }

    @Override
    protected void remove(C object) {
        if (predicate.test(object)) {
            distinctWhereSnapshot = null;
            where.remove(mappingFunction.apply(object));
        } else {
            distinctWhereNotSnapshot = null;
            whereNot.remove(mappingFunction.apply(object));
        }
    }

    @Override
    protected void add(C object) {
        if (predicate.test(object)) {
            distinctWhereSnapshot = null;
            where.add(mappingFunction.apply(object));
        } else {
            distinctWhereNotSnapshot = null;
            whereNot.add(mappingFunction.apply(object));
        }
    }

    public boolean contains(R r) {
        return where.contains(r);
    }

    public Stream<R> streamNonDistinctWhere() {
        return where.stream();
    }

    public Stream<R> streamDistinctWhere() {
        return where.entrySet().stream().map(Entry::getElement);
    }

    public Stream<R> streamNonDistinctWhereNot() {
        return whereNot.stream();
    }

    public Stream<R> streamDistinctWhereNot() {
        return whereNot.entrySet().stream().map(Entry::getElement);
    }

    public ImmutableSet<R> getDistinctWhere() {
        if (distinctWhereSnapshot == null) {
            distinctWhereSnapshot = streamDistinctWhere().collect(ImmutableSet.toImmutableSet());
        }
        return distinctWhereSnapshot;
    }

    public ImmutableSet<R> getDistinctWhereNot() {
        if (distinctWhereNotSnapshot == null) {
            distinctWhereNotSnapshot = streamDistinctWhereNot().collect(ImmutableSet.toImmutableSet());
        }
        return distinctWhereNotSnapshot;
    }
}
