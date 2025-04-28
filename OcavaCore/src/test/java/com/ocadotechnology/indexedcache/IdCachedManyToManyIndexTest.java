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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.SimpleLongIdentified;
import com.ocadotechnology.indexedcache.IndexedImmutableObjectCache.Hints;

@DisplayName("IdCachedManyToManyIndex class tests")
class IdCachedManyToManyIndexTest {
    private IndexedImmutableObjectCache<TestObject, TestObject> cache;
    private ManyToManyIndex<Integer, TestObject> index;

    @BeforeEach
    void init() {
        cache = IndexedImmutableObjectCache.createHashMapBackedCache();
        index = cache.addManyToManyIndex("index name", TestObject::getKeys, Hints.optimiseForInfrequentChanges);
    }

    @Test
    void stream_whenKeyExists_returnsStreamOfObjects() {
        TestObject testObject = new TestObject(1, Set.of(1, 2, 3));
        cache.add(testObject);

        assertThat(index.stream(1)).containsExactly(testObject);
    }

    @Test
    void stream_whenKeyDoesNotExist_returnsEmptyStream() {
        assertThat(index.stream(1)).isEmpty();
    }

    @Test
    void stream_whenMultipleObjects_returnsStreamOfObjects() {
        TestObject testObject = new TestObject(1, Set.of(1, 2, 3));
        TestObject testObject2 = new TestObject(2, Set.of(1, 2, 4));
        cache.add(testObject);
        cache.add(testObject2);

        assertThat(index.stream(1)).containsExactlyInAnyOrder(testObject, testObject2);
        assertThat(index.stream(2)).containsExactlyInAnyOrder(testObject, testObject2);
        assertThat(index.stream(3)).containsExactlyInAnyOrder(testObject);
        assertThat(index.stream(4)).containsExactlyInAnyOrder(testObject2);
    }

    @Test
    void containsKey_whenKeyExists_thenTrue() {
        TestObject testObject = new TestObject(1, Set.of(1, 2, 3));
        cache.add(testObject);

        assertThat(index.containsKey(1)).isTrue();
    }

    @Test
    void containsKey_whenKeyDoesNotExist_thenFalse() {
        assertThat(index.containsKey(2)).isFalse();
    }

    @Test
    void streamKeySet_whenKeysExist_returnsStreamOfKeys() {
        TestObject testObject = new TestObject(1, Set.of(1, 2, 3));
        cache.add(testObject);

        assertThat(index.streamKeySet()).containsExactlyInAnyOrder(1, 2, 3);
    }

    @Test
    void streamKeySet_whenNoKeys_returnsEmptyStream() {
        assertThat(index.streamKeySet()).isEmpty();
    }

    @Test
    void countKeys_whenObjectExists_returnsKeyCount() {
        TestObject testObject = new TestObject(1, Set.of(1, 2, 3));
        cache.add(testObject);

        assertThat(index.countKeys()).isEqualTo(3);
    }

    @Test
    void countKeys_whenNoKeys_returns0() {
        assertThat(index.countKeys()).isEqualTo(0);
    }

    @Test
    void count_whenKeyExists_returnsCountOfObjects() {
        TestObject testObject = new TestObject(1, Set.of(1, 2, 3));
        cache.add(testObject);

        assertThat(index.count(1)).isEqualTo(1);
    }

    @Test
    void count_whenKeyDoesNotExist_returns0() {
        assertThat(index.count(1)).isEqualTo(0);
    }

    @Test
    void count_whenMultipleObjects_returnsCountOfObjects() {
        TestObject testObject = new TestObject(1, Set.of(1, 2, 3));
        TestObject testObject2 = new TestObject(2, Set.of(1, 2, 4));
        cache.add(testObject);
        cache.add(testObject2);

        assertThat(index.count(1)).isEqualTo(2);
        assertThat(index.count(2)).isEqualTo(2);
        assertThat(index.count(3)).isEqualTo(1);
        assertThat(index.count(4)).isEqualTo(1);
    }

    private static class TestObject extends SimpleLongIdentified<TestObject> {
        private final Set<Integer> keys;

        private TestObject(int id, Set<Integer> keys) {
            super(Id.create(id));
            this.keys = keys;
        }

        public Set<Integer> getKeys() {
            return keys;
        }

        @Override
        public String toString() {
            return "TestObject{id=" + getId() + ", " + "keys=" + keys + '}';
        }
    }
}