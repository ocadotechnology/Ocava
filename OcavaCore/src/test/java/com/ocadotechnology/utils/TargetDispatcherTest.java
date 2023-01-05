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
package com.ocadotechnology.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.utils.TargetDispatcher.UnknownTargetException;

class TargetDispatcherTest {
    private TargetDispatcher<UUID, FunObject> dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new TargetDispatcher<>();
    }

    @Test
    void givenIdExists_whenTargetIsRegistered_thenReturnsFalse() {
        FunObject funObject = new FunObject(10);
        FunObject funObject2 = new FunObject(5);
        UUID funObjectId = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId, funObject);
        assertFalse(dispatcher.registerTarget(funObjectId, funObject2));
    }

    @Test
    void givenIdDoesNotExist_whenTargetIsRegistered_thenReturnsTrue() {
        FunObject funObject = new FunObject(5);
        UUID funObjectId = UUID.randomUUID();
        assertTrue(dispatcher.registerTarget(funObjectId, funObject));
    }

    @Test
    void whenTargetIsRegistered_thenHasTarget() {
        FunObject funObject = new FunObject(10);
        UUID funObjectId = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId, funObject);
        assertTrue(dispatcher.hasTarget(funObjectId));
    }

    @Test
    void whenTargetIsRegistered_thenGetTargetReturnsTarget() {
        FunObject funObject = new FunObject(10);
        UUID funObjectId = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId, funObject);
        assertEquals(funObject, dispatcher.getTarget(funObjectId));
        assertTrue(dispatcher.maybeGetTarget(funObjectId).isPresent());
    }

    @Test
    void whenMultipleTargetsRegistered_thenGetTargetReturnsCorrectTarget() {
        FunObject funObject = new FunObject(10);
        UUID funObjectId = UUID.randomUUID();
        dispatcher.registerTarget(UUID.randomUUID(), new FunObject(5));
        dispatcher.registerTarget(funObjectId, funObject);
        dispatcher.registerTarget(UUID.randomUUID(), new FunObject(25));
        assertEquals(funObject, dispatcher.getTarget(funObjectId));
    }

    @Test
    void whenMultipleTargetsRegistered_thenGetAllTargetsReturnsAllTargets() {
        FunObject funObject = new FunObject(10);
        FunObject funObject2 = new FunObject(5);
        FunObject funObject3 = new FunObject(25);
        dispatcher.registerTarget(UUID.randomUUID(), funObject);
        dispatcher.registerTarget(UUID.randomUUID(), funObject2);
        dispatcher.registerTarget(UUID.randomUUID(), funObject3);
        assertEquals(3, dispatcher.getAllTargets().size());
        assertTrue(dispatcher.getAllTargets().values().containsAll(ImmutableSet.of(funObject, funObject2, funObject3)));
    }

    @Test
    void givenMultipleTargetsRegistered_whenTargetDeregistered_thenGetTargetThrows() {
        FunObject funObject = new FunObject(10);
        UUID funObjectId = UUID.randomUUID();
        dispatcher.registerTarget(UUID.randomUUID(), new FunObject(5));
        dispatcher.registerTarget(funObjectId, funObject);
        dispatcher.registerTarget(UUID.randomUUID(), new FunObject(25));

        assertEquals(funObject, dispatcher.deregisterTarget(funObjectId));
        assertThrows(UnknownTargetException.class, () -> dispatcher.getTarget(funObjectId));
    }

    @Test
    void givenMultipleTargetsRegistered_whenAllTargetsDeregistered_thenGetAllTargetReturnsEmpty() {
        dispatcher.registerTarget(UUID.randomUUID(), new FunObject(5));
        dispatcher.registerTarget(UUID.randomUUID(), new FunObject(10));
        dispatcher.registerTarget(UUID.randomUUID(), new FunObject(25));

        dispatcher.deregisterAllTargets();
        assertTrue(dispatcher.getAllTargets().isEmpty());
    }

    @Test
    void whenDoTargetAction_thenTargetUpdated() {
        FunObject funObject = new FunObject(10);
        UUID funObjectId = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId, funObject);

        dispatcher.doTargetAction(funObjectId, fo -> fo.number++);
        assertEquals(11, dispatcher.getTarget(funObjectId).number);
    }

    @Test
    void givenNoTargets_whenDoTargetAction_thenThrows() {
        assertThrows(UnknownTargetException.class, () -> dispatcher.doTargetAction(UUID.randomUUID(), fo -> fo.number++));
    }

    @Test
    void whenDoTargetActionIfRegistered_thenTargetUpdated() {
        FunObject funObject = new FunObject(10);
        UUID funObjectId = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId, funObject);

        dispatcher.doTargetActionIfRegistered(funObjectId, fo -> fo.number++);
        assertEquals(11, dispatcher.getTarget(funObjectId).number);
    }

    @Test
    void givenNoTargets_whenDoTargetActionIfRegistered_thenDoesNotThrow() {
        assertDoesNotThrow(() -> dispatcher.doTargetActionIfRegistered(UUID.randomUUID(), fo -> fo.number++));
    }

    @Test
    void whenTargetUpdated_thenGetTargetReturnsLatest() {
        FunObject funObject = new FunObject(10);
        UUID funObjectId = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId, funObject);

        FunObject newFunObject = new FunObject(15);
        dispatcher.updateTarget(funObjectId, newFunObject);
        assertEquals(newFunObject, dispatcher.getTarget(funObjectId));
    }

    @Test
    void whenDoTargetFunction_thenTargetUpdated() {
        FunObject funObject = new FunObject(10);
        UUID funObjectId = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId, funObject);

        assertEquals(10, (int) dispatcher.doTargetFunction(funObjectId, fo -> fo.number));
    }

    @Test
    void givenNoTargets_whenDoTargetFunction_thenThrows() {
        assertThrows(UnknownTargetException.class, () -> dispatcher.doTargetFunction(UUID.randomUUID(), fo -> fo.number));
    }

    @Test
    void whenDoForAllTargets_thenAllTargetsUpdated() {
        FunObject funObject = new FunObject(10);
        UUID funObjectId = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId, funObject);

        FunObject funObject2 = new FunObject(12);
        UUID funObjectId2 = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId2, funObject2);

        dispatcher.doForAllTargets(fo -> fo.number++);
        assertEquals(11, dispatcher.getTarget(funObjectId).number);
        assertEquals(13, dispatcher.getTarget(funObjectId2).number);
    }

    @Test
    void whenDoForAllTargetsWhere_thenMatchingTargetsUpdated() {
        FunObject funObject = new FunObject(10);
        UUID funObjectId = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId, funObject);

        FunObject funObject2 = new FunObject(12);
        UUID funObjectId2 = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId2, funObject2);

        FunObject funObject3 = new FunObject(13);
        UUID funObjectId3 = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId3, funObject3);

        dispatcher.doForAllTargetsWhere(fo -> fo.number % 2 == 0, fo -> fo.number++);
        assertEquals(11, dispatcher.getTarget(funObjectId).number);
        assertEquals(13, dispatcher.getTarget(funObjectId2).number);
        assertEquals(13, dispatcher.getTarget(funObjectId3).number);
    }

    @Test
    void whenMapAllTargets_thenMappedTargetsReturned() {
        FunObject funObject = new FunObject(10);
        UUID funObjectId = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId, funObject);

        FunObject funObject2 = new FunObject(12);
        UUID funObjectId2 = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId2, funObject2);

        assertEquals(ImmutableSet.of(11, 13), dispatcher.mapAllTargets(fo -> fo.number + 1).collect(ImmutableSet.toImmutableSet()));
    }

    @Test
    void whenFilterAllTargets_thenMatchingTargetsReturned() {
        FunObject funObject = new FunObject(10);
        UUID funObjectId = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId, funObject);

        FunObject funObject2 = new FunObject(12);
        UUID funObjectId2 = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId2, funObject2);

        FunObject funObject3 = new FunObject(13);
        UUID funObjectId3 = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId3, funObject3);

        assertEquals(ImmutableSet.of(funObject, funObject2, funObject3), dispatcher.streamAllTargets().collect(ImmutableSet.toImmutableSet()));
    }

    @Test
    void whenStreamAllTargets_thenAllTargetsReturned() {
        FunObject funObject = new FunObject(10);
        UUID funObjectId = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId, funObject);

        FunObject funObject2 = new FunObject(12);
        UUID funObjectId2 = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId2, funObject2);

        FunObject funObject3 = new FunObject(13);
        UUID funObjectId3 = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId3, funObject3);

        assertEquals(ImmutableSet.of(funObject, funObject2), dispatcher.filterAllTargets(fo -> fo.number % 2 == 0).collect(ImmutableSet.toImmutableSet()));
    }

    @Test
    void givenMatchingTargets_whenAnyTargetMatches_thenReturnsTrue() {
        FunObject funObject = new FunObject(10);
        UUID funObjectId = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId, funObject);

        FunObject funObject2 = new FunObject(12);
        UUID funObjectId2 = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId2, funObject2);

        assertTrue(dispatcher.anyTargetMatches(fo -> fo.number % 3 == 0));
    }

    @Test
    void givenNoMatchingTargets_whenAnyTargetMatches_thenReturnsFalse() {
        FunObject funObject = new FunObject(10);
        UUID funObjectId = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId, funObject);

        FunObject funObject2 = new FunObject(12);
        UUID funObjectId2 = UUID.randomUUID();
        dispatcher.registerTarget(funObjectId2, funObject2);

        assertFalse(dispatcher.anyTargetMatches(fo -> fo.number % 2 == 1));
    }

    private static class FunObject {
        public int number;

        FunObject(int number) {
            this.number = number;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(number);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof FunObject && ((FunObject) obj).number == this.number;
        }
    }
}