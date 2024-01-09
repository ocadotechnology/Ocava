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

import java.util.Collection;
public class Stats {
    private Stats() {
        // utility class
    }

    public static double stdDevOfProductOfIndependentVariables(MeanAndStandardDeviation first, MeanAndStandardDeviation second) {
        double variance1 = first.getVariance();
        double variance2 = second.getVariance();
        double productVariance = variance1 * variance2 + variance1 * Math.pow(
                second.getMean(), 2
        ) + Math.pow(
                first.getMean(), 2
        ) * variance2;
        return Math.sqrt(productVariance);
    }

    public static double stdDevOfSumOfIndependentVariables(Collection<Double> stdDevs) {
        return Math.sqrt(stdDevs.stream().mapToDouble(stdDev -> Math.pow(stdDev, 2)).sum());
    }
}

