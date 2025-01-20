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
package com.ocadotechnology.random;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import com.google.common.collect.ImmutableList;

public class AccessLoggingInstancedRepeatableRandomTest {
    @AfterEach
    void tearDown() {
        RepeatableRandom.setLogAccess(false);
        RepeatableRandom.clear();
    }

    /**
     * Verify that the access logging class overrides every public instance method of the parent class, in case someone
     * adds new methods in the future and doesn't know about the logging class.
     */
    @Test
    void validateCompleteness() {
        Class<InstancedRepeatableRandom> parentClass = InstancedRepeatableRandom.class;
        Class<AccessLoggingInstancedRepeatableRandom> childClass = AccessLoggingInstancedRepeatableRandom.class;

        // Check that all methods in the parent class are overridden in the child class
        for (Method method : parentClass.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) || !Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            try {
                childClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException e) {
                throw new AssertionError("Method " + method + " is not overridden in " + childClass);
            }
        }
    }

    @Test
    void whenMethodCalled_andFeatureInactive_nothingLogged() {
        TestAppender testAppender = setupTestLogger(Level.TRACE);

        RepeatableRandom.initialiseWithSeed(1234);
        RepeatableRandom.nextUUID();

        Assertions.assertTrue(testAppender.getLog().isEmpty());
    }

    @Test
    void whenMethodCalled_andLogLevelTooHigh_nothingLogged() {
        TestAppender testAppender = setupTestLogger(Level.INFO);
        RepeatableRandom.setLogAccess(true);

        RepeatableRandom.initialiseWithSeed(1234);
        RepeatableRandom.nextUUID();

        Assertions.assertTrue(testAppender.getLog().isEmpty());
    }

    @Test
    void whenMethodCalled_andLogLevelSetLow_accessIsLogged() {
        TestAppender testAppender = setupTestLogger(Level.TRACE);
        RepeatableRandom.setLogAccess(true);

        RepeatableRandom.initialiseWithSeed(1234);
        RepeatableRandom.nextUUID();

        Assertions.assertEquals(2, testAppender.getLog().size());
        Assertions.assertTrue(testAppender.getLog().get(0).getFormattedMessage().contains("initialiseWithSeed"));
        Assertions.assertTrue(testAppender.getLog().get(1).getFormattedMessage().contains("nextUUID"));
    }

    @Test
    void whenMethodCalled_afterFeatureHasBeenDeactivated_nothingMoreLogged() {
        TestAppender testAppender = setupTestLogger(Level.TRACE);
        RepeatableRandom.setLogAccess(true);

        RepeatableRandom.initialiseWithSeed(1234);

        RepeatableRandom.setLogAccess(false);
        RepeatableRandom.nextUUID();

        Assertions.assertEquals(1, testAppender.getLog().size());
        Assertions.assertTrue(testAppender.getLog().get(0).getFormattedMessage().contains("initialiseWithSeed"));
    }

    private static TestAppender setupTestLogger(Level info) {
        ch.qos.logback.classic.Logger loggerToUpdate = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(AccessLoggingInstancedRepeatableRandom.class);
        TestAppender testAppender = new TestAppender();
        testAppender.start();
        loggerToUpdate.addAppender(testAppender);
        loggerToUpdate.setLevel(info);
        return testAppender;
    }

    private static class TestAppender extends AppenderBase<ILoggingEvent> {
        private final List<ILoggingEvent> log = new ArrayList<>();

        @Override
        protected void append(ILoggingEvent iLoggingEvent) {
            log.add(iLoggingEvent);
        }

        public ImmutableList<ILoggingEvent> getLog() {
            return ImmutableList.copyOf(log);
        }
    }
}
