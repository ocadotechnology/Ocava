/*
 * Copyright © 2017-2021 Ocado (Ocava)
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

import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.id.Identified;

public interface OptionalOneToOneIndex<R, C extends Identified<?>> {
    C getOrNull(R r);

    /** @return value associated with key 'r' (if any) */
    default Optional<C> get(R r) {
        return Optional.ofNullable(getOrNull(r));
    }

    /** @return key associated with value 'c' (if any) */
    Optional<R> getKeyFor(C c);

    boolean containsKey(R r);

    /** @return unordered stream of all currently-mapped keys.
     *  <br><em>There is no consistency guarantee between calls.</em>
     */
    Stream<R> streamKeys();

    /** @return unordered stream of all currently-mapped values.
     *  <br><em>There is no consistency guarantee between calls.</em>
     */
    Stream<C> streamValues();

    boolean isEmpty();

    ImmutableMap<R, C> snapshot();
}
