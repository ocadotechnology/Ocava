/*
 * Copyright © 2017-2026 Ocado (Ocava)
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.base.Preconditions;
import com.ocadotechnology.id.Identified;
import com.ocadotechnology.id.Identity;
import com.ocadotechnology.wrappers.Pair;

public class OneToOneJoinView<A extends Identified<A_ID>, A_ID, B extends Identified<B_ID>, B_ID, Z> {

    private final Function<Z, Optional<A>> zAMapping;
    private final Function<Z, Optional<B>> zBMapping;

    private final Function<A, Optional<Z>> aZMapping;
    private final Function<B, Optional<Z>> bZMapping;

    private final Map<Identity<A_ID>, Pair<A, B>> aIdToPair = new HashMap<>();
    private final Map<Identity<B_ID>, Pair<A, B>> bIdToPair = new HashMap<>();

    private final List<CacheStateChangeListener<Pair<A, B>>> stateChangeListeners = new ArrayList<>();
    private final List<CacheStateRemovedListener<Pair<A, B>>> stateRemovedListeners = new ArrayList<>();
    private final List<CacheStateAddedListener<Pair<A, B>>> stateAddedListeners = new ArrayList<>();

    /// Creates a [OneToOneJoinView] using [OptionalOneToOneIndex] instances to provide the
    /// bidirectional mappings between the join key type Z and types A and B.
    ///
    /// @param aCache           the listenable cache containing objects of type A
    /// @param bCache           the listenable cache containing objects of type B
    /// @param zAOneToOneIndex  an index mapping join keys of type Z to objects of type A
    /// @param zBOneToOneIndex  an index mapping join keys of type Z to objects of type B
    public OneToOneJoinView(StateChangeListenable<A> aCache, StateChangeListenable<B> bCache, OptionalOneToOneIndex<Z, A> zAOneToOneIndex, OptionalOneToOneIndex<Z, B> zBOneToOneIndex) {
        this(aCache, bCache, zAOneToOneIndex::get, zBOneToOneIndex::get, zAOneToOneIndex::getKeyFor, zBOneToOneIndex::getKeyFor);
    }

    /// Creates a `OneToOneJoinView` using explicit mapping functions to define the relationship
    /// between the join key type Z and the cached types A and B.
    /// The view registers itself as a state change listener on both caches.
    ///
    /// @param aCache     the listenable cache containing objects of type A
    /// @param bCache     the listenable cache containing objects of type B
    /// @param zAMapping  function to look up an A given a join key Z
    /// @param zBMapping  function to look up a B given a join key Z
    /// @param aZMapping  function to derive the join key Z from an A
    /// @param bZMapping  function to derive the join key Z from a B
    public OneToOneJoinView(
            StateChangeListenable<A> aCache,
            StateChangeListenable<B> bCache,
            Function<Z, Optional<A>> zAMapping,
            Function<Z, Optional<B>> zBMapping,
            Function<A, Optional<Z>> aZMapping,
            Function<B, Optional<Z>> bZMapping) {
        this.zAMapping = zAMapping;
        this.zBMapping = zBMapping;
        this.aZMapping = aZMapping;
        this.bZMapping = bZMapping;

        aCache.registerStateChangeListener(this::aHasChanged);
        bCache.registerStateChangeListener(this::bHasChanged);

        aCache.stream().forEach(this::aWasAdded);
    }

    private void aHasChanged(A previous, A updated) {
        Pair<A, B> oldMapping = previous != null ? aWasRemoved(previous) : null;
        Pair<A, B> updatedMapping = updated != null ? aWasAdded(updated) : null;
        updateStateChangeListeners(oldMapping, updatedMapping);
    }

    private void bHasChanged(B previous, B updated) {
        Pair<A, B> oldMapping = previous != null ? bWasRemoved(previous) : null;
        Pair<A, B> updatedMapping = updated != null ? bWasAdded(updated) : null;
        updateStateChangeListeners(oldMapping, updatedMapping);
    }

    private Pair<A, B> aWasRemoved(A a) {
        Preconditions.checkNotNull(a);
        Pair<A, B> removed = aIdToPair.remove(a.getId());
        if (removed != null) {
            bIdToPair.remove(removed.b.getId());
        }
        return removed;
    }

    private Pair<A, B> bWasRemoved(B b) {
        Preconditions.checkNotNull(b);
        Pair<A, B> removed = bIdToPair.remove(b.getId());
        if (removed != null) {
            aIdToPair.remove(removed.a.getId());
        }
        return removed;
    }

    private Pair<A, B> aWasAdded(A a) {
        return aZMapping.apply(a)
                .flatMap(zBMapping)
                .map(b -> add(a, b))
                .orElse(null);
    }

    private Pair<A, B> bWasAdded(B b) {
        return bZMapping.apply(b)
                .flatMap(zAMapping)
                .map(a -> add(a, b))
                .orElse(null);
    }

    private Pair<A, B> add(A a, B b) {
        Pair<A, B> newPair = Pair.of(a, b);
        Pair<A, B> putA = aIdToPair.put(a.getId(), newPair);
        Pair<A, B> putB = bIdToPair.put(b.getId(), newPair);
        Preconditions.checkState(putA == null, "Trying to add new pair [%s] to the OneToOneJoinView, but [%s] already exists in pair [%s].", newPair, a, putA);
        Preconditions.checkState(putB == null, "Trying to add new pair [%s] to the OneToOneJoinView, but [%s] already exists in pair [%s].", newPair, b, putB);
        return newPair;
    }

    private void updateStateChangeListeners(Pair<A, B> old, Pair<A, B> updated) {
        if (updated == null) {
            if (old != null) {
                stateRemovedListeners.forEach(l -> l.stateRemoved(old));
            }
            return;
        }
        if (old == null) {
            stateAddedListeners.forEach(l -> l.stateAdded(updated));
            return;
        }
        stateChangeListeners.forEach(l -> l.stateChanged(old, updated));
    }

    public Optional<A> getA(Identity<B_ID> bId) {
        return Optional.ofNullable(bIdToPair.get(bId)).map(p -> p.a);
    }

    public Optional<B> getB(Identity<A_ID> aId) {
        return Optional.ofNullable(aIdToPair.get(aId)).map(p -> p.b);
    }

    public void registerStateChangeListener(CacheStateChangeListener<Pair<A, B>> listener) {
        stateChangeListeners.add(listener);
    }

    public void registerStateAddedListener(CacheStateAddedListener<Pair<A, B>> stateAddedListener) {
        stateAddedListeners.add(stateAddedListener);
    }

    public void registerStateRemovedListener(CacheStateRemovedListener<Pair<A, B>> stateRemovedListener) {
        stateRemovedListeners.add(stateRemovedListener);
    }
}
