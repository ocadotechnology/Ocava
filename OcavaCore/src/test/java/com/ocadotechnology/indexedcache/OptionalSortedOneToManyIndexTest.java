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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
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

    @Nested
    class OptionalSubTypeSortedOneToManyIndexTests extends IndexTests {
        @Override
        OptionalSortedOneToManyIndex<Integer, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            // IMPORTANT:
            // DO NOT inline indexFunction, as that will not fail to compile should addOptionalSubTypeSortedOneToManyIndex()
            // require a type of Function<ExtendedTestState, Optional<Integer>> instead of
            // Function<? super ExtendedTestState, Optional<Integer>>, due to type coercion of the lambda.
            Comparator<LocationState> comparator = Comparator.comparingInt(LocationState::getComparatorValue);

            // This is unchecked, but we need to force the type of the index to match the return type of the function in
            // order to use the test suite.
            @SuppressWarnings("unchecked")
            OptionalSortedOneToManyIndex<Integer, TestState> subTypeIndex =
                    (OptionalSortedOneToManyIndex<Integer, TestState>) (OptionalSortedOneToManyIndex<?, ?>)
                            cache.addOptionalSubTypeSortedOneToManyIndex(INDEX_NAME, ExtendedTestState.class, LocationState::getIndexingValue, comparator);
            return subTypeIndex;
        }

        @Override
        TestState createState(long id, Integer comparatorValue, Optional<Integer> indexingValue) {
            return new ExtendedTestState(Id.create(id), comparatorValue, indexingValue);
        }

        @Test
        void add_whenBaseTypeStateAdded_thenStateNotIndexed() {
            cache.add(new TestState(Id.create(1), 1, INDEXING_VALUE));

            assertThat(index.streamKeySet().mapToInt(index::size).sum()).isEqualTo(0);
        }

        @Test
        void add_whenBaseAndSubTypeStatesAdded_thenOnlySubTypeStateIndexed() {
            cache.add(new TestState(Id.create(1), 1, INDEXING_VALUE));
            ExtendedTestState extended = new ExtendedTestState(Id.create(2), 2, INDEXING_VALUE);
            cache.add(extended);

            assertThat(index.stream(INDEXING_VALUE.get())).containsExactly(extended);
        }

        @Test
        void update_whenBaseTypeStateReplacedBySubType_thenThrowsException() {
            TestState baseState = new TestState(Id.create(1), 1, INDEXING_VALUE);
            cache.add(baseState);

            ExtendedTestState extended = new ExtendedTestState(Id.create(1), 1, INDEXING_VALUE);

            assertThatThrownBy(() -> cache.update(baseState, extended))
                    .isInstanceOf(CacheUpdateException.class)
                    .hasRootCauseInstanceOf(IndexUpdateException.class);

            assertThat(index.streamKeySet().mapToInt(index::size).sum()).isEqualTo(0);
        }

        @Test
        void update_whenSubTypeStateReplacedByBaseType_thenThrowsException() {
            ExtendedTestState extended = new ExtendedTestState(Id.create(1), 1, INDEXING_VALUE);
            cache.add(extended);

            TestState baseState = new TestState(Id.create(1), 1, INDEXING_VALUE);

            assertThatThrownBy(() -> cache.update(extended, baseState))
                    .isInstanceOf(CacheUpdateException.class)
                    .hasRootCauseInstanceOf(IndexUpdateException.class);

            assertThat(index.stream(INDEXING_VALUE.get())).containsExactly(extended);
        }
    }

    private abstract static class IndexTests {

        static final Optional<Integer> INDEXING_VALUE = Optional.of(1);
        static final Optional<Integer> DIFFERENT_INDEXING_VALUE = Optional.of(2);
        static final int NOT_EXISTING_INDEXING_VALUE = 999;

        IndexedImmutableObjectCache<TestState, TestState> cache;
        OptionalSortedOneToManyIndex<Integer, TestState> index;

        abstract OptionalSortedOneToManyIndex<Integer, TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache);

        TestState createState(long id, Integer comparatorValue, Optional<Integer> indexingValue) {
            return new TestState(Id.create(id), comparatorValue, indexingValue);
        }

        @BeforeEach
        void init() {
            cache = IndexedImmutableObjectCache.createHashMapBackedCache();
            index = addIndexToCache(cache);
        }

        @Test
        void addToCache_whenOptionalIsEmpty_thenStateNotIndexed() {
            cache.add(createState(1, 1, Optional.empty()));

            assertThat(index.streamKeySet().mapToInt(index::size).sum()).isEqualTo(0);
        }

        @Test
        void addToCache_whenOptionalIsPresent_thenStateIndexed() {
            TestState testState = createState(100, 1, INDEXING_VALUE);
            cache.add(testState);

            assertThat(index.asList(INDEXING_VALUE.get()).get(0)).isEqualTo(testState);
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndexCompareAsEqual_thenThrowsExceptionOnSecondAdd() {
            TestState stateOne = createState(1, 1, INDEXING_VALUE);
            TestState stateTwo = createState(2, 1, INDEXING_VALUE);

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
            TestState stateOne = createState(1, 1, INDEXING_VALUE);
            TestState stateTwo = createState(2, 1, INDEXING_VALUE);

            assertThatThrownBy(() -> cache.addAll(ImmutableSet.of(stateOne, stateTwo)))
                    .isInstanceOf(CacheUpdateException.class)
                    .has(CacheExceptionUtils.validateCacheUpdateException(INDEX_NAME));

            //Test rollback
            assertThat(cache.isEmpty()).isTrue();
            assertThat(index.streamKeySet()).isEmpty();
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndexCompareAsEqual_thenThrowsExceptionOnFirstInconsistence() {
            TestState stateOne = createState(1, 1, INDEXING_VALUE);
            TestState stateTwo = createState(2, 2, INDEXING_VALUE);
            TestState stateThree = createState(3, 1, INDEXING_VALUE);

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
            TestState stateOne = createState(1, 1, INDEXING_VALUE);
            TestState stateTwo = createState(2, 1, DIFFERENT_INDEXING_VALUE);

            assertThatCode(() -> cache.addAll(ImmutableSet.of(stateOne, stateTwo))).doesNotThrowAnyException();
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndex_thenFirstAndLastCorrect() {
            TestState stateOne = createState(1, 1, INDEXING_VALUE);
            TestState stateTwo = createState(2, 2, INDEXING_VALUE);
            TestState stateThree = createState(3, 3, INDEXING_VALUE);
            TestState stateFour = createState(4, 4, INDEXING_VALUE);
            TestState stateFive = createState(5, 5, INDEXING_VALUE);
            ImmutableList<TestState> expected = ImmutableList.of(stateOne, stateTwo, stateThree, stateFour, stateFive);
            cache.addAll(expected.reverse());

            assertThat(index.getFirst(INDEXING_VALUE.get())).isEqualTo(Optional.of(stateOne));
            assertThat(index.getLast(INDEXING_VALUE.get())).isEqualTo(Optional.of(stateFive));
        }

        @Test
        void addToCache_whenMultipleTestStatesWithTheSameIndex_thenIndexIsSorted() {
            TestState stateOne = createState(1, 1, INDEXING_VALUE);
            TestState stateTwo = createState(2, 2, INDEXING_VALUE);
            TestState stateThree = createState(3, 3, INDEXING_VALUE);
            TestState stateFour = createState(4, 4, INDEXING_VALUE);
            TestState stateFive = createState(5, 5, INDEXING_VALUE);
            ImmutableList<TestState> expected = ImmutableList.of(stateOne, stateTwo, stateThree, stateFour, stateFive);
            cache.addAll(expected.reverse());

            ImmutableList<TestState> actual = index.asList(INDEXING_VALUE.get());
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void forEachWithFilter_appliesConsumerToEach() {
            TestState stateOne = createState(1, 1, DIFFERENT_INDEXING_VALUE);
            TestState stateTwo = createState(2, 2, Optional.empty());
            TestState stateThree = createState(3, 3, INDEXING_VALUE);
            cache.addAll(ImmutableSet.of(stateOne, stateTwo, stateThree));

            ArrayList<TestState> arrayList = new ArrayList<>();
            index.forEach(INDEXING_VALUE.orElseThrow(), arrayList::add);

            assertEquals(1, arrayList.size());
            assertEquals(3, arrayList.get(0).getId().id);
        }

        @Test
        void updateCache_whenComparatorValuesAreSwapped_testStatesAreSorted() {
            TestState stateOne = createState(1, 1, INDEXING_VALUE);
            TestState stateTwo = createState(2, 2, INDEXING_VALUE);
            cache.addAll(ImmutableSet.of(stateOne, stateTwo));

            Change<TestState> updateOne = Change.update(stateOne, createState(1, 2, INDEXING_VALUE));
            Change<TestState> updateTwo = Change.update(stateTwo, createState(2, 1, INDEXING_VALUE));
            cache.updateAll(ImmutableSet.of(updateOne, updateTwo));

            ImmutableList<TestState> testStates = index.asList(INDEXING_VALUE.get());
            assertThat(testStates.get(0)).isSameAs(updateTwo.newObject);
            assertThat(testStates.get(1)).isSameAs(updateOne.newObject);
        }

        @Test
        void snapshot_whenIndexIsEmpty_returnsEmptySnapshot() {
            assertThat(index.snapshot()).isEqualTo(ImmutableListMultimap.of());
        }

        @Test
        void snapshot_whenOptionalIsPresent_returnsSnapshotWithSingleElement() {
            TestState testState = createState(1, 1, INDEXING_VALUE);
            cache.add(testState);

            assertThat(index.snapshot().values()).containsOnly(testState);
        }

        @Test
        void snapshot_whenOptionalIsNotPresent_returnsSnapshotWithoutElement() {
            TestState stateOne = createState(1, 1, Optional.empty());
            TestState stateTwo = createState(2, 2, INDEXING_VALUE);
            cache.addAll(ImmutableSet.of(stateOne, stateTwo));

            assertThat(index.snapshot().values()).containsOnly(stateTwo);
        }

        @Test
        void snapshot_whenIndexRemovedFrom_returnsSnapshotWithoutThatElement() {
            TestState stateOne = createState(1, 1, INDEXING_VALUE);
            TestState stateTwo = createState(2, 2, DIFFERENT_INDEXING_VALUE);
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
            TestState testState = createState(1, 1, INDEXING_VALUE);
            cache.add(testState);

            Object firstSnapshot = index.snapshot();
            Object secondSnapshot = index.snapshot();

            assertThat(firstSnapshot).isSameAs(secondSnapshot);
        }

        @Test
        void snapshot_whenIndexAddedTo_newObjectReturned() {
            Object firstSnapshot = index.snapshot();

            TestState testState = createState(1, 1, INDEXING_VALUE);
            cache.add(testState);
            Object secondSnapshot = index.snapshot();

            assertThat(firstSnapshot).isNotSameAs(secondSnapshot);
        }

        @Test
        void snapshot_whenIndexRemovedFrom_newObjectReturned() {
            TestState testState = createState(1, 1, INDEXING_VALUE);
            cache.add(testState);

            Object firstSnapshot = index.snapshot();

            cache.delete(testState.getId());

            Object secondSnapshot = index.snapshot();
            assertThat(firstSnapshot).isNotSameAs(secondSnapshot);
        }

        @Test
        void snapshot_whenIndexNotAddedTo_thenSameObjectReturned() {
            // Need to ensure a non-empty initial index, otherwise snapshot will always be ImmutableMultimap.of()
            TestState testState1 = createState(1, 1, INDEXING_VALUE);
            TestState testState2 = createState(2, 2, Optional.empty());
            cache.add(testState1);

            Object firstSnapshot = index.snapshot();

            cache.add(testState2);

            Object secondSnapshot = index.snapshot();
            assertThat(firstSnapshot).isSameAs(secondSnapshot);
        }

        @Test
        void snapshot_whenIndexNotRemovedFrom_thenSameObjectReturned() {
            // Need to ensure a non-empty initial index, otherwise snapshot will always be ImmutableMultimap.of()
            TestState testState1 = createState(1, 1, INDEXING_VALUE);
            TestState testState2 = createState(2, 2, Optional.empty());
            cache.add(testState1);
            cache.add(testState2);

            Object firstSnapshot = index.snapshot();

            cache.delete(testState2.getId());

            Object secondSnapshot = index.snapshot();
            assertThat(firstSnapshot).isSameAs(secondSnapshot);
        }

        @Test
        void snapshot_whenMultipleStatesPerValue_thenStatesAreSorted() {
            TestState testState1 = createState(1, 3, INDEXING_VALUE);
            TestState testState2 = createState(2, 1, INDEXING_VALUE);
            TestState testState3 = createState(3, 6, DIFFERENT_INDEXING_VALUE);
            TestState testState4 = createState(4, 2, DIFFERENT_INDEXING_VALUE);
            TestState testState5 = createState(5, 4, DIFFERENT_INDEXING_VALUE);
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

        @Test
        void beforeAndAfter_whenProvidedItemWithSameIndexingValueToOneProvided_shouldReturnApplicableValue() {
            TestState testState1 = createState(1, 3, INDEXING_VALUE);
            TestState testState2 = createState(2, 1, INDEXING_VALUE);
            TestState testState3 = createState(3, 6, DIFFERENT_INDEXING_VALUE);
            TestState testState4 = createState(4, 2, DIFFERENT_INDEXING_VALUE);
            TestState testState5 = createState(5, 4, DIFFERENT_INDEXING_VALUE);
            cache.add(testState1);
            cache.add(testState2);
            cache.add(testState3);
            cache.add(testState4);
            cache.add(testState5);

            assertThat(index.before(DIFFERENT_INDEXING_VALUE.get(), testState4)).isEmpty();
            assertThat(index.after(DIFFERENT_INDEXING_VALUE.get(), testState4)).contains(testState5);
            assertThat(index.before(DIFFERENT_INDEXING_VALUE.get(), testState5)).contains(testState4);
            assertThat(index.after(DIFFERENT_INDEXING_VALUE.get(), testState5)).contains(testState3);
        }

        @Test
        void beforeAndAfter_whenProvidedItemWithDifferentIndexingValueToOneProvided_shouldStillReturnApplicableValue() {
            TestState testState1 = createState(1, 3, INDEXING_VALUE);
            TestState testState2 = createState(2, 1, INDEXING_VALUE);
            TestState testState3 = createState(3, 6, DIFFERENT_INDEXING_VALUE);
            TestState testState4 = createState(4, 2, DIFFERENT_INDEXING_VALUE);
            TestState testState5 = createState(5, 4, DIFFERENT_INDEXING_VALUE);
            cache.add(testState1);
            cache.add(testState2);
            cache.add(testState3);
            cache.add(testState4);
            cache.add(testState5);

            // the provided value need not necessarily have the same indexing value as the one
            // provided. It should work all the same.
            assertThat(index.before(INDEXING_VALUE.get(), testState4)).contains(testState2);
            assertThat(index.after(INDEXING_VALUE.get(), testState4)).contains(testState1);
            assertThat(index.after(INDEXING_VALUE.get(), testState5)).isEmpty();

            assertThat(index.before(DIFFERENT_INDEXING_VALUE.get(), testState4)).isEmpty();
            assertThat(index.after(DIFFERENT_INDEXING_VALUE.get(), testState4)).contains(testState5);
            assertThat(index.before(DIFFERENT_INDEXING_VALUE.get(), testState5)).contains(testState4);
            assertThat(index.after(DIFFERENT_INDEXING_VALUE.get(), testState5)).contains(testState3);
        }

        @Test
        void beforeAndAfter_whenProvidedNotExistingIndexValue_shouldReturnEmpty() {
            TestState testState1 = createState(1, 3, INDEXING_VALUE);
            TestState testState2 = createState(2, 1, INDEXING_VALUE);
            TestState testState3 = createState(3, 6, DIFFERENT_INDEXING_VALUE);
            TestState testState4 = createState(4, 2, DIFFERENT_INDEXING_VALUE);
            TestState testState5 = createState(5, 4, DIFFERENT_INDEXING_VALUE);
            cache.add(testState1);
            cache.add(testState2);
            cache.add(testState3);
            cache.add(testState4);
            cache.add(testState5);

            assertThat(index.before(NOT_EXISTING_INDEXING_VALUE, testState4)).isEmpty();
        }
    }

    private interface LocationState {
        Integer getComparatorValue();
        Optional<Integer> getIndexingValue();
    }

    private static sealed class TestState extends SimpleLongIdentified<TestState> implements LocationState permits ExtendedTestState {
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

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("id", getId()).add("indexingValue", indexingValue).add("comparatorValue", comparatorValue).toString();
        }
    }

    private static final class ExtendedTestState extends TestState {
        private ExtendedTestState(Id<TestState> id, Integer comparatorValue, Optional<Integer> indexingValue) {
            super(id, comparatorValue, indexingValue);
        }
    }
}
