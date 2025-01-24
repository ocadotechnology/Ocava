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
package com.ocadotechnology.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

class LookupTest {

    static class TestClass extends SimpleIdentified<TestClass, StringId<TestClass>> {
        final long excitingData;

        TestClass(StringId<TestClass> id, long excitingData) {
            super(id);
            this.excitingData = excitingData;
        }
    }

    static final StringId<TestClass> FOO = StringId.create("foo");
    static final StringId<TestClass> BAR = StringId.create("bar");
    static final StringId<TestClass> BAZ = StringId.create("baz");
    static final StringId<TestClass> ABSENT_KEY = StringId.create("frob");
    static final String SENTINEL_STRING = "*SENTINEL*";
    static final String DEFAULT_STRING = "*DEFAULT*";

    static final Set<StringId<TestClass>> ID_SET = Set.of(FOO, BAR, BAZ);
    static final Map<StringId<TestClass>, String> ID_TO_STRING_MAP = new HashMap<>();
    static final Map<String, StringId<TestClass>> STRING_STRING_ID_MAP = new HashMap<>();

    static {
        ID_TO_STRING_MAP.put(FOO, "FOO");
        ID_TO_STRING_MAP.put(BAR, "BAR");
        ID_TO_STRING_MAP.put(BAZ, null);

        STRING_STRING_ID_MAP.put("FOO", FOO);
        STRING_STRING_ID_MAP.put("BAR", BAR);
    }

    @Test
    void givenSetContainsLookupInstance_whenInCalled_thenOptionalOfInstanceIsReturned() {
        assertEquals(Optional.of(FOO), FOO.getIn(ID_SET));
    }

    @Test
    void givenSetDoesNotContainLookupInstance_whenInCalled_thenEmptyOptionalIsReturned() {
        assertEquals(Optional.empty(), ABSENT_KEY.getIn(ID_SET));
    }

    @Test
    void givenNullSet_whenInCalled_thenEmptyOptionalIsReturned() {
        assertEquals(Optional.empty(), FOO.getIn((Set)null));
    }

    @Test
    void givenInstanceIsKeyInMap_whenGetInCalled_thenOptionalOfMappedValueIsReturned() {
        assertEquals(Optional.of("FOO"), FOO.getIn(ID_TO_STRING_MAP));
    }

    @Test
    void givenInstanceIsMappedToNullInMap_whenGetInCalled_thenEmptyOptionalIsReturned() {
        assertEquals(Optional.empty(), BAZ.getIn(ID_TO_STRING_MAP));
    }

    @Test
    void givenInstanceIsNotKeyInMap_whenGetInCalled_thenEmptyOptionalIsReturned() {
        assertEquals(Optional.empty(), ABSENT_KEY.getIn(ID_TO_STRING_MAP));
    }

    @Test
    void givenMapIsNull_whenGetInCalled_thenEmptyOptionalIsReturned() {
        assertEquals(Optional.empty(), ABSENT_KEY.getIn((Map<StringId<TestClass>, String>)null));
    }

    @Test
    void givenInstanceIsKeyInMap_whenGetInWithDefaultCalled_thenOptionalOfMappedValueIsReturned() {
        assertEquals(Optional.of("FOO"), FOO.getIn(ID_TO_STRING_MAP, DEFAULT_STRING));
    }

    @Test
    void givenInstanceIsNotKeyInMap_whenGetInWithDefaultCalled_thenOptionalOfDefaultIsReturned() {
        assertEquals(Optional.of(DEFAULT_STRING), ABSENT_KEY.getIn(ID_TO_STRING_MAP, DEFAULT_STRING));
    }

    @Test
    void givenMapIsNull_whenGetInWithDefaultCalled_thenEmptyOptionalIsReturned() {
        assertEquals(Optional.of(DEFAULT_STRING), ABSENT_KEY.getIn(null, DEFAULT_STRING));
    }

    @Test
    void givenInstanceIsNotKeyInMap_whenInstanceWithNullDefaultInCalled_thenEmptyOptionalIsReturned() {
        assertEquals(Optional.empty(), ABSENT_KEY.getIn(ID_TO_STRING_MAP, null));
    }

    @Test
    void givenMapIsNull_whenInstanceInWithNullDefaultCalled_thenEmptyOptionalIsReturned() {
        assertEquals(Optional.empty(), ABSENT_KEY.getIn(null, null));
    }

