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

import java.util.Collection;

import com.google.common.collect.ImmutableList;

/**
 * A class for managing a list of StepFutures
 */
public class ListStepFuture<T> implements StepFuture<ImmutableList<T>> {

    private final ImmutableList<StepFuture<T>> stepFutures;

    public ListStepFuture(ImmutableList<StepFuture<T>> stepFutures) {
        this.stepFutures = stepFutures;
    }

    /**
     * Creates a ListStepFuture instance from StepFutures provided as separate parameters
     */
    @SafeVarargs
    public static <T> ListStepFuture<T> of(StepFuture<T>... stepFutures) {
        return new ListStepFuture<>(ImmutableList.copyOf(stepFutures));
    }

    /**
     * Creates a ListStepFuture instance from StepFutures provided as a collection
     */
    public static <T> ListStepFuture<T> of(Collection<StepFuture<T>> stepFutures) {
        return new ListStepFuture<>(ImmutableList.copyOf(stepFutures));
    }

    /**
     * Returns the values of all the StepFutures in the list.
     * Will throw an IllegalStateException if any of the StepFutures have not been populated
     */
    @Override
    public ImmutableList<T> get() {
        return stepFutures.stream()
                .map(StepFuture::get)
                .collect(ImmutableList.toImmutableList());
    }

    /**
     * Returns the values of all the StepFutures in the list that have been populated
     */
    public ImmutableList<T> getAnyPopulated() {
        return stepFutures.stream()
                .filter(StepFuture::hasBeenPopulated)
                .map(StepFuture::get)
                .collect(ImmutableList.toImmutableList());
    }

    /**
     * Returns true if all the StepFutures in the list have been populated
     */
    @Override
    public boolean hasBeenPopulated() {
        return stepFutures.stream()
                .map(StepFuture::hasBeenPopulated)
                .reduce(true, Boolean::logicalAnd);
        }

}
