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

import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Id;

class PredicateIndexTest {

    @Nested
    class CacheTypeTests extends IndexTests {
        @Override
        PredicateIndex<TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            return cache.addPredicateIndex(TestState::isSomething);
        }
    }

    @Nested
    class CacheSubTypeTests extends IndexTests {
        @Override
        PredicateIndex<TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            // IMPORTANT:
            // DO NOT inline indexFunction, as that will not fail to compile should addPredicateIndex() require a type of
            // Predicate<TestState> instead of Predicate<? super TestState>, due to automatic type coercion of the lambda
            Predicate<TestThing> indexFunction = TestThing::isSomething;
            return cache.addPredicateIndex(indexFunction);
        }
    }

    private abstract static class IndexTests {

        private IndexedImmutableObjectCache<TestState, TestState> cache;
        private PredicateIndex<TestState> index;

        abstract PredicateIndex<TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache);

        @BeforeEach
        void init() {
            cache = IndexedImmutableObjectCache.createHashMapBackedCache();
            index = addIndexToCache(cache);
        }

        @Test
        void putOrUpdate_whenIsSomethingChanges_thenStateIsMovedAcrossIndex() {
            TestState firstState = new TestState(Id.create(1), true, 0);
            cache.add(firstState);
            assertThat(index.stream()).first().isEqualTo(firstState);

            TestState secondState = new TestState(Id.create(1), false, 0);
            cache.update(firstState, secondState);
            assertThat(index.stream()).isEmpty();
            assertThat(index.streamWhereNot()).first().isEqualTo(firstState);
        }

        @Test
        void count_givesTheNumberOfStatesInTheIndex() {
            TestState stateOne = new TestState(Id.create(1), true, 0);
            TestState stateTwo = new TestState(Id.create(2), true, 0);
            TestState stateThree = new TestState(Id.create(3), true, 0);

            cache.addAll(ImmutableSet.of(stateOne, stateTwo, stateThree));
            assertThat(index.count()).isEqualTo(3);
        }
    }
}
