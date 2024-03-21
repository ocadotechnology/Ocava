/*
 * Copyright © 2017-2024 Ocado (Ocava)
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
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.SimpleLongIdentified;

@DisplayName("ManyToManyIndex class tests")
class ManyToManyIndexTest {

    private ManyToManyIndex<Integer, TestObject> index;

    @BeforeEach
    void init() {
        index = new ManyToManyIndex<>(TestObject::getKeys);
    }

    @Test
    void containsKey_whenKeyExists_returnsTrue() {
        TestObject testObject = new TestObject(1, Set.of(1, 2, 3));
        index.add(testObject);

        assertThat(index.containsKey(1)).isTrue();
    }

    @Test
    void containsKey_whenKeyDoesNotExist_returnsFalse() {
        assertThat(index.containsKey(1)).isFalse();
    }

    @Test
    void stream_whenKeyExists_returnsStreamOfObjects() {
        TestObject testObject = new TestObject(1, Set.of(1, 2, 3));
        index.add(testObject);

        assertThat(index.stream(1)).containsExactly(testObject);
    }

    @Test
    void stream_whenKeyDoesNotExist_returnsEmptyStream() {
        assertThat(index.stream(1)).isEmpty();
    }

    @Test
    void count_whenKeyExists_returnsCorrectCount() {
        TestObject testObject1 = new TestObject(1, Set.of(1, 2, 3));
        TestObject testObject2 = new TestObject(2, Set.of(1, 4, 5));
        index.add(testObject1);
        index.add(testObject2);

        assertThat(index.count(1)).isEqualTo(2);
    }

    @Test
    void count_whenKeyDoesNotExist_returnsZero() {
        assertThat(index.count(1)).isEqualTo(0);
    }

    @Test
    void countKeys_whenObjectsAdded_returnsCorrectCount() {
        TestObject testObject1 = new TestObject(1, Set.of(1, 2, 3));
        TestObject testObject2 = new TestObject(2, Set.of(4, 5, 6));
        index.add(testObject1);
        index.add(testObject2);

        assertThat(index.countKeys()).isEqualTo(6);
    }

    @Test
    void countKeys_whenNoObjectsAdded_returnsZero() {
        assertThat(index.countKeys()).isEqualTo(0);
    }

    @Test
    void streamIncludingDuplicates_whenSingleKeyExists_returnsStreamOfObjects() {
        TestObject testObject1 = new TestObject(1, Set.of(1, 2, 3));
        TestObject testObject2 = new TestObject(2, Set.of(1, 4, 5));
        index.add(testObject1);
        index.add(testObject2);

        Stream<TestObject> result = index.streamIncludingDuplicates(ImmutableSet.of(1));
        assertThat(result).containsExactlyInAnyOrder(testObject1, testObject2);
    }

    @Test
    void streamIncludingDuplicates_whenKeysExist_returnsStreamOfObjects() {
        TestObject testObject1 = new TestObject(1, Set.of(1, 2, 3));
        TestObject testObject2 = new TestObject(2, Set.of(1, 4, 5));
        index.add(testObject1);
        index.add(testObject2);

        Stream<TestObject> result = index.streamIncludingDuplicates(ImmutableSet.of(1, 2, 3, 4));
        assertThat(result).containsExactlyInAnyOrder(testObject1, testObject2, testObject1, testObject1, testObject2);
    }

    @Test
    void streamIncludingDuplicates_whenKeysDoNotExist_returnsEmptyStream() {
        Stream<TestObject> result = index.streamIncludingDuplicates(ImmutableSet.of(1));
        assertThat(result).isEmpty();
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