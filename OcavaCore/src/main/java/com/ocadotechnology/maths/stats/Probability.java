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

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

@ParametersAreNonnullByDefault
public class Probability implements Comparable<Probability> {
    public static final Probability ZERO = new Probability(0);
    public static final Probability ONE = new Probability(1);
    private final double probability;

    public Probability(double v) {
        Preconditions.checkState(v >= 0 && v <= 1, "Probability must be between 0 and 1");
        this.probability = v;
    }

    @Override
    public int compareTo(Probability that) {
        return Double.compare(probability, that.probability);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Probability that = (Probability) o;
        return Double.compare(probability, that.probability) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(probability);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("probability", probability)
                .toString();
    }

    public double getProbability() {
        return probability;
    }
}
