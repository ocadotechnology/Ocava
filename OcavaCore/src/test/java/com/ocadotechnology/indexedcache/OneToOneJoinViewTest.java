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

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.SimpleLongIdentified;

@DisplayName("A OneToOneJoinView")
class OneToOneJoinViewTest {
    private static class ObjectA extends SimpleLongIdentified<ObjectA> {
        private final Optional<String> joinId;

        ObjectA(long id, String joinId) {
            super(Id.create(id));
            this.joinId = Optional.ofNullable(joinId);
        }

        Optional<String> getJoinId() {
            return joinId;
        }
    }

    private static class ObjectB extends SimpleLongIdentified<ObjectB> {
        private final Optional<String> joinId;

        ObjectB(long id, String joinId) {
            super(Id.create(id));
            this.joinId = Optional.ofNullable(joinId);
        }

        Optional<String> getJoinId() {
            return joinId;
        }
    }

    @Nested
    @DisplayName("constructed with OptionalOneToOneIndex instances")
    class WithIndexes {
        private IndexedImmutableObjectCache<ObjectA, ObjectA> objectACache;
        private IndexedImmutableObjectCache<ObjectB, ObjectB> objectBCache;
        private OptionalOneToOneIndex<String, ObjectA> objectAByJoinIndex;
        private OptionalOneToOneIndex<String, ObjectB> objectBByJoinIndex;
        private OneToOneJoinView<ObjectA, ObjectA, ObjectB, ObjectB, String> joinView;

        @BeforeEach
        void setUp() {
            objectACache = IndexedImmutableObjectCache.createHashMapBackedCache();
            objectBCache = IndexedImmutableObjectCache.createHashMapBackedCache();

            objectAByJoinIndex = objectACache.addOptionalOneToOneIndex("objectAByJoin", ObjectA::getJoinId);
            objectBByJoinIndex = objectBCache.addOptionalOneToOneIndex("objectBByJoin", ObjectB::getJoinId);

            joinView = new OneToOneJoinView<>(objectACache, objectBCache, objectAByJoinIndex, objectBByJoinIndex);
        }
        
        @Test
        @DisplayName("in pre-populated cache, pairs are established")
        void existingElementsArePaired() {
            // Re-create with pre-populated caches
            objectACache = IndexedImmutableObjectCache.createHashMapBackedCache();
            objectBCache = IndexedImmutableObjectCache.createHashMapBackedCache();
            objectAByJoinIndex = objectACache.addOptionalOneToOneIndex("objectAByJoin", ObjectA::getJoinId);
            objectBByJoinIndex = objectBCache.addOptionalOneToOneIndex("objectBByJoin", ObjectB::getJoinId);

            ObjectA objectA = new ObjectA(1, "join-Z");
            ObjectB objectB = new ObjectB(100, "join-Z");
            objectACache.add(objectA);
            objectBCache.add(objectB);

            // Create join view AFTER both caches are populated
            joinView = new OneToOneJoinView<>(objectACache, objectBCache, objectAByJoinIndex, objectBByJoinIndex);

            assertThat(joinView.getB(objectA.getId())).contains(objectB);
            assertThat(joinView.getA(objectB.getId())).contains(objectA);
        }

        @Nested
        @DisplayName("when adding elements")
        class Adding {
            @Test
            @DisplayName("adding A then B creates a pair")
            void addAThenB_createsPair() {
                ObjectA objectA = new ObjectA(1, "join-1");
                ObjectB objectB = new ObjectB(100, "join-1");

                objectACache.add(objectA);
                objectBCache.add(objectB);

                assertThat(joinView.getB(objectA.getId())).contains(objectB);
                assertThat(joinView.getA(objectB.getId())).contains(objectA);
            }

            @Test
            @DisplayName("adding B then A creates a pair")
            void addBThenA_createsPair() {
                ObjectB objectB = new ObjectB(100, "join-2");
                ObjectA objectA = new ObjectA(2, "join-2");

                objectBCache.add(objectB);
                objectACache.add(objectA);

                assertThat(joinView.getB(objectA.getId())).contains(objectB);
                assertThat(joinView.getA(objectB.getId())).contains(objectA);
            }

            @Test
            @DisplayName("adding A without a matching B returns empty")
            void addAWithoutMatchingB_returnsEmpty() {
                ObjectA objectA = new ObjectA(1, "join-orphan");
                objectACache.add(objectA);

                assertThat(joinView.getB(objectA.getId())).isEmpty();
            }

            @Test
            @DisplayName("adding B without a matching A returns empty")
            void addBWithoutMatchingA_returnsEmpty() {
                ObjectB objectB = new ObjectB(100, "join-orphan");
                objectBCache.add(objectB);

                assertThat(joinView.getA(objectB.getId())).isEmpty();
            }

