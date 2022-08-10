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
package com.ocadotechnology.junit5.suite.engine;

import static org.junit.platform.commons.util.ReflectionUtils.isStatic;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.support.filter.ClasspathScanningSupport.buildClassNamePredicate;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.engine.config.DefaultJupiterConfiguration;
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;
import org.junit.jupiter.engine.discovery.DiscoverySelectorResolver;
import org.junit.jupiter.engine.discovery.TestTemplateWrappingDiscoverySelectorResolver;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.platform.commons.util.ClassFilter;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class SuiteEngine extends HierarchicalTestEngine<JupiterEngineExecutionContext> {

    private static final String ENGINE_ID = "suite-engine";
    private static final String TEMPLATE_ALL_TESTS = "com.ocadotechnology.junit5.suite.engine.templateAllTests";
    private static final String EXIT_ON_EXCEPTION = "com.ocadotechnology.junit5.suite.engine.systemExitOnException";

    @Override
    public String getId() {
        return ENGINE_ID;
    }

    @SuppressFBWarnings(value = "DM_EXIT", justification = "We do expect to exit here")
    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        JupiterEngineDescriptor engine = new JupiterEngineDescriptor(
                uniqueId,
                new DefaultJupiterConfiguration(discoveryRequest.getConfigurationParameters()));

        ClassFilter classFilter = ClassFilter.of(buildClassNamePredicate(discoveryRequest), c -> true);

        // just class selection is needed for this engine
        List<? extends Class<?>> candidates = discoveryRequest
                .getSelectorsByType(ClassSelector.class)
                .stream()
                .map(ClassSelector::getJavaClass)
                .filter(classFilter)
                .filter(c -> c.isAnnotationPresent(DynamicTestSuite.class))
                .collect(Collectors.toList());

        boolean exitOnException = getExitOnExceptionFlag(engine);

        for (Class<?> candidate : candidates) {
            try {
                handleCandidate(engine, candidate);
            } catch (Exception e) {
                e.printStackTrace();

                if (exitOnException) {
                    System.err.println("Exception caught while getting test descriptor. Exiting system in order to fail tests");
                    System.exit(3);
                }
            }
        }

        return engine;
    }

    private Boolean getExitOnExceptionFlag(JupiterEngineDescriptor engine) {
        return engine.getConfiguration().getRawConfigurationParameter(EXIT_ON_EXCEPTION)
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    @Override
    protected JupiterEngineExecutionContext createExecutionContext(ExecutionRequest request) {
        return new JupiterEngineExecutionContext(
                request.getEngineExecutionListener(),
                new DefaultJupiterConfiguration(request.getConfigurationParameters()));
    }

    private void handleCandidate(JupiterEngineDescriptor engine, Class<?> candidate) throws InvocationTargetException, IllegalAccessException {
        SelectFromMethod methodDefinition = candidate.getAnnotation(SelectFromMethod.class);
        if (methodDefinition == null) {
            throw new IllegalStateException("No @SelectFromMethod annotation provided for test class " + candidate.getSimpleName());
        }

        Method suite;
        try {
            //in the future, we make this configurable or look for an annotation that points to a method with name
            suite = candidate.getDeclaredMethod(methodDefinition.name());
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("No method matching declared name " + methodDefinition.name() + " found on test class " + candidate.getSimpleName());
        }

        if (!isStatic(suite)) {
            throw new IllegalStateException("Suite method " + methodDefinition.name() + " on test class " + candidate.getSimpleName() + " is not static.");
        }

        Stream<Class> returnedObject = convertToClasses(suite);

        boolean useTestTemplateWrappingDiscoverySelectorForAllTests = engine.getConfiguration().getRawConfigurationParameter(TEMPLATE_ALL_TESTS)
                .map(Boolean::parseBoolean)
                .orElse(false);

        returnedObject
                .forEach(clazz -> {
                    EngineDiscoveryRequest discoveryRequest = request().selectors(selectClass(clazz)).build();
                    DiscoverySelectorResolver discoverySelectorResolver = getDiscoverySelectorResolver(clazz, useTestTemplateWrappingDiscoverySelectorForAllTests);
                    discoverySelectorResolver.resolveSelectors(discoveryRequest, engine);
                });
    }

    /**
     * Returns discoverySelectorResolver for a given test class.
     * If the templateAllTests config parameter is set to true it always returns TestTemplateWrappingDiscoverySelectorResolver.
     * If the test is annotated with @ExtendsWith(...) and one of the extended classes implements TestSuiteTemplateExtension
     * it returns TestTemplateWrappingDiscoverySelectorResolver.
     * Otherwise it returns the default DiscoverySelectorResolver.
     */
    private DiscoverySelectorResolver getDiscoverySelectorResolver(Class clazz, boolean useTestTemplateWrappingDiscoverySelectorForAllTests) {
        if (useTestTemplateWrappingDiscoverySelectorForAllTests) {
            return new TestTemplateWrappingDiscoverySelectorResolver();
        }

        // check if the class is annotated with @ExtendsWIth(...)
        ExtendWith extendWith = (ExtendWith)clazz.getAnnotation(ExtendWith.class);
        if (extendWith == null) {
            return new DiscoverySelectorResolver();
        }

        Class<? extends Extension>[] extensionClasses = extendWith.value();
        // check if any of the extended classes implement TestSuiteTemplateExtension
        boolean classHasTestSuiteTemplateExtension = Arrays.stream(extensionClasses)
                .anyMatch(extensionClass ->
                        Arrays.stream(extensionClass.getInterfaces())
                                .anyMatch(i -> i.isAssignableFrom(TestSuiteTemplateExtension.class)));

        return classHasTestSuiteTemplateExtension
                ? new TestTemplateWrappingDiscoverySelectorResolver()
                : new DiscoverySelectorResolver();
    }

    private Stream<Class> convertToClasses(Method suite) throws InvocationTargetException, IllegalAccessException {
        Object value = suite.invoke(null);
        Stream<?> stream;
        if (value instanceof Stream) {
            stream = (Stream) value;
        } else if (value instanceof Collection) {
            stream = ((Collection) value).stream();
        } else if (value instanceof Object[]) {
            stream = Stream.of((Object[]) value);
        } else {
            throw new IllegalStateException("Suite method return type is not a Collection, Stream or Array");
        }
        return stream
                .map(e -> {
                    if (e instanceof Class) {
                        return (Class) e;
                    }
                    throw new IllegalStateException("Suite method return value elements are not of type class" + e);
                });
    }
}
