/*
 * Copyright © 2017-2025 Ocado (Ocava)
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.SimpleLongIdentified;

@DisplayName("A SortedOneToManyIndexTest")
class SortedOneToManyIndexTest {
    private static final String INDEX_NAME = "TEST_SORTED_ONE_TO_MANY_INDEX";
    
    @Nested
    class CacheTypeTests extends IndexTests {
        @Override
        SortedOneToManyIndex<Integer, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            return cache.addSortedOneToManyIndex(INDEX_NAME, TestState::getIndexingValue, Comparator.comparingInt(TestState::getComparatorValue));
        }
    }

    @Nested
    class CacheSubTypeTests extends IndexTests {
        @Override
        SortedOneToManyIndex<Integer, TestState>  addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            // IMPORTANT:
            // DO NOT inline indexFunction or comparator, as that will not fail to compile should addSortedOneToManyIndex()
            // require a type of Function<TestState, Optional<Coordinate>> instead of Function<? super TestState, Optional<Coordinate>>,
            // due to automatic type coercion of the lambda.
            Function<LocationState, Integer> indexFunction = LocationState::getIndexingValue;
            Comparator<LocationState> comparator = Comparator.comparingInt(LocationState::getComparatorValue);
            return cache.addSortedOneToManyIndex(INDEX_NAME, indexFunction, comparator);
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

            assertThat(index.first(INDEXING_VALUE).get()).isEqualTo(testState);
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndex_thenFirstAndLastCorrect() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUE);
            TestState stateTwo = new TestState(Id.create(2), 2, INDEXING_VALUE);
            TestState stateThree = new TestState(Id.create(3), 3, INDEXING_VALUE);
            TestState stateFour = new TestState(Id.create(4), 4, INDEXING_VALUE);
            TestState stateFive = new TestState(Id.create(5), 5, INDEXING_VALUE);
            ImmutableList<TestState> expected = ImmutableList.of(stateOne, stateTwo, stateThree, stateFour, stateFive);
            cache.addAll(expected.reverse());

            assertThat(index.first(INDEXING_VALUE).get()).isEqualTo(stateOne);
            assertThat(index.last(INDEXING_VALUE).get()).isEqualTo(stateFive);
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
        void forEachWithFilter_appliesConsumerToEach() {
            TestState stateOne = new TestState(Id.create(1), 1, DIFFERENT_INDEXING_VALUE);
            TestState stateTwo = new TestState(Id.create(2), 2, INDEXING_VALUE);
            TestState stateThree = new TestState(Id.create(3), 3, INDEXING_VALUE);
            cache.addAll(ImmutableList.of(stateOne, stateTwo, stateThree));

            ArrayList<TestState> arrayList = new ArrayList<>();
            index.forEach(INDEXING_VALUE, arrayList::add);

            assertEquals(2, arrayList.size());
            assertEquals(2, arrayList.get(0).getId().id);
            assertEquals(3, arrayList.get(1).getId().id);
        }

        @Test
        void findFirstValueSatisfying_whenNothingMatchesPredicate_returnsNull() {
            TestState stateOne = new TestState(Id.create(1), 1, DIFFERENT_INDEXING_VALUE);
            TestState stateTwo = new TestState(Id.create(2), 2, INDEXING_VALUE);
            TestState stateThree = new TestState(Id.create(3), 3, INDEXING_VALUE);
            cache.addAll(ImmutableList.of(stateOne, stateTwo, stateThree));

            assertNull(index.findFirstValueSatisfying(INDEXING_VALUE, testState -> testState.getId().id <= 1));
        }

        @Test
        void findFirstValueSatisfying_whenNoKeyInIndex_returnsNull() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUE);
            TestState stateTwo = new TestState(Id.create(2), 2, INDEXING_VALUE);
            TestState stateThree = new TestState(Id.create(3), 3, INDEXING_VALUE);
            cache.addAll(ImmutableList.of(stateOne, stateTwo, stateThree));

            assertNull(index.findFirstValueSatisfying(DIFFERENT_INDEXING_VALUE, testState -> testState.getId().id <= 1));
        }

        @Test
        void findFirstValueSatisfying_whenValuesMatchPredicate_thenReturnFirst() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUE);
            TestState stateTwo = new TestState(Id.create(2), 2, INDEXING_VALUE);
            TestState stateThree = new TestState(Id.create(3), 3, INDEXING_VALUE);
            cache.addAll(ImmutableList.of(stateOne, stateTwo, stateThree));

            TestState state = index.findFirstValueSatisfying(INDEXING_VALUE, testState -> testState.getId().id > 1);
            assertEquals(stateTwo, state);
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
        
        @Test
        void snapshot_whenIndexIsEmpty_returnsEmptySnapshot() {
            assertThat(index.snapshot()).isEqualTo(ImmutableListMultimap.of());
        }

        @Test
        void snapshot_whenOptionalIsPresent_returnsSnapshotWithSingleElement() {
            TestState testState = new TestState(Id.create(1), 1, INDEXING_VALUE);
            cache.add(testState);

            assertThat(index.snapshot().values()).containsOnly(testState);
        }

        @Test
        void snapshot_whenIndexRemovedFrom_returnsSnapshotWithoutThatElement() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUE);
            TestState stateTwo = new TestState(Id.create(2), 2, DIFFERENT_INDEXING_VALUE);
            cache.addAll(ImmutableSet.of(stateOne, stateTwo));
            index.snapshot();  // So call below is not first call

            cache.delete(stateOne.getId());

            assertThat(index.snapshot().values()).containsOnly(stateTwo);
        }

        @Test
        void snapshot_whenNoChangesToEmptyCache_thenSameObjectReturned() {
            Object firstSnapshot = index.snapshot();
            Object secondSnapshot = index.snapshot();

            assertThat(firstSnapshot).isSameAs(secondSnapshot);
        }

        @Test
        void snapshot_whenNoChangesToNonEmptyCache_thenSameObjectReturned() {
            TestState testState = new TestState(Id.create(1), 1, INDEXING_VALUE);
            cache.add(testState);

            Object firstSnapshot = index.snapshot();
            Object secondSnapshot = index.snapshot();

            assertThat(firstSnapshot).isSameAs(secondSnapshot);
        }

        @Test
        void snapshot_whenIndexAddedTo_newObjectReturned() {
            Object firstSnapshot = index.snapshot();

            TestState testState = new TestState(Id.create(1), 1, INDEXING_VALUE);
            cache.add(testState);
            Object secondSnapshot = index.snapshot();

            assertThat(firstSnapshot).isNotSameAs(secondSnapshot);
        }

        @Test
        void snapshot_whenIndexRemovedFrom_newObjectReturned() {
            TestState testState = new TestState(Id.create(1), 1, INDEXING_VALUE);
            cache.add(testState);

            Object firstSnapshot = index.snapshot();

            cache.delete(testState.getId());

            Object secondSnapshot = index.snapshot();
            assertThat(firstSnapshot).isNotSameAs(secondSnapshot);
        }

        @Test
        void snapshot_whenMultipleStatesPerValue_thenStatesAreSorted() {
            TestState testState1 = new TestState(Id.create(1), 3, INDEXING_VALUE);
            TestState testState2 = new TestState(Id.create(2), 1, INDEXING_VALUE);
            TestState testState3 = new TestState(Id.create(3), 6, DIFFERENT_INDEXING_VALUE);
            TestState testState4 = new TestState(Id.create(4), 2, DIFFERENT_INDEXING_VALUE);
            TestState testState5 = new TestState(Id.create(5), 4, DIFFERENT_INDEXING_VALUE);
            cache.add(testState1);
            cache.add(testState2);
            cache.add(testState3);
            cache.add(testState4);
            cache.add(testState5);

            ImmutableListMultimap<Integer, TestState> snapshot = index.snapshot();
            assertThat(snapshot.containsKey(1)).isTrue();
            assertThat(snapshot.containsKey(2)).isTrue();

            assertThat(snapshot.get(1)).isEqualTo(ImmutableList.of(testState2, testState1));
            assertThat(snapshot.get(2)).isEqualTo(ImmutableList.of(testState4, testState5, testState3));
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
    }

}
