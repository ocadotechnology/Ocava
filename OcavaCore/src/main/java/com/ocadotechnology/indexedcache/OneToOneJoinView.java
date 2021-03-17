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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.ocadotechnology.id.Identified;
import com.ocadotechnology.id.Identity;
import com.ocadotechnology.wrappers.Pair;

public class OneToOneJoinView<A extends Identified<A_ID>, A_ID, B extends Identified<B_ID>, B_ID, Z> {

    private final OptionalOneToOneIndex<Z, A> zAOneToOneIndex;
    private final OptionalOneToOneIndex<Z, B> zBOneToOneIndex;

    private final Map<Identity<A_ID>, Pair<A, B>> aIdToPair = new HashMap<>();
    private final Map<Identity<B_ID>, Pair<A, B>> bIdToPair = new HashMap<>();

    private final List<CacheStateChangeListener<Pair<A, B>>> stateChangeListeners = new ArrayList<>();
    private final List<CacheStateRemovedListener<Pair<A, B>>> stateRemovedListeners = new ArrayList<>();
    private final List<CacheStateAddedListener<Pair<A, B>>> stateAddedListeners = new ArrayList<>();

    public OneToOneJoinView(StateChangeListenable<A> aCache, StateChangeListenable<B> bCache, OptionalOneToOneIndex<Z, A> zAOneToOneIndex, OptionalOneToOneIndex<Z, B> zBOneToOneIndex) {
        this.zAOneToOneIndex = zAOneToOneIndex;
        this.zBOneToOneIndex = zBOneToOneIndex;

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
        return zAOneToOneIndex.getKeyFor(a)
                .flatMap(zBOneToOneIndex::get)
                .map(b -> add(a, b))
                .orElse(null);
    }

    private Pair<A, B> bWasAdded(B b) {
        return zBOneToOneIndex.getKeyFor(b)
                .flatMap(zAOneToOneIndex::get)
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

