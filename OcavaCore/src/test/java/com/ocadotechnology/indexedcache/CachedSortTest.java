/*
 * Copyright © 2017 Ocado (Ocava)
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

@DisplayName("A CachedSortTest")
class CachedSortTest {
    @Nested
    class CacheTypeTests extends IndexTests {
        @Override
        CachedSort<TestState> addSortToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            return cache.addCacheSort(Comparator.comparingInt(TestState::getIndex));
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
            return cache.addCacheSort(comparing);
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
            Assertions.assertThrows(NullPointerException.class, () -> cache.add(stateOne));
        }

        @Test
        void addToCache_whenMultipleTestStatesMapToTheSameIndex_thenThrowsExceptionOnSecondAdd() {
            TestState stateOne = new TestState(Id.create(1), 1);
            TestState stateTwo = new TestState(Id.create(2), 1);

            Assertions.assertDoesNotThrow(() -> cache.add(stateOne));
            Assertions.assertThrows(IllegalStateException.class, () -> cache.add(stateTwo));
        }

        @Test
        void addToCache_whenMultipleTestStatesMapToTheSameIndex_thenThrowsExceptionOnAtomicAdd() {
            TestState stateOne = new TestState(Id.create(1), 1);
            TestState stateTwo = new TestState(Id.create(2), 1);

            Assertions.assertThrows(IllegalStateException.class, () -> cache.addAll(ImmutableSet.of(stateOne, stateTwo)));
        }

        @Test
        void addToCache_whenTestStateMapsToCoordinateMappedToBySomeThingElse_thenThrowsExceptionOnFirstInconsistence() {
            TestState stateOne = new TestState(Id.create(1), 1);
            TestState stateTwo = new TestState(Id.create(2), 2);
            TestState stateThree = new TestState(Id.create(3), 2);

            Assertions.assertDoesNotThrow(() -> cache.addAll(ImmutableSet.of(stateOne, stateTwo)));
            Assertions.assertThrows(IllegalStateException.class, () -> cache.add(stateThree));
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
            Assertions.assertEquals(expected, actual);
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
            Assertions.assertSame(testStates.get(0), updateTwo.newObject);
            Assertions.assertSame(testStates.get(1), updateOne.newObject);
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
