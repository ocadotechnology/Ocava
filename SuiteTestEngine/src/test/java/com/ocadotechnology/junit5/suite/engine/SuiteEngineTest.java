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
package com.ocadotechnology.junit5.suite.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.OutputDirectoryProvider;

class SuiteEngineTest {

    private final SuiteEngine engine = new SuiteEngine();
    private final OutputDirectoryProvider outputDirectoryProvider = new TestOutputDirectoryProvider();

    @Test
    void testEngine() {
        EngineDiscoveryRequest discoveryRequest = request()
                .selectors(selectClass(SomeDynamicTestSuite.class))
                .outputDirectoryProvider(outputDirectoryProvider)
                .build();
        TestDescriptor testsDiscovered = engine.discover(discoveryRequest, UniqueId.forEngine(engine.getId()));

        TestExecutionListener executionListener = new TestExecutionListener();
        ExecutionRequest executionRequest = ExecutionRequest.create(
                testsDiscovered,
                executionListener,
                new TestConfigurationParameters(),
                outputDirectoryProvider);
        engine.execute(executionRequest);

        assertEquals(8, executionListener.getExecutedTests().size(), "1 engine + 2 classes + 2 regular test methods + 3 parameterized test templates expected to be discovered and executed");
    }
}

@DynamicTestSuite
@SelectFromMethod(name = "suite")
class SomeDynamicTestSuite {

    static List<Class> suite() {
        return Arrays.asList(FirstTestClass.class, SecondTestClass.class);
    }
}

class FirstTestClass {

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

class SecondTestClass {

    @Test
    void bTest() {
        assertTrue(true);
    }
}