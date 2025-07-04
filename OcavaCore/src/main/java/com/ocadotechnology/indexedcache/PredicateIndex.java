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

import java.util.function.Consumer;
import java.util.stream.Stream;

import com.ocadotechnology.id.Identified;

public interface PredicateIndex<C extends Identified<?>> {
    Stream<C> stream();

    int count();

    Stream<C> streamWhereNot();

    int countWhereNot();

    boolean isEmpty();

    /**
     * Applies the given consumer to each index value matching the predicate.
     */
    void forEach(Consumer<C> consumer);
}
