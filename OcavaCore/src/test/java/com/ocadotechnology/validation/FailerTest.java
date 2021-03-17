/*
 * Copyright © 2017-2021 Ocado (Ocava)
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
package com.ocadotechnology.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ocadotechnology.testing.UtilityClassTest;

@DisplayName("The Failer utility class")
class FailerTest implements UtilityClassTest {

    @Override
    public Class<?> getTestSubject() {
        return Failer.class;
    }

    @Nested
    @DisplayName("fail() method")
    class FailTests {

        @Test
        @DisplayName("throws an IllegalStateException")
        void throwsAnIllegalStateException() {
            assertThatThrownBy(() -> { throw Failer.fail("boom"); })
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("boom");
        }

        @Test
        @DisplayName("throws an IllegalStateException even when return value is ignored")
        @SuppressWarnings("ResultOfMethodCallIgnored")
        void throwsAnIllegalStateException_beforeMethodCanReturn() {
            assertThatThrownBy(() -> Failer.fail("boom"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("boom");
        }

        @Test
        @DisplayName("produces message from format String and arguments")
        void producesMessageFromFormatStringAndArgs() {
            assertThatThrownBy(() -> { throw Failer.fail("Value %s; %s; %s", "eins", 2, "trois"); })
                    .hasMessage("Value eins; 2; trois");
        }

        @Test
        @DisplayName("throws exception from Guava Preconditions class")
        void throwsExceptionFromGuavaPrecondition() {
            assertThatThrownBy(() -> { throw Failer.fail("boom"); })
                    .hasStackTraceContaining("com.google.common.base.Preconditions");
        }
    }

    @Nested
    @DisplayName("valueExpected() method")
    class ValueExpectedTests {

        @Test
        @DisplayName("throws correct exception")
        void hasCorrectException() {
            assertThatThrownBy(Failer::valueExpected)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Value expected to be present");
        }

        @Test
        @DisplayName("throws exception from Guava Preconditions class")
        void throwsExceptionFromGuavaPrecondition() {
            assertThatThrownBy(() -> { throw Failer.valueExpected(); })
                    .hasStackTraceContaining("com.google.common.base.Preconditions");
        }

        @Test
        @DisplayName("typical usage with Optional works")
        @SuppressWarnings("ResultOfMethodCallIgnored")
        void worksWithJavaOptional() {
            assertThatThrownBy(() -> Optional.empty().orElseThrow(Failer::valueExpected))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Value expected to be present");
        }
    }
}
