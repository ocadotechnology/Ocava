/*
 * Copyright © 2017-2024 Ocado (Ocava)
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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Preconditions;
import com.ocadotechnology.simulation.Simulation;

/**
 * A collection of steps used to make assertions about the values of StepFutures
 */
@ParametersAreNonnullByDefault
public class AssertionThenSteps<S extends Simulation> extends AbstractThenSteps<S, AssertionThenSteps<S>> {

    public AssertionThenSteps(
            StepManager<S> stepManager,
            NotificationCache notificationCache) {
        super(stepManager, notificationCache, CheckStepExecutionType.ordered());
    }

    private AssertionThenSteps(
            StepManager<S> stepManager,
            NotificationCache notificationCache,
            CheckStepExecutionType checkStepExecutionType) {
        super(stepManager, notificationCache, checkStepExecutionType);
    }

    @Override
    protected AssertionThenSteps<S> create(
            StepManager<S> stepManager,
            NotificationCache notificationCache,
            CheckStepExecutionType checkStepExecutionType) {
        return new AssertionThenSteps<>(stepManager, notificationCache, checkStepExecutionType);
    }

    /**
     * Asserts that the value of the first StepFuture provided as a parameter is found in the collection of StepFutures provided as the second parameter.
     * @throws IllegalStateException if futureValue is not populated when the step is executed. Unpopulated futures in futureCollection are ignored.
     */
    public final <T> void valueIsInCollection(StepFuture<T> futureValue, Collection<StepFuture<T>> futureCollection) {
        var futureValuesCollection = ListStepFuture.of(futureCollection);

        addExecuteStep(
                () -> {
                    List<T> values = futureValuesCollection.getAnyPopulated();
                    T value = futureValue.get();

                    Preconditions.checkState(
                            values.contains(value),
                            "Value: %s is not in the collection of values: %s",
                            value,
                            values
                    );
                }
        );
    }

    /**
     * Asserts that the values of two StepFutures provided as parameters are equal.
     * @throws IllegalStateException if either futureValue or otherFutureValue is unpopulated when the step is executed.
     */
    public final <T> void valuesAreEqual(StepFuture<T> firstValue, StepFuture<T> secondValue) {
        addExecuteStep(
                () -> {
                    T value = firstValue.get();
                    T otherValue = secondValue.get();

                    Preconditions.checkState(
                            Objects.equals(value, otherValue),
                            "Value: %s is not equal to: %s",
                            value,
                            otherValue
                    );
                }
        );
    }

    /**
     * Asserts that the values of all StepFutures provided as parameters are distinct.
     * At least two StepFutures must be provided.
     * @throws IllegalStateException if any of the StepFutures are unpopulated when the step is executed.
     */
    @SafeVarargs
    public final <T> void valuesAreDistinct(StepFuture<T> firstValue, StepFuture<T> secondValue, StepFuture<T>... additionalValues) {
        StepFuture<T>[] expectedDistinctFutureValues = Arrays.copyOf(additionalValues, additionalValues.length + 2);
        expectedDistinctFutureValues[additionalValues.length] = firstValue;
        expectedDistinctFutureValues[additionalValues.length + 1] = secondValue;
        var valuesListFuture = ListStepFuture.of(expectedDistinctFutureValues);

        addExecuteStep(
                () -> {
                    List<T> expectedDistinctValues = valuesListFuture.get();

                    long actualNumDistinctValues = expectedDistinctValues.stream().distinct().count();
                    long expectedNumDistinctValues = expectedDistinctValues.size();

                    Preconditions.checkState(
                            expectedNumDistinctValues == actualNumDistinctValues,
                            "Not all values are distinct. Values: %s",
                            expectedDistinctValues
                    );
                }
        );
    }

}
