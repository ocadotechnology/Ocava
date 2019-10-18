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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.SimpleLongIdentified;

@DisplayName("A SortedOneToManyIndexTest")
class SortedOneToManyIndexTest {

    @Nested
    class CacheTypeTests extends IndexTests {
        @Override
        SortedOneToManyIndex<Integer, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            return cache.addSortedOneToManyIndex(TestState::getIndexingValue, Comparator.comparingInt(TestState::getComparatorValue));
        }
    }

    @Nested
    class CacheSubTypeTests extends IndexTests {
        @Override
        SortedOneToManyIndex<Integer, TestState>  addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            // IMPORTANT:
            // DO NOT inline indexFunction, as that will not fail to compile should addSortedOneToManyIndex() require a type
            // of Function<TestState, Optional<Coordinate>> instead of Function<? super TestState, Optional<Coordinate<>, due
            // to automatic type coercion of the lambda.
            Comparator<LocationState> comparator = Comparator.comparingInt(LocationState::getComparatorValue);
            return cache.addSortedOneToManyIndex(LocationState::getIndexingValue, comparator);
        }
    }

    private abstract static class IndexTests {

        private static final Integer INDEXING_VALUE = 1;
        private static final Integer DIFFERENT_INDEXING_VALUE = 2;

        private IndexedImmutableObjectCache<TestState, TestState> cache;
        private SortedOneToManyIndex<Integer, TestState> index;

        abstract SortedOneToManyIndex<Integer, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache);

        @BeforeEach
        void init() {
            cache = IndexedImmutableObjectCache.createHashMapBackedCache();
            index = addIndexToCache(cache);
        }

        @Test
        void addToCache_whenElementIsAdded_thenStateIsIndexed() {
            TestState testState = new TestState(Id.create(100), 1, INDEXING_VALUE);
            cache.add(testState);

            Assertions.assertEquals(testState, index.first(INDEXING_VALUE));
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndexCompareAsEqual_thenThrowsExceptionOnSecondAdd() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUE);
            TestState stateTwo = new TestState(Id.create(2), 1, INDEXING_VALUE);

            cache.add(stateOne);
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
        void addToCache_whenMultipleTestStatesWithTheSameIndex_thenIndexIsSorted() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUE);
            TestState stateTwo = new TestState(Id.create(2), 2, INDEXING_VALUE);
            TestState stateThree = new TestState(Id.create(3), 3, INDEXING_VALUE);
            TestState stateFour = new TestState(Id.create(4), 4, INDEXING_VALUE);
            TestState stateFive = new TestState(Id.create(5), 5, INDEXING_VALUE);
            ImmutableList<TestState> expected = ImmutableList.of(stateOne, stateTwo, stateThree, stateFour, stateFive);
            cache.addAll(expected.reverse());

            ImmutableList<TestState> actual = ImmutableList.copyOf(index.iterator(INDEXING_VALUE));
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

            ImmutableList<TestState> testStates = ImmutableList.copyOf(index.iterator(INDEXING_VALUE));
            Assertions.assertSame(testStates.get(0), updateTwo.newObject);
            Assertions.assertSame(testStates.get(1), updateOne.newObject);
        }

    }

    @Test
    void addToCache_whenMultipleTestStatesWithDifferentIndicesCompareAsEqual_thenDifferentTestStatesAreNeverCompared() {
        IndexedImmutableObjectCache<TestState, TestState> cache = IndexedImmutableObjectCache.createHashMapBackedCache();
        Comparator<TestState> requireSameComparator = (t, o) -> {
            Assertions.assertSame(t, o);
            return 0;
        };
        cache.addSortedOneToManyIndex(TestState::getIndexingValue, requireSameComparator);

        TestState stateOne = new TestState(Id.create(1), 1, IndexTests.INDEXING_VALUE);
        TestState stateTwo = new TestState(Id.create(2), 1, IndexTests.DIFFERENT_INDEXING_VALUE);
        cache.addAll(ImmutableSet.of(stateOne, stateTwo));
    }

    private interface LocationState {
        Integer getComparatorValue();
        Integer getIndexingValue();
    }

    private static class TestState extends SimpleLongIdentified<TestState> implements LocationState {
        private final Integer indexingValue;
        private final Integer comparatorValue;

        private TestState(Id<TestState> id, Integer comparatorValue, Integer indexingValue) {
            super(id);
            this.indexingValue = indexingValue;
            this.comparatorValue = comparatorValue;
        }

        @Override
        public Integer getIndexingValue() {
            return indexingValue;
        }

        @Override
        public Integer getComparatorValue() {
            return comparatorValue;
        }
    }

}
