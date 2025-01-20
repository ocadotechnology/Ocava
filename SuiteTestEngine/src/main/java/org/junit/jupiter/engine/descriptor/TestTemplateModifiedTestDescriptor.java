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
package org.junit.jupiter.engine.descriptor;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

import com.ocadotechnology.junit5.suite.engine.repeating.TestTemplateInvocationContextModifierExtension;

/**
 * A TestTemplateTestDescriptor which will use a registered TestTemplateInvocationContextModifierExtension
 * in order to modify the predefined TestTemplateInvocationContextProvider.
 * Adapted from org.junit.jupiter.engine.descriptor.TestTemplateTestDescriptor junit-jupiter-engine-5.5.2
 */
public class TestTemplateModifiedTestDescriptor extends TestTemplateTestDescriptor {
    public TestTemplateModifiedTestDescriptor(
            UniqueId uniqueId,
            Class<?> testClass,
            Method templateMethod,
            JupiterConfiguration configuration) {
        super(uniqueId, testClass, templateMethod, configuration);
    }

    @Override
    public JupiterEngineExecutionContext execute(JupiterEngineExecutionContext context, DynamicTestExecutor dynamicTestExecutor) throws Exception {

        ExtensionContext extensionContext = context.getExtensionContext();

        List<TestTemplateInvocationContextProvider> testTemplateProviders = validateNonModifierContextProviders(extensionContext, context.getExtensionRegistry());

        List<TestTemplateInvocationContextModifierExtension> testTemplateModifierExtensions = validateTestTemplateModifierExtensions(context.getExtensionRegistry());

        AtomicInteger invocationIndex = new AtomicInteger();
        // @formatter:off
        testTemplateProviders.stream()
                .flatMap(provider -> provider.provideTestTemplateInvocationContexts(extensionContext))
                // This is the key difference in this class
                // This will apply each testTemplateModifierExtensions to the normal test template descriptors
                .flatMap(testTemplate -> applyTestTemplateModifiers(testTemplate, testTemplateModifierExtensions, extensionContext))
                .map(invocationContext -> createInvocationTestDescriptor(invocationContext, invocationIndex.incrementAndGet()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(invocationTestDescriptor -> execute(dynamicTestExecutor, invocationTestDescriptor));
        // @formatter:on
        validateWasAtLeastInvokedOnce(invocationIndex.get());
        return context;
    }

    private Stream<TestTemplateInvocationContext> applyTestTemplateModifiers(
            TestTemplateInvocationContext targetTestTemplate,
            List<TestTemplateInvocationContextModifierExtension> testTemplateModifierExtensions,
            ExtensionContext extensionContext) {
        return testTemplateModifierExtensions.stream().flatMap(testTemplateModifierExtension -> testTemplateModifierExtension.provideTestTemplateInvocationContexts(targetTestTemplate, extensionContext));
    }

    private Optional<TestDescriptor> createInvocationTestDescriptor(
            TestTemplateInvocationContext invocationContext,
            int index) {
        UniqueId uniqueId = getUniqueId().append(TestTemplateInvocationTestDescriptor.SEGMENT_TYPE, "#" + index);
        if (getDynamicDescendantFilter().test(uniqueId, index)) {
            return Optional.of(new TestTemplateInvocationTestDescriptor(uniqueId, getTestClass(), getTestMethod(),
                                                                        invocationContext, index, configuration));
        }
        return Optional.empty();
    }

    private void execute(DynamicTestExecutor dynamicTestExecutor, TestDescriptor testDescriptor) {
        addChild(testDescriptor);
        dynamicTestExecutor.execute(testDescriptor);
    }

    private void validateWasAtLeastInvokedOnce(int invocationIndex) {
        Preconditions.condition(invocationIndex > 0, () -> "No supporting "
                + TestTemplateInvocationContextProvider.class.getSimpleName() + " provided an invocation context");
    }

    private List<TestTemplateInvocationContextProvider> validateNonModifierContextProviders(ExtensionContext extensionContext, ExtensionRegistry extensionRegistry) {

        // @formatter:off
        List<TestTemplateInvocationContextProvider> providers = extensionRegistry.stream(TestTemplateInvocationContextProvider.class)
                .filter(provider -> provider.supportsTestTemplate(extensionContext))
                .filter(provider -> !(provider instanceof TestTemplateInvocationContextModifierExtension))
                .collect(toList());
        // @formatter:on

        return Preconditions.notEmpty(
                providers,
                () -> String.format(
                        "You must register at least one %s that supports @TestTemplate method [%s]",
                        TestTemplateInvocationContextProvider.class.getSimpleName(),
                        getTestMethod()));
    }

    private List<TestTemplateInvocationContextModifierExtension> validateTestTemplateModifierExtensions(ExtensionRegistry extensionRegistry) {

        List<TestTemplateInvocationContextModifierExtension> providers = extensionRegistry.stream(TestTemplateInvocationContextModifierExtension.class)
                .collect(toList());

        return Preconditions.notEmpty(
                providers,
                () -> String.format("You must register at least one %s for method [%s]",
                        TestTemplateInvocationContextModifierExtension.class.getSimpleName(), getTestMethod()));
    }
}
