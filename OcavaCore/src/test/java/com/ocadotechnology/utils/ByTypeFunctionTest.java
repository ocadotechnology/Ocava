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
package com.ocadotechnology.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.ocadotechnology.utils.ByTypeFunction.ByTypeFunctionBuilder;

class ByTypeFunctionTest {
    public static final String STRING_PREFIX = "This is a string: ";
    public static final String INTEGER_PREFIX = "This is an integer: ";
    public static final String IOEXCEPTION_PREFIX = "This is an IOException ";

    private static final Function<Object, String> STRICT_BY_TYPE_FUNCTION = ByTypeFunction.<Object, String>builder()
            .withFunctionFor(String.class, s -> STRING_PREFIX + s)
            .withFunctionFor(Integer.class, i -> INTEGER_PREFIX + i)
            .withFunctionFor(IOException.class, e -> IOEXCEPTION_PREFIX + e)
            .build();

    @Test
    void whenClassIsDefined_thenCallsExpectedFunction() {
        assertThat(STRICT_BY_TYPE_FUNCTION.apply("TestString")).startsWith(STRING_PREFIX);
        assertThat(STRICT_BY_TYPE_FUNCTION.apply(15)).startsWith(INTEGER_PREFIX);
        assertThat(STRICT_BY_TYPE_FUNCTION.apply(new IOException())).startsWith(IOEXCEPTION_PREFIX);
    }

    @Test
    void whenClassIsNotDefined_thenThrowsException() {
        assertThatThrownBy(() -> STRICT_BY_TYPE_FUNCTION.apply(1.5)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void whenClassIsNotDefinedAsBehaviourIsNullable_thenReturnsNull() {
        Function<Object, String> permissiveByTypeFunction = ByTypeFunction.<Object, String>builder()
                .withFunctionFor(String.class, s -> STRING_PREFIX + s)
                .withFunctionFor(Integer.class, i -> INTEGER_PREFIX + i)
                .withFunctionFor(IOException.class, e -> IOEXCEPTION_PREFIX + e)
                .returningNullWhenNoFunctionFound()
                .build();
        assertThat(permissiveByTypeFunction.apply(1.5)).isNull();
    }

    @Test
    void whenOnlySubclassDefined_thenTreatedAsMissingEntry() {
        assertThatThrownBy(() -> STRICT_BY_TYPE_FUNCTION.apply(new Exception())).isInstanceOf(NullPointerException.class);
    }

    @Test
    void whenOnlySuperclassDefined_thenTreatedAsMissingEntry() {
        Function<Object, String> strictSuperclassByTypeFunction = ByTypeFunction.<Object, String>builder()
                .withFunctionFor(Exception.class, e -> "This is an Exception " + e)
                .build();
        assertThatThrownBy(() -> strictSuperclassByTypeFunction.apply(new IOException())).isInstanceOf(NullPointerException.class);
    }

    @Test
    void whenSameClassDefinedTwiceInBuilder_thenThrowsException() {
        ByTypeFunctionBuilder<Object, String> builder = ByTypeFunction.<Object, String>builder()
                .withFunctionFor(String.class, e -> STRING_PREFIX + e)
                .withFunctionFor(String.class, e -> STRING_PREFIX + e);
        assertThatThrownBy(builder::build).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void whenSuperclassAndSubclassAreDefinedInTheSameFunction_thenUsesTheCorrectFunction() {
        String exceptionPrefix = "This is an Exception ";
        Function<Object, String> testByTypeFunction = ByTypeFunction.<Object, String>builder()
                .withFunctionFor(Exception.class, e -> exceptionPrefix + e)
                .withFunctionFor(IOException.class, e -> IOEXCEPTION_PREFIX + e)
                .build();

        assertThat(testByTypeFunction.apply(new IOException())).startsWith(IOEXCEPTION_PREFIX);
        assertThat(testByTypeFunction.apply(new Exception())).startsWith(exceptionPrefix);
    }
}