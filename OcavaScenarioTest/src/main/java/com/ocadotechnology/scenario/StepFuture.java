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
package com.ocadotechnology.scenario;

import java.util.function.Function;

/**
 * Class created to pass a value between steps which is determined by the execution of the first step.
 * The value stored in the StepFuture will be populated when the first step is executed, and evaluated
 * when the second step is executed.
 *
 * For example,
 *
 * {@code
 * StepFuture<Actor> actor = then.actor.anyStartsTask();
 * then.actor.finishesTask(actor);
 * }
 *
 * In most cases, a {@link MutableStepFuture} should be instantiated to store a result while {@link StepFuture#of} can
 * be used to satisfy the same API if the value is known in advance.
 */
public interface StepFuture<T> {
    T get();
    boolean hasBeenPopulated();

    /**
     * Utility method to allow a StepFuture instance to be created in cases where the value is known in advance
     */
    static <T> StepFuture<T> of(T thing) {
        return new PopulatedStepFuture<>(thing);
    }

    /**
     * Returns a new StepFuture implementation which wraps the original and applies a mapping function to its returned
     * value when {@link StepFuture#get} is called.  This can be used, for example, to define the 'other' element in a
     * test.
     *
     * For example,
     *
     * {@code
     * StepFuture<Actor> actor = then.actor.anyStartsTask();
     * StepFuture<Actor> otherActor = actor.map(a -> a.equals(ACTOR_1) ? ACTOR_2 : ACTOR_1);
     * then.actor.never().startsTask(otherActor);
     * }
     */
    default <V> StepFuture<V> map(Function<T, V> pureMapper) {
        return new MappedStepFuture<T, V>(this, pureMapper);
    }
}