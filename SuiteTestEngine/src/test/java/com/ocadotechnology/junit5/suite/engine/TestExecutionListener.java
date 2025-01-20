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

import java.util.ArrayList;
import java.util.Collection;

import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;

/**
 * A simple EngineExecutionListener which will accumulate the number executed TestDescriptors
 */
class TestExecutionListener implements EngineExecutionListener {

    private Collection<TestDescriptor> executedTests = new ArrayList<>();

    @Override
    public void dynamicTestRegistered(TestDescriptor testDescriptor) {

    }

    @Override
    public void executionSkipped(TestDescriptor testDescriptor, String s) {

    }

    @Override
    public void executionStarted(TestDescriptor testDescriptor) {
    }

    @Override
    public void executionFinished(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
        executedTests.add(testDescriptor);
    }

    @Override
    public void reportingEntryPublished(TestDescriptor testDescriptor, ReportEntry reportEntry) {

    }

    public Collection<TestDescriptor> getExecutedTests() {
        return executedTests;
    }
}