            @Test
            @DisplayName("elements with no join key are not paired")
            void elementsWithNoKey_areNotPaired() {
                ObjectA objectA = new ObjectA(1, null);
                ObjectB objectB = new ObjectB(100, null);

                objectACache.add(objectA);
                objectBCache.add(objectB);

                assertThat(joinView.getB(objectA.getId())).isEmpty();
                assertThat(joinView.getA(objectB.getId())).isEmpty();
            }

            @Test
            @DisplayName("multiple distinct pairs")
            void multipleDistinctPairs() {
                ObjectA objectA1 = new ObjectA(1, "join-X");
                ObjectA objectA2 = new ObjectA(2, "join-Y");
                ObjectB objectB1 = new ObjectB(100, "join-X");
                ObjectB objectB2 = new ObjectB(200, "join-Y");

                objectACache.add(objectA1);
                objectACache.add(objectA2);
                objectBCache.add(objectB1);
                objectBCache.add(objectB2);

                assertThat(joinView.getB(objectA1.getId())).contains(objectB1);
                assertThat(joinView.getB(objectA2.getId())).contains(objectB2);
                assertThat(joinView.getA(objectB1.getId())).contains(objectA1);
                assertThat(joinView.getA(objectB2.getId())).contains(objectA2);
            }
        }

        @Nested
        @DisplayName("when removing elements")
        class Removing {
            @Test
            @DisplayName("removing A breaks the pair")
            void removingA_breaksPair() {
                ObjectA objectA = new ObjectA(1, "join-1");
                ObjectB objectB = new ObjectB(100, "join-1");
                objectACache.add(objectA);
                objectBCache.add(objectB);

                assertThat(joinView.getA(objectB.getId())).contains(objectA);
                objectACache.delete(objectA.getId());

                assertThat(joinView.getA(objectB.getId())).isEmpty();
            }

            @Test
            @DisplayName("removing B breaks the pair")
            void removingB_breaksPair() {
                ObjectA objectA = new ObjectA(1, "join-1");
                ObjectB objectB = new ObjectB(100, "join-1");
                objectACache.add(objectA);
                objectBCache.add(objectB);

                assertThat(joinView.getB(objectA.getId())).contains(objectB);
                objectBCache.delete(objectB.getId());

                assertThat(joinView.getB(objectA.getId())).isEmpty();
            }
        }

        @Nested
        @DisplayName("when updating elements")
        class Updating {
            @Test
            @DisplayName("updating A's join key breaks old pair and creates new pair")
            void updatingAJoinKey_breaksOldAndCreatesNew() {
                ObjectA objectA = new ObjectA(1, "join-1");
                ObjectB objectB1 = new ObjectB(100, "join-1");
                ObjectB objectB2 = new ObjectB(200, "join-2");
                objectACache.add(objectA);
                objectBCache.add(objectB1);
                objectBCache.add(objectB2);

                assertThat(joinView.getB(objectA.getId())).contains(objectB1);

                ObjectA updatedObjectA = new ObjectA(1, "join-2");
                objectACache.update(objectA, updatedObjectA);

                assertThat(joinView.getB(updatedObjectA.getId())).contains(objectB2);
                assertThat(joinView.getA(objectB1.getId())).isEmpty();
                assertThat(joinView.getA(objectB2.getId())).contains(updatedObjectA);
            }

            @Test
            @DisplayName("updating B's join key breaks old pair and creates new pair")
            void updatingBJoinKey_breaksOldAndCreatesNew() {
                ObjectA objectA1 = new ObjectA(1, "join-1");
                ObjectA objectA2 = new ObjectA(2, "join-2");
                ObjectB objectB = new ObjectB(100, "join-1");
                objectACache.add(objectA1);
                objectACache.add(objectA2);
                objectBCache.add(objectB);

                assertThat(joinView.getA(objectB.getId())).contains(objectA1);

                ObjectB updatedObjectB = new ObjectB(100, "join-2");
                objectBCache.update(objectB, updatedObjectB);

                assertThat(joinView.getA(updatedObjectB.getId())).contains(objectA2);
                assertThat(joinView.getB(objectA1.getId())).isEmpty();
                assertThat(joinView.getB(objectA2.getId())).contains(updatedObjectB);
            }

            @Test
            @DisplayName("updating A to remove join key breaks the pair")
            void updatingAToRemoveKey_breaksPair() {
                ObjectA objectA = new ObjectA(1, "join-1");
                ObjectB objectB = new ObjectB(100, "join-1");
                objectACache.add(objectA);
                objectBCache.add(objectB);
                assertThat(joinView.getB(objectA.getId())).contains(objectB);
                assertThat(joinView.getA(objectB.getId())).contains(objectA);

                ObjectA updatedObjectA = new ObjectA(1, null);
                objectACache.update(objectA, updatedObjectA);

                assertThat(joinView.getB(updatedObjectA.getId())).isEmpty();
                assertThat(joinView.getA(objectB.getId())).isEmpty();
            }
        }
    }
}
