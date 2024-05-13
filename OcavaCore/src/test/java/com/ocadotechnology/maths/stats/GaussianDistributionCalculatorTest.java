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
package com.ocadotechnology.maths.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GaussianDistributionCalculatorTest {

    @Test
    void calculateProbability_shouldCorrectlyCalculateProbability() {
        GaussianDistributionCalculator testSubject = new GaussianDistributionCalculator(new MeanAndStandardDeviation(0, 1));
        assertEquals(testSubject.calculateProbability(0), Probability.ONE);
        assertEquals(testSubject.calculateProbability(1).getProbability(), 0.31731050786291404);
        assertEquals(testSubject.calculateProbability(-1).getProbability(), 0.31731050786291404);
        assertEquals(testSubject.calculateProbability(-0.5).getProbability(), 0.617075077451974);
    }
}
