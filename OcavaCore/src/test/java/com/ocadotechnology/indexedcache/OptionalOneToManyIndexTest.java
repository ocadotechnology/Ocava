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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
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
            // of Function<TestState, Optional<Coordinate>> instead of Function<? super TestState, Optional<Coordinate>>, due
            // to automatic type coercion of the lambda.
            Function<LocationState, Optional<CoordinateLikeTestObject>> indexFunction = LocationState::getLocation;
            return cache.addOptionalOneToManyIndex(indexFunction);
        }
    }

    @Nested
    class OptionalSubTypeOneToManyIndexTests extends IndexTests {
        @Override
        OptionalOneToManyIndex<CoordinateLikeTestObject, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            // IMPORTANT:
            // DO NOT inline indexFunction, as that will not fail to compile should addOptionalSubTypeOneToManyIndex()
            // require a type of Function<ExtendedTestState, Optional<CoordinateLikeTestObject>> instead of
            // Function<? super ExtendedTestState, Optional<CoordinateLikeTestObject>>, due to type coercion of the lambda.
            Function<LocationState, Optional<CoordinateLikeTestObject>> indexFunction = LocationState::getLocation;

            // This is unchecked, but we need to force the type of the index to match the return type of the function in
            // order to use the test suite.
            @SuppressWarnings("unchecked")
            OptionalOneToManyIndex<CoordinateLikeTestObject, TestState> subTypeIndex =
                    (OptionalOneToManyIndex<CoordinateLikeTestObject, TestState>) (OptionalOneToManyIndex<?, ?>)
                            cache.addOptionalSubTypeOneToManyIndex(null, ExtendedTestState.class, indexFunction);
            return subTypeIndex;
        }

        @Override
        TestState createState(long id, Optional<CoordinateLikeTestObject> location) {
            return new ExtendedTestState(Id.create(id), location);
        }

        @Test
        void add_whenBaseTypeStateAdded_thenStateNotIndexed() {
            cache.add(new TestState(Id.create(1), Optional.of(CoordinateLikeTestObject.ORIGIN)));

            assertThat(index.streamKeys().mapToInt(index::count).sum()).isEqualTo(0);
        }

        @Test
        void add_whenBaseAndSubTypeStatesAdded_thenOnlySubTypeStateIndexed() {
            cache.add(new TestState(Id.create(1), Optional.of(CoordinateLikeTestObject.ORIGIN)));
            ExtendedTestState extended = new ExtendedTestState(Id.create(2), Optional.of(CoordinateLikeTestObject.ORIGIN));
            cache.add(extended);

            assertThat(index.stream(CoordinateLikeTestObject.ORIGIN)).containsExactly(extended);
        }

        @Test
        void update_whenBaseTypeStateReplacedBySubType_thenThrowsException() {
            TestState baseState = new TestState(Id.create(1), Optional.of(CoordinateLikeTestObject.ORIGIN));
            cache.add(baseState);

            ExtendedTestState extended = new ExtendedTestState(Id.create(1), Optional.of(CoordinateLikeTestObject.ORIGIN));

            assertThatThrownBy(() -> cache.update(baseState, extended))
                    .isInstanceOf(CacheUpdateException.class)
                    .hasRootCauseInstanceOf(IndexUpdateException.class);

            assertThat(index.streamKeys().mapToInt(index::count).sum()).isEqualTo(0);
        }

        @Test
        void update_whenSubTypeStateReplacedByBaseType_thenThrowsException() {
            ExtendedTestState extended = new ExtendedTestState(Id.create(1), Optional.of(CoordinateLikeTestObject.ORIGIN));
            cache.add(extended);

            TestState baseState = new TestState(Id.create(1), Optional.of(CoordinateLikeTestObject.ORIGIN));

            assertThatThrownBy(() -> cache.update(extended, baseState))
                    .isInstanceOf(CacheUpdateException.class)
                    .hasRootCauseInstanceOf(IndexUpdateException.class);

            assertThat(index.stream(CoordinateLikeTestObject.ORIGIN)).containsExactly(extended);
        }
    }

    private abstract static class IndexTests {

        IndexedImmutableObjectCache<TestState, TestState> cache;
        OptionalOneToManyIndex<CoordinateLikeTestObject, TestState> index;

        abstract OptionalOneToManyIndex<CoordinateLikeTestObject, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache);

        TestState createState(long id, Optional<CoordinateLikeTestObject> location) {
            return new TestState(Id.create(id), location);
        }

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
                cache.add(createState(1, Optional.empty()));
                assertThat(index.streamKeys().mapToInt(index::count).sum()).isEqualTo(0);
            }

            @Test
            void add_whenOptionalIsPresent_thenStateIndexed() {
                TestState testState = createState(1, Optional.of(CoordinateLikeTestObject.ORIGIN));
                cache.add(testState);

                assertThat(index.stream(CoordinateLikeTestObject.ORIGIN)).first().isEqualTo(testState);
            }

            @Test
            void snapshot_whenIndexIsEmpty_returnsEmptySnapshot() {
                assertThat(index.snapshot()).isEqualTo(ImmutableMultimap.of());
            }

            @Test
            void snapshot_whenOptionalIsPresent_returnsSnapshotWithSingleElement() {
                TestState testState = createState(1, Optional.of(CoordinateLikeTestObject.ORIGIN));
                cache.add(testState);

                assertThat(index.snapshot().values()).containsOnly(testState);
            }

            @Test
            void snapshot_whenOptionalIsNotPresent_returnsSnapshotWithoutElement() {
                TestState stateOne = createState(1, Optional.empty());
                TestState stateTwo = createState(2, Optional.of(CoordinateLikeTestObject.ORIGIN));
                cache.addAll(ImmutableSet.of(stateOne, stateTwo));

                assertThat(index.snapshot().values()).containsOnly(stateTwo);
            }

            @Test
            void snapshot_whenIndexRemovedFrom_returnsSnapshotWithoutThatElement() {
                TestState stateOne = createState(1, Optional.of(CoordinateLikeTestObject.create(0, 1)));
                TestState stateTwo = createState(2, Optional.of(CoordinateLikeTestObject.create(1, 0)));
                cache.addAll(ImmutableSet.of(stateOne, stateTwo));
                index.snapshot();  // So call below is not first call

                cache.delete(stateOne.getId());

                assertThat(index.snapshot().values()).containsOnly(stateTwo);
            }

            @Test
            void forEach_appliesConsumerToEach() {
                TestState stateOne = createState(1, Optional.of(CoordinateLikeTestObject.create(0, 1)));
                TestState stateTwo = createState(2, Optional.of(CoordinateLikeTestObject.ORIGIN));
                TestState stateThree = createState(3, Optional.empty());
                cache.addAll(ImmutableSet.of(stateOne, stateTwo, stateThree));

                ArrayList<TestState> arrayList = new ArrayList<>();
                index.forEach(arrayList::add);

                assertEquals(2, arrayList.size());
                assertEquals(1, arrayList.get(0).getId().id);
                assertEquals(2, arrayList.get(1).getId().id);
            }

            @Test
            void forEachWithFilter_appliesConsumerToEach() {
                TestState stateOne = createState(1, Optional.of(CoordinateLikeTestObject.create(0, 1)));
                TestState stateTwo = createState(2, Optional.of(CoordinateLikeTestObject.ORIGIN));
                TestState stateThree = createState(3, Optional.empty());
                cache.addAll(ImmutableSet.of(stateOne, stateTwo, stateThree));

                ArrayList<TestState> arrayList = new ArrayList<>();
                index.forEach(CoordinateLikeTestObject.ORIGIN, arrayList::add);

                assertEquals(1, arrayList.size());
                assertEquals(2, arrayList.get(0).getId().id);
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
                TestState testState = createState(1, Optional.of(CoordinateLikeTestObject.ORIGIN));
                cache.add(testState);

                Object firstSnapshot = index.snapshot();
                Object secondSnapshot = index.snapshot();

                assertThat(firstSnapshot).isSameAs(secondSnapshot);
            }

            @Test
            void snapshot_whenIndexAddedTo_newObjectReturned() {
                Object firstSnapshot = index.snapshot();

                TestState testState = createState(1, Optional.of(CoordinateLikeTestObject.ORIGIN));
                cache.add(testState);
                Object secondSnapshot = index.snapshot();

                assertThat(firstSnapshot).isNotSameAs(secondSnapshot);
            }

            @Test
            void snapshot_whenIndexRemovedFrom_newObjectReturned() {
                TestState testState = createState(1, Optional.of(CoordinateLikeTestObject.ORIGIN));
                cache.add(testState);

                Object firstSnapshot = index.snapshot();

                cache.delete(testState.getId());

                Object secondSnapshot = index.snapshot();
                assertThat(firstSnapshot).isNotSameAs(secondSnapshot);
            }

            @Test
            void snapshot_whenIndexNotAddedTo_thenSameObjectReturned() {
                // Need to ensure a non-empty initial index, otherwise snapshot will always be ImmutableMultimap.of()
                TestState testState1 = createState(1, Optional.of(CoordinateLikeTestObject.ORIGIN));
                TestState testState2 = createState(2, Optional.empty());
                cache.add(testState1);

                Object firstSnapshot = index.snapshot();

                cache.add(testState2);

                Object secondSnapshot = index.snapshot();
                assertThat(firstSnapshot).isSameAs(secondSnapshot);
            }

            @Test
            void snapshot_whenIndexNotRemovedFrom_thenSameObjectReturned() {
                // Need to ensure a non-empty initial index, otherwise snapshot will always be ImmutableMultimap.of()
                TestState testState1 = createState(1, Optional.of(CoordinateLikeTestObject.ORIGIN));
                TestState testState2 = createState(2, Optional.empty());
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

    private static sealed class TestState extends SimpleLongIdentified<TestState> implements LocationState permits ExtendedTestState {
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

    private static final class ExtendedTestState extends TestState {
        private ExtendedTestState(Id<TestState> id, Optional<CoordinateLikeTestObject> location) {
            super(id, location);
        }
    }
}
