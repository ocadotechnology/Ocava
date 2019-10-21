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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.SimpleLongIdentified;

class StateChangedListenerTest {

    @Nested
    class CacheTypeTests extends ListenerTests {
        @Override
        TestListener<TestState> addListener(IndexedImmutableObjectCache<TestState, TestState> cache) {
            TestListener<TestState> listener = new TestListener<>();
            cache.registerStateChangeListener(listener);
            return listener;
        }
    }

    @Nested
    class CacheSubTypeTests extends ListenerTests {
        @Override
        TestListener<LocationState> addListener(IndexedImmutableObjectCache<TestState, TestState> cache) {
            // IMPORTANT:
            // DO NOT inline listener, as that will not fail to compile should registerStateAddedListener() require a type of
            // CacheStateChangeListener<TestState> instead of CacheStateChangeListener<? super TestState>, due to automatic
            // type coercion of the lambda.
            TestListener<LocationState> listener = new TestListener<>();
            cache.registerStateChangeListener(listener);
            return listener;
        }
    }

    private abstract static class ListenerTests {
        private IndexedImmutableObjectCache<TestState, TestState> cache;
        private TestListener<? super TestState> stateChangedListener;

        abstract TestListener<? super TestState> addListener(IndexedImmutableObjectCache<TestState, TestState> cache);
        
        @BeforeEach
        void init() {
            cache = IndexedImmutableObjectCache.createHashMapBackedCache();
            stateChangedListener = addListener(cache);
        }

        @Test
        void stateAdded_nullState_listenerNotCalled() {
            addToCache((TestState)null);
            assertThat(stateChangedListener.statesUpdated).isEmpty();
        }

        @Test
        void stateAdded_nonNullState_listenerCalledOnce() {
            TestState state = new TestState(Id.create(1), Coordinate.create(0, 0));
            addToCache(state);
            assertThat(stateChangedListener.hasReceived(Update.of(null, state))).isTrue();
        }

        @Test
        void stateAdded_repeatedState_listenerCalledOnce() {
            TestState state = new TestState(Id.create(1), Coordinate.create(0, 0));
            addToCache(state, state);
            assertThat(stateChangedListener.hasReceived(Update.of(null, state))).isTrue();
        }

        @Test
        void stateAdded_multipleStates_listenerCalledForEachInOrder() {
            TestState state1 = new TestState(Id.create(1), Coordinate.create(0, 0));
            TestState state2 = new TestState(Id.create(2), Coordinate.create(0, 0));
            TestState state3 = new TestState(Id.create(3), Coordinate.create(0, 0));
            addToCache(state1, state2, state3);
            assertThat(stateChangedListener.hasReceived(
                    Update.of(null, state1),
                    Update.of(null, state2),
                    Update.of(null, state3)
            )).isTrue();
        }

        @Test
        void stateAddedByUpdate_nullState_listenerNotCalled() {
            addToCacheViaUpdate((TestState)null);
            assertThat(stateChangedListener.statesUpdated).isEmpty();
        }

        @Test
        void stateAddedByUpdate_nonNullState_listenerCalledOnce() {
            TestState state = new TestState(Id.create(1), Coordinate.create(0, 0));
            addToCacheViaUpdate(state);
            assertThat(stateChangedListener.hasReceived(Update.of(null, state))).isTrue();
        }

        @Test
        void stateAddedByUpdate_repeatedState_listenerCalledOnce() {
            TestState state = new TestState(Id.create(1), Coordinate.create(0, 0));
            addToCacheViaUpdate(state, state);
            assertThat(stateChangedListener.hasReceived(Update.of(null, state))).isTrue();
        }

        @Test
        void stateAddedByUpdate_multipleStates_listenerCalledForEachInOrder() {
            TestState state1 = new TestState(Id.create(1), Coordinate.create(0, 0));
            TestState state2 = new TestState(Id.create(2), Coordinate.create(0, 0));
            TestState state3 = new TestState(Id.create(3), Coordinate.create(0, 0));
            addToCacheViaUpdate(state1, state2, state3);
            assertThat(stateChangedListener.hasReceived(
                    Update.of(null, state1),
                    Update.of(null, state2),
                    Update.of(null, state3)
            )).isTrue();
        }

        @Test
        void stateRemoved_nullState_listenerNotCalled() {
            addToCache((TestState)null);
            removeFromCache((TestState)null);
            assertThat(stateChangedListener.statesUpdated).isEmpty();
        }

