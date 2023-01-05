/*
 * Copyright © 2017-2023 Ocado (Ocava)
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ConcurrentModificationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ocadotechnology.id.Id;

public class ConcurrentAccessTest {
    private static final Id<TestState> ID_1 = Id.create(1);
    private static final Id<TestState> ID_2 = Id.create(2);

    private final IndexedImmutableObjectCache<TestState, TestState> cache = IndexedImmutableObjectCache.createHashMapBackedCache();

    @BeforeEach
    void setup() {
        cache.add(new TestState(ID_1, true, 10));
        cache.add(new TestState(ID_2, true, 13));
    }

    @Test
    void updateConcurrentlyFromSameThread_fails() {
        cache.registerStateChangeListener((oldState, newState) -> cache.delete(ID_2));

        ConcurrentModificationException e = assertThrows(ConcurrentModificationException.class, () -> cache.delete(ID_1));
        assertTrue(e.getMessage().contains(Thread.currentThread().getName()), "Error message should reference current thread");
    }

    @Test
    void queryConcurrentlyFromSameThread_passes() {
        cache.registerStateChangeListener((oldState, newState) -> cache.get(ID_2));
        cache.delete(ID_1);
    }
}
