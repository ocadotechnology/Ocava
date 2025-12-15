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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.SimpleLongIdentified;

@DisplayName("A OneToManyCount")
class OneToManyCountValueTest {
    private static final String INDEX_NAME = "TEST_ONE_TO_MANY_COUNT";

    @Nested
    class CacheTypeTests extends CountTests {
        @Override
        OneToManyCountValue<CountableType, TestState> addCountToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            return cache.addOneToManyCount(INDEX_NAME, TestState::getCountableType);
        }
    }

    @Nested
    class CacheSubTypeTests extends CountTests {
        @Override
        OneToManyCountValue<CountableType, TestState> addCountToCache(IndexedImmutableObjectCache<TestState, TestState> cache) {
            // IMPORTANT:
            // DO NOT inline indexFunction, as that will not fail to compile should addOneToManyCount() require a type
            // of Function<TestState, CountableType> instead of Function<? super CountableType, CountableType>, due to
            // automatic type coercion of the lambda.
            Function<StateWithCountableType, CountableType> indexFunction = StateWithCountableType::getCountableType;
            return cache.addOneToManyCount(INDEX_NAME, indexFunction);
        }
    }

    private abstract static class CountTests {
        private static final Id<TestState> ID_1 = Id.create(1);
        private static final Id<TestState> ID_2 = Id.create(2);

        private IndexedImmutableObjectCache<TestState, TestState> cache;
        private OneToManyCountValue<CountableType, TestState> count;

        abstract OneToManyCountValue<CountableType, TestState> addCountToCache(IndexedImmutableObjectCache<TestState, TestState> cache);

        @BeforeEach
        void init() {
            cache = IndexedImmutableObjectCache.createHashMapBackedCache();
            count = addCountToCache(cache);
        }

        @Test
        void getValue_whenCacheIsEmpty_thenCountForAnyTypeIsZero() {
            assertEquals(0, count.getValue(CountableType.VALUE_1));
        }

        @Test
        void add_whenObjectWithTypeIsAdded_thenCountIsOne() {
            cache.add(new TestState(ID_1, CountableType.VALUE_1));
            assertEquals(1, count.getValue(CountableType.VALUE_1));
        }

        @Test
        void add_whenTwoObjectsWithSameTypeAreAdded_thenCountIsTwo() {
            cache.addAll(ImmutableSet.of(
                    new TestState(ID_1, CountableType.VALUE_1),
                    new TestState(ID_2, CountableType.VALUE_1)
            ));
            assertEquals(2, count.getValue(CountableType.VALUE_1));
        }

        @Test
        void add_whenTwoObjectsWithDifferentTypesAreAdded_thenCountForEachIsOne() {
            cache.addAll(ImmutableSet.of(
                    new TestState(ID_1, CountableType.VALUE_1),
                    new TestState(ID_2, CountableType.VALUE_2)
            ));
            assertEquals(1, count.getValue(CountableType.VALUE_1));
            assertEquals(1, count.getValue(CountableType.VALUE_2));
        }

        @Test
        void add_whenFunctionReturnsNull_thenThrowException() {
            assertThatThrownBy(() -> cache.add(new TestState(ID_1, null)))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(INDEX_NAME);
        }

        @Test
        void remove_whenObjectIsRemoved_thenCountIsZero() {
            cache.add(new TestState(ID_1, CountableType.VALUE_1));
            assertEquals(1, count.getValue(CountableType.VALUE_1));

            cache.delete(ID_1);
            assertEquals(0, count.getValue(CountableType.VALUE_1));
        }

        @Test
        void remove_whenTwoObjectsWithSameTypeAreAddedAndOneIsRemoved_thenCountIsOne() {
            cache.addAll(ImmutableSet.of(
                    new TestState(ID_1, CountableType.VALUE_1),
                    new TestState(ID_2, CountableType.VALUE_1)
            ));
            assertEquals(2, count.getValue(CountableType.VALUE_1));

            cache.delete(ID_1);
            assertEquals(1, count.getValue(CountableType.VALUE_1));
        }

        @Test
        void remove_whenObjectIsRemoved_thenCountOfOtherTypesAreUnaffected() {
            cache.addAll(ImmutableSet.of(
                    new TestState(ID_1, CountableType.VALUE_1),
                    new TestState(ID_2, CountableType.VALUE_2)
            ));
            assertEquals(1, count.getValue(CountableType.VALUE_1));
            assertEquals(1, count.getValue(CountableType.VALUE_2));

            cache.delete(ID_1);
            assertEquals(0, count.getValue(CountableType.VALUE_1));
            assertEquals(1, count.getValue(CountableType.VALUE_2));
        }
    }

    private interface StateWithCountableType {
        CountableType getCountableType();
    }

    private static final class TestState extends SimpleLongIdentified<TestState> implements StateWithCountableType {
        private final CountableType countableType;

        private TestState(Id<TestState> id, CountableType countableType) {
            super(id);
            this.countableType = countableType;
        }

        @Override
        public CountableType getCountableType() {
            return countableType;
        }
    }

    private enum CountableType {
        VALUE_1,
        VALUE_2
    }
}