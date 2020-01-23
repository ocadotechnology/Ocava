/*
 * Copyright © 2017-2020 Ocado (Ocava)
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for classes that are not intended to only contain static utility methods.
 */
public interface UtilityClassTest extends ClassTest {

    @Test
    @DisplayName("cannot be instantiated")
    default void cannotBeInstantiated() {
        Class<?> clazz = getTestSubject();

        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            assertThat(Modifier.isPrivate(constructor.getModifiers())).as("Constructor not private: %s", constructor.toGenericString()).isTrue();

            if (constructor.getParameterCount() == 0) {
                constructor.setAccessible(true);
                assertThatThrownBy(constructor::newInstance).hasCauseInstanceOf(UnsupportedOperationException.class);
            }
        }

        for (Method method : clazz.getDeclaredMethods()) {
            assertThat(method).isNot(new Condition<>(m -> m.getReturnType().equals(clazz), "Method returns class type: %s", clazz));
        }
    }

    @Test
    @DisplayName("is final")
    default void isFinal() {
        assertThat(getTestSubject()).isFinal();
    }

    @Test
    @DisplayName("contains only static methods")
    default void isStatic() {
        for (Method method : getTestSubject().getDeclaredMethods()) {
            assertThat(Modifier.isStatic(method.getModifiers())).as("Method not static: %s", method.toGenericString()).isTrue();
        }
    }

    @Test
    @DisplayName("does not subclass another class")
    default void isNotASubclass() {
        assertThat(getTestSubject().getSuperclass()).isEqualTo(Object.class);
    }
}
