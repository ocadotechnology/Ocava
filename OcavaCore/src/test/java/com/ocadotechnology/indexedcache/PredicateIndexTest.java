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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.function.Predicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.indexedcache.IndexedImmutableObjectCache.Hints;

class PredicateIndexTest {

    @Nested
    class CacheTypeTests extends IndexTests {
        @Override
        PredicateIndex<TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache, Hints hint) {
            return cache.addPredicateIndex(TestState::isSomething, hint);
        }
    }

    @Nested
    class CacheSubTypeTests extends IndexTests {
        @Override
        PredicateIndex<TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache, Hints hint) {
            // IMPORTANT:
            // DO NOT inline indexFunction, as that will not fail to compile should addPredicateIndex() require a type of
            // Predicate<TestState> instead of Predicate<? super TestState>, due to automatic type coercion of the lambda
            Predicate<TestThing> indexFunction = TestThing::isSomething;
            return cache.addPredicateIndex(indexFunction, hint);
        }
    }

    abstract static class IndexTests {

        protected IndexedImmutableObjectCache<TestState, TestState> cache;
        protected PredicateIndex<TestState> index;

        abstract PredicateIndex<TestState> addIndexToCache(IndexedImmutableObjectCache<TestState, TestState> cache, Hints hint);
        protected PredicateIndex<TestState> getIndex() {
            return index;
        }

        protected void initialise(Hints hint) {
            cache = IndexedImmutableObjectCache.createHashMapBackedCache();
            index = addIndexToCache(cache, hint);
        }

        @ParameterizedTest
        @EnumSource(Hints.class)
        void putOrUpdate_whenIsSomethingChanges_thenStateIsMovedAcrossIndex(Hints hint) {
            initialise(hint);

            TestState firstState = new TestState(Id.create(1), true, 0);
            cache.add(firstState);
            assertThat(index.stream()).first().isEqualTo(firstState);

            TestState secondState = new TestState(Id.create(1), false, 0);
            cache.update(firstState, secondState);
            assertThat(index.stream()).isEmpty();
            assertThat(index.streamWhereNot()).first().isEqualTo(firstState);
        }

        @ParameterizedTest
        @EnumSource(Hints.class)
        void forEach_appliesConsumerToEach(Hints hint) {
            initialise(hint);

            cache.add(new TestState(Id.create(1), false, 0));
            cache.add(new TestState(Id.create(2), true, 1));
            cache.add(new TestState(Id.create(3), true, 2));

            ArrayList<TestState> arrayList = new ArrayList<>();
            index.forEach(arrayList::add);

            assertEquals(2, arrayList.size());
            assertEquals(2, arrayList.get(0).getId().id);
            assertEquals(3, arrayList.get(1).getId().id);
        }

        @ParameterizedTest
        @EnumSource(Hints.class)
        void count_givesTheNumberOfStatesInTheIndex(Hints hint) {
            initialise(hint);

            TestState stateOne = new TestState(Id.create(1), true, 0);
            TestState stateTwo = new TestState(Id.create(2), true, 1);
            TestState stateThree = new TestState(Id.create(3), true, 2);

            cache.addAll(ImmutableSet.of(stateOne, stateTwo, stateThree));
            assertThat(index.count()).isEqualTo(3);
        }
    }
}
