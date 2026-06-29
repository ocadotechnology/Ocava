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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Id;

/**
 * Verifies that {@link IndexedImmutableObjectCache#addAllWithParallelIndexUpdates} populates the object store and every registered
 * index exactly as the sequential {@link IndexedImmutableObjectCache#addAll} would, and that listeners still fire.
 */
class AddAllWithParallelIndexUpdatesTest {

    private final IndexedImmutableObjectCache<TestState, TestState> cache = IndexedImmutableObjectCache.createHashMapBackedCache();
    private final OneToOneIndex<Long, TestState> valueIndex = cache.addOneToOneIndex(TestState::getValue);
    private final PredicateCountValue<TestState> somethingCount = cache.addPredicateCount(TestState::isSomething);
    private final TestIndex customIndex = cache.registerCustomIndex(new TestIndex());

    private static ImmutableList<TestState> buildStates(int count) {
        List<TestState> states = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            states.add(new TestState(Id.create(i), i % 2 == 0, i));
        }
        return ImmutableList.copyOf(states);
    }

    @Test
    void addAllWithParallelIndexUpdates_populatesObjectStoreAndEveryIndex() throws CacheUpdateException {
        ImmutableList<TestState> states = buildStates(1_000);

        cache.addAllWithParallelIndexUpdates(ImmutableSet.copyOf(states));

        assertThat(cache.size()).isEqualTo(states.size());
        assertThat(valueIndex.streamKeySet()).hasSize(states.size());

        long expectedSomething = states.stream().filter(TestState::isSomething).count();
        assertThat(somethingCount.getValue()).isEqualTo((int) expectedSomething);

        states.forEach(state -> {
            assertThat(cache.get(state.getId())).isSameAs(state);
            assertThat(valueIndex.get(state.getValue())).isSameAs(state);
        });

        // The custom index must have seen every object added exactly once, and nothing removed.
        assertThat(customIndex.getRecordedActions()).hasSize(states.size());
    }

    @Test
    void addAllWithParallelIndexUpdates_producesSameIndexStateAsSequentialAddAll() throws CacheUpdateException {
        ImmutableSet<TestState> states = ImmutableSet.copyOf(buildStates(500));

        IndexedImmutableObjectCache<TestState, TestState> sequentialCache = IndexedImmutableObjectCache.createHashMapBackedCache();
        OneToOneIndex<Long, TestState> sequentialValueIndex = sequentialCache.addOneToOneIndex(TestState::getValue);
        PredicateCountValue<TestState> sequentialSomethingCount = sequentialCache.addPredicateCount(TestState::isSomething);
        sequentialCache.addAll(states);

        cache.addAllWithParallelIndexUpdates(states);

        assertThat(cache.size()).isEqualTo(sequentialCache.size());
        assertThat(somethingCount.getValue()).isEqualTo(sequentialSomethingCount.getValue());
        assertThat(valueIndex.streamKeySet())
                .containsExactlyInAnyOrderElementsOf(sequentialValueIndex.streamKeySet().collect(ImmutableList.toImmutableList()));
    }

    @Test
    void addAllWithParallelIndexUpdates_withEmptyCollection_isANoOp() throws CacheUpdateException {
        cache.addAllWithParallelIndexUpdates(ImmutableSet.of());

        assertThat(cache.size()).isZero();
        assertThat(somethingCount.getValue()).isZero();
        assertThat(customIndex.getRecordedActions()).isEmpty();
    }

    @Test
    void addAllWithParallelIndexUpdates_notifiesStateAddedListenerForEveryObject() throws CacheUpdateException {
        List<TestState> notified = new ArrayList<>();
        cache.registerStateAddedListener(notified::add);

        ImmutableList<TestState> states = buildStates(200);
        cache.addAllWithParallelIndexUpdates(ImmutableSet.copyOf(states));

        assertThat(notified).containsExactlyInAnyOrderElementsOf(states);
    }

    @Test
    void addAllWithParallelIndexUpdates_notifiesEveryStateAddedListenerOnTheCallingThread() throws CacheUpdateException {
        // The index updates fan out across worker threads, but state change notifications must not: register two
        // listeners and assert each one is notified, and that every notification arrives on the single calling
        // (source) thread - never on a parallel index-update worker thread.
        Thread callingThread = Thread.currentThread();
        List<Thread> firstListenerThreads = Collections.synchronizedList(new ArrayList<>());
        List<Thread> secondListenerThreads = Collections.synchronizedList(new ArrayList<>());
        cache.registerStateAddedListener(state -> firstListenerThreads.add(Thread.currentThread()));
        cache.registerStateAddedListener(state -> secondListenerThreads.add(Thread.currentThread()));

        cache.addAllWithParallelIndexUpdates(ImmutableSet.copyOf(buildStates(200)));

        assertThat(firstListenerThreads).isNotEmpty().containsOnly(callingThread);
        assertThat(secondListenerThreads).isNotEmpty().containsOnly(callingThread);
    }
}
