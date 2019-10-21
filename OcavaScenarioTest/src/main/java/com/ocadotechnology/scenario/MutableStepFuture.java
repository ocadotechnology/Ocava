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

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

/**
 * The standard implementation of StepFuture which will be populated at a later point in the test execution.
 */
public class MutableStepFuture<T> implements StepFuture<T> {
    private T t;
    private boolean populated;

    public void populate(T t) {
        this.t = t;
        populated = true;
    }

    @Override
    public T get() {
        Preconditions.checkState(populated, "Called get() on MutableStepFuture before calling populate()");
        return t;
    }

    @Override
    public boolean hasBeenPopulated() {
        return populated;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("value", t)
                .toString();
    }
}