        @Test
        void stateRemoved_nonNullState_listenerCalledOnce() {
            TestState state = new TestState(Id.create(1), Coordinate.create(0, 0));
            addToCache(state);
            stateChangedListener.clear();
            removeFromCache(state);
            assertThat(stateChangedListener.hasReceived(Update.of(state, null))).isTrue();
        }

        @Test
        void stateRemoved_nonNullState_stateNotInCache_listenerNotCalled() {
            TestState state = new TestState(Id.create(1), Coordinate.create(0, 0));
            removeFromCache(state);
            assertThat(stateChangedListener.statesUpdated).isEmpty();
        }

        @Test
        void stateRemoved_repeatedState_listenerCalledOnce() {
            TestState state = new TestState(Id.create(1), Coordinate.create(0, 0));
            addToCache(state);
            stateChangedListener.clear();
            removeFromCache(state, state);
            assertThat(stateChangedListener.hasReceived(Update.of(state, null))).isTrue();
        }

        @Test
        void stateRemoved_multipleStates_listenerCalledForEachInOrder() {
            TestState state1 = new TestState(Id.create(1), Coordinate.create(0, 0));
            TestState state2 = new TestState(Id.create(2), Coordinate.create(0, 0));
            TestState state3 = new TestState(Id.create(3), Coordinate.create(0, 0));
            addToCache(state1, state2, state3);
            stateChangedListener.clear();
            removeFromCache(state1, state2, state3);
            assertThat(stateChangedListener.hasReceived(
                    Update.of(state1, null),
                    Update.of(state2, null),
                    Update.of(state3, null)
            )).isTrue();
        }

        @Test
        void stateRemovedByUpdate_nullState_listenerNotCalled() {
            addToCache((TestState)null);
            removeFromCacheViaUpdate((TestState)null);
            assertThat(stateChangedListener.statesUpdated).isEmpty();
        }

        @Test
        void stateRemovedByUpdate_nonNullState_listenerCalledOnce() {
            TestState state = new TestState(Id.create(1), Coordinate.create(0, 0));
            addToCache(state);
            stateChangedListener.clear();
            removeFromCacheViaUpdate(state);
            assertThat(stateChangedListener.hasReceived(Update.of(state, null))).isTrue();
        }

        @Test
        void stateRemovedByUpdate_nonNullState_stateNotInCache_listenerNotCalled() {
            TestState state = new TestState(Id.create(1), Coordinate.create(0, 0));
            removeFromCacheViaUpdate(state);
            assertThat(stateChangedListener.statesUpdated).isEmpty();
        }

        @Test
        void stateRemovedByUpdate_repeatedState_listenerCalledOnce() {
            TestState state = new TestState(Id.create(1), Coordinate.create(0, 0));
            addToCache(state);
            stateChangedListener.clear();
            removeFromCacheViaUpdate(state, state);
            assertThat(stateChangedListener.hasReceived(Update.of(state, null))).isTrue();
        }

        @Test
        void stateRemovedByUpdate_multipleStates_listenerCalledForEachInOrder() {
            TestState state1 = new TestState(Id.create(1), Coordinate.create(0, 0));
            TestState state2 = new TestState(Id.create(2), Coordinate.create(0, 0));
            TestState state3 = new TestState(Id.create(3), Coordinate.create(0, 0));
            addToCache(state1, state2, state3);
            stateChangedListener.clear();
            removeFromCacheViaUpdate(state1, state2, state3);
            assertThat(stateChangedListener.hasReceived(
                    Update.of(state1, null),
                    Update.of(state2, null),
                    Update.of(state3, null)
            )).isTrue();
        }

        @Test
        void stateUpdated_nullState_listenerNotCalled() {
            addToCache((TestState)null);
            updateStatesInCache((TestState)null);
            assertThat(stateChangedListener.statesUpdated).isEmpty();
        }

        @Test
        void stateUpdated_nonNullState_listenerCalledOnce() {
            TestState initialState = new TestState(Id.create(1), Coordinate.create(0, 0));
            TestState updatedState = initialState.withLocation(Coordinate.create(0, 1));
            addToCache(initialState);
            stateChangedListener.clear();
            updateStatesInCache(updatedState);
            assertThat(stateChangedListener.hasReceived(Update.of(initialState, updatedState))).isTrue();
        }

        @Test
        void stateUpdated_nonNullState_stateNotInCache_listenerNotCalled() {
            TestState initialState = new TestState(Id.create(1), Coordinate.create(0, 0));
            TestState updatedState = initialState.withLocation(Coordinate.create(0, 1));
            updateStatesInCache(updatedState);
            assertThat(stateChangedListener.statesUpdated).isEmpty();
        }

