/*
 * Copyright © 2017-2019 Ocado (Ocava)
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

import com.google.common.collect.UnmodifiableIterator;
import com.ocadotechnology.id.Identified;

public interface StateChangeListenable<C extends Identified<?>> {
    /**
     * Register an index which will be notified whenever the contents of the cache changes. That is, when an object is
     * added, removed or updated. Indexes should not make any attempt to modify objects passed to them.
     */
    void registerCustomIndex(Index<? super C> index);

    @Deprecated
    void registerStateAddedOrRemovedListener(Consumer<? super C> consumer);

    /**
     * Register a listener which will be notified whenever an object in the cache is changed, including when it is added
     * to or removed from the cache. Listeners should not make any attempt to modify objects passed to them.
     */
    void registerStateChangeListener(CacheStateChangeListener<? super C> listener);

    /**
     * Register a listener which will be notified whenever an object is added with an ID that is not currently known to
     * the cache. Listeners should not make any attempt to modify objects passed to them.
     */
    void registerStateAddedListener(CacheStateAddedListener<? super C> listener);

    /**
     * Register a listener which will be notified whenever an object is removed from the cache. Listeners should not
     * make any attempt to modify objects passed to them.
     */
    void registerStateRemovedListener(CacheStateRemovedListener<? super C> listener);

    Stream<C> stream();

    default void forEach(Consumer<C> action) {
        stream().forEach(action);
    }

    UnmodifiableIterator<C> iterator();

    /**
     * Register a listener which will be notified whenever an object in the cache changes, including when it is added to
     * or removed from the cache. Listeners should not make any attempt to modify objects passed to them.
     */
    void registerAtomicStateChangeListener(AtomicStateChangeListener<? super C> listener);

    /**
     * Removal by Object (reference) equality.
     */
    void removeStateChangeListener(CacheStateChangeListener<? super C> listener);
}
