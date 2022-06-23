/*
 * Copyright © 2017-2022 Ocado (Ocava)
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.SimpleLongIdentified;

@DisplayName("A OneToOneIndex")
class OneToOneIndexTest {
    private static final String INDEX_NAME = "TEST_ONE_TO_ONE_INDEX";

    @Nested
    class CacheTypeTests extends IndexTests {
        @Override
        OneToOneIndex<CoordinateLikeTestObject, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            return cache.addOneToOneIndex(INDEX_NAME, TestState::getLocation);
        }
    }

    @Nested
    class CacheSubTypeTests extends IndexTests {
        @Override
        OneToOneIndex<CoordinateLikeTestObject, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            // IMPORTANT:
            // DO NOT inline indexFunction, as that will not fail to compile should addOneToOneIndex() require a type of
            // Function<TestState, Coordinate> instead of Function<? super TestState, Coordinate>, due to automatic type
            // coercion of the lambda.
            Function<LocationState, CoordinateLikeTestObject> indexFunction = LocationState::getLocation;
            return cache.addOneToOneIndex(INDEX_NAME, indexFunction);
        }
    }

    private abstract static class IndexTests {

        private IndexedImmutableObjectCache<TestState, TestState> cache;
        private OneToOneIndex<CoordinateLikeTestObject, TestState> index;

        abstract OneToOneIndex<CoordinateLikeTestObject, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache);

        @BeforeEach
        void init() {
            cache = IndexedImmutableObjectCache.createHashMapBackedCache();
            index = addIndexToCache(cache);
        }

        /**
         * Black-box tests which verify the behaviour of a OneToOneIndex as defined by the public API.
         */
        @Nested
        class BehaviourTests {
            @Test
            void add_whenFunctionReturnsNull_thenExceptionThrownOnAdd() {
                TestState stateOne = new TestState(Id.create(1), null);

                assertThatThrownBy(() -> cache.add(stateOne))
                        .isInstanceOf(NullPointerException.class);
            }

            @Test
            void add_whenMultipleTestStatesMapToTheSameCoordinate_thenExceptionThrownOnAdd() {
                TestState stateOne = new TestState(Id.create(1), CoordinateLikeTestObject.create(0, 0));
                TestState stateTwo = new TestState(Id.create(2), CoordinateLikeTestObject.create(0, 0));

                assertThatCode(() -> cache.add(stateOne)).doesNotThrowAnyException();
                assertThatThrownBy(() -> cache.add(stateTwo))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining(INDEX_NAME);
            }

            @Test
            void addAll_whenMultipleTestStatesMapToTheSameCoordinate_thenExceptionThrownOnAdd() {
                TestState stateOne = new TestState(Id.create(1), CoordinateLikeTestObject.create(0, 0));
                TestState stateTwo = new TestState(Id.create(2), CoordinateLikeTestObject.create(0, 0));

                assertThatThrownBy(() -> cache.addAll(ImmutableSet.of(stateOne, stateTwo)))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining(INDEX_NAME);
            }

            @Test
            void addAll_whenTestStateMapsToCoordinateMappedToBySomeThingElse_thenExceptionThrownOnAdd() {
                TestState stateOne = new TestState(Id.create(1), CoordinateLikeTestObject.create(0, 0));
                TestState stateTwo = new TestState(Id.create(2), CoordinateLikeTestObject.create(0, 1));
                TestState stateThree = new TestState(Id.create(3), CoordinateLikeTestObject.create(0, 0));

                assertThatCode(() -> cache.add(stateOne)).doesNotThrowAnyException();
                assertThatThrownBy(() -> cache.addAll(ImmutableSet.of(stateTwo, stateThree)))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining(INDEX_NAME);
            }

            @Test
            void addAll_whenLocationsAreSwappedTheyAreSwappedInIndex() {
                TestState stateOne = new TestState(Id.create(1), CoordinateLikeTestObject.create(0, 1));
                TestState stateTwo = new TestState(Id.create(2), CoordinateLikeTestObject.create(1, 0));

                cache.addAll(ImmutableSet.of(stateOne, stateTwo));

                Change<TestState> updateOne = Change.update(stateOne, new TestState(Id.create(1), CoordinateLikeTestObject.create(1, 0)));
                Change<TestState> updateTwo = Change.update(stateTwo, new TestState(Id.create(2), CoordinateLikeTestObject.create(0, 1)));

                cache.updateAll(ImmutableSet.of(updateOne, updateTwo));

                assertThat(index.get(CoordinateLikeTestObject.create(1, 0))).isEqualTo(stateOne);
                assertThat(index.get(CoordinateLikeTestObject.create(0, 1))).isEqualTo(stateTwo);
            }

            @Test
            void snapshot_whenIndexIsEmpty_returnsEmptySnapshot() {
                assertThat(index.snapshot()).isEmpty();
            }

            @Test
            void snapshot_whenNoChangesToCache_returnsSnapshotWithSameContent() {
                TestState testState = new TestState(Id.create(1), CoordinateLikeTestObject.ORIGIN);
                cache.add(testState);

                Object firstSnapshot = index.snapshot();
                Object secondSnapshot = index.snapshot();

                assertThat(firstSnapshot).isEqualTo(secondSnapshot);
            }

            @Test
            void snapshot_whenIndexAddedTo_returnsSnapshotWithThatElement() {
                index.snapshot();  // So call below is not first call
                TestState testState = new TestState(Id.create(1), CoordinateLikeTestObject.ORIGIN);
                cache.add(testState);

                assertThat(index.snapshot()).isEqualTo(ImmutableMap.of(testState.getLocation(), testState));
            }

            @Test
            void snapshot_whenIndexRemovedFrom_returnsSnapshotWithoutThatElement() {
                TestState stateOne = new TestState(Id.create(1), CoordinateLikeTestObject.create(0, 1));
                TestState stateTwo = new TestState(Id.create(2), CoordinateLikeTestObject.create(1, 0));
                cache.addAll(ImmutableSet.of(stateOne, stateTwo));
                index.snapshot();  // So call below is not first call

                cache.delete(stateOne.getId());

                assertThat(index.snapshot()).isEqualTo(ImmutableMap.of(stateTwo.getLocation(), stateTwo));
            }
        }

        /**
         * White-box tests which verify implementation details of OneToOneIndex that do not form part of the public API.
         * The behaviours verified by these tests are subject to change and should not be relied upon by users of the
         * OneToOneIndex class.
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
                TestState testState = new TestState(Id.create(1), CoordinateLikeTestObject.ORIGIN);
                cache.add(testState);

                Object firstSnapshot = index.snapshot();
                Object secondSnapshot = index.snapshot();

                assertThat(firstSnapshot).isSameAs(secondSnapshot);
            }

            @Test
            void snapshot_whenIndexAddedTo_newObjectReturned() {
                Object firstSnapshot = index.snapshot();

                TestState testState = new TestState(Id.create(1), CoordinateLikeTestObject.ORIGIN);
                cache.add(testState);
                Object secondSnapshot = index.snapshot();

                assertThat(firstSnapshot).isNotSameAs(secondSnapshot);
            }

            @Test
            void snapshot_whenIndexRemovedFrom_newObjectReturned() {
                TestState testState = new TestState(Id.create(1), CoordinateLikeTestObject.ORIGIN);
                cache.add(testState);

                Object firstSnapshot = index.snapshot();

                cache.delete(testState.getId());

                Object secondSnapshot = index.snapshot();
                assertThat(firstSnapshot).isNotSameAs(secondSnapshot);
            }
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
