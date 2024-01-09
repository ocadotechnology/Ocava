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

import static com.ocadotechnology.maths.stats.Stats.stdDevOfProductOfIndependentVariables;
import static com.ocadotechnology.maths.stats.Stats.stdDevOfSumOfIndependentVariables;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StatsTest {

    @Test
    void stdDevOfProductOfIndependentVariables_whenCalled_thenReturnsCorrectValue() {
        // given
        double mean1 = 3.0;
        double mean2 = 3.0;
        double stdDev1 = 2.0;
        double stdDev2 = 1.0;
        double expectedStandardDeviation = 7.0;

        // when
        double calculatedStandardDeviation = stdDevOfProductOfIndependentVariables(
                new MeanAndStandardDeviation(mean1, stdDev1),
                new MeanAndStandardDeviation(mean2, stdDev2)
        );

        // then
        Assertions.assertEquals(expectedStandardDeviation, calculatedStandardDeviation);

    }

    @Test
    void stdDevOfSumOfIndependentVariables_whenCalled_thenReturnsCorrectValue() {
        // given
        List<Double> stdDevs = Arrays.asList(3.0, 4.0, 4.0, 2.0, 2.0);
        double expectedStandardDeviation = 7.0;

        // when
        double calculateStandardDeviation = stdDevOfSumOfIndependentVariables(stdDevs);

        // then
        Assertions.assertEquals(expectedStandardDeviation, calculateStandardDeviation);
    }

}

