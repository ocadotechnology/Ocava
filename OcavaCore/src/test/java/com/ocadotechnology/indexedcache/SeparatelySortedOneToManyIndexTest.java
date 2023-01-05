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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Comparator;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.SimpleLongIdentified;

@DisplayName("A SortedOneToManyIndexTest")
class SeparatelySortedOneToManyIndexTest {
    private static final String INDEX_NAME = "TEST_SORTED_ONE_TO_MANY_INDEX";
    
    @Nested
    class CacheTypeTests extends IndexTests {
        @Override
        SeparatelySortedOneToManyIndex<Integer, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            return cache.addSeparatelySortedOneToManyIndex(INDEX_NAME, TestState::getIndexingValue, getComparator());
        }
    }

    @Nested
    class CacheSubTypeTests extends IndexTests {
        @Override
        SeparatelySortedOneToManyIndex<Integer, TestState>  addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            // IMPORTANT:
            // DO NOT inline indexFunction or comparator, as that will not fail to compile should addSeparatelySortedOneToManyIndex()
            // require a type of Function<TestState, Integer> instead of Function<? super TestState, Integer>,
            // due to automatic type coercion of the lambda.
            Function<LocationState, Integer> indexFunction = LocationState::getIndexingValue;
            Function<Integer, Comparator<LocationState>> comparatorFunction = getComparator();
            return cache.addSeparatelySortedOneToManyIndex(INDEX_NAME, indexFunction, comparatorFunction);
        }
    }

    private static <T extends LocationState> Function<Integer, Comparator<T>> getComparator() {
            return i -> i % 2 == 0
                    ? Comparator.<T>comparingInt(LocationState::getComparatorValue).reversed()
                    : Comparator.comparingInt(LocationState::getComparatorValue);
    }

    private abstract static class IndexTests {

        private static final Integer INDEXING_VALUE = 1;
        private static final Integer DIFFERENT_INDEXING_VALUE = 2;

        private IndexedImmutableObjectCache<TestState, TestState> cache;
        private SeparatelySortedOneToManyIndex<Integer, TestState> index;

        abstract SeparatelySortedOneToManyIndex<Integer, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache);

        @BeforeEach
        void init() {
            cache = IndexedImmutableObjectCache.createHashMapBackedCache();
            index = addIndexToCache(cache);
        }

        @Test
        void addToCache_whenElementIsAdded_thenStateIsIndexed() {
            TestState testState = new TestState(Id.create(100), 1, INDEXING_VALUE);
            cache.add(testState);

            assertThat(index.first(INDEXING_VALUE)).contains(testState);
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndexCompareAsEqual_thenThrowsExceptionOnSecondAdd() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUE);
            TestState stateTwo = new TestState(Id.create(2), 1, INDEXING_VALUE);

            cache.add(stateOne);
            assertThatThrownBy(() -> cache.add(stateTwo))
                    .isInstanceOf(CacheUpdateException.class)
                    .has(CacheExceptionUtils.validateCacheUpdateException(INDEX_NAME));

            //Test rollback
            assertThat(cache.stream()).containsExactly(stateOne);
            assertThat(index.keySet()).containsExactly(INDEXING_VALUE);
            assertThat(index.getCopy(INDEXING_VALUE)).containsExactly(stateOne);
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndexCompareAsEqual_thenThrowsExceptionOnAtomicAdd() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUE);
            TestState stateTwo = new TestState(Id.create(2), 1, INDEXING_VALUE);

            assertThatThrownBy(() -> cache.addAll(ImmutableSet.of(stateOne, stateTwo)))
                    .isInstanceOf(CacheUpdateException.class)
                    .has(CacheExceptionUtils.validateCacheUpdateException(INDEX_NAME));

            //Test rollback
            assertThat(cache.isEmpty()).isTrue();
            assertThat(index.keySet()).isEmpty();
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndexCompareAsEqual_thenThrowsExceptionOnFirstInconsistence() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUE);
            TestState stateTwo = new TestState(Id.create(2), 2, INDEXING_VALUE);
            TestState stateThree = new TestState(Id.create(3), 1, INDEXING_VALUE);

            assertThatCode(() -> cache.addAll(ImmutableSet.of(stateOne, stateTwo))).doesNotThrowAnyException();
            assertThatThrownBy(() -> cache.add(stateThree))
                    .isInstanceOf(CacheUpdateException.class)
                    .has(CacheExceptionUtils.validateCacheUpdateException(INDEX_NAME));

            //Test rollback
            assertThat(cache.stream()).containsExactly(stateOne, stateTwo);
            assertThat(index.keySet()).containsExactly(INDEXING_VALUE);
            assertThat(index.getCopy(INDEXING_VALUE)).containsExactly(stateOne, stateTwo);
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
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void addToCache_whenDifferentIndexWithDifferentSort_thenIndexValuesAreSortedIndependently() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUE);
            TestState stateTwo = new TestState(Id.create(2), 2, INDEXING_VALUE);
            TestState stateThree = new TestState(Id.create(3), 1, DIFFERENT_INDEXING_VALUE);
            TestState stateFour = new TestState(Id.create(4), 2, DIFFERENT_INDEXING_VALUE);
            cache.addAll(ImmutableList.of(stateOne, stateTwo, stateThree, stateFour));

            assertThat(index.stream(INDEXING_VALUE)).containsExactly(stateOne, stateTwo);
            assertThat(index.stream(DIFFERENT_INDEXING_VALUE)).containsExactly(stateFour, stateThree);
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
            assertThat(testStates.get(0)).isSameAs(updateTwo.newObject);
            assertThat(testStates.get(1)).isSameAs(updateOne.newObject);
        }

    }

    @Test
    void addToCache_whenMultipleTestStatesWithDifferentIndicesCompareAsEqual_thenDifferentTestStatesAreNeverCompared() {
        IndexedImmutableObjectCache<TestState, TestState> cache = IndexedImmutableObjectCache.createHashMapBackedCache();
        Comparator<TestState> requireSameComparator = (t, o) -> {
            assertThat(t).isSameAs(o);
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

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("indexingValue", indexingValue)
                    .add("comparatorValue", comparatorValue)
                    .toString();
        }
    }

}
