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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Comparator;
import java.util.function.Predicate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.indexedcache.IndexedImmutableObjectCache.Hints;

@DisplayName("A SortedPredicateIndexTest")
class SortedPredicateIndexTest {
    private static final String INDEX_NAME = "TEST_SORTED_PREDICATE_INDEX";
    private static final Comparator<TestState> TEST_STATE_COMPARATOR = Comparator.comparingLong(TestState::getValue);
    private static final Comparator<TestThing> TEST_THING_COMPARATOR = Comparator.comparingLong(TestThing::getValue);

    @Nested
    class CacheTypeTests extends IndexTests {
        @Override
        SortedPredicateIndex<TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache, Hints hint) {
            return cache.addSortedPredicateIndex(INDEX_NAME, TestState::isSomething, TEST_STATE_COMPARATOR, hint);
        }
    }

    @Nested
    class CacheSubTypeTests extends IndexTests {
        @Override
        SortedPredicateIndex<TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache, Hints hint) {
            // IMPORTANT:
            // DO NOT inline indexFunction, as that will not fail to compile should addPredicateIndex() require a type of
            // Predicate<TestState> instead of Predicate<? super TestState>, due to automatic type coercion of the lambda
            Predicate<TestThing> indexFunction = TestThing::isSomething;
            return cache.addSortedPredicateIndex(INDEX_NAME, indexFunction, TEST_THING_COMPARATOR, hint);
        }
    }

    private abstract static class IndexTests extends PredicateIndexTest.IndexTests {

        @Override
        abstract SortedPredicateIndex<TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache, Hints hint);

        @Override
        protected SortedPredicateIndex<TestState> getIndex() {
            return (SortedPredicateIndex<TestState>) index;
        }

        @ParameterizedTest
        @EnumSource(Hints.class)
        void givenManyElementsPassingPredicate_thenElementsAreSorted(Hints hint) {
            initialise(hint);

            TestState stateOne = new TestState(Id.create(1), true, 2);
            TestState stateTwo = new TestState(Id.create(2), true, 0);
            TestState stateThree = new TestState(Id.create(3), true, 1);

            ImmutableList<TestState> expectedOrder = ImmutableList.of(stateTwo, stateThree, stateOne);

            cache.addAll(ImmutableSet.of(stateOne, stateTwo, stateThree));

            ImmutableList<TestState> actualOrder = getIndex().stream().collect(ImmutableList.toImmutableList());

            assertThat(actualOrder).containsExactlyElementsOf(expectedOrder);
        }

        @ParameterizedTest
        @EnumSource(Hints.class)
        void givenSortOrderChangesWithUpdate_thenElementsAreSortedCorrectly(Hints hint) {
            initialise(hint);

            TestState stateOne = new TestState(Id.create(1), true, 2);
            TestState stateTwo = new TestState(Id.create(2), true, 0);
            TestState stateThree = new TestState(Id.create(3), true, 1);

            ImmutableList<TestState> initialExpectedOrder = ImmutableList.of(stateTwo, stateThree, stateOne);

            cache.addAll(ImmutableSet.of(stateOne, stateTwo, stateThree));

            ImmutableList<TestState> initialActualOrder = getIndex().stream().collect(ImmutableList.toImmutableList());

            assertThat(initialActualOrder).containsExactlyElementsOf(initialExpectedOrder);

            TestState updatedStateTwo = new TestState(stateTwo.getId(), true, 3);
            cache.update(stateTwo, updatedStateTwo);

            ImmutableList<TestState> updatedExpectedOrder = ImmutableList.of(stateThree, stateOne, updatedStateTwo);

            ImmutableList<TestState> updatedActualOrder = getIndex().stream().collect(ImmutableList.toImmutableList());

            assertThat(updatedActualOrder).containsExactlyElementsOf(updatedExpectedOrder);
        }

        @ParameterizedTest
        @EnumSource(Hints.class)
        void testGetFirst(Hints hint) {
            initialise(hint);

            TestState stateOne = new TestState(Id.create(1), true, 2);
            TestState stateTwo = new TestState(Id.create(2), true, 0);
            TestState stateThree = new TestState(Id.create(3), true, 1);

            cache.addAll(ImmutableSet.of(stateOne, stateTwo, stateThree));

            assertThat(getIndex().getFirst()).contains(stateTwo);

            cache.update(stateTwo, getWithInvertedSomething(stateTwo));

            assertThat(getIndex().getFirst()).contains(stateThree);

            cache.update(stateThree, getWithInvertedSomething(stateThree));

            assertThat(getIndex().getFirst()).contains(stateOne);

            cache.update(stateOne, getWithInvertedSomething(stateOne));

            assertThat(getIndex().getFirst()).isEmpty();
        }

        @ParameterizedTest
        @EnumSource(Hints.class)
        void testGetLast(Hints hint) {
            initialise(hint);

            TestState stateOne = new TestState(Id.create(1), true, 2);
            TestState stateTwo = new TestState(Id.create(2), true, 0);
            TestState stateThree = new TestState(Id.create(3), true, 1);

            cache.addAll(ImmutableSet.of(stateOne, stateTwo, stateThree));

            assertThat(getIndex().getLast()).contains(stateOne);

            cache.update(stateOne, getWithInvertedSomething(stateOne));

            assertThat(getIndex().getLast()).contains(stateThree);

            cache.update(stateThree, getWithInvertedSomething(stateThree));

            assertThat(getIndex().getLast()).contains(stateTwo);

            cache.update(stateTwo, getWithInvertedSomething(stateTwo));

            assertThat(getIndex().getLast()).isEmpty();
        }

        @ParameterizedTest
        @EnumSource(Hints.class)
        void testIterator(Hints hint) {
            initialise(hint);

            TestState stateOne = new TestState(Id.create(1), true, 2);
            TestState stateTwo = new TestState(Id.create(2), false, 0);
            TestState stateThree = new TestState(Id.create(3), true, 1);

            cache.addAll(ImmutableSet.of(stateOne, stateTwo, stateThree));

            assertThat(getIndex().iterator()).toIterable().containsExactly(stateThree, stateOne);
        }

        @ParameterizedTest
        @EnumSource(Hints.class)
        void testAfter(Hints hint) {
            initialise(hint);

            TestState stateOne = new TestState(Id.create(1), true, 2);
            TestState stateTwo = new TestState(Id.create(2), true, 0);
            TestState stateThree = new TestState(Id.create(3), true, 1);

            cache.addAll(ImmutableSet.of(stateOne, stateTwo, stateThree));

            assertThat(getIndex().after(stateOne)).isEmpty();
            assertThat(getIndex().after(stateTwo)).contains(stateThree);
            assertThat(getIndex().after(stateThree)).contains(stateOne);
            assertThatThrownBy(() -> getIndex().after(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @EnumSource(Hints.class)
        void testAfter_givenPreviousValuesNotInCache(Hints hint) {
            initialise(hint);

            TestState stateOne = new TestState(Id.create(1), true, 3);
            TestState stateTwo = new TestState(Id.create(2), true, 0);
            TestState stateThree = new TestState(Id.create(3), true, 1);

            cache.addAll(ImmutableSet.of(stateOne, stateTwo, stateThree));

            TestState stateFour = new TestState(Id.create(4), false, 2);

            assertThat(getIndex().after(stateFour)).contains(stateOne);

            // Add to cache, but does not pass predicate. Should not affect the result of after
            cache.update(null, stateFour);

            assertThat(getIndex().after(stateFour)).contains(stateOne);
        }

        private TestState getWithInvertedSomething(TestState state) {
            return new TestState(state.getId(), !state.isSomething(), state.getValue());
        }
    }
}
