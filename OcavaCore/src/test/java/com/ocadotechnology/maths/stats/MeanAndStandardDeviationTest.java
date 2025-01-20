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
package com.ocadotechnology.maths.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class MeanAndStandardDeviationTest {
    @Test
    void calculateObservedMeanAndStandardDeviation_shouldCorrectlyCalculateObservedMeanAndStandardDeviation() {
        assertEquals(MeanAndStandardDeviation.calculateObservedMeanAndStandardDeviation(List.of()), new MeanAndStandardDeviation(0, 0));
        assertEquals(MeanAndStandardDeviation.calculateObservedMeanAndStandardDeviation(List.of(1.0, 1.0)), new MeanAndStandardDeviation(1, 0));
        assertEquals(MeanAndStandardDeviation.calculateObservedMeanAndStandardDeviation(List.of(1.0, 2.0)), new MeanAndStandardDeviation(1.5, 0.5));
        assertEquals(MeanAndStandardDeviation.calculateObservedMeanAndStandardDeviation(List.of(1.0, 2.0, 3.0, 4.0)), new MeanAndStandardDeviation(2.5, Math.sqrt(5) / 2));
    }
}
