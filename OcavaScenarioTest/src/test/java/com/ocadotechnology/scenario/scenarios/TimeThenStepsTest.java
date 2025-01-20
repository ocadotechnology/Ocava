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
package com.ocadotechnology.scenario.scenarios;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.scenario.AbstractFrameworkTestStory;
import com.ocadotechnology.scenario.StepFuture;
import com.ocadotechnology.scenario.Story;

/**
 * A set of tests that validate the TimeThenSteps methods
 */
@Story
class TimeThenStepsTest extends AbstractFrameworkTestStory {
    private static final String EVENT_1 = "EVENT_1";

    @Test
    void waitUntil_withAbsoluteValue() {
        when.simStarts();
        then.time.waitUntil(1, TimeUnit.MINUTES);
        then.testEvent.timeIsExactly(60_000);
    }

    @Test
    void waitUntil_withStepFuture() {
        when.simStarts();
        double expectedTime = 123456;
        StepFuture<Double> future = when.testEvent.populateFuture(expectedTime);
        then.time.waitUntil(future);
        then.testEvent.timeIsExactly(expectedTime);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 100.0})
    public void getCurrentTime(double simStartTime) {
        double expectedTimeAfterOneMinute = (1000 * 60) + simStartTime;

        given.test.setSchedulerStartTime(simStartTime);

        when.simStarts();
        StepFuture<Double> extractedSimStartTime = then.time.getCurrentTime();
        then.futures.assertEquals(simStartTime, extractedSimStartTime);

        then.time.waitUntil(1, TimeUnit.MINUTES);
        StepFuture<Double> extractedTimeAfterOneMinute = then.time.getCurrentTime();
        then.futures.assertEquals(expectedTimeAfterOneMinute, extractedTimeAfterOneMinute);
    }

    @Test
    void waitForNextClockTick() {
        when.simStarts();
        then.time.waitForNextClockTick();
        then.testEvent.timeIsExactly(EventScheduler.ONE_CLOCK_CYCLE);
    }

    @Test
    void waitForDuration_withAbsoluteValue() {
        when.simStarts();
        then.time.waitUntil(1, TimeUnit.MINUTES);
        then.time.waitForDuration(1, TimeUnit.SECONDS);
        then.testEvent.timeIsExactly(61_000);
    }

    @Test
    void waitForDuration_withDurationValue() {
        when.simStarts();
        then.time.waitUntil(1, TimeUnit.MINUTES);
        then.time.waitForDuration(Duration.ofMinutes(1));
        then.testEvent.timeIsExactly(120_000);
    }

    @Test
    void waitForDuration_withStepFuture() {
        when.simStarts();
        then.time.waitUntil(1, TimeUnit.MINUTES);
        double additionalTime = 123456;
        StepFuture<Double> future = when.testEvent.populateFuture(additionalTime);
        then.time.waitForDuration(future);
        then.testEvent.timeIsExactly(additionalTime + 60_000);
    }

    @Test
    void timeIsLessThan_withAbsoluteValue_passes() {
        when.simStarts();
        when.testEvent.scheduled(10, EVENT_1);
        then.testEvent.occurs(EVENT_1);
        then.time.timeIsLessThan(11, TimeUnit.MILLISECONDS);
    }

    @Test
    void timeIsLessThan_withStepFuture_passes() {
        when.simStarts();
        StepFuture<Double> delay = when.testEvent.scheduled(10, EVENT_1);
        then.testEvent.occurs(EVENT_1);
        then.time.timeIsLessThan(delay.map(t -> t + 1));
    }
}
