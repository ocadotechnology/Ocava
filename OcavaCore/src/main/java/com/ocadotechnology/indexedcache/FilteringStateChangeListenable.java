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

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.CheckForNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.ocadotechnology.id.Identified;

/** A (mapping and) filtering listeneable.
 *  <br>
 *  Example usage:<br>
 *    Suppose you have a cache containing multiple types (common baseclass T) A, B, C,
 *    and you wish to be notified about changes only to C.
 *  <pre>
 *      IndexImmutableObjectCache&lt;T&gt; cache;
 *
 *      StateCacheListener&lt;C&gt; cListener;
 *      (new FilteringStateCacheListenable&lt;I,T,C&gt;(cache, t -&gt; (t instanceof C) ? (C)t : null);
 *  </pre>
 *  <code>cListener</code> will only be notified of changes in the cache to objects of type C.
 *
 *  <br>
 *  <em>Note: Custom indexes and removing listeners are not supported.</em>
 */
public class FilteringStateChangeListenable<T extends Identified<?>, C extends Identified<?>> implements StateChangeListenable<C> {
    private final StateChangeListenable<T> backingListenable;
    private final Function<? super T, C> mapAndFilter;

    /** @param mapAndFilter may (optionally) convert the backing objects to a wider/narrower scope.
     *  Argument is non-null, but return may be null, and is interpreted as "not present".
     *  It is, therefore, possible that several updatesto the backing listenable (cache)
     *  will cause additions and removals to be propagated.
     */
    public FilteringStateChangeListenable(StateChangeListenable<T> backingListenable, Function<? super T, C> mapAndFilter) {
        this.backingListenable = backingListenable; // cache;
        this.mapAndFilter = t -> t != null ? mapAndFilter.apply(t) : null;
    }

    /** Custom indexes are not supported for Filtering listenable.<br>
     *  Use the backing listenable directly and implement the filtering within the (custom) index.
     */
    @Override
    public <T extends Index<? super C>> T registerCustomIndex(T index) {
        throw new UnsupportedOperationException("registerCustomIndex");
    }

    @Override
    public void registerStateAddedOrRemovedListener(Consumer<? super C> consumer) {
        backingListenable.registerStateChangeListener((old, updated) -> {
            C oldPlannable = mapAndFilter.apply(old);
            C updatedPlannable = mapAndFilter.apply(updated);
            if (oldPlannable == null && updatedPlannable != null) {
                consumer.accept(updatedPlannable);
            } else if (oldPlannable != null && updatedPlannable == null) {
                consumer.accept(oldPlannable);
            }
        });
    }

    @Override
    public void registerStateChangeListener(CacheStateChangeListener<? super C> listener) {
        backingListenable.registerStateChangeListener((old, updated) -> {
            C oldPlannable = mapAndFilter.apply(old);
            C updatedPlannable = mapAndFilter.apply(updated);
            if (oldPlannable != null || updatedPlannable != null) {
                listener.stateChanged(oldPlannable, updatedPlannable);
            }
        });
    }

    @Override
    public void registerStateAddedListener(CacheStateAddedListener<? super C> listener) {
        backingListenable.registerStateChangeListener((old, updated) -> {
            C oldPlannable = mapAndFilter.apply(old);
            C updatedPlannable = mapAndFilter.apply(updated);
            if (oldPlannable == null && updatedPlannable != null) {
                listener.stateAdded(updatedPlannable);
            }
        });
    }

    @Override
    public void registerStateRemovedListener(CacheStateRemovedListener<? super C> listener) {
        backingListenable.registerStateChangeListener((old, updated) -> {
            C oldPlannable = mapAndFilter.apply(old);
            C updatedPlannable = mapAndFilter.apply(updated);
            if (oldPlannable != null && updatedPlannable == null) {
                listener.stateRemoved(oldPlannable);
            }
        });
    }

    @Override
    public Stream<C> stream() {
        return backingListenable.stream().map(mapAndFilter).filter(Objects::nonNull);
    }

    @Override
    public UnmodifiableIterator<C> iterator() {
        return new UnmodifiableIterator<C>() {
            private final UnmodifiableIterator<T> backingIterator = backingListenable.iterator();

            private @CheckForNull C nextValue = backingNext();

            @Override
            public boolean hasNext() {
                return nextValue != null;
            }

            @Override
            public C next() {
                C c = nextValue;
                if (c == null) {
                    throw new NoSuchElementException();
                }
                nextValue = backingNext();
                return c;
            }

            private C backingNext() {
                C c = null;
                while (c == null && backingIterator.hasNext()) {
                    c = mapAndFilter.apply(backingIterator.next());
                }
                return c;
            }
        };
    }

    @Override
    public void registerAtomicStateChangeListener(AtomicStateChangeListener<? super C> listener) {
        backingListenable.registerAtomicStateChangeListener(changes -> {
            ImmutableList<Change<C>> plannableChanges = StreamSupport.stream(changes.spliterator(), false)
                    .map(this::wrapChange)
                    .filter(Objects::nonNull)
                    .collect(ImmutableList.toImmutableList());
            ((AtomicStateChangeListener<C>)listener).stateChanged(plannableChanges);  // cast needed because of "? super"
        });
    }

    /** Remove listener not supported for filtering listenable<br>
     *  Use the backing listenable and implement the filtering directly.
     */
    @Override
    public void removeStateChangeListener(CacheStateChangeListener<? super C> listener) {
        // No easy way to do this: store a map of listener -> wrappedListener?
        // Or, have the backing listenable able to search through and find the correct wrapper?
        throw new UnsupportedOperationException("removeStateChangeListener");
    }

    private @CheckForNull Change<C> wrapChange(Change<T> change) {
        C updatedPlannableState = mapAndFilter.apply(change.newObject);
        C oldPlannableState = mapAndFilter.apply(change.originalObject);
        return oldPlannableState != null || updatedPlannableState != null
                ? Change.change(oldPlannableState, updatedPlannableState)
                : null;
    }
}
