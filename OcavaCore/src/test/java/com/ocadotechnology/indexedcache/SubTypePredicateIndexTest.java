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

import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.SimpleLongIdentified;

class SubTypePredicateIndexTest {
    private IndexedImmutableObjectCache<TestState, TestState> cache;
    private PredicateIndex<ExtendedTestState> index;

    @BeforeEach
    void init() {
        cache = IndexedImmutableObjectCache.createHashMapBackedCache();
        // Do not inline indexFunction: this verifies addSubTypePredicateIndex accepts a Predicate<? super S>.
        Predicate<TestState> indexFunction = TestState::isSomething;
        index = cache.addSubTypePredicateIndex(null, ExtendedTestState.class, indexFunction);
    }

    @Test
    void add_whenBaseTypeStateAdded_thenStateNotIndexed() {
        cache.add(new TestState(Id.create(1), true));

        assertThat(index.count()).isZero();
        assertThat(index.stream()).isEmpty();
        assertThat(index.streamWhereNot()).isEmpty();
    }

    @Test
    void add_whenBaseAndSubTypeStatesAdded_thenOnlySubTypeStatesIndexedByPredicateResult() {
        cache.add(new TestState(Id.create(1), true));

        ExtendedTestState matching = new ExtendedTestState(Id.create(2), true);
        ExtendedTestState notMatching = new ExtendedTestState(Id.create(3), false);
        cache.add(matching);
        cache.add(notMatching);

        assertThat(index.stream()).containsExactly(matching);
        assertThat(index.streamWhereNot()).containsExactly(notMatching);
    }

    @Test
    void update_whenBaseTypeStateReplacedBySubType_thenThrowsException() {
        TestState baseState = new TestState(Id.create(1), true);
        cache.add(baseState);

        ExtendedTestState extended = new ExtendedTestState(Id.create(1), true);

        assertThatThrownBy(() -> cache.update(baseState, extended))
                .isInstanceOf(CacheUpdateException.class)
                .hasRootCauseInstanceOf(IndexUpdateException.class);

        assertThat(index.count()).isZero();
    }

    @Test
    void update_whenSubTypeStateReplacedByBaseType_thenThrowsException() {
        ExtendedTestState extended = new ExtendedTestState(Id.create(1), true);
        cache.add(extended);

        TestState baseState = new TestState(Id.create(1), true);

        assertThatThrownBy(() -> cache.update(extended, baseState))
                .isInstanceOf(CacheUpdateException.class)
                .hasRootCauseInstanceOf(IndexUpdateException.class);

        assertThat(index.stream()).containsExactly(extended);
    }

    private static class TestState extends SimpleLongIdentified<TestState> {
        private final boolean something;

        TestState(Id<TestState> id, boolean something) {
            super(id);
            this.something = something;
        }

        boolean isSomething() {
            return something;
        }
    }

    private static final class ExtendedTestState extends TestState {
        ExtendedTestState(Id<TestState> id, boolean something) {
            super(id, something);
        }
    }
}

