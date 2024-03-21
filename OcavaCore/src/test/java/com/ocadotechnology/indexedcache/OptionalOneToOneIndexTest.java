/*
 * Copyright © 2017-2024 Ocado (Ocava)
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.Identity;
import com.ocadotechnology.id.SimpleLongIdentified;
import com.ocadotechnology.indexedcache.IndexedImmutableObjectCache.Hints;

@DisplayName("An OptionalOneToOneIndex")
class OptionalOneToOneIndexTest {
    private static final String INDEX_NAME = "TEST_OPTIONAL_ONE_TO_ONE_INDEX";

    @Nested
    class CacheTypeWithUpdateHintTests extends IndexTests {
        @Override
        OptionalOneToOneIndex<CoordinateLikeTestObject, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            return cache.addOptionalOneToOneIndex(INDEX_NAME, TestState::getLocation, Hints.optimiseForUpdate);
        }
    }

    @Nested
    class CacheTypeWithQueryHintTests extends IndexTests {
        @Override
        OptionalOneToOneIndex<CoordinateLikeTestObject, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            return cache.addOptionalOneToOneIndex(INDEX_NAME, TestState::getLocation, Hints.optimiseForQuery);
        }
    }

    @Nested
    class CacheSubTypeWithUpdateHintTests extends IndexTests {
        @Override
        OptionalOneToOneIndex<CoordinateLikeTestObject, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            // IMPORTANT:
            // DO NOT inline indexFunction, as that will not fail to compile should addOptionalOneToOneIndex() require a type
            // of Function<TestState, Optional<Coordinate>> instead of Function<? super TestState, Optional<Coordinate<>, due
            // to automatic type coercion of the lambda.
            Function<LocationState, Optional<CoordinateLikeTestObject>> indexFunction = LocationState::getLocation;
            return cache.addOptionalOneToOneIndex(INDEX_NAME, indexFunction, Hints.optimiseForUpdate);
        }
    }

    @Nested
    class CacheSubTypeWithQueryHintTests extends IndexTests {
        @Override
        OptionalOneToOneIndex<CoordinateLikeTestObject, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            // IMPORTANT:
            // DO NOT inline indexFunction, as that will not fail to compile should addOptionalOneToOneIndex() require a type
            // of Function<TestState, Optional<Coordinate>> instead of Function<? super TestState, Optional<Coordinate<>, due
            // to automatic type coercion of the lambda.
            Function<LocationState, Optional<CoordinateLikeTestObject>> indexFunction = LocationState::getLocation;
            return cache.addOptionalOneToOneIndex(INDEX_NAME, indexFunction, Hints.optimiseForQuery);
        }
    }

    private abstract static class IndexTests {

        private IndexedImmutableObjectCache<TestState, TestState> cache;
        private OptionalOneToOneIndex<CoordinateLikeTestObject, TestState> index;

        abstract OptionalOneToOneIndex<CoordinateLikeTestObject, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache);

        @BeforeEach
        void init() {
            cache = IndexedImmutableObjectCache.createHashMapBackedCache();
            index = addIndexToCache(cache);
        }

        /**
         * Black-box tests which verify the behaviour of an OptionalOneToOneIndex as defined by the public API.
         */
        @Nested
        class BehaviourTests {
            @Test
            void add_whenOptionalIsEmpty_thenStateNotIndexed() {
                cache.add(new TestState(Id.create(1), Optional.empty()));
                assertThat(index.streamKeys().count()).isEqualTo(0);
            }

            @Test
            void add_whenOptionalIsPresent_thenStateIndexed() {
                TestState testState = new TestState(Id.create(1), Optional.of(CoordinateLikeTestObject.ORIGIN));
                cache.add(testState);

                assertThat(index.getOrNull(CoordinateLikeTestObject.ORIGIN)).isEqualTo(testState);
            }

            @Test
            void update_addAndRemoveAreIdempotent() {
                TestState original = new TestState(Id.create(1), Optional.of(CoordinateLikeTestObject.ORIGIN));
                assertThat(index.streamKeys().count()).isEqualTo(0);
                cache.add(original);
                assertThat(index.streamKeys().count()).isEqualTo(1);

                TestState sameIdNoCoordinate = new TestState(Id.create(1), Optional.empty());
                cache.update(original, sameIdNoCoordinate);
                assertThat(index.streamKeys().count()).isEqualTo(0);

                cache.update(sameIdNoCoordinate, original);
                assertThat(index.streamKeys().count()).isEqualTo(1);
            }

            @Test
            void update_whenKeyChangedValueIsUpdated() {
                TestState original = new TestState(Id.create(1), Optional.of(CoordinateLikeTestObject.ORIGIN));
                cache.add(original);

                TestState sameIdChangedCoordinate = new TestState(Id.create(1), Optional.of(CoordinateLikeTestObject.create(1, 1)));
                cache.update(original, sameIdChangedCoordinate);
                assertThat(index.streamKeys().count()).isEqualTo(1);
                assertThat(index.get(sameIdChangedCoordinate.getLocation().get()).get()).isEqualTo(sameIdChangedCoordinate);
            }

            @Test
            void count_isAlwaysSameAsStream() {
                assertThat(cache.size()).isEqualTo(0);
                assertThat(index.count()).isEqualTo(0);

                TestState one = new TestState(Id.create(1), Optional.of(CoordinateLikeTestObject.ORIGIN));
                cache.add(one);
                assertThat(index.count()).isEqualTo(1);

                TestState two = new TestState(Id.create(2), Optional.of(CoordinateLikeTestObject.create(1, 1)));
                cache.add(two);
                assertThat(index.count()).isEqualTo(2);

                cache.update(one, null);
                assertThat(index.count()).isEqualTo(1);

                cache.delete(two.getId());
                assertThat(index.count()).isEqualTo(0);
            }

            @Test
            void add_whenNewValueHasSameKeyAsOld_thenThrowsExceptionAndRollsBackChanges() {
                TestState original = new TestState(Id.create(1), Optional.of(CoordinateLikeTestObject.ORIGIN));
                TestState clash = new TestState(Id.create(2), Optional.of(CoordinateLikeTestObject.ORIGIN));
                cache.add(original);

                //Check exception
                assertThatThrownBy(() -> cache.add(clash))
                        .isInstanceOf(CacheUpdateException.class)
                        .has(CacheExceptionUtils.validateCacheUpdateException(INDEX_NAME));

                //Check rollback
                assertThat(index.get(CoordinateLikeTestObject.ORIGIN))
                        .containsSame(original);
                assertThat(cache.stream().collect(ImmutableSet.toImmutableSet())).containsExactly(original);
            }

            @Test
            void snapshot_whenIndexIsEmpty_returnsEmptySnapshot() {
                assertThat(index.snapshot()).isEmpty();
            }

            @Test
            void snapshot_whenOptionalIsPresent_returnsSnapshotWithSingleElement() {
                TestState testState = new TestState(Id.create(1), Optional.of(CoordinateLikeTestObject.ORIGIN));
                cache.add(testState);

                assertThat(index.snapshot()).isEqualTo(ImmutableMap.of(CoordinateLikeTestObject.ORIGIN, testState));
            }

            @Test
            void snapshot_whenOptionalIsNotPresent_returnsSnapshotWithoutElement() {
                TestState stateOne = new TestState(Id.create(1), Optional.empty());
                TestState stateTwo = new TestState(Id.create(2), Optional.of(CoordinateLikeTestObject.ORIGIN));
                cache.addAll(ImmutableSet.of(stateOne, stateTwo));

                assertThat(index.snapshot()).isEqualTo(ImmutableMap.of(CoordinateLikeTestObject.ORIGIN, stateTwo));
            }

            @Test
            void snapshot_whenIndexRemovedFrom_returnsSnapshotWithoutThatElement() {
                TestState stateOne = new TestState(Id.create(1), Optional.of(CoordinateLikeTestObject.create(0, 1)));
                TestState stateTwo = new TestState(Id.create(2), Optional.of(CoordinateLikeTestObject.create(1, 0)));
                cache.addAll(ImmutableSet.of(stateOne, stateTwo));
                index.snapshot();  // So call below is not first call

                cache.delete(stateOne.getId());

                assertThat(index.snapshot()).isEqualTo(ImmutableMap.of(stateTwo.getLocation().get(), stateTwo));
            }
        }

        /**
         * White-box tests which verify implementation details of OptionalOneToOneIndex that do not form part of the
         * public API. The behaviours verified by these tests are subject to change and should not be relied upon by
         * users of the OptionalOneToOneIndex class.
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
                // Need to ensure a non-empty initial index, otherwise snapshot will always be ImmutableMap.of()
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
                // Need to ensure a non-empty initial index, otherwise snapshot will always be ImmutableMap.of()
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

        @Nested
        class PerformanceTests {
            // @Test  // Uncomment to test performance (takes a couple of minutes to run)
            void checkThatUpdateIsFast() {
                List<TestState>[] statess = IntStream.range(90, 110)
                        .mapToObj(frequency -> getStates(1000, frequency))
                        .map(Arrays::asList)
                        .toArray(List[]::new);

                Runtime.getRuntime().gc();
                Runtime.getRuntime().gc();
                Runtime.getRuntime().gc();

                System.out.println("States generated");
                statess[0].forEach(cache::add);
                for (int i = 1; i < statess.length; i++) {
                    update(statess[i-1], statess[i]);
                }
                update(statess[statess.length-1], statess[0]);
                ImmutableMap<Identity<? extends TestState>, TestState> snapshot1 = cache.snapshotObjects();
                System.out.println("Warm-up complete");

                // Sample production code update mix was:
                // oldKey = newKey: 25%, oldKey != newKey: 0%, new: 0%, removed: 1%, not present: 74%

                // This code is much heavier on the oldKey = newKey update codepath
                int numberOfRepeats = 10;
                Long[] durationMillis = new Long[numberOfRepeats];
                for (int repeats = 0; repeats < numberOfRepeats; repeats++) {
                    long start = System.nanoTime();
                    for (int i = 1; i < statess.length; i++) {
                        update(statess[i - 1], statess[i]);
                    }
                    update(statess[statess.length - 1], statess[0]);
                    long end = System.nanoTime();
                    durationMillis[repeats] = (end - start) / (1_000_000 * statess.length);
                    System.out.println("Repeat:" + repeats + ", duration:" + durationMillis[repeats] + "ms per " + statess[0].size() + " updates");

                    ImmutableMap<Identity<? extends TestState>, TestState> snapshot2 = cache.snapshotObjects();
                    Assertions.assertEquals(snapshot1, snapshot2);
                }
                Arrays.sort(durationMillis);
                System.out.println("Median duration:" + (((durationMillis.length % 2) == 0) ? (durationMillis[durationMillis.length / 2 - 1] + durationMillis[durationMillis.length / 2]) / 2 : durationMillis[durationMillis.length / 2]) + "ms");
                double mean = LongStream.range(1, durationMillis.length - 1).map(index -> durationMillis[(int)index]).average().orElse(Double.NaN);
                System.out.println("Mean duration:" + mean + "ms");
            }

            private void update(List<TestState> froms, List<TestState> tos) {
                for (int i = 0; i < froms.size(); i++) {
                    cache.update(froms.get(i), tos.get(i));
                }
            }

            private TestState[] getStates(int count, int emptyFrequency) {
                return IntStream.range(1, count + 1)
                        .mapToObj(x ->
                                IntStream.range(1, count + 1)
                                        .mapToObj(y -> {
                                            Optional<CoordinateLikeTestObject> location = ((x + y) % emptyFrequency) == 0 ? Optional.empty() : Optional.of(CoordinateLikeTestObject.create(x, y));
                                            return new TestState(Id.create(x * count * 10 + y), location);
                                        }))
                        .flatMap(Function.identity())
                        .toArray(TestState[]::new);
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
