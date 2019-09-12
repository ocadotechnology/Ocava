/*
 * Copyright © 2017 Ocado (Ocava)
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

/**
 * Used to signify that a step is expected to fail the scenario tests. This works in conjunction with the @FixRequired
 * annotation
 */
public class FailingStep<T> extends CheckStep<T> {
    private final CheckStep<T> checkStep;

    FailingStep(CheckStep<T> checkStep) {
        super(checkStep.type, checkStep.notificationCache, false, checkStep.predicate);
        this.checkStep = checkStep;
    }

    /**
     * Override the execute step and always throw a {@link FailingStepException}
     */
    @Override
    public void execute() {
        super.execute();
        throw new FailingStepException(!isFinished(), checkStep, this, getLastSeen());
    }

    static class FailingStepException extends RuntimeException {
        final boolean failed;
        final CheckStep<?> checkStep;
        final NamedStep namedStep;
        final Object lastSeen;

        FailingStepException(boolean failed, CheckStep<?> checkStep, NamedStep namedStep, Object lastSeen) {
            this.failed = failed;
            this.checkStep = checkStep;
            this.namedStep = namedStep;
            this.lastSeen = lastSeen;
        }
    }
}
