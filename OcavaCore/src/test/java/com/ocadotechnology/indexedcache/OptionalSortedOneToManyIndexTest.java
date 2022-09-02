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

import java.util.Comparator;
import java.util.Optional;

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
    private static final String INDEX_NAME = "TEST_OPTIONAL_SORTED_ONE_TO_MANY_INDEX";

    @Nested
    class CacheTypeTests extends IndexTests {
        @Override
        OptionalSortedOneToManyIndex<Integer, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            return cache.addOptionalSortedOneToManyIndex(INDEX_NAME, TestState::getIndexingValue, Comparator.comparingInt(TestState::getComparatorValue));
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
            return cache.addOptionalSortedOneToManyIndex(INDEX_NAME, LocationState::getIndexingValue, comparator);
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

            assertThat(index.streamKeySet().mapToInt(index::size).sum()).isEqualTo(0);
        }

        @Test
        void addToCache_whenOptionalIsPresent_thenStateIndexed() {
            TestState testState = new TestState(Id.create(100), 1, INDEXING_VALUE);
            cache.add(testState);

            assertThat(index.asList(INDEXING_VALUE.get()).get(0)).isEqualTo(testState);
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndexCompareAsEqual_thenThrowsExceptionOnSecondAdd() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUE);
            TestState stateTwo = new TestState(Id.create(2), 1, INDEXING_VALUE);

            assertThatCode(() -> cache.add(stateOne)).doesNotThrowAnyException();
            assertThatThrownBy(() -> cache.add(stateTwo))
                    .isInstanceOf(CacheUpdateException.class)
                    .has(CacheExceptionUtils.validateCacheUpdateException(INDEX_NAME));

            //Test rollback
            assertThat(cache.stream()).containsExactly(stateOne);
            assertThat(index.streamKeySet()).containsExactly(INDEXING_VALUE.get());
            assertThat(index.stream(INDEXING_VALUE.get())).containsExactly(stateOne);
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
            assertThat(index.streamKeySet()).isEmpty();
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
            assertThat(cache.stream()).containsExactlyInAnyOrder(stateOne, stateTwo);
            assertThat(index.streamKeySet()).containsExactly(INDEXING_VALUE.get());
            assertThat(index.stream(INDEXING_VALUE.get())).containsExactly(stateOne, stateTwo);
        }

        @Test
        void addToCache_whenMultipleTestStatesWithDifferentIndicesCompareAsEqual_thenIndexedSuccessfully() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUE);
            TestState stateTwo = new TestState(Id.create(2), 1, DIFFERENT_INDEXING_VALUE);

            assertThatCode(() -> cache.addAll(ImmutableSet.of(stateOne, stateTwo))).doesNotThrowAnyException();
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

            assertThat(index.getFirst(INDEXING_VALUE.get())).isEqualTo(Optional.of(stateOne));
            assertThat(index.getLast(INDEXING_VALUE.get())).isEqualTo(Optional.of(stateFive));
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
            assertThat(actual).isEqualTo(expected);
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
            assertThat(testStates.get(0)).isSameAs(updateTwo.newObject);
            assertThat(testStates.get(1)).isSameAs(updateOne.newObject);
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
