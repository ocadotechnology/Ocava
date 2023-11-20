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
package com.ocadotechnology.scenario;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;
import com.ocadotechnology.notification.Notification;
import com.ocadotechnology.scenario.CheckStepExecutionType.Type;

/**
 * Unit tests for the interaction of different abstract then steps methods
 */
@ParametersAreNonnullByDefault
public class AbstractThenStepsTest {
    private static final String STEP_NAME = "STEP_NAME";

    private final CapturingStepManager stepManager = new CapturingStepManager();
    private final AbstractThenSteps<?, ?> steps = new TestEventThenSteps(stepManager, null, CheckStepExecutionType.ordered(), null);

    enum DecoratorType {
        UNORDERED(
                Type.UNORDERED,
                AbstractThenSteps::unordered,
                ats -> ats.unordered(STEP_NAME)),
        NEVER(
                Type.NEVER,
                AbstractThenSteps::never,
                ats -> ats.never(STEP_NAME)),
        WITHIN(
                Type.WITHIN,
                ats -> ats.within(Duration.ofMillis(10)),
                ats -> ats.within(10, TimeUnit.MILLISECONDS),
                ats -> ats.within(StepFuture.of(10d))),
        AFTER_EXACTLY(
                Type.AFTER_EXACTLY,
                ats -> ats.afterExactly(Duration.ofMillis(10)),
                ats -> ats.afterExactly(10, TimeUnit.MILLISECONDS),
                ats -> ats.afterExactly(StepFuture.of(10d))),
        AFTER_AT_LEAST(
                Type.AFTER_AT_LEAST,
                ats -> ats.afterAtLeast(Duration.ofMillis(10)),
                ats -> ats.afterAtLeast(10, TimeUnit.MILLISECONDS),
                ats -> ats.afterAtLeast(StepFuture.of(10d))),
        FAILING_STEP(null, AbstractThenSteps::failingStep) {
            @Override
            public boolean executionTypeSatisfies(CheckStepExecutionType checkStepExecutionType) {
                return checkStepExecutionType.isFailingStep();
            }
        };

        public final Type executionType;
        public final ImmutableList<UnaryOperator<AbstractThenSteps<?, ?>>> invocations;

        @SafeVarargs
        DecoratorType(@CheckForNull Type executionType, UnaryOperator<AbstractThenSteps<?, ?>>... invocations) {
            this.executionType = executionType;
            this.invocations = ImmutableList.copyOf(invocations);
        }

        public boolean executionTypeSatisfies(CheckStepExecutionType checkStepExecutionType) {
            return checkStepExecutionType.getTypeForTesting().equals(executionType);
        }

        private static boolean areIncompatible(DecoratorType type1, DecoratorType type2) {
            switch (type1) {
                case UNORDERED:
                case NEVER:
                case WITHIN:
                case AFTER_EXACTLY:
                case AFTER_AT_LEAST:
                    return !FAILING_STEP.equals(type2);
                case FAILING_STEP:
                    return FAILING_STEP.equals(type2);
                default:
                    throw new UnsupportedOperationException("Unsupported type: " + type1);
            }
        }
    }

    static Stream<Arguments> getAllCombinations() {
        return Stream.of(DecoratorType.values())
                .flatMap(t1 -> Stream.of(DecoratorType.values()).map(t2 -> Arguments.of(t1, t2)));
    }

    @ParameterizedTest
    @MethodSource("getAllCombinations")
    void testDecoratorCombinations(DecoratorType type1, DecoratorType type2) {
        boolean shouldFail = DecoratorType.areIncompatible(type1, type2);

        for (UnaryOperator<AbstractThenSteps<?, ?>> type1Invocation : type1.invocations) {
            for (UnaryOperator<AbstractThenSteps<?, ?>> type2Invocation : type2.invocations) {
                if (shouldFail) {
                    Assertions.assertThrows(IllegalStateException.class,
                            () -> type2Invocation.apply(type1Invocation.apply(steps)));
                } else {
                    AbstractThenSteps<?, ?> result = type2Invocation.apply(type1Invocation.apply(steps));
                    //Execute steps cannot be modified by anything other than a failingStep method call.
                    Assertions.assertThrows(IllegalStateException.class, () -> result.addExecuteStep(System.out::println));

                    result.addCheckStep(Notification.class, n -> true);
                    CheckStepExecutionType checkStepExecutionType = stepManager.getAndResetRecordedCheckStepType();
                    Assertions.assertTrue(type1.executionTypeSatisfies(checkStepExecutionType));
                    Assertions.assertTrue(type2.executionTypeSatisfies(checkStepExecutionType));
                }
            }
        }
    }
}
