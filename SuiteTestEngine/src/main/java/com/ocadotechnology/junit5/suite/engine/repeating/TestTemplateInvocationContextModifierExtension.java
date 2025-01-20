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
package com.ocadotechnology.junit5.suite.engine.repeating;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

/**
 * An Extension to modify a given TestTemplateInvocationContext.
 * An example use of this may be to repeat a test programmatically.
 * */
public interface TestTemplateInvocationContextModifierExtension extends Extension {

    /**
     * Creates a stream of TestTemplateInvocationContext given a single TestTemplateInvocationContext.
     * Allows for wrapping and/or duplication of TestTemplateInvocationContexts
     * @param parameterizedContext the subject TestTemplateInvocationContext
     * @param extensionContext context for this test
     * @return a stream of TestTemplateInvocationContext to be invoked an a test
     */
    Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(TestTemplateInvocationContext parameterizedContext, ExtensionContext extensionContext);

}
