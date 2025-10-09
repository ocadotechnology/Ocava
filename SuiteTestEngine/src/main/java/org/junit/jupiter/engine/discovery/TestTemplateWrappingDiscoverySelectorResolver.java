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

import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;
import org.junit.jupiter.engine.descriptor.Validatable;
import org.junit.jupiter.engine.discovery.predicates.TestClassPredicates;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;
import org.junit.platform.engine.support.discovery.EngineDiscoveryRequestResolver;

/**
 * An alternative to DiscoverySelectorResolver which will wrap all test methods into TestTemplateTestDescriptor.
 * <p>
 * Adapted from org.junit.jupiter.engine.discovery.DiscoverySelectorResolver. The only difference between this
 * class and DiscoverySelectorResolver is replacing MethodSelectorResolver with TestTemplateWrappingMethodSelectorResolver junit-jupiter-engine-5.13.1
 */
public class TestTemplateWrappingDiscoverySelectorResolver {

    // @formatter:off
    private static final EngineDiscoveryRequestResolver<JupiterEngineDescriptor> resolver = EngineDiscoveryRequestResolver.<JupiterEngineDescriptor>builder()
            .addClassContainerSelectorResolverWithContext(context -> new TestClassPredicates(
                    context.getIssueReporter()).looksLikeNestedOrStandaloneTestClass)
            .addSelectorResolver(context -> new ClassSelectorResolver(
                    context.getClassNameFilter(),
                    context.getEngineDescriptor().getConfiguration(),
                    context.getIssueReporter()))
            // This line is the only different from DiscoverySelectorResolver.
            // Using TestTemplateWrappingMethodSelectorResolver instead of MethodSelectorResolver.
            .addSelectorResolver(context -> new TestTemplateWrappingMethodSelectorResolver(
                    context.getEngineDescriptor().getConfiguration(),
                    context.getIssueReporter()))
            .addTestDescriptorVisitor(context -> TestDescriptor.Visitor.composite(
                    new ClassOrderingVisitor(context.getEngineDescriptor().getConfiguration(), context.getIssueReporter()),
                    new MethodOrderingVisitor(context.getEngineDescriptor().getConfiguration(), context.getIssueReporter()),
                    descriptor -> {
                        if (descriptor instanceof Validatable) {
                            ((Validatable) descriptor).validate(context.getIssueReporter());
                        }
                    }))
            .build();
    // @formatter:on

    public static void resolveSelectors(EngineDiscoveryRequest request, JupiterEngineDescriptor engineDescriptor, DiscoveryIssueReporter issueReporter) {
        resolver.resolve(request, engineDescriptor, issueReporter);
    }
}
