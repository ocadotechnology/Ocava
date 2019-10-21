/*
 * Copyright © 2017-2019 Ocado (Ocava)
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
package com.ocadotechnology.scenario;

import java.net.URL;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.slf4j.MDC;

import com.google.common.base.Preconditions;
import com.ocadotechnology.utils.Types;

/**
 * Class which sets up scenario test logging and functions as a decorator around @Test methods, calling the
 * {@code configure} and {@code run} methods before and after the @Test method sets up the scenario steps.
 * <p>
 * Note that only one instance of this class is created for all tests being run.  The API of junit 5 guarantees that
 * postProcessTestInstance is called before afterTestExecution, which should mean that {@code story} is the current test
 * instance when afterTestExecution is called.
 */
public class ScenarioTestWrapper implements BeforeAllCallback, TestInstancePostProcessor, AfterTestExecutionCallback, TestExecutionExceptionHandler {
    public static final URL logConfigUrl = ScenarioTestWrapper.class.getClassLoader().getResource("logbackTestFramework.groovy");
    private boolean exceptionHasOccurred;

    static {
        if (logConfigUrl != null) {
            System.setProperty("logback.configurationFile", logConfigUrl.getFile());
        }
    }

    private AbstractStory story;

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        MDC.put("TEST_SCOPE", "SCENARIO");
        MDC.put("TEST_NAME", extensionContext.getTestClass().map(Class::getSimpleName).orElse(""));
    }

    @Override
    public void postProcessTestInstance(Object testObject, ExtensionContext extensionContext) {
        exceptionHasOccurred = false;
        story = Types.fromTypeOrFail(testObject, AbstractStory.class);
        story.init();
    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) {
        Preconditions.checkNotNull(story, "Story not set up.");
        if (!exceptionHasOccurred) {
            story.executeTestSteps();
        }
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        exceptionHasOccurred = true;
        story.logStepFailure(throwable);
        if (!story.isFixRequired()) {
            throw throwable;
        }
    }
}
