/*
 * Copyright © 2017 Ocado (Ocava)
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.SimpleLongIdentified;

@DisplayName("A OneToManyIndex")
class OneToManyIndexTest {

    @Nested
    class CacheTypeTests extends IndexTests {
        @Override
        OneToManyIndex<Coordinate, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            return cache.addOneToManyIndex(TestState::getLocation);
        }
    }

    @Nested
    class CacheSubTypeTests extends IndexTests {
        @Override
        OneToManyIndex<Coordinate, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            // IMPORTANT:
            // DO NOT inline indexFunction, as that will not fail to compile should addOneToManyIndex() require a type of
            // Function<TestState, Coordinate> instead of Function<? super TestState, Coordinate>, due to automatic type
            // coercion of the lambda.
            Function<LocationState, Coordinate> indexFunction = LocationState::getLocation;
            return cache.addOneToManyIndex(indexFunction);
        }
    }

    private abstract static class IndexTests {

        private IndexedImmutableObjectCache<TestState, TestState> cache;
        private OneToManyIndex<Coordinate, TestState> index;

        abstract OneToManyIndex<Coordinate, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache);

        @BeforeEach
        void init() {
            cache = IndexedImmutableObjectCache.createHashMapBackedCache();
            index = addIndexToCache(cache);
        }

        @Test
        void putOrUpdate_whenFunctionReturnsNull_thenThrowException() {
            assertThatThrownBy(() -> cache.add(new TestState(Id.create(1), null)))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void putOrUpdate_whenMultipleStatesHaveSameLocation_thenAllStatesReturnedInStreamOfIndexForLocation() {
            ImmutableSet<TestState> states = ImmutableSet.of(
                    new TestState(Id.create(1), Coordinate.ORIGIN),
                    new TestState(Id.create(2), Coordinate.ORIGIN),
                    new TestState(Id.create(3), Coordinate.ORIGIN));

            cache.addAll(states);
            assertThat(index.stream(Coordinate.ORIGIN))
                    .containsExactlyElementsOf(states);
        }

        @Test
        void remove_whenStateIsRemoved_thenItIsRemovedFromIndex() {
            TestState testState = new TestState(Id.create(1), Coordinate.ORIGIN);
            cache.add(testState);
            assertThat(index.stream(Coordinate.ORIGIN)).first().isEqualTo(testState);

            cache.delete(testState.getId());
            assertThat(index.count(Coordinate.ORIGIN)).isEqualTo(0);
        }

        @Test
        void remove_whenMultipleStatesAreRemoved_thenTheyAreAllRemovedFromIndex() {
            ImmutableSet<TestState> testStates = ImmutableSet.of(
                    new TestState(Id.create(1), Coordinate.ORIGIN),
                    new TestState(Id.create(2), Coordinate.ORIGIN),
                    new TestState(Id.create(3), Coordinate.ORIGIN));
            cache.addAll(testStates);
            assertThat(index.stream(Coordinate.ORIGIN))
                    .containsExactlyElementsOf(testStates);

            cache.deleteAll(testStates.stream().map(TestState::getId).collect(ImmutableSet.toImmutableSet()));
            assertThat(index.count(Coordinate.ORIGIN)).isEqualTo(0);
        }

        @Test
        void clear_whenStatesAreCleared_thenTheyAreClearedFromIndex() {
            ImmutableSet<TestState> testStates = ImmutableSet.of(
                    new TestState(Id.create(1), Coordinate.ORIGIN),
                    new TestState(Id.create(2), Coordinate.ORIGIN),
                    new TestState(Id.create(3), Coordinate.ORIGIN));
            cache.addAll(testStates);
            assertThat(index.stream(Coordinate.ORIGIN))
                    .containsExactlyElementsOf(testStates);

            cache.clear();
            assertThat(index.count(Coordinate.ORIGIN)).isEqualTo(0);
        }

        @Test
        void snapshot_whenEmpty_returnsEmptySnapshot() {
            ImmutableMultimap<Coordinate, TestState> firstSnapshot = index.snapshot();

            assertThat(firstSnapshot.keySet()).isEmpty();
        }

        @Test
        void snapshot_whenValuePresent_returnsSnapshotWithSingleElement() {
            TestState testState = new TestState(Id.create(1), Coordinate.ORIGIN);
            cache.add(testState);
            ImmutableMultimap<Coordinate, TestState> firstSnapshot = index.snapshot();

            assertThat(firstSnapshot.values()).containsOnly(testState);
        }

        @Test
        void snapshot_whenNoChangesToCache_thenSameObjectReturned() {
            ImmutableMultimap<Coordinate, TestState> firstSnapshot = index.snapshot();
            ImmutableMultimap<Coordinate, TestState> secondSnapshot = index.snapshot();

            assertThat(firstSnapshot).isSameAs(secondSnapshot);
        }

        @Test
        void snapshot_whenIndexAddedTo_newObjectReturned() {
            ImmutableMultimap<Coordinate, TestState> firstSnapshot = index.snapshot();

            TestState testState = new TestState(Id.create(1), Coordinate.ORIGIN);
            cache.add(testState);
            ImmutableMultimap<Coordinate, TestState> secondSnapshot = index.snapshot();

            assertThat(firstSnapshot).isNotSameAs(secondSnapshot);
        }

        @Test
        void snapshot_whenIndexRemovedFrom_newObjectReturned() {
            TestState testState = new TestState(Id.create(1), Coordinate.ORIGIN);
            cache.add(testState);

            ImmutableMultimap<Coordinate, TestState> firstSnapshot = index.snapshot();

            cache.delete(testState.getId());

            ImmutableMultimap<Coordinate, TestState> secondSnapshot = index.snapshot();
            assertThat(firstSnapshot).isNotSameAs(secondSnapshot);
        }
    }

    interface LocationState {
        Coordinate getLocation();
    }

    private static final class TestState extends SimpleLongIdentified<TestState> implements LocationState {
        private final Coordinate location;

        private TestState(Id<TestState> id, Coordinate location) {
            super(id);
            this.location = location;
        }

        @Override
        public Coordinate getLocation() {
            return location;
        }
    }
}
