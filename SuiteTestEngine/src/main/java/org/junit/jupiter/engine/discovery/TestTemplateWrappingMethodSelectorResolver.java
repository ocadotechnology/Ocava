/*
 * Copyright © 2017-2023 Ocado (Ocava)
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.Filterable;
import org.junit.jupiter.engine.descriptor.TestFactoryTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestTemplateModifiedTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestTemplateTestDescriptor;
import org.junit.jupiter.engine.discovery.predicates.IsTestFactoryMethod;
import org.junit.jupiter.engine.discovery.predicates.IsTestMethod;
import org.junit.jupiter.engine.discovery.predicates.IsTestTemplateMethod;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.ClassUtils;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.MethodSelector;

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

    TestTemplateWrappingMethodSelectorResolver(JupiterConfiguration configuration) {
        super(configuration);
    }

    @Override
    public Resolution resolve(MethodSelector selector, Context context) {
        // @formatter:off
        Set<Match> matches = Arrays.stream(MethodType.values())
                .map(methodType -> methodType.resolveMethodSelector(selector, context, configuration))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(testDescriptor -> Match.exact(testDescriptor, expansionCallback(testDescriptor)))
                .collect(toSet());
        //@formatter:on

        if (matches.size() > 1) {
            logger.warn(() -> {
                Stream<TestDescriptor> testDescriptors = matches.stream().map(Match::getTestDescriptor);
                return String.format(
                        "Possible configuration error: method [%s] resulted in multiple TestDescriptors %s. "
                                + "This is typically the result of annotating a method with multiple competing annotations "
                                + "such as @Test, @RepeatedTest, @ParameterizedTest, @TestFactory, etc.",
                        selector.getJavaMethod(), testDescriptors.map(d -> d.getClass().getName()).collect(toList()));
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

        TEST(new IsTestMethod(), TestTemplateTestDescriptor.SEGMENT_TYPE) {
            @Override
            protected TestDescriptor createTestDescriptor(UniqueId uniqueId, Class<?> testClass, Method method, JupiterConfiguration configuration) {
                // Test method is wrapped in template descriptor
                return new TestTemplateTestDescriptor(uniqueId, testClass, method, configuration);
            }
        },

        TEST_FACTORY(new IsTestFactoryMethod(), TestFactoryTestDescriptor.SEGMENT_TYPE) {
            @Override
            protected TestDescriptor createTestDescriptor(UniqueId uniqueId, Class<?> testClass, Method method, JupiterConfiguration configuration) {
                // Test factory remains mapped to factory descriptor
                return new TestFactoryTestDescriptor(uniqueId, testClass, method, configuration);
            }
        },

        TEST_TEMPLATE(new IsTestTemplateMethod(), TestTemplateModifiedTestDescriptor.SEGMENT_TYPE) {
            @Override
            protected TestDescriptor createTestDescriptor(UniqueId uniqueId, Class<?> testClass, Method method, JupiterConfiguration configuration) {

                // Test template mapped to TestTemplateModifiedTestDescriptor in order to modify any already templated tests
                return new TestTemplateModifiedTestDescriptor(uniqueId, testClass, method, configuration);
            }
        };

        private final Predicate<Method> methodPredicate;
        private final String segmentType;

        MethodType(Predicate<Method> methodPredicate, String segmentType) {
            this.methodPredicate = methodPredicate;
            this.segmentType = segmentType;
        }

        private Optional<TestDescriptor> resolveMethodSelector(MethodSelector selector, Context resolver, JupiterConfiguration configuration) {
            if (!methodPredicate.test(selector.getJavaMethod())) {
                return Optional.empty();
            }
            Class<?> testClass = selector.getJavaClass();
            Method method = selector.getJavaMethod();
            return resolver.addToParent(() -> selectClass(testClass),
                                        parent -> Optional.of(
                                                createTestDescriptor(createUniqueId(method, parent), testClass, method, configuration)));
        }

        private UniqueId createUniqueId(Method method, TestDescriptor parent) {
            String methodId = String.format("%s(%s)", method.getName(),
                                            ClassUtils.nullSafeToString(method.getParameterTypes()));
            return parent.getUniqueId().append(segmentType, methodId);
        }

        protected abstract TestDescriptor createTestDescriptor(UniqueId uniqueId, Class<?> testClass, Method method, JupiterConfiguration configuration);

    }
}
