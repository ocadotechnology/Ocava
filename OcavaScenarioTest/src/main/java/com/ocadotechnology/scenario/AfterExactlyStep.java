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
package com.ocadotechnology.scenario;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AtomicDouble;
import com.ocadotechnology.event.EventUtil;
import com.ocadotechnology.event.scheduling.EventScheduler;

/**
 * A variant of CheckStep which enforces that the step is completed after exactly a specified time has passed.
 */
@ParametersAreNonnullByDefault
class AfterExactlyStep<T> extends CheckStep<T> {
    /**
     * In order to avoid failing with rounding errors, we tolerate the event completing within a tolerated fraction of
     * the expected time. In order to be tolerant of various time scales in different simulations, the tolerance is
     * defined as a fraction of the expected time.
     *
     * For reference, this value gives a tolerance of ~1ms for an expected time in 2023 based on an epoch of 1st Jan 1970
     */
    private static final double TOLERANCE_FRACTION = 1e-12;

    private final Supplier<EventScheduler> schedulerSupplier;
    private final StepFuture<Double> duration;

    private final AtomicBoolean startedTimer = new AtomicBoolean(false);
    private final AtomicDouble expectedTime = new AtomicDouble(Double.NaN);
    private final AtomicDouble tolerance = new AtomicDouble(Double.NEGATIVE_INFINITY);

    AfterExactlyStep(CheckStep<T> checkStep, Supplier<EventScheduler> schedulerSupplier, StepFuture<Double> duration) {
        super(checkStep.type, checkStep.notificationCache, checkStep.predicate);
        this.schedulerSupplier = schedulerSupplier;
        this.duration = duration;
    }

    @Override
    public void execute(@CheckForNull Object notification) {
        Preconditions.checkState(startedTimer.get(), "Called execute on AfterExactlyStep before setActive");
        super.execute(notification);

        if (isFinished()) {
            double finishedTime = schedulerSupplier.get().getTimeProvider().getTime();
            Assertions.assertTrue(Math.abs(finishedTime - expectedTime.get()) <= tolerance.get(),
                    String.format("After Exactly step failed - finished at time %s not at expected time %s",
                            EventUtil.eventTimeToString(finishedTime),
                            EventUtil.eventTimeToString(expectedTime.get())));
        }
    }

    @Override
    public void setActive() {
        Preconditions.checkState(!startedTimer.get(), "Started AfterAtLeastStep twice");
        EventScheduler eventScheduler = schedulerSupplier.get();
        double expectedTime = eventScheduler.getTimeProvider().getTime() + duration.get();
        double tolerance = expectedTime * TOLERANCE_FRACTION;

        eventScheduler.doAt(
                expectedTime + tolerance,
                () -> Assertions.assertTrue(
                        isFinished(),
                        String.format("After Exactly step failed - didn't finish within %s", EventUtil.durationToString(duration.get()))),
                "Timeout event");
        this.expectedTime.set(expectedTime);
        this.tolerance.set(tolerance);
        this.startedTimer.set(true);
    }
}
