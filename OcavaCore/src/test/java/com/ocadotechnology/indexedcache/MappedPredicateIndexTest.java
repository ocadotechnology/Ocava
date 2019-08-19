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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
    }
}
