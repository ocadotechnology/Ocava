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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Comparator;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.SimpleLongIdentified;

class SubTypeSortedOneToManyIndexTest {
    private static final int KEY = 100;

    private IndexedImmutableObjectCache<TestState, TestState> cache;
    private SortedOneToManyIndex<Integer, ExtendedTestState> index;

    @BeforeEach
    void init() {
        cache = IndexedImmutableObjectCache.createHashMapBackedCache();
        // Do not inline: this verifies addSubTypeSortedOneToManyIndex accepts Function<? super S, R> and Comparator<? super S>.
        Function<TestState, Integer> indexFunction = TestState::getKey;
        Comparator<TestState> comparator = Comparator.comparingLong(state -> state.getId().id);
        index = cache.addSubTypeSortedOneToManyIndex(null, ExtendedTestState.class, indexFunction, comparator);
    }

    @Test
    void add_whenBaseTypeStateAdded_thenStateNotIndexed() {
        cache.add(new TestState(Id.create(1), KEY));
        assertThat(index.size(KEY)).isZero();
    }

    @Test
    void add_whenBaseAndSubTypeStatesAdded_thenOnlySubTypeStateIndexed() {
        cache.add(new TestState(Id.create(1), KEY));
        ExtendedTestState extended = new ExtendedTestState(Id.create(2), KEY);
        cache.add(extended);

        assertThat(index.stream(KEY)).containsExactly(extended);
    }

    @Test
    void update_whenBaseTypeStateReplacedBySubType_thenThrowsException() {
        TestState baseState = new TestState(Id.create(1), KEY);
        cache.add(baseState);

        ExtendedTestState extended = new ExtendedTestState(Id.create(1), KEY);

        assertThatThrownBy(() -> cache.update(baseState, extended))
                .isInstanceOf(CacheUpdateException.class)
                .hasRootCauseInstanceOf(IndexUpdateException.class);

        assertThat(index.size(KEY)).isZero();
    }

    @Test
    void update_whenSubTypeStateReplacedByBaseType_thenThrowsException() {
        ExtendedTestState extended = new ExtendedTestState(Id.create(1), KEY);
        cache.add(extended);

        TestState baseState = new TestState(Id.create(1), KEY);

        assertThatThrownBy(() -> cache.update(extended, baseState))
                .isInstanceOf(CacheUpdateException.class)
                .hasRootCauseInstanceOf(IndexUpdateException.class);

        assertThat(index.stream(KEY)).containsExactly(extended);
    }

    private static class TestState extends SimpleLongIdentified<TestState> {
        private final int key;

        TestState(Id<TestState> id, int key) {
            super(id);
            this.key = key;
        }

        Integer getKey() {
            return key;
        }
    }

    private static final class ExtendedTestState extends TestState {
        ExtendedTestState(Id<TestState> id, int key) {
            super(id, key);
        }
    }
}
