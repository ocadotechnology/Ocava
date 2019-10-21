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

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.SimpleLongIdentified;

@DisplayName("An OptionalSortedManyToManyIndexTest")
class OptionalSortedManyToManyIndexTest {

    @Nested
    class CacheTypeTests extends IndexTests {
        @Override
        OptionalSortedManyToManyIndex<Integer, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            return cache.addOptionalSortedManyToManyIndex(TestState::getIndexingValues, Comparator.comparingInt(TestState::getComparatorValue));
        }
    }

    @Nested
    class CacheSubTypeTests extends IndexTests {
        @Override
        OptionalSortedManyToManyIndex<Integer, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            // IMPORTANT:
            // DO NOT inline indexFunction, as that will not fail to compile should addOptionalOneToManyIndex() require a type
            // of Function<TestState, Optional<Coordinate>> instead of Function<? super TestState, Optional<Coordinate<>, due
            // to automatic type coercion of the lambda.
            Comparator<LocationState> comparator = Comparator.comparingInt(LocationState::getComparatorValue);
            return cache.addOptionalSortedManyToManyIndex(LocationState::getIndexingValues, comparator);
        }
    }

    private abstract static class IndexTests {

        private static final Optional<Set<Integer>> INDEXING_VALUES = Optional.of(ImmutableSet.of(1, 2));
        private static final Optional<Set<Integer>> DIFFERENT_INDEXING_VALUES = Optional.of(ImmutableSet.of(3, 4));

        private IndexedImmutableObjectCache<TestState, TestState> cache;
        private OptionalSortedManyToManyIndex<Integer, TestState> index;

        abstract OptionalSortedManyToManyIndex<Integer, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache);

        @BeforeEach
        void init() {
            cache = IndexedImmutableObjectCache.createHashMapBackedCache();
            index = addIndexToCache(cache);
        }

        @Test
        void addToCache_whenOptionalIsEmpty_thenStateNotIndexed() {
            cache.add(new TestState(Id.create(1), 1, Optional.empty()));

            Assertions.assertEquals(0, index.streamKeySet().mapToInt(index::size).sum());
        }

        @Test
        void addToCache_whenOptionalIsPresent_thenStateIndexed() {
            TestState testState = new TestState(Id.create(100), 1, INDEXING_VALUES);
            cache.add(testState);

            INDEXING_VALUES.get().forEach(indexValue -> Assertions.assertEquals(testState, index.asList(indexValue).get(0)));
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndicesCompareAsEqual_thenThrowsExceptionOnSecondAdd() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUES);
            TestState stateTwo = new TestState(Id.create(2), 1, INDEXING_VALUES);

            Assertions.assertDoesNotThrow(() -> cache.add(stateOne));
            Assertions.assertThrows(IllegalStateException.class, () -> cache.add(stateTwo));
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndicesCompareAsEqual_thenThrowsExceptionOnAtomicAdd() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUES);
            TestState stateTwo = new TestState(Id.create(2), 1, INDEXING_VALUES);

            Assertions.assertThrows(IllegalStateException.class, () -> cache.addAll(ImmutableSet.of(stateOne, stateTwo)));
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndicesCompareAsEqual_thenThrowsExceptionOnFirstInconsistence() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUES);
            TestState stateTwo = new TestState(Id.create(2), 2, INDEXING_VALUES);
            TestState stateThree = new TestState(Id.create(3), 1, INDEXING_VALUES);

            Assertions.assertDoesNotThrow(() -> cache.addAll(ImmutableSet.of(stateOne, stateTwo)));
            Assertions.assertThrows(IllegalStateException.class, () -> cache.add(stateThree));
        }

        @Test
        void addToCache_whenMultipleTestStatesWithDifferentIndicesCompareAsEqual_thenIndexedSuccessfully() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUES);
            TestState stateTwo = new TestState(Id.create(2), 1, DIFFERENT_INDEXING_VALUES);

            Assertions.assertDoesNotThrow(() -> cache.addAll(ImmutableSet.of(stateOne, stateTwo)));
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndices_thenIndexIsSorted() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUES);
            TestState stateTwo = new TestState(Id.create(2), 2, INDEXING_VALUES);
            TestState stateThree = new TestState(Id.create(3), 3, INDEXING_VALUES);
            TestState stateFour = new TestState(Id.create(4), 4, INDEXING_VALUES);
            TestState stateFive = new TestState(Id.create(5), 5, INDEXING_VALUES);
            ImmutableList<TestState> expected = ImmutableList.of(stateOne, stateTwo, stateThree, stateFour, stateFive);
            cache.addAll(expected.reverse());

            INDEXING_VALUES.get().forEach(indexValue -> {
                ImmutableList<TestState> actual = index.asList(indexValue);
                Assertions.assertEquals(expected, actual);
            });
        }

        @Test
        void updateCache_whenComparatorValuesAreSwapped_testStatesAreSorted() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUES);
            TestState stateTwo = new TestState(Id.create(2), 2, INDEXING_VALUES);
            cache.addAll(ImmutableSet.of(stateOne, stateTwo));

            Change<TestState> updateOne = Change.update(stateOne, new TestState(Id.create(1), 2, INDEXING_VALUES));
            Change<TestState> updateTwo = Change.update(stateTwo, new TestState(Id.create(2), 1, INDEXING_VALUES));
            cache.updateAll(ImmutableSet.of(updateOne, updateTwo));

            INDEXING_VALUES.get().forEach(indexValue -> {
                ImmutableList<TestState> testStates = index.asList(indexValue);
                Assertions.assertSame(testStates.get(0), updateTwo.newObject);
                Assertions.assertSame(testStates.get(1), updateOne.newObject);
            });
        }
    }

    private interface LocationState {
        Integer getComparatorValue();
        Optional<Set<Integer>> getIndexingValues();
    }

    private static class TestState extends SimpleLongIdentified<TestState> implements LocationState {
        private final Optional<Set<Integer>> indexingValues;
        private final Integer comparatorValue;

        private TestState(Id<TestState> id, Integer comparatorValue, Optional<Set<Integer>> indexingValues) {
            super(id);
            this.indexingValues = indexingValues;
            this.comparatorValue = comparatorValue;
        }

        @Override
        public Optional<Set<Integer>> getIndexingValues() {
            return indexingValues;
        }

        @Override
        public Integer getComparatorValue() {
            return comparatorValue;
        }
    }
}
