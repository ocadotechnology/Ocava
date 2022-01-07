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
package com.ocadotechnology.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

abstract class SimpleIdentifiedTest<I extends Identity<Object>, T extends SimpleIdentified<Object, I>> {

    private final I id;
    
    private T simpleIdentified;

    public SimpleIdentifiedTest(I id) {
        this.id = id;
    }
    
    abstract T buildTestInstance(I id);
    
    abstract I getDifferentId(I id);

    @BeforeEach
    void setUp() {
        simpleIdentified = buildTestInstance(id);
    }

    @Test
    void getIdReturnsIdPassedIntoConstructor() {
        assertSame(id, simpleIdentified.getId());
    }

    @Test
    void hashCodeReturnsHashCodeOfId() {
        assertEquals(id.hashCode(), simpleIdentified.hashCode());
    }

    @Test
    @SuppressWarnings({"ObjectEqualsNull", "ConstantConditions", "SimplifiableJUnitAssertion"})
    void isNotEqualToNull() {
        // IMPORTANT: do not use assertNotEquals() as that will not actually call .equals() so the test will be invalid
        assertFalse(simpleIdentified.equals(null));
    }

    @Test
    void isEqualToItself() {
        assertEquals(simpleIdentified, simpleIdentified);
    }

    @Test
    void isEqualToInstanceWithSameId() {
        SimpleIdentified<Object, I> instanceWithSameId = buildTestInstance(simpleIdentified.getId());
        assertEquals(simpleIdentified, instanceWithSameId);
        assertEquals(instanceWithSameId, simpleIdentified);
    }

    @Test
    void isNotEqualToInstanceWithDifferentId() {
        I otherId = getDifferentId(simpleIdentified.getId());
        assertNotEquals(simpleIdentified.getId(), otherId);
        T instanceWithDifferentId = buildTestInstance(otherId);
        assertNotEquals(simpleIdentified, instanceWithDifferentId);
        assertNotEquals(instanceWithDifferentId, simpleIdentified);
    }
    
    @Test
    void isNotEqualToInstanceWithDifferentClass() {
        SimpleIdentified<Object, I> instanceWithDifferentClass = new SimpleIdentified<Object, I>(simpleIdentified.getId()) {};
        assertNotEquals(simpleIdentified, instanceWithDifferentClass);
        assertNotEquals(instanceWithDifferentClass, simpleIdentified);
    }
}
