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
package com.ocadotechnology.testing;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for classes that are not meant to be exposed to users of Ocava.
 */
public interface InternalClassTest extends ClassTest {

    @Test
    @DisplayName("is a class internal to Ocava")
    default void isPackagePrivateClass() {
        assertThat(Modifier.isPublic(getTestSubject().getModifiers()))
                .describedAs("This class is internal to Ocava, so should not be public.")
                .isFalse();
    }
}
