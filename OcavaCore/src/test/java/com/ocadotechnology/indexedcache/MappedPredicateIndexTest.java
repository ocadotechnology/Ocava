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

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Id;

class MappedPredicateIndexTest {

    @Nested
    class CacheTypeTests extends IndexTests {
        MappedPredicateIndex<TestState, Long> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            return cache.addMappedPredicateIndex(TestState::isSomething, TestState::getValue);
        }
    }

    @Nested
    class CacheSubTypeTests extends IndexTests {
        MappedPredicateIndex<TestState, Long> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            // IMPORTANT:
            // DO NOT inline indexFunction or mappingFunction, as that will not fail to compile should addMappedPredicateIndex()
            // require parameter types of Predicate<TestState> or Function<TestState, Long> instead of Predicate<? super TestState>
            // or Function<? super TestState, Long>, due to automatic type coercion of the lambda
            Predicate<TestThing> indexFunction = TestThing::isSomething;
            Function<TestThing, Long> mappingFunction = TestThing::getValue;
            return cache.addMappedPredicateIndex(indexFunction, mappingFunction);
        }
    }

    private abstract static class IndexTests {

        private IndexedImmutableObjectCache<TestState, TestState> cache;
        private MappedPredicateIndex<TestState, Long> mappedPredicateIndex;

        abstract MappedPredicateIndex<TestState, Long> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache);

        @BeforeEach
        void init() {
            cache = IndexedImmutableObjectCache.createHashMapBackedCache();
            mappedPredicateIndex = addIndexToCache(cache);
        }

        /**
         * Black-box tests which verify the behaviour of an MappedPredicateIndex as defined by the public API.
         */
        @Nested
        class BehaviourTests {
            @Test
            void testAdd() {
                cache.add(new TestState(Id.create(1), true, 1));
                assertThat(mappedPredicateIndex.contains(1L)).isTrue();
            }

            @Test
            void testUpdate() {
                TestState firstState = new TestState(Id.create(1), true, 1);
                TestState secondState = new TestState(Id.create(1), false, 1);
                cache.add(firstState);
                cache.update(firstState, secondState);
                assertThat(mappedPredicateIndex.contains(1L)).isFalse();
            }

            @Test
            void testStreamDistinctWhere() {
                cache.add(new TestState(Id.create(1), true, 1));
                cache.add(new TestState(Id.create(2), true, 2));
                cache.add(new TestState(Id.create(3), false, 3));

                Set<Long> where = mappedPredicateIndex.streamDistinctWhere().collect(Collectors.toSet());

                assertThat(where).hasSize(2);
                assertThat(where).containsOnly(1L, 2L);
            }

            @Test
            void testStreamDistinctWhereNot() {
                cache.add(new TestState(Id.create(1), true, 1));
                cache.add(new TestState(Id.create(2), false, 2));
                cache.add(new TestState(Id.create(3), false, 3));

                Set<Long> whereNot = mappedPredicateIndex.streamDistinctWhereNot().collect(Collectors.toSet());

                assertThat(whereNot).hasSize(2);
                assertThat(whereNot).containsOnly(2L, 3L);
            }

            @Test
            void testMultipleStatesMapToSameObject() {
                TestState firstState = new TestState(Id.create(1), true, 1);
                cache.add(firstState);
                cache.add(new TestState(Id.create(2), true, 1));
                assertThat(mappedPredicateIndex.streamNonDistinctWhere()).hasSize(2);
                assertThat(mappedPredicateIndex.streamDistinctWhere()).hasSize(1);

                cache.update(firstState, new TestState(Id.create(1), false, 1));

                assertThat(mappedPredicateIndex.contains(1L)).isTrue();
            }

            @Test
            void testSnapshotsWhenIndexIsEmpty() {
                assertThat(mappedPredicateIndex.getDistinctWhere()).isEmpty();
                assertThat(mappedPredicateIndex.getDistinctWhereNot()).isEmpty();
            }

            @Test
            void testSnapshotsWhenIndexIsNonEmpty() {
                TestState testState1 = new TestState(Id.create(1), true, 1);
                TestState testState2 = new TestState(Id.create(2), false, 2);
                cache.addAll(ImmutableSet.of(testState1, testState2));

                assertThat(mappedPredicateIndex.getDistinctWhere()).containsOnly(1L);
                assertThat(mappedPredicateIndex.getDistinctWhereNot()).containsOnly(2L);
            }

            @Test
            void testSnapshotsWhenIndexRemovedFrom() {
                TestState testState1 = new TestState(Id.create(1), true, 1);
                TestState testState2 = new TestState(Id.create(2), true, 2);
                TestState testState3 = new TestState(Id.create(3), false, 3);
                TestState testState4 = new TestState(Id.create(4), false, 4);
                cache.addAll(ImmutableSet.of(testState1, testState2, testState3, testState4));

                // So calls below are not first calls
                mappedPredicateIndex.getDistinctWhere();
                mappedPredicateIndex.getDistinctWhereNot();

                cache.delete(testState1.getId());
                cache.delete(testState3.getId());

                assertThat(mappedPredicateIndex.getDistinctWhere()).containsOnly(testState2.getValue());
                assertThat(mappedPredicateIndex.getDistinctWhereNot()).containsOnly(testState4.getValue());
            }
        }
        
        /**
         * White-box tests which verify implementation details of MappedPredicateIndex that do not form part of the
         * public API. The behaviours verified by these tests are subject to change and should not be relied upon by
         * users of the MappedPredicateIndex class.
         */
        @Nested
        class ImplementationTests {
            private TestState testState1 = new TestState(Id.create(1), true, 1);
            private TestState testState2 = new TestState(Id.create(2), false, 2);

            Set<Long> firstWhereSnapshot = null;
            Set<Long> firstWhereNotSnapshot = null;

            @BeforeEach
            void initialiseCache() {
                // Ensure neither index is empty at start, to guarantee that tests will fail if one being tested is
                // regenerated: ImmutableSet.empty() is a singleton, so regenerated instances will be the same object
                TestState testState1 = new TestState(Id.create(1), true, 1);
                TestState testState2 = new TestState(Id.create(2), false, 2);
                cache.add(testState1);
                cache.add(testState2);

                firstWhereSnapshot = mappedPredicateIndex.getDistinctWhere();
                firstWhereNotSnapshot = mappedPredicateIndex.getDistinctWhereNot();

                assertThat(firstWhereSnapshot).containsOnly(1L);
                assertThat(firstWhereNotSnapshot).containsOnly(2L);
            }

            @Test
            void testSnapshotsWhenPredicateHoldsForAddedObject() {
                TestState testState3 = new TestState(Id.create(3), true, 3);
                cache.add(testState3);

                Set<Long> secondWhereSnapshot = mappedPredicateIndex.getDistinctWhere();
                Set<Long> secondWhereNotSnapshot = mappedPredicateIndex.getDistinctWhereNot();

                assertThat(firstWhereSnapshot).isNotSameAs(secondWhereSnapshot);
                assertThat(firstWhereNotSnapshot).isSameAs(secondWhereNotSnapshot);
            }

            @Test
            void testSnapshotsWhenPredicateDoesNotHoldForAddedObject() {
                TestState testState3 = new TestState(Id.create(3), false, 3);
                cache.add(testState3);

                Set<Long> secondWhereSnapshot = mappedPredicateIndex.getDistinctWhere();
                Set<Long> secondWhereNotSnapshot = mappedPredicateIndex.getDistinctWhereNot();

                assertThat(firstWhereSnapshot).isSameAs(secondWhereSnapshot);
                assertThat(firstWhereNotSnapshot).isNotSameAs(secondWhereNotSnapshot);
            }

            @Test
            void testSnapshotsWhenPredicateHoldsForRemovedObject() {
                cache.delete(testState1.getId());

                Set<Long> secondWhereSnapshot = mappedPredicateIndex.getDistinctWhere();
                Set<Long> secondWhereNotSnapshot = mappedPredicateIndex.getDistinctWhereNot();

                assertThat(firstWhereSnapshot).isNotSameAs(secondWhereSnapshot);
                assertThat(firstWhereNotSnapshot).isSameAs(secondWhereNotSnapshot);
            }

            @Test
            void testSnapshotsWhenPredicateDoesNotHoldForRemovedObject() {
                cache.delete(testState2.getId());

                Set<Long> secondWhereSnapshot = mappedPredicateIndex.getDistinctWhere();
                Set<Long> secondWhereNotSnapshot = mappedPredicateIndex.getDistinctWhereNot();

                assertThat(firstWhereSnapshot).isSameAs(secondWhereSnapshot);
                assertThat(firstWhereNotSnapshot).isNotSameAs(secondWhereNotSnapshot);
            }
        }
    }
}
