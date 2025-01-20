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
package com.ocadotechnology.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import com.ocadotechnology.utils.ByTypeBiFunction.ByTypeBiFunctionBuilder;

class ByTypeBiFunctionTest {
    public static final String STRING_PREFIX = "This is a string: ";
    public static final String INTEGER_PREFIX = "This is an integer: ";
    public static final String IOEXCEPTION_PREFIX = "This is an IOException ";

    public static final String ANY_STRING = "Some input string";

    // Do not add Double.class or Exception.class -- used in test
    private static final BiFunction<Object, String, String> STRICT_BY_TYPE_FUNCTION = ByTypeBiFunction.<Object, String, String>builder()
            .withBiFunctionFor(String.class, (s, s2) -> STRING_PREFIX + s)
            .withBiFunctionFor(Integer.class, (i, s2) -> INTEGER_PREFIX + i)
            .withBiFunctionFor(IOException.class, (e, s2) -> IOEXCEPTION_PREFIX + e)
            .build();

    @Test
    void whenClassIsDefined_thenCallsExpectedFunction() {
        assertThat(STRICT_BY_TYPE_FUNCTION.apply("TestString", ANY_STRING)).startsWith(STRING_PREFIX);
        assertThat(STRICT_BY_TYPE_FUNCTION.apply(15, ANY_STRING)).startsWith(INTEGER_PREFIX);
        assertThat(STRICT_BY_TYPE_FUNCTION.apply(new IOException(), ANY_STRING)).startsWith(IOEXCEPTION_PREFIX);
    }

    @Test
    void whenClassIsNotDefined_thenThrowsException() {
        assertThatThrownBy(() -> STRICT_BY_TYPE_FUNCTION.apply(1.5, ANY_STRING)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void whenClassIsNotDefinedAsBehaviourIsNullable_thenReturnsNull() {
        BiFunction<Object, String, String> permissiveByTypeBiFunction = ByTypeBiFunction.<Object, String, String>builder()
                .withBiFunctionFor(String.class, (s, s2) -> STRING_PREFIX + s)
                .withBiFunctionFor(Integer.class, (i, s2) -> INTEGER_PREFIX + i)
                .withBiFunctionFor(IOException.class, (e, s2) -> IOEXCEPTION_PREFIX + e)
                .returningNullWhenNoBiFunctionFound()
                .build();
        assertThat(permissiveByTypeBiFunction.apply(1.5, ANY_STRING)).isNull();
    }

    @Test
    void whenOnlySubclassDefined_thenTreatedAsMissingEntry() {
        assertThatThrownBy(() -> STRICT_BY_TYPE_FUNCTION.apply(new Exception(), ANY_STRING)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void whenOnlySuperclassDefined_thenTreatedAsMissingEntry() {
        BiFunction<Object, String, String> strictSuperclassByTypeBiFunction = ByTypeBiFunction.<Object, String, String>builder()
                .withBiFunctionFor(Exception.class, (e, s2) -> "This is an Exception " + e)
                .build();
        assertThatThrownBy(() -> strictSuperclassByTypeBiFunction.apply(new IOException(), ANY_STRING)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void whenSameClassDefinedTwiceInBuilder_thenThrowsException() {
        ByTypeBiFunctionBuilder<Object, String, String> builder = ByTypeBiFunction.<Object, String, String>builder()
                .withBiFunctionFor(String.class, (s, s2) -> STRING_PREFIX + s)
                .withBiFunctionFor(String.class, (s, s2) -> STRING_PREFIX + s);
        assertThatThrownBy(builder::build).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void whenSuperclassAndSubclassAreDefinedInTheSameFunction_thenUsesTheCorrectFunction() {
        String exceptionPrefix = "This is an Exception ";
        BiFunction<Object, String, String> testByTypeBiFunction = ByTypeBiFunction.<Object, String, String>builder()
                .withBiFunctionFor(Exception.class, (e, s2) -> exceptionPrefix + e)
                .withBiFunctionFor(IOException.class, (e, s2) -> IOEXCEPTION_PREFIX + e)
                .build();

        assertThat(testByTypeBiFunction.apply(new IOException(), ANY_STRING)).startsWith(IOEXCEPTION_PREFIX);
        assertThat(testByTypeBiFunction.apply(new Exception(), ANY_STRING)).startsWith(exceptionPrefix);
    }
}