        @Test
        void stateUpdated_repeatedState_listenerCalledTwiceWithDifferentStatePairs() {
            TestState initialState = new TestState(Id.create(1), Coordinate.create(0, 0));
            TestState updatedState = initialState.withLocation(Coordinate.create(0, 1));
            addToCache(initialState);
            stateChangedListener.clear();
            updateStatesInCache(updatedState, updatedState);
            assertThat(stateChangedListener.hasReceived(
                    Update.of(initialState, updatedState),
                    Update.of(updatedState, updatedState)
            )).isTrue();
        }

        @Test
        void stateUpdated_noChange_listenerNotCalled() {
            TestState state = new TestState(Id.create(1), Coordinate.create(0, 0));
            addToCache(state);
            stateChangedListener.clear();
            updateStatesInCache(state);
            assertThat(stateChangedListener.hasReceived(Update.of(state, state))).isTrue();
        }

        @Test
        void stateUpdated_multipleStates_listenerCalledForEachInOrder() {
            TestState initialState1 = new TestState(Id.create(1), Coordinate.create(0, 0));
            TestState initialState2 = new TestState(Id.create(2), Coordinate.create(0, 0));
            TestState initialState3 = new TestState(Id.create(3), Coordinate.create(0, 0));
            TestState updatedState1 = initialState1.withLocation(Coordinate.create(0, 1));
            TestState updatedState2 = initialState2.withLocation(Coordinate.create(0, 1));
            TestState updatedState3 = initialState3.withLocation(Coordinate.create(0, 1));
            
            addToCache(initialState1, initialState2, initialState3);
            stateChangedListener.clear();
            updateStatesInCache(updatedState1, updatedState2, updatedState3);
            assertThat(stateChangedListener.hasReceived(
                    Update.of(initialState1, updatedState1),
                    Update.of(initialState2, updatedState2),
                    Update.of(initialState3, updatedState3)
            )).isTrue();
        }

        private void addToCache(TestState... states) {
            doForStates(cache::add, states);
        }

        private void addToCacheViaUpdate(TestState... states) {
            doForStates(state -> cache.update(null, state), states);
        }

        private void removeFromCache(TestState... states) {
            doForStates(state -> cache.delete(state == null ? null : state.getId()), states);
        }
        
        private void removeFromCacheViaUpdate(TestState... states) {
            doForStates(state -> cache.update(state, null), states);
        }
        
        private void updateStatesInCache(TestState... states) {
            doForStates(this::updateStateInCache, states);
        }

        private void updateStateInCache(TestState updatedState) {
            // Where possible, avoid passing in null as the current state, as that triggers a deletion
            Id<TestState> id = updatedState == null ? null : updatedState.getId();
            TestState currentState = cache.get(id);
            if (currentState == null) {
                currentState = updatedState;
            }
            cache.update(currentState, updatedState);
        }
        
        private void doForStates(Consumer<TestState> action, TestState... states) {
            for (TestState state : states) {
                try {
                    action.accept(state);
                } catch (Exception e) {
                    // ignore any exceptions thrown
                }
            }
        }
    }
    
    private static final class TestListener<T> implements CacheStateChangeListener<T> {
        final List<Update<T>> statesUpdated = new ArrayList<>();

        @Override
        public void stateChanged(T oldState, T updatedState) {
            statesUpdated.add(Update.of(oldState, updatedState));
        }

        @SafeVarargs
        final boolean hasReceived(Update<T>... updates) {
            return statesUpdated.equals(Arrays.asList(updates));
        }
        
        void clear() {
            statesUpdated.clear();
        }
    }
    
    private static final class Update<T> {
        private final T original;
        private final T updated;

        private Update(T original, T updated) {
            this.original = original;
            this.updated = updated;
        }
        
        static <T> Update<T> of(T original, T updated) {
            return new Update<>(original, updated);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Update<?> update = (Update<?>) o;
            return Objects.equals(original, update.original)
                    && Objects.equals(updated, update.updated);
        }

        @Override
        public int hashCode() {
            return Objects.hash(original, updated);
        }
    }
    
    private interface LocationState {
        Coordinate getLocation();
    }

    private static class TestState extends SimpleLongIdentified<TestState> implements LocationState {
        private final Coordinate location;

        private TestState(Id<TestState> id, Coordinate location) {
            super(id);
            this.location = location;
        }

        @Override
        public Coordinate getLocation() {
            return location;
        }
        
        public TestState withLocation(Coordinate location) {
            return new TestState(getId(), location);
        }
    }
}
