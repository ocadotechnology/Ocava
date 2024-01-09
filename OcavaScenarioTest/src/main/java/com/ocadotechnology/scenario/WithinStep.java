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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;

import com.google.common.base.Preconditions;
import com.ocadotechnology.event.EventUtil;
import com.ocadotechnology.event.scheduling.EventScheduler;

/**
 * A variant of CheckStep which enforces that the step is completed in less than a specified duration.
 */
@ParametersAreNonnullByDefault
class WithinStep<T> extends CheckStep<T> {
    private final Supplier<EventScheduler> schedulerSupplier;
    private final StepFuture<Double> duration;

    private final AtomicBoolean startedTimer = new AtomicBoolean(false);

    WithinStep(CheckStep<T> checkStep, Supplier<EventScheduler> schedulerSupplier, StepFuture<Double> duration) {
        super(checkStep.type, checkStep.notificationCache, checkStep.predicate);
        this.schedulerSupplier = schedulerSupplier;
        this.duration = duration;
    }

    @Override
    public void execute(@CheckForNull Object notification) {
        Preconditions.checkState(startedTimer.get(), "Called execute on WithinStep before setActive");
        super.execute(notification);
    }

    @Override
    public void setActive() {
        Preconditions.checkState(!startedTimer.get(), "Started WithinStep twice");
        schedulerSupplier.get().doIn(
                duration.get(),
                () -> Assertions.assertTrue(isFinished(), String.format("Within step failed - didn't finish within %s", EventUtil.durationToString(duration.get()))),
                "Timeout event");
        startedTimer.set(true);
    }
}
