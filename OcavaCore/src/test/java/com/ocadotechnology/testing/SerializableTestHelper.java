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
package com.ocadotechnology.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class SerializableTestHelper {
    
    private SerializableTestHelper() {
    }
    
    /**
     * Verifies that the given object defines a {@code private static final long serialVersionUID} field. All
     * serializable objects should have this field defined.
     *
     * @param object A serializable object.
     */
    public static void hasSerialVersionUID(Serializable object) {
        hasSerialVersionUID(object.getClass());
    }

    private static void hasSerialVersionUID(Class<?> clazz) {
        try {
            if (clazz.isInterface()) {
                // Interfaces do not require a serialVersionUID field
                return;
            }

            Field field = clazz.getDeclaredField("serialVersionUID");
            assertNotNull(field, "[" + clazz + "] must define a serialVersionUID field");

            int fieldModifier = field.getModifiers();
            assertTrue(Modifier.isPrivate(fieldModifier), "serialVersionUID should be private");
            assertTrue(Modifier.isStatic(fieldModifier), "serialVersionUID should be static");
            assertTrue(Modifier.isFinal(fieldModifier), "serialVersionUID should be final");
            assertEquals("long", field.getType().getName(), "serialVersionUID should be a primitive long");

        } catch (NoSuchFieldException ex) {
            fail("[" + clazz + "] does not define a serialVersionUID field");
        }
        
        Class<?> superClazz = clazz.getSuperclass();
        if (Serializable.class.isAssignableFrom(superClazz)) {
            hasSerialVersionUID(superClazz);
        }
    }

    /**
     * Verifies that the given object can be serialized and deserialized again, and that certain properties are
     * invariant.
     *
     * @param object An object which should be serializable.
     */
    public static void isSerializable(Serializable object) {

        Class<?> inClass = object.getClass();

        byte[] buffer;

        try {

            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);
            objectOut.writeObject(object);

            buffer = byteOut.toByteArray();

        } catch (IOException ex) {
            fail("Exception on serialization of test object [" + object + "] reason [" + ex + "]");
            return;
        }

        try {

            ByteArrayInputStream byteIn = new ByteArrayInputStream(buffer);
            ObjectInputStream objectIn = new ObjectInputStream(byteIn);

            Object reconstructedObject = inClass.cast(objectIn.readObject());

            assertNotNull(reconstructedObject, "Deserialization of object [" + object + "] failed");
            // Although the following tests will fail when equality and hash code are based on memory address, or when
            // toString() includes the memory address, such objects are not truly serializable in the strictest sense
            assertEquals(object, reconstructedObject, "Object [" + object + "] is not equal to reconstructed copy [" + reconstructedObject + "]");
            assertEquals(reconstructedObject, object, "Reconstructed copy [" + reconstructedObject + "] is not equal to original [" + object + "]");
            assertEquals(object.hashCode(), reconstructedObject.hashCode(), "Object [" + object + "] and [" + reconstructedObject + "] do not have equal hashCode()");
            assertEquals(object.toString(), reconstructedObject.toString(), "Object [" + object + "] and [" + reconstructedObject + "] do not have equal toString()");

        } catch (ClassNotFoundException | IOException ex) {
            fail("Exception on deserialization of test instance [" + object + "] reason [" + ex + "]");
        }
    }
}
