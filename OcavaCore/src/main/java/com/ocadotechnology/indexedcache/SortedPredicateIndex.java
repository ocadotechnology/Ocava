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

import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.collect.UnmodifiableIterator;
import com.ocadotechnology.id.Identified;

/**
 * A {@link PredicateIndex} that defines a sort order for the elements that satisfy the predicate.
 */
public interface SortedPredicateIndex<C extends Identified<?>> extends PredicateIndex<C> {

    /**
     * @return a Stream of all elements that satisfy the predicate. Stream is sorted.
     */
    Stream<C> stream();

    /**
     * @return a Stream of all elements that do not satisfy the predicate. Stream is <strong>not</strong> guaranteed to be sorted, but order will be consistent.
     */
    Stream<C> streamWhereNot();

    /**
     * @return the first element in the given sort order that satisfies the predicate, if any.
     */
    Optional<C> getFirst();

    /**
     * @return the last element in the given sort order that satisfies the predicate, if any.
     */
    Optional<C> getLast();

    /**
     * @return the first element in the given sort order that is greater than {@code previous} and satisfies the predicate, if any.
     * Note that {@code previous} does not have to exist in the set, or pass the predicate - the next element will still be returned.
     */
    Optional<C> after(C previous);

    /**
     * @return an iterator over the elements in the set, in the given sort order.
     */
    UnmodifiableIterator<C> iterator();
}
