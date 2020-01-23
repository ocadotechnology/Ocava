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

import java.util.Comparator;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.SimpleLongIdentified;

@DisplayName("An OptionalSortedOneToManyIndexTest")
class OptionalSortedOneToManyIndexTest {

    @Nested
    class CacheTypeTests extends IndexTests {
        @Override
        OptionalSortedOneToManyIndex<Integer, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            return cache.addOptionalSortedOneToManyIndex(TestState::getIndexingValue, Comparator.comparingInt(TestState::getComparatorValue));
        }
    }

    @Nested
    class CacheSubTypeTests extends IndexTests {
        @Override
        OptionalSortedOneToManyIndex<Integer, TestState>  addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            // IMPORTANT:
            // DO NOT inline indexFunction, as that will not fail to compile should addOptionalOneToManyIndex() require a type
            // of Function<TestState, Optional<Coordinate>> instead of Function<? super TestState, Optional<Coordinate<>, due
            // to automatic type coercion of the lambda.
            Comparator<LocationState> comparator = Comparator.comparingInt(LocationState::getComparatorValue);
            return cache.addOptionalSortedOneToManyIndex(LocationState::getIndexingValue, comparator);
        }
    }

    private abstract static class IndexTests {

        private static final Optional<Integer> INDEXING_VALUE = Optional.of(1);
        private static final Optional<Integer> DIFFERENT_INDEXING_VALUE = Optional.of(2);

        private IndexedImmutableObjectCache<TestState, TestState> cache;
        private OptionalSortedOneToManyIndex<Integer, TestState> index;

        abstract OptionalSortedOneToManyIndex<Integer, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache);

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
            TestState testState = new TestState(Id.create(100), 1, INDEXING_VALUE);
            cache.add(testState);

            Assertions.assertEquals(testState, index.asList(INDEXING_VALUE.get()).get(0));
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndexCompareAsEqual_thenThrowsExceptionOnSecondAdd() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUE);
            TestState stateTwo = new TestState(Id.create(2), 1, INDEXING_VALUE);

            Assertions.assertDoesNotThrow(() -> cache.add(stateOne));
            Assertions.assertThrows(IllegalStateException.class, () -> cache.add(stateTwo));
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndexCompareAsEqual_thenThrowsExceptionOnAtomicAdd() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUE);
            TestState stateTwo = new TestState(Id.create(2), 1, INDEXING_VALUE);

            Assertions.assertThrows(IllegalStateException.class, () -> cache.addAll(ImmutableSet.of(stateOne, stateTwo)));
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndexCompareAsEqual_thenThrowsExceptionOnFirstInconsistence() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUE);
            TestState stateTwo = new TestState(Id.create(2), 2, INDEXING_VALUE);
            TestState stateThree = new TestState(Id.create(3), 1, INDEXING_VALUE);

            Assertions.assertDoesNotThrow(() -> cache.addAll(ImmutableSet.of(stateOne, stateTwo)));
            Assertions.assertThrows(IllegalStateException.class, () -> cache.add(stateThree));
        }

        @Test
        void addToCache_whenMultipleTestStatesWithDifferentIndicesCompareAsEqual_thenIndexedSuccessfully() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUE);
            TestState stateTwo = new TestState(Id.create(2), 1, DIFFERENT_INDEXING_VALUE);

            Assertions.assertDoesNotThrow(() -> cache.addAll(ImmutableSet.of(stateOne, stateTwo)));
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndex_thenIndexIsSorted() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUE);
            TestState stateTwo = new TestState(Id.create(2), 2, INDEXING_VALUE);
            TestState stateThree = new TestState(Id.create(3), 3, INDEXING_VALUE);
            TestState stateFour = new TestState(Id.create(4), 4, INDEXING_VALUE);
            TestState stateFive = new TestState(Id.create(5), 5, INDEXING_VALUE);
            ImmutableList<TestState> expected = ImmutableList.of(stateOne, stateTwo, stateThree, stateFour, stateFive);
            cache.addAll(expected.reverse());

            ImmutableList<TestState> actual = index.asList(INDEXING_VALUE.get());
            Assertions.assertEquals(expected, actual);
        }

        @Test
        void updateCache_whenComparatorValuesAreSwapped_testStatesAreSorted() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUE);
            TestState stateTwo = new TestState(Id.create(2), 2, INDEXING_VALUE);
            cache.addAll(ImmutableSet.of(stateOne, stateTwo));

            Change<TestState> updateOne = Change.update(stateOne, new TestState(Id.create(1), 2, INDEXING_VALUE));
            Change<TestState> updateTwo = Change.update(stateTwo, new TestState(Id.create(2), 1, INDEXING_VALUE));
            cache.updateAll(ImmutableSet.of(updateOne, updateTwo));

            ImmutableList<TestState> testStates = index.asList(INDEXING_VALUE.get());
            Assertions.assertSame(testStates.get(0), updateTwo.newObject);
            Assertions.assertSame(testStates.get(1), updateOne.newObject);
        }

    }

    private interface LocationState {
        Integer getComparatorValue();
        Optional<Integer> getIndexingValue();
    }

    private static class TestState extends SimpleLongIdentified<TestState> implements LocationState {
        private final Optional<Integer> indexingValue;
        private final Integer comparatorValue;

        private TestState(Id<TestState> id, Integer comparatorValue, Optional<Integer> indexingValue) {
            super(id);
            this.indexingValue = indexingValue;
            this.comparatorValue = comparatorValue;
        }

        @Override
        public Optional<Integer> getIndexingValue() {
            return indexingValue;
        }

        @Override
        public Integer getComparatorValue() {
            return comparatorValue;
        }
    }
}
