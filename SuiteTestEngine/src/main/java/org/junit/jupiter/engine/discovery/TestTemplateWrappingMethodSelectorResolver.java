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
package org.junit.jupiter.engine.discovery;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.support.discovery.SelectorResolver.Resolution.matches;
import static org.junit.platform.engine.support.discovery.SelectorResolver.Resolution.unresolved;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.Filterable;
import org.junit.jupiter.engine.descriptor.TestFactoryTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestTemplateModifiedTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestTemplateTestDescriptor;
import org.junit.jupiter.engine.discovery.predicates.IsTestFactoryMethodPredicate;
import org.junit.jupiter.engine.discovery.predicates.IsTestMethodPredicate;
import org.junit.jupiter.engine.discovery.predicates.IsTestTemplateMethodPredicate;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.ClassUtils;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.NestedMethodSelector;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;

/**
 * A MethodSelectorResolver which alters the behaviour of MethodType.
 * This wraps test methods and test template methods in another layer of templates.
 *
 * <ul>
 *     <li>TEST now creates a TestTemplateTestDescriptor</li>
 *     <li>TEST_FACTORY remains the same</li>
 *     <li>TEST_TEMPLATE creates a TestTemplateModifiedTestDescriptor</li>
 * </ul>
 * Adapted from ge org.junit.jupiter.engine.discovery.MethodSelectorResolver junit-jupiter-engine-5.5.2
 */
public class TestTemplateWrappingMethodSelectorResolver extends MethodSelectorResolver {

    private static final Logger logger = LoggerFactory.getLogger(MethodSelectorResolver.class);
    private final JupiterConfiguration configuration;

    TestTemplateWrappingMethodSelectorResolver(JupiterConfiguration configuration, DiscoveryIssueReporter discoveryIssueReporter) {
        super(configuration, discoveryIssueReporter);
        this.configuration = configuration;
    }

    @Override
    public Resolution resolve(NestedMethodSelector selector, Context context) {
        return resolve(context, selector.getNestedClass(), selector::getMethod);
    }

    @Override
    public Resolution resolve(DiscoverySelector selector, Context context) {
        if (selector instanceof DeclaredMethodSelector methodSelector) {
            var testClasses = methodSelector.testClasses();
            if (testClasses.size() == 1) {
                return resolve(context, testClasses.get(0), methodSelector::method);
            }
            int lastIndex = testClasses.size() - 1;
            return resolve(context, testClasses.get(lastIndex), methodSelector::method);
        }
        return unresolved();
    }

    @Override
    public Resolution resolve(MethodSelector selector, Context context) {
        return resolve(context, selector.getJavaClass(), selector::getJavaMethod);
    }

    private Resolution resolve(Context context, Class<?> testClass, Supplier<Method> methodSupplier) {
        Method method = methodSupplier.get();
        BiFunction<TestDescriptor, Supplier<Set<? extends DiscoverySelector>>, Match> matchFactory = Match::exact;
        // @formatter:off
        Set<Match> matches = Arrays.stream(MethodType.values())
                .map(methodType -> methodType.resolveMethod( testClass, method, context, configuration))
                .flatMap(Optional::stream)
                .map(testDescriptor -> matchFactory.apply(testDescriptor, expansionCallback(testDescriptor)))
                .collect(toSet());
        // @formatter:on
        if (matches.size() > 1) {
            logger.warn(() -> {
                Stream<TestDescriptor> testDescriptors = matches.stream().map(Match::getTestDescriptor);
                return String.format(
                        "Possible configuration error: method [%s] resulted in multiple TestDescriptors %s. "
                                + "This is typically the result of annotating a method with multiple competing annotations "
                                + "such as @Test, @RepeatedTest, @ParameterizedTest, @TestFactory, etc.",
                        method.toGenericString(), testDescriptors.map(d -> d.getClass().getName()).collect(toList()));
            });
        }
        return matches.isEmpty() ? unresolved() : matches(matches);
    }

    private Supplier<Set<? extends DiscoverySelector>> expansionCallback(TestDescriptor testDescriptor) {
        return () -> {
            if (testDescriptor instanceof Filterable) {
                ((Filterable) testDescriptor).getDynamicDescendantFilter().allowAll();
            }
            return emptySet();
        };
    }

    private enum MethodType {

        TEST(new IsTestMethodPredicate(), TestTemplateTestDescriptor.SEGMENT_TYPE) {
            @Override
            protected TestDescriptor createTestDescriptor(UniqueId uniqueId, Class<?> testClass, Method method, Supplier<List<Class<?>>> enclosingInstanceTypes, JupiterConfiguration configuration) {
                // Test method is wrapped in template descriptor
                return new TestTemplateTestDescriptor(uniqueId, testClass, method, enclosingInstanceTypes, configuration);
            }
        },

        TEST_FACTORY(new IsTestFactoryMethodPredicate(), TestFactoryTestDescriptor.SEGMENT_TYPE) {
            @Override
            protected TestDescriptor createTestDescriptor(UniqueId uniqueId, Class<?> testClass, Method method, Supplier<List<Class<?>>> enclosingInstanceTypes, JupiterConfiguration configuration) {
                // Test factory remains mapped to factory descriptor
                return new TestFactoryTestDescriptor(uniqueId, testClass, method, enclosingInstanceTypes, configuration);
            }
        },

        TEST_TEMPLATE(new IsTestTemplateMethodPredicate(), TestTemplateModifiedTestDescriptor.SEGMENT_TYPE) {
            @Override
            protected TestDescriptor createTestDescriptor(UniqueId uniqueId, Class<?> testClass, Method method, Supplier<List<Class<?>>> enclosingInstanceTypes, JupiterConfiguration configuration) {
                // Test template mapped to TestTemplateModifiedTestDescriptor in order to modify any already templated tests
                return new TestTemplateModifiedTestDescriptor(uniqueId, testClass, method, enclosingInstanceTypes, configuration);
            }
        };

        private final Predicate<Method> methodPredicate;
        private final String segmentType;

        MethodType(Predicate<Method> methodPredicate, String segmentType) {
            this.methodPredicate = methodPredicate;
            this.segmentType = segmentType;
        }

        private Optional<TestDescriptor> resolveMethod(Class<?> testClass, Method method, Context resolver, JupiterConfiguration configuration) {
            if (!methodPredicate.test(method)) {
                return Optional.empty();
            }
            return resolver.addToParent(
                    () -> selectClass(testClass),
                    parent -> createMaybeTestDescriptor(parent, testClass, method, configuration));
        }

        private Optional<TestDescriptor> createMaybeTestDescriptor(TestDescriptor parent, Class<?> testClass, Method method, JupiterConfiguration configuration) {
            return Optional.of(createTestDescriptor(
                    createUniqueId(method, parent),
                    testClass,
                    method,
                    testClass.getEnclosingClass() == null
                            ? Collections::emptyList
                            : () -> List.of(testClass.getEnclosingClass()),
                    configuration));
        }

        private UniqueId createUniqueId(Method method, TestDescriptor parent) {
            String methodId = String.format("%s(%s)", method.getName(),
                                            ClassUtils.nullSafeToString(method.getParameterTypes()));
            return parent.getUniqueId().append(segmentType, methodId);
        }

        protected abstract TestDescriptor createTestDescriptor(UniqueId uniqueId, Class<?> testClass, Method method, Supplier<List<Class<?>>> enclosingInstanceTypes, JupiterConfiguration configuration);
    }
}
