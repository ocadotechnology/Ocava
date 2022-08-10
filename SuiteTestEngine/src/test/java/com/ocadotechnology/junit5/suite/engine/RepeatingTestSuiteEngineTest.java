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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

import com.ocadotechnology.junit5.suite.engine.repeating.RepeatedTestTemplateInvocationContextWrapper;
import com.ocadotechnology.junit5.suite.engine.repeating.RepetitionTestTemplateInvocationContext;
import com.ocadotechnology.junit5.suite.engine.repeating.TestTemplateInvocationContextModifierExtension;

class RepeatingTestSuiteEngineTest {

    private SuiteEngine engine = new SuiteEngine();

    @Test
    void testEngine() {
        EngineDiscoveryRequest discoveryRequest = request()
                .selectors(selectClass(RepeatingDynamicTestSuite.class))
                .configurationParameter("com.ocadotechnology.junit5.suite.engine.templateAllTests", "true")
                .build();
        TestDescriptor testsDiscovered = engine.discover(discoveryRequest, UniqueId.forEngine(engine.getId()));

        TestExecutionListener executionListener = new TestExecutionListener();
        ExecutionRequest executionRequest = new ExecutionRequest(testsDiscovered, executionListener, new TestConfigurationParameters());
        engine.execute(executionRequest);

        assertEquals(26, executionListener.getExecutedTests().size(), ""
                + "1 engine "
                + "+ 2 classes "
                + "+ 2 TestTemplateTestDescriptors for each non-parameterized test method "
                + "+ (2 * 5) TestTemplateInvocationTestDescriptors for each non-parameterized method "
                + "+ 1 TestTemplateModifiedTestDescriptors for each parameterized test parameter "
                + "+ (2 * 5) TestTemplateInvocationTestDescriptors for the parameterized test executions tests "
                + "expected to be discovered and executed");
    }
}

@DynamicTestSuite
@SelectFromMethod(name = "suite")
class RepeatingDynamicTestSuite {

    static List<Class> suite() {
        return Arrays.asList(FirstRepeatingTestClass.class, SecondRepeatingTestClass.class);
    }
}

@ExtendWith(RepeatingTestRandomSeedScenarioExtension.class)
class FirstRepeatingTestClass {

    @Test
    void aTest() {
        assertTrue(true);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void parameterizedTest(int num) {
        assertTrue(true);
    }
}

@ExtendWith(RepeatingTestRandomSeedScenarioExtension.class)
class SecondRepeatingTestClass {

    @Test
    void bTest() {
        assertTrue(true);
    }
}

class RepeatingTestRandomSeedScenarioExtension implements
        TestTemplateInvocationContextProvider,
        TestTemplateInvocationContextModifierExtension,
        TestSuiteTemplateExtension {

    private static final int NUMBER_OF_REPETITIONS = 5;

    public RepeatingTestRandomSeedScenarioExtension() {
    }

    @Override
    public boolean supportsTestTemplate(ExtensionContext extensionContext) {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext extensionContext) {
        int numberOfRepetitions = NUMBER_OF_REPETITIONS;
        return IntStream.range(1, numberOfRepetitions + 1)
                .mapToObj(i -> new RepetitionTestTemplateInvocationContext(i, numberOfRepetitions));
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(TestTemplateInvocationContext parameterizedContext, ExtensionContext extensionContext) {
        int numberOfRepetitions = NUMBER_OF_REPETITIONS;
        return IntStream.range(1, numberOfRepetitions + 1)
                .mapToObj(i -> new RepeatedTestTemplateInvocationContextWrapper(parameterizedContext, new RepetitionTestTemplateInvocationContext(i, numberOfRepetitions)));
    }
}