    @Test
    void givenInstanceIsMappedToNullInMap_whenGetInWithDefaultCalled_thenEmptyOptionalIsReturned() {
        assertEquals(Optional.empty(), BAZ.getIn(ID_TO_STRING_MAP, SENTINEL_STRING),
                "a default value used as a sentinel will not be returned if the value mapped was null");
    }

    /**
     * Although this test is identical to {@link #givenInstanceIsNotKeyInMap_whenGetInWithDefaultCalled_thenOptionalOfDefaultIsReturned()},
     * it illustrates along with {@link #givenInstanceIsMappedToNullInMap_whenGetInWithDefaultCalled_thenEmptyOptionalIsReturned()}
     * how a sentinel value can be used to distinguish between the case where the key is mapped to null vs. when the key is absent
     * from the map.
     */
    @Test
    void givenInstanceIsNotKeyInMap_whenGetInWithSentinelDefaultValueCalled_thenOptionalOfSentinelIsReturned() {
        assertEquals(Optional.of(SENTINEL_STRING), ABSENT_KEY.getIn(ID_TO_STRING_MAP, SENTINEL_STRING),
                "a default value used as a sentinel will be returned if the key is absent from the map");
    }

    // Tests to check type hierarchy works with all inheritors of Identity

    @Test
    void givenSetContainsId_whenInCalled_thenOptionalOfIdIsReturned() {
        Id<String> id = Id.create(42);
        Set<Id<String>> ids = Set.of(id);
        assertEquals(Optional.of(id), id.getIn(ids));
    }

    @Test
    void givenSetContainsStringId_whenInCalled_thenOptionalOfStringIdIsReturned() {
        StringId<Long> id = StringId.create("foo");
        Set<StringId<Long>> ids = Set.of(id);
        StringId<Long> gottenId = id.getIn(ids).orElseThrow();
        assertEquals(id, gottenId);
    }

    @Test
    void givenAMapIsKeyedOnAThingThatCanBeLookedUpAgainst_whenInCalled_thenOptionalOfAJoinedValueIsReturned() {
        TestClass thing = new TestClass(FOO, 42);
        String relatedDataString = "related data";
        Map<StringId<TestClass>, String> map = ImmutableMap.of(
                FOO, relatedDataString
        );
        assertEquals(Optional.of(relatedDataString), thing.getId().getIn(map));
    }

    @Test
    void givenValueIsInASet_whenIsInIsCalled_thenTrueIsReturned() {
        assertTrue(FOO.isIn(ID_SET));
    }

    @Test
    void givenValueIsNotInASet_whenIsInIsCalled_thenFalseIsReturned() {
        assertFalse(ABSENT_KEY.isIn(ID_SET));
    }

    @Test
    void givenArgumentSetIsNull_whenIsInIsCalled_thenFalseIsReturned() {
        assertFalse(FOO.isIn(null));
    }

    @Test
    void givenEntryWithKeyIsInMap_whenIsKeyInIsCalled_thenTrueIsReturned() {
        assertTrue(FOO.isKeyIn(ID_TO_STRING_MAP));
    }

    @Test
    void givenEntryWithKeyIsNotInMap_whenIsKeyInIsCalled_thenFalseIsReturned() {
        assertFalse(ABSENT_KEY.isKeyIn(ID_TO_STRING_MAP));
    }

    @Test
    void givenEntryWithKeyIsMappedToNullInMap_whenIsKeyInIsCalled_thenTrueIsReturned() {
        assertTrue(BAZ.isKeyIn(ID_TO_STRING_MAP));
    }

    @Test
    void givenArgumentMapIsNullInMap_whenIsKeyInIsCalled_thenFalseIsReturned() {
        assertFalse(BAZ.isKeyIn(null));
    }

    @Test
    void givenEntryWithValueIsInMap_whenIsValueInIsCalled_thenTrueIsReturned() {
        assertTrue(FOO.isValueIn(STRING_STRING_ID_MAP));
    }

    @Test
    void givenEntryWithValueIsNotInMap_whenIsValueInIsCalled_thenFalseIsReturned() {
        assertFalse(BAZ.isValueIn(STRING_STRING_ID_MAP));
    }

    @Test
    void givenArgumentMapIsNullInMap_whenIsValueInIsCalled_thenFalseIsReturned() {
        assertFalse(BAZ.isValueIn(null));
    }
}