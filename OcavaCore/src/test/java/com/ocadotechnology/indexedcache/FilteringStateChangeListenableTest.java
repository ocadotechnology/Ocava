/*
 * Copyright © 2017-2022 Ocado (Ocava)
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.ocadotechnology.id.Id;

public class FilteringStateChangeListenableTest {
    private static final TestState state1 = new TestState(Id.create(1), true, 999);
    private static final TestState extended1 = new ExtendedTestState(Id.create(2), false, 1);

    private static final TestState state1Updated = new TestState(state1.getId(), false, 999);
    private static final TestState extended1Updated = new ExtendedTestState(extended1.getId(), true, 1);

    private static final TestState state2 = new TestState(Id.create(10), true, 100);
    private static final TestState extended2 = new ExtendedTestState(Id.create(20), false, 200);

    private IndexedImmutableObjectCache<TestState, TestState> cache;

    private boolean result;
    private TestState resultOld;
    private TestState resultUpdated;

    @BeforeEach
    void setup() {
        cache = IndexedImmutableObjectCache.createHashMapBackedCache();
        resetResult();
    }

    private void resetResult() {
        result = false;
        resultOld = null;
        resultUpdated = null;
    }

    @Test
    void whenOnlyListeningForExtendedStates_thenAddOnlyReceivesNewExtendedStates() {
        (new FilteringStateChangeListenable<>(cache, this::asExtended)).registerStateAddedListener(s -> {
            result = true;
            resultUpdated = s;
        });
        cache.add(state1);
        assertFalse(result);
        cache.add(extended1);
        assertTrue(result);
        assertEquals(extended1, resultUpdated);
        resetResult();;

        // Check that add doesn't fire on updates
        cache.update(state1, state1Updated);
        assertFalse(result);
        cache.update(extended1, extended1Updated);
        assertFalse(result);

        // Check that add doesn't fire on removals
        cache.delete(state1Updated.getId());
        assertFalse(result);
        cache.delete(extended1Updated.getId());
        assertFalse(result);
    }

    @Test
    void whenOnlyListeningForExtendedStates_thenChangeReceivesAllExtendedStateChanges() {
        (new FilteringStateChangeListenable<>(cache, this::asExtended)).registerStateChangeListener((old, updated) -> {
            result = true;
            resultOld = old;
            resultUpdated = updated;
        });

        cache.add(state1);
        assertFalse(result);
        cache.add(extended1);
        assertTrue(result);
        assertNull(resultOld);
        assertEquals(extended1, resultUpdated);
        resetResult();

        cache.update(state1, state1Updated);
        assertFalse(result);
        cache.update(extended1, extended1Updated);
        assertTrue(result);
        assertEquals(extended1, resultOld);
        assertEquals(extended1Updated, resultUpdated);
        resetResult();

        cache.delete(state1Updated.getId());
        assertFalse(result);
        cache.delete(extended1Updated.getId());
        assertEquals(extended1Updated, resultOld);
        assertNull(resultUpdated);
    }

    @Test
    void whenOnlyListeningForExtendedStates_thenAtomicChangeReceivesAllExtendedStateChanges() {
        cache.add(state1);
        cache.add(extended1);

        List<Change> updates = new ArrayList<>();
        (new FilteringStateChangeListenable<>(cache, this::asExtended)).registerAtomicStateChangeListener(changes -> {
            result = true;
            changes.forEach(updates::add);
        });

        cache.updateAll(ImmutableList.of(
                Change.change(extended1, extended1Updated),
                Change.add(state2),
                Change.add(extended2),
                Change.delete(state1)));

        assertTrue(result);
        assertEquals(2, updates.size());
        assertEquals(extended1, updates.get(0).originalObject);
        assertEquals(extended1Updated, updates.get(0).newObject);
        assertNull(updates.get(1).originalObject);
        assertEquals(extended2, updates.get(1).newObject);
    }

    @Test
    void whenOnlyListeningForExtendedStates_thenAtomicChangeReceivesAllExtendedStateDeletions() {
        cache.add(state1);
        cache.add(extended1);
        cache.add(state2);
        cache.add(extended2);

        List<Change> updates = new ArrayList<>();
        (new FilteringStateChangeListenable<>(cache, this::asExtended)).registerAtomicStateChangeListener(changes -> {
            result = true;
            changes.forEach(updates::add);
        });

        cache.updateAll(ImmutableList.of(
                Change.delete(state1),
                Change.delete(state2),
                Change.delete(extended1),
                Change.delete(extended2)));

        assertTrue(result);
        assertEquals(2, updates.size());
        assertEquals(extended1, updates.get(0).originalObject);
        assertNull(updates.get(0).newObject);
        assertEquals(extended2, updates.get(1).originalObject);
        assertNull(updates.get(0).newObject);
    }

    @Test
    void whenOnlyListeningForExtendedStates_thenRemoveOnlyReceivesDeletedExtendedStates() {
        (new FilteringStateChangeListenable<>(cache, this::asExtended)).registerStateRemovedListener(s -> {
            result = true;
            resultOld = s;
        });
        cache.add(state1);
        assertFalse(result);
        cache.add(extended1);
        assertFalse(result);

        // Check that add doesn't fire on updates
        cache.update(state1, state1Updated);
        assertFalse(result);
        cache.update(extended1, extended1Updated);
        assertFalse(result);

        // Check that add doesn't fire on removals
        cache.delete(state1Updated.getId());
        assertFalse(result);
        cache.delete(extended1Updated.getId());
        assertTrue(result);
        assertEquals(extended1Updated, resultOld);
    }

    @Test
    void whenOnlyListeningForExtendedStates_thenAddOrRemovedOnlyReceivedExtendedStates() {
        (new FilteringStateChangeListenable<>(cache, this::asExtended)).registerStateAddedOrRemovedListener(s -> {
            result = true;
            resultUpdated = s;
        });
        cache.add(state1);
        assertFalse(result);
        cache.add(extended1);
        assertTrue(result);
        assertEquals(extended1, resultUpdated);
        resetResult();

        // Check that add+remove doesn't fire on updates
        cache.update(state1, state1Updated);
        assertFalse(result);
        cache.update(extended1, extended1Updated);
        assertFalse(result);

        // Check that add+remove fires on removals
        cache.delete(state1Updated.getId());
        assertFalse(result);
        cache.delete(extended1Updated.getId());
        assertTrue(result);
        assertEquals(extended1Updated, resultUpdated);
    }

    @Test
    void whenOnlyListeningForExtendedStates_thenStreamOnlyReturnsThemInOrder() {
        cache.add(state1);
        cache.add(extended1);
        cache.add(state2);
        cache.add(extended2);
        ImmutableList<ExtendedTestState> states = (new FilteringStateChangeListenable<>(cache, this::asExtended)).stream().collect(ImmutableList.toImmutableList());
        assertEquals(2, states.size());
        assertEquals(extended1, states.get(0));
        assertEquals(extended2, states.get(1));
    }

    @Test
    void whenOnlyListeningForExtendedStates_thenIteratorOnlyReturnsThemInOrder() {
        cache.add(state1);
        cache.add(extended1);
        cache.add(state2);
        cache.add(extended2);
        UnmodifiableIterator<ExtendedTestState> iterator = (new FilteringStateChangeListenable<>(cache, this::asExtended)).iterator();
        assertTrue(iterator.hasNext());
        assertEquals(extended1, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(extended2, iterator.next());
        assertFalse(iterator.hasNext());
    }

    private ExtendedTestState asExtended(TestState s) {
        return s instanceof ExtendedTestState ? (ExtendedTestState)s : null;
    }
}
