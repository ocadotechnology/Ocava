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

public class MeanAndStandardDeviation {
    private final double mean;
    private final double stdDev;

    public MeanAndStandardDeviation(double mean, double stdDev) {
        this.mean = mean;
        this.stdDev = stdDev;
    }

    public static MeanAndStandardDeviation fromMeanAndVariance(double mean, double variance) {
        return new MeanAndStandardDeviation(mean, Math.sqrt(variance));
    }

    public double getMean() {
        return mean;
    }

    public double getStdDev() {
        return stdDev;
    }

    public double getVariance() {
        return Math.pow(getStdDev(), 2);
    }
}
