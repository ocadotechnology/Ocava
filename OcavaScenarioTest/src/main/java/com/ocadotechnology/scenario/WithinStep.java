/*
 * Copyright © 2017-2020 Ocado (Ocava)
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

class WithinStep<T> extends UnorderedCheckStep<T> {
    WithinStep(CheckStep<T> checkStep, EventScheduler eventScheduler, double duration) {
        super(checkStep, true);
        eventScheduler.doIn(
                duration,
                () -> Assertions.assertTrue(isFinished(), String.format("Within step failed - didn't finish within %s", EventUtil.durationToString(duration))),
                "Timeout event");
    }
}
