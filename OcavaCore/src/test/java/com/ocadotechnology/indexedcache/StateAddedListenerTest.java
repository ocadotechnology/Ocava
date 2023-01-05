/*
 * Copyright © 2017-2023 Ocado (Ocava)
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
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.SimpleLongIdentified;

class StateAddedListenerTest {

    @Nested
    class CacheTypeTests extends ListenerTests {
        @Override
        TestListener<TestState> addListener(IndexedImmutableObjectCache<TestState, TestState> cache) {
            TestListener<TestState> listener = new TestListener<>();
            cache.registerStateAddedListener(listener);
            return listener;
        }
    }

    @Nested
    class CacheSubTypeTests extends ListenerTests {
        @Override
        TestListener<LocationState> addListener(IndexedImmutableObjectCache<TestState, TestState> cache) {
            // IMPORTANT:
            // DO NOT inline listener, as that will not fail to compile should registerStateAddedListener() require a type of
            // CacheStateAddedListener<TestState> instead of CacheStateAddedListener<? super TestState>, due to automatic type
            // coercion of the lambda.
            TestListener<LocationState> listener = new TestListener<>();
            cache.registerStateAddedListener(listener);
            return listener;
        }
    }

    private abstract static class ListenerTests {
        private IndexedImmutableObjectCache<TestState, TestState> cache;
        private TestListener<? super TestState> stateAddedListener;

        abstract TestListener<? super TestState> addListener(IndexedImmutableObjectCache<TestState, TestState> cache);

        @BeforeEach
        void init() {
            cache = IndexedImmutableObjectCache.createHashMapBackedCache();
            stateAddedListener = addListener(cache);
        }

        @Test
        void stateAdded_nullState_listenerNotCalled() {
            addToCache((TestState)null);
            assertThat(stateAddedListener.statesAdded).isEmpty();
        }

        @Test
        void stateAdded_nonNullState_listenerCalledOnce() {
            TestState state = new TestState(Id.create(1), CoordinateLikeTestObject.create(0, 0));
            addToCache(state);
            assertThat(stateAddedListener.hasReceived(state)).isTrue();
        }

        @Test
        void stateAdded_repeatedState_listenerCalledOnce() {
            TestState state = new TestState(Id.create(1), CoordinateLikeTestObject.create(0, 0));
            addToCache(state, state);
            assertThat(stateAddedListener.hasReceived(state)).isTrue();
        }

        @Test
        void stateAdded_multipleStates_listenerCalledForEachInOrder() {
            TestState state1 = new TestState(Id.create(1), CoordinateLikeTestObject.create(0, 0));
            TestState state2 = new TestState(Id.create(2), CoordinateLikeTestObject.create(0, 0));
            TestState state3 = new TestState(Id.create(3), CoordinateLikeTestObject.create(0, 0));
            addToCache(state1, state2, state3);
            assertThat(stateAddedListener.hasReceived(state1, state2, state3)).isTrue();
        }

        @Test
        void stateUpdated_nullState_listenerNotCalled() {
            addToCacheViaUpdate((TestState)null);
            assertThat(stateAddedListener.statesAdded).isEmpty();
        }

        @Test
        void stateUpdated_nonNullState_listenerCalledOnce() {
            TestState state = new TestState(Id.create(1), CoordinateLikeTestObject.create(0, 0));
            addToCacheViaUpdate(state);
            assertThat(stateAddedListener.hasReceived(state)).isTrue();
        }

        @Test
        void stateUpdated_repeatedState_listenerCalledOnce() {
            TestState state = new TestState(Id.create(1), CoordinateLikeTestObject.create(0, 0));
            addToCacheViaUpdate(state, state);
            assertThat(stateAddedListener.hasReceived(state)).isTrue();
        }

        @Test
        void stateUpdated_multipleStates_listenerCalledForEachInOrder() {
            TestState state1 = new TestState(Id.create(1), CoordinateLikeTestObject.create(0, 0));
            TestState state2 = new TestState(Id.create(2), CoordinateLikeTestObject.create(0, 0));
            TestState state3 = new TestState(Id.create(3), CoordinateLikeTestObject.create(0, 0));
            addToCacheViaUpdate(state1, state2, state3);
            assertThat(stateAddedListener.hasReceived(state1, state2, state3)).isTrue();
        }

        private void addToCache(TestState... states) {
            doForStates(cache::add, states);
        }

        private void addToCacheViaUpdate(TestState... states) {
            doForStates(state -> cache.update(null, state), states);
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

    private static final class TestListener<T> implements CacheStateAddedListener<T> {
        final List<T> statesAdded = new ArrayList<>();

        @Override
        public void stateAdded(T addedState) {
            statesAdded.add(addedState);
        }

        boolean hasReceived(T... states) {
            return statesAdded.equals(Arrays.asList(states));
        }
    }

    private interface LocationState {
        CoordinateLikeTestObject getLocation();
    }

    private static class TestState extends SimpleLongIdentified<TestState> implements LocationState {
        private final CoordinateLikeTestObject location;

        private TestState(Id<TestState> id, CoordinateLikeTestObject location) {
            super(id);
            this.location = location;
        }

        @Override
        public CoordinateLikeTestObject getLocation() {
            return location;
        }
    }
}
