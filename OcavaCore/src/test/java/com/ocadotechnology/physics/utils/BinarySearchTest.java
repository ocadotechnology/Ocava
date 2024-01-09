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
package com.ocadotechnology.physics.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.common.collect.ImmutableList;
import com.ocadotechnology.validation.Failer;
import com.ocadotechnology.wrappers.Pair;

class BinarySearchTest {
    private static final ImmutableList<Pair<Integer, Integer>> ranges = IntStream.range(0, 1000)
            .mapToObj(i -> Pair.of(i, i + 1))
            .collect(ImmutableList.toImmutableList());
    private int counter = 0;

    private static Pair<Integer, Integer> getRangeForValue(double value) {
        return ranges.stream()
                .filter(r -> r.a <= value && value < r.b)
                .findFirst()
                .orElseThrow(Failer::valueExpected);
    }

    private double compare(Pair<Integer, Integer> range, double target) {
        ++counter;
        if (range.a <= target && target < range.b) {
            return 0;
        }

        return range.a - target;
    }

    @BeforeEach
    void setup() {
        counter = 0;
    }

    @Test
    void find_whenLowerBoundIsGreaterThanUpperBound_thenThrowsException() {
        assertThatThrownBy(() -> BinarySearch.find(BinarySearchTest::getRangeForValue, r -> 0, 10, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void find_whenComparatorReturnsGreaterThanZeroForInstanceAtLowerBound_thenThrowsException() {
        assertThatThrownBy(() -> runFind(-2000)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void find_whenComparatorReturnsLessThanZeroForInstanceAtUpperBound_thenThrowsException() {
        assertThatThrownBy(() -> runFind(2000)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "target = {0}")
    @ValueSource(doubles = {0, 1.01, 331, 998, 999})
    void find_whenTargetIsWithinRange_thenCallsComparatorFewTimes(double target) {
        int limit = 12;
        Pair<Integer, Integer> result = runFind(target);
        assertThat(result).isEqualTo(ranges.get((int)target));
        assertThat(counter).isLessThanOrEqualTo(limit);
    }

    private Pair<Integer, Integer> runFind(double target) {
        return BinarySearch.find(BinarySearchTest::getRangeForValue, r -> compare(r, target), 0, 999.9999999);
    }
}
