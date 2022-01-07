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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ocadotechnology.id.Id;

class ChangeTest {
    private static TestState OBJECT1_V1 = new TestState(Id.create(1), false, 0);
    private static TestState OBJECT1_V2 = new TestState(Id.create(1), true, 1);
    private static TestState OBJECT2 = new TestState(Id.create(2), false, 0);

    @Test
    void add_withNonNull_returnsExpected() {
        Change<TestState> add = Change.add(OBJECT1_V1);
        Assertions.assertNull(add.originalObject, "Original object set unexpectedly");
        Assertions.assertEquals(OBJECT1_V1, add.newObject, "New object not set as expected");
    }

    @Test
    void add_withNull_throwsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Change.add(null), "Expected exception on adding a null state");
    }

    @Test
    void update_withNonNullMatched_returnsExpected() {
        Change<TestState> update = Change.update(OBJECT1_V1, OBJECT1_V2);
        Assertions.assertEquals(OBJECT1_V1, update.originalObject, "Original object not set as expected");
        Assertions.assertEquals(OBJECT1_V2, update.newObject, "New object not set as expected");
    }

    @Test
    void update_withOldNull_throwsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Change.update(null, OBJECT1_V1), "Expected exception on updating from a null state");
    }

    @Test
    void update_withNewNull_throwsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Change.update(OBJECT1_V1, null), "Expected exception on updating to a null state");
    }

    @Test
    void update_withMismatchedIds_throwsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Change.update(OBJECT1_V1, OBJECT2), "Expected exception on updating with mismatched Ids");
    }

    @Test
    void delete_withNonNull_returnsExpected() {
        Change<TestState> delete = Change.delete(OBJECT1_V1);
        Assertions.assertEquals(OBJECT1_V1, delete.originalObject, "Original object not set as expected");
        Assertions.assertNull(delete.newObject, "New object set unexpectedly");
    }

    @Test
    void delete_withNull_throwsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Change.delete(null), "Expected exception on deleting a null state");
    }

    @Test
    void identity_withNonNull_returnsExpected() {
        Change<TestState> identity = Change.identity(OBJECT1_V1);
        Assertions.assertEquals(OBJECT1_V1, identity.originalObject, "Original object not set as expected");
        Assertions.assertEquals(OBJECT1_V1, identity.newObject, "New object not set as expected");
    }

    @Test
    void identity_withNull_throwsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Change.identity(null), "Expected exception on creating an identity change with a null state");
    }

    @Test
    void change_withNullOriginal_returnsAdd() {
        Change<TestState> change = Change.change(null, OBJECT1_V1);
        Assertions.assertNull(change.originalObject, "Original object set unexpectedly");
        Assertions.assertEquals(OBJECT1_V1, change.newObject, "New object not set as expected");
    }

    @Test
    void change_withNonNullMatched_returnsUpdate() {
        Change<TestState> change = Change.change(OBJECT1_V1, OBJECT1_V2);
        Assertions.assertEquals(OBJECT1_V1, change.originalObject, "Original object not set as expected");
        Assertions.assertEquals(OBJECT1_V2, change.newObject, "New object not set as expected");
    }

    @Test
    void change_withNullNew_returnsDelete() {
        Change<TestState> change = Change.change(OBJECT1_V1, null);
        Assertions.assertEquals(OBJECT1_V1, change.originalObject, "Original object not set as expected");
        Assertions.assertNull(change.newObject, "New object set unexpectedly");
    }

    @Test
    void change_withBothNull_throwsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Change.change(null, null), "Expected exception on changing null to null");
    }

    @Test
    void change_withMismatch_throwsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Change.change(OBJECT1_V1, OBJECT2), "Expected exception on changing with mismatched ids");
    }

    @Test
    void map_onChangingUpdate_thenReturnsExpected() {
        Change<TestState> update = Change.update(OBJECT1_V1, OBJECT1_V2);
        Change<TestState> mapped = update.map(s -> new TestState(s.getId(), s.isSomething(), s.longProperty + 5));
        Assertions.assertNotNull(mapped.newObject, "New object unset unexpectedly");
        Assertions.assertEquals(6, mapped.newObject.longProperty, "New object not updated as expected");
        Assertions.assertEquals(OBJECT1_V1.getId(), mapped.newObject.getId(), "New object not updated as expected");
    }

    @Test
    void map_onChangingAdd_thenReturnsExpected() {
        Change<TestState> update = Change.add(OBJECT1_V1);
        Change<TestState> mapped = update.map(s -> new TestState(s.getId(), s.isSomething(), s.longProperty + 5));
        Assertions.assertNotNull(mapped.newObject, "New object unset unexpectedly");
        Assertions.assertEquals(5, mapped.newObject.longProperty, "New object not updated as expected");
        Assertions.assertEquals(OBJECT1_V1.getId(), mapped.newObject.getId(), "New object not updated as expected");
    }

    @Test
    void map_onChangingDelete_thenThrowsException() {
        Change<TestState> update = Change.delete(OBJECT1_V1);
        Assertions.assertThrows(NullPointerException.class, () -> update.map(s -> OBJECT1_V2), "Expected exception on mapping a delete");
    }

    @Test
    void map_withNullMapper_thenThrowsException() {
        Change<TestState> update = Change.update(OBJECT1_V1, OBJECT1_V2);
        Assertions.assertThrows(IllegalArgumentException.class, () -> update.map(null), "Expected exception on null mapper");
    }

    @Test
    void map_withNullMapperReturn_thenThrowsException() {
        Change<TestState> update = Change.update(OBJECT1_V1, OBJECT1_V2);
        Assertions.assertThrows(NullPointerException.class, () -> update.map(s -> null), "Expected exception on null mapper return value");
    }

    @Test
    void map_withMismatchedMapperReturn_thenThrowsException() {
        Change<TestState> update = Change.update(OBJECT1_V1, OBJECT1_V2);
        Assertions.assertThrows(IllegalArgumentException.class, () -> update.map(s -> OBJECT2), "Expected exception on mapper return with mismatched id");
    }
}
