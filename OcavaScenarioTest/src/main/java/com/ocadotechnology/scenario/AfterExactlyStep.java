/*
 * Copyright © 2017-2021 Ocado (Ocava)
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

import org.junit.jupiter.api.Assertions;

import com.ocadotechnology.event.EventUtil;
import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.time.TimeProvider;

class AfterExactlyStep<T> extends UnorderedCheckStep<T> {
    private static final double TOLERANCE_FRACTION = 1e-9;

    private final TimeProvider timeProvider;
    private final double expectedTime;
    private final double tolerance;

    AfterExactlyStep(CheckStep<T> checkStep, EventScheduler eventScheduler, double duration) {
        super(checkStep, true);
        this.timeProvider = eventScheduler.getTimeProvider();

        expectedTime = timeProvider.getTime() + duration;
        tolerance = expectedTime * TOLERANCE_FRACTION;

        eventScheduler.doAt(
                expectedTime + tolerance,
                () -> Assertions.assertTrue(
                        isFinished(),
                        String.format("After Exactly step failed - didn't finish within %s", EventUtil.durationToString(duration))),
                "Timeout event");
    }

    @Override
    public void execute() {
        super.execute();

        if (isFinished()) {
            double finishedTime = timeProvider.getTime();
            Assertions.assertTrue(Math.abs(finishedTime - expectedTime) <= tolerance,
                    String.format("After Exactly step failed - finished at time %s not at expected time %s",
                            EventUtil.eventTimeToString(finishedTime),
                            EventUtil.eventTimeToString(expectedTime)));
        }
    }
}
