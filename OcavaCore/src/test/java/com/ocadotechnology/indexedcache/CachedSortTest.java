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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.SimpleLongIdentified;

@DisplayName("A CachedSortTest")
class CachedSortTest {
    private static final String INDEX_NAME = "TEST_SORT_INDEX";

    @Nested
    class CacheTypeTests extends IndexTests {
        @Override
        CachedSort<TestState> addSortToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            return cache.addCacheSort(INDEX_NAME, Comparator.comparingInt(TestState::getIndex));
        }
    }

    @Nested
    class CacheSubTypeTests extends IndexTests {
        @Override
        CachedSort<TestState> addSortToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            // IMPORTANT:
            // DO NOT inline indexFunction, as that will not fail to compile should addOneToOneIndex() require a type of
            // Function<TestState, Coordinate> instead of Function<? super TestState, Coordinate>, due to automatic type
            // coercion of the lambda.
            Comparator<LocationState> comparing = Comparator.comparingInt(LocationState::getIndex);
            return cache.addCacheSort(INDEX_NAME, comparing);
        }
    }

    private abstract static class IndexTests {

        private IndexedImmutableObjectCache<TestState, TestState> cache;
        private CachedSort<TestState> sort;

        abstract CachedSort<TestState> addSortToCache(IndexedImmutableObjectCache<TestState, TestState> cache);

        @BeforeEach
        void init() {
            cache = IndexedImmutableObjectCache.createHashMapBackedCache();
            sort = addSortToCache(cache);
        }

        @Test
        void addToCache_whenFunctionReturnsNull_thenExceptionThrownOnAdd() {
            TestState stateOne = new TestState(Id.create(1), null);
            assertThatThrownBy(() -> cache.add(stateOne))
                    .isInstanceOf(NullPointerException.class);
            //Cannot enforce the error message here as the exception is thrown by the comparator
        }

        @Test
        void addToCache_whenMultipleTestStatesMapToTheSameIndex_thenThrowsExceptionOnSecondAdd() {
            TestState stateOne = new TestState(Id.create(1), 1);
            TestState stateTwo = new TestState(Id.create(2), 1);

            assertThatCode(() -> cache.add(stateOne)).doesNotThrowAnyException();
            assertThatThrownBy(() -> cache.add(stateTwo))
                    .isInstanceOf(CacheUpdateException.class)
                    .has(CacheExceptionUtils.validateCacheUpdateException(INDEX_NAME));

            //Test rollback
            assertThat(cache.stream()).containsExactly(stateOne);
            assertThat(sort.asList()).containsExactly(stateOne);
        }

        @Test
        void addToCache_whenMultipleTestStatesMapToTheSameIndex_thenThrowsExceptionOnAtomicAdd() {
            TestState stateOne = new TestState(Id.create(1), 1);
            TestState stateTwo = new TestState(Id.create(2), 1);

            assertThatThrownBy(() -> cache.addAll(ImmutableSet.of(stateOne, stateTwo)))
                    .isInstanceOf(CacheUpdateException.class)
                    .has(CacheExceptionUtils.validateCacheUpdateException(INDEX_NAME));

            //Test rollback
            assertThat(cache.isEmpty()).isTrue();
            assertThat(sort.asList()).isEmpty();
        }

        @Test
        void addToCache_whenTestStateMapsToCoordinateMappedToBySomeThingElse_thenThrowsExceptionOnFirstInconsistence() {
            TestState stateOne = new TestState(Id.create(1), 1);
            TestState stateTwo = new TestState(Id.create(2), 2);
            TestState stateThree = new TestState(Id.create(3), 2);

            assertThatCode(() -> cache.addAll(ImmutableSet.of(stateOne, stateTwo))).doesNotThrowAnyException();
            assertThatThrownBy(() -> cache.add(stateThree))
                    .isInstanceOf(CacheUpdateException.class)
                    .has(CacheExceptionUtils.validateCacheUpdateException(INDEX_NAME));

            //Test rollback
            assertThat(cache.stream()).containsExactly(stateOne, stateTwo);
            assertThat(sort.asList()).containsExactly(stateOne, stateTwo);
        }

        @Test
        void putOrUpdateAll_whenMultipleTestStatesInserted_thenIndexIsSorted() {
            TestState stateOne = new TestState(Id.create(1), 1);
            TestState stateTwo = new TestState(Id.create(2), 2);
            TestState stateThree = new TestState(Id.create(3), 3);
            TestState stateFour = new TestState(Id.create(4), 4);
            TestState stateFive = new TestState(Id.create(5), 5);
            ImmutableList<TestState> expected = ImmutableList.of(stateOne, stateTwo, stateThree, stateFour, stateFive);
            cache.addAll(expected.reverse());

            ImmutableList<TestState> actual = sort.asList();
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        void updateCache_whenComparatorValuesAreSwapped_testStatesAreSorted() {
            TestState stateOne = new TestState(Id.create(1), 1);
            TestState stateTwo = new TestState(Id.create(2), 2);
            cache.addAll(ImmutableSet.of(stateOne, stateTwo));

            Change<TestState> updateOne = Change.update(stateOne, new TestState(Id.create(1), 2));
            Change<TestState> updateTwo = Change.update(stateTwo, new TestState(Id.create(2), 1));
            cache.updateAll(ImmutableSet.of(updateOne, updateTwo));

            ImmutableList<TestState> testStates = sort.asList();
            assertThat(testStates.get(0)).isSameAs(updateTwo.newObject);
            assertThat(testStates.get(1)).isSameAs(updateOne.newObject);
        }
    }

    private interface LocationState {
        Integer getIndex();
    }

    private static class TestState extends SimpleLongIdentified<TestState> implements LocationState {
        private final Integer index;

        private TestState(Id<TestState> id, Integer index) {
            super(id);
            this.index = index;
        }

        @Override
        public Integer getIndex() {
            return index;
        }
    }
}
