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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

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
    private static final String INDEX_NAME = "TEST_OPTIONAL_SORTED_MANY_TO_MANY_INDEX";

    @Nested
    class CacheTypeTests extends IndexTests {
        @Override
        OptionalSortedManyToManyIndex<Integer, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            return cache.addOptionalSortedManyToManyIndex(INDEX_NAME, TestState::getIndexingValues, Comparator.comparingInt(TestState::getComparatorValue));
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
            return cache.addOptionalSortedManyToManyIndex(INDEX_NAME, LocationState::getIndexingValues, comparator);
        }
    }

    private abstract static class IndexTests {

        private static final Optional<Set<Integer>> INDEXING_VALUES = Optional.of(ImmutableSet.of(1, 2, 3));
        private static final Optional<Set<Integer>> CLASHING_INDEXING_VALUES = Optional.of(ImmutableSet.of(-1, 0, 1)); //Only clashes on the last
        private static final Optional<Set<Integer>> DIFFERENT_INDEXING_VALUES = Optional.of(ImmutableSet.of(4, 5, 6));

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

            assertThat(index.streamKeySet().mapToInt(index::size).sum()).isEqualTo(0);
        }

        @Test
        void addToCache_whenOptionalIsPresent_thenStateIndexed() {
            TestState testState = new TestState(Id.create(100), 1, INDEXING_VALUES);
            cache.add(testState);

            INDEXING_VALUES.get().forEach(indexValue -> assertThat(index.asList(indexValue).get(0)).isEqualTo(testState));
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndex_thenFirstAndLastCorrect() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUES);
            TestState stateTwo = new TestState(Id.create(2), 2, INDEXING_VALUES);
            TestState stateThree = new TestState(Id.create(3), 3, INDEXING_VALUES);
            TestState stateFour = new TestState(Id.create(4), 4, INDEXING_VALUES);
            TestState stateFive = new TestState(Id.create(5), 5, INDEXING_VALUES);
            ImmutableList<TestState> expected = ImmutableList.of(stateOne, stateTwo, stateThree, stateFour, stateFive);
            cache.addAll(expected.reverse());

            assertThat(index.first(1).get()).isEqualTo(stateOne);
            assertThat(index.last(1).get()).isEqualTo(stateFive);
            assertThat(index.first(2).get()).isEqualTo(stateOne);
            assertThat(index.last(2).get()).isEqualTo(stateFive);
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndicesCompareAsEqual_thenThrowsExceptionOnSecondAdd() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUES);
            TestState stateTwo = new TestState(Id.create(2), 1, CLASHING_INDEXING_VALUES);

            assertThatCode(() -> cache.add(stateOne)).doesNotThrowAnyException();
            assertThatThrownBy(() -> cache.add(stateTwo))
                    .isInstanceOf(CacheUpdateException.class)
                    .has(CacheExceptionUtils.validateCacheUpdateException(INDEX_NAME));

            //Test rollback
            assertThat(cache.stream()).containsExactly(stateOne);
            assertThat(index.streamKeySet()).containsExactlyElementsOf(INDEXING_VALUES.get());
            INDEXING_VALUES.get().forEach(i -> assertThat(index.stream(i)).containsExactly(stateOne));
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndicesCompareAsEqual_thenThrowsExceptionOnAtomicAdd() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUES);
            TestState stateTwo = new TestState(Id.create(2), 1, INDEXING_VALUES);

            assertThatThrownBy(() -> cache.addAll(ImmutableSet.of(stateOne, stateTwo)))
                    .isInstanceOf(CacheUpdateException.class)
                    .has(CacheExceptionUtils.validateCacheUpdateException(INDEX_NAME));

            //Test rollback
            assertThat(cache.isEmpty()).isTrue();
            assertThat(index.streamKeySet()).isEmpty();
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndicesCompareAsEqual_thenThrowsExceptionOnFirstInconsistence() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUES);
            TestState stateTwo = new TestState(Id.create(2), 2, INDEXING_VALUES);
            TestState stateThree = new TestState(Id.create(3), 1, CLASHING_INDEXING_VALUES);

            assertThatCode(() -> cache.addAll(ImmutableSet.of(stateOne, stateTwo))).doesNotThrowAnyException();
            assertThatThrownBy(() -> cache.add(stateThree))
                    .isInstanceOf(CacheUpdateException.class)
                    .has(CacheExceptionUtils.validateCacheUpdateException(INDEX_NAME));

            //Test rollback
            assertThat(cache.stream()).containsExactlyInAnyOrder(stateOne, stateTwo);
            assertThat(index.streamKeySet()).containsExactlyElementsOf(INDEXING_VALUES.get());
            INDEXING_VALUES.get().forEach(i -> assertThat(index.stream(i)).containsExactly(stateOne, stateTwo));
        }

        @Test
        void addToCache_whenMultipleTestStatesWithDifferentIndicesCompareAsEqual_thenIndexedSuccessfully() {
            TestState stateOne = new TestState(Id.create(1), 1, INDEXING_VALUES);
            TestState stateTwo = new TestState(Id.create(2), 1, DIFFERENT_INDEXING_VALUES);

            assertThatCode(() -> cache.addAll(ImmutableSet.of(stateOne, stateTwo))).doesNotThrowAnyException();
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
                assertThat(actual).isEqualTo(expected);
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
                assertThat(testStates.get(0)).isSameAs(updateTwo.newObject);
                assertThat(testStates.get(1)).isSameAs(updateOne.newObject);
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
