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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.SimpleLongIdentified;

@DisplayName("An OptionalOneToManyIndex")
class OptionalOneToManyIndexTest {

    @Nested
    class CacheTypeTests extends IndexTests {
        @Override
        OptionalOneToManyIndex<CoordinateLikeTestObject, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            return cache.addOptionalOneToManyIndex(TestState::getLocation);
        }
    }

    @Nested
    class CacheSubTypeTests extends IndexTests {
        @Override
        OptionalOneToManyIndex<CoordinateLikeTestObject, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            // IMPORTANT:
            // DO NOT inline indexFunction, as that will not fail to compile should addOptionalOneToManyIndex() require a type
            // of Function<TestState, Optional<Coordinate>> instead of Function<? super TestState, Optional<Coordinate<>, due
            // to automatic type coercion of the lambda.
            Function<LocationState, Optional<CoordinateLikeTestObject>> indexFunction = LocationState::getLocation;
            return cache.addOptionalOneToManyIndex(indexFunction);
        }
    }

    private abstract static class IndexTests {

        private IndexedImmutableObjectCache<TestState, TestState> cache;
        private OptionalOneToManyIndex<CoordinateLikeTestObject, TestState> index;

        abstract OptionalOneToManyIndex<CoordinateLikeTestObject, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache);

        @BeforeEach
        void init() {
            cache = IndexedImmutableObjectCache.createHashMapBackedCache();
            index = addIndexToCache(cache);
        }

        /**
         * Black-box tests which verify the behaviour of an OptionalOneToManyIndex as defined by the public API.
         */
        @Nested
        class BehaviourTests {
            @Test
            void add_whenOptionalIsEmpty_thenStateNotIndexed() {
                cache.add(new TestState(Id.create(1), Optional.empty()));
                assertThat(index.streamKeys().mapToInt(index::count).sum()).isEqualTo(0);
            }

            @Test
            void add_whenOptionalIsPresent_thenStateIndexed() {
                TestState testState = new TestState(Id.create(1), Optional.of(CoordinateLikeTestObject.ORIGIN));
                cache.add(testState);

                assertThat(index.stream(CoordinateLikeTestObject.ORIGIN)).first().isEqualTo(testState);
            }

            @Test
            void snapshot_whenIndexIsEmpty_returnsEmptySnapshot() {
                assertThat(index.snapshot()).isEqualTo(ImmutableMultimap.of());
            }

            @Test
            void snapshot_whenOptionalIsPresent_returnsSnapshotWithSingleElement() {
                TestState testState = new TestState(Id.create(1), Optional.of(CoordinateLikeTestObject.ORIGIN));
                cache.add(testState);

                assertThat(index.snapshot().values()).containsOnly(testState);
            }

            @Test
            void snapshot_whenOptionalIsNotPresent_returnsSnapshotWithoutElement() {
                TestState stateOne = new TestState(Id.create(1), Optional.empty());
                TestState stateTwo = new TestState(Id.create(2), Optional.of(CoordinateLikeTestObject.ORIGIN));
                cache.addAll(ImmutableSet.of(stateOne, stateTwo));

                assertThat(index.snapshot().values()).containsOnly(stateTwo);
            }

            @Test
            void snapshot_whenIndexRemovedFrom_returnsSnapshotWithoutThatElement() {
                TestState stateOne = new TestState(Id.create(1), Optional.of(CoordinateLikeTestObject.create(0, 1)));
                TestState stateTwo = new TestState(Id.create(2), Optional.of(CoordinateLikeTestObject.create(1, 0)));
                cache.addAll(ImmutableSet.of(stateOne, stateTwo));
                index.snapshot();  // So call below is not first call

                cache.delete(stateOne.getId());

                assertThat(index.snapshot().values()).containsOnly(stateTwo);
            }
        }

        /**
         * White-box tests which verify implementation details of OptionalOneToManyIndex that do not form part of the
         * public API. The behaviours verified by these tests are subject to change and should not be relied upon by
         * users of the OptionalOneToManyIndex class.
         */
        @Nested
        class ImplementationTests {
            @Test
            void snapshot_whenNoChangesToEmptyCache_thenSameObjectReturned() {
                Object firstSnapshot = index.snapshot();
                Object secondSnapshot = index.snapshot();

                assertThat(firstSnapshot).isSameAs(secondSnapshot);
            }

            @Test
            void snapshot_whenNoChangesToNonEmptyCache_thenSameObjectReturned() {
                TestState testState = new TestState(Id.create(1), Optional.of(CoordinateLikeTestObject.ORIGIN));
                cache.add(testState);

                Object firstSnapshot = index.snapshot();
                Object secondSnapshot = index.snapshot();

                assertThat(firstSnapshot).isSameAs(secondSnapshot);
            }

            @Test
            void snapshot_whenIndexAddedTo_newObjectReturned() {
                Object firstSnapshot = index.snapshot();

                TestState testState = new TestState(Id.create(1), Optional.of(CoordinateLikeTestObject.ORIGIN));
                cache.add(testState);
                Object secondSnapshot = index.snapshot();

                assertThat(firstSnapshot).isNotSameAs(secondSnapshot);
            }

            @Test
            void snapshot_whenIndexRemovedFrom_newObjectReturned() {
                TestState testState = new TestState(Id.create(1), Optional.of(CoordinateLikeTestObject.ORIGIN));
                cache.add(testState);

                Object firstSnapshot = index.snapshot();

                cache.delete(testState.getId());

                Object secondSnapshot = index.snapshot();
                assertThat(firstSnapshot).isNotSameAs(secondSnapshot);
            }

            @Test
            void snapshot_whenIndexNotAddedTo_thenSameObjectReturned() {
                // Need to ensure a non-empty initial index, otherwise snapshot will always be ImmutableMultimap.of()
                TestState testState1 = new TestState(Id.create(1), Optional.of(CoordinateLikeTestObject.ORIGIN));
                TestState testState2 = new TestState(Id.create(2), Optional.empty());
                cache.add(testState1);

                Object firstSnapshot = index.snapshot();

                cache.add(testState2);

                Object secondSnapshot = index.snapshot();
                assertThat(firstSnapshot).isSameAs(secondSnapshot);
            }

            @Test
            void snapshot_whenIndexNotRemovedFrom_thenSameObjectReturned() {
                // Need to ensure a non-empty initial index, otherwise snapshot will always be ImmutableMultimap.of()
                TestState testState1 = new TestState(Id.create(1), Optional.of(CoordinateLikeTestObject.ORIGIN));
                TestState testState2 = new TestState(Id.create(2), Optional.empty());
                cache.add(testState1);
                cache.add(testState2);

                Object firstSnapshot = index.snapshot();

                cache.delete(testState2.getId());

                Object secondSnapshot = index.snapshot();
                assertThat(firstSnapshot).isSameAs(secondSnapshot);
            }
        }
    }

    interface LocationState {
        Optional<CoordinateLikeTestObject> getLocation();
    }

    private static final class TestState extends SimpleLongIdentified<TestState> implements LocationState {
        private final Optional<CoordinateLikeTestObject> location;

        private TestState(Id<TestState> id, Optional<CoordinateLikeTestObject> location) {
            super(id);
            this.location = location;
        }

        @Override
        public Optional<CoordinateLikeTestObject> getLocation() {
            return location;
        }
    }
}
