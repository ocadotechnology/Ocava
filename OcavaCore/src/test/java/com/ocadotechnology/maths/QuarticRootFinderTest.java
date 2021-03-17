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
package com.ocadotechnology.maths;

import static com.ocadotechnology.maths.PolynomialRootUtilsTest.assertEquals;

import org.apache.commons.math.complex.Complex;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.ocadotechnology.testing.UtilityClassTest;
import com.ocadotechnology.wrappers.Pair;

class QuarticRootFinderTest implements UtilityClassTest {

    @Override
    public Class<?> getTestSubject() {
        return QuarticRootFinder.class;
    }

    @Test
    void find_whenQuarticHasFourRealRoots_thenReturnsCorrectRoots() {
        ImmutableList<Complex> roots = QuarticRootFinder.find(3, 6, -123, -126, 1080);

        ImmutableList<Pair<Double, Double>> expectedRoots = ImmutableList.of(
                Pair.of(5.0, 0.0),
                Pair.of(3.0, 0.0),
                Pair.of(-4.0, 0.0),
                Pair.of(-6.0, 0.0)
        );

        assertEquals(expectedRoots, roots);
    }

    @Test
    void find_whenTrivialQuarticGiven_thenReturnsCorrectRoots() {
        ImmutableList<Complex> roots = QuarticRootFinder.find(1, 0, 0, 0, 0);

        ImmutableList<Pair<Double, Double>> expectedRoots = ImmutableList.of(
                Pair.of(0.0, 0.0),
                Pair.of(0.0, 0.0),
                Pair.of(0.0, 0.0),
                Pair.of(0.0, 0.0)
        );
        assertEquals(expectedRoots, roots);
    }

    @Test
    void find_whenQuarticHasFourEqualRealRoots_thenReturnsCorrectRoots() {
        ImmutableList<Complex> roots = QuarticRootFinder.find(1, -4, 6, -4, 1);

        ImmutableList<Pair<Double, Double>> expectedRoots = ImmutableList.of(
                Pair.of(1.0, 0.0),
                Pair.of(1.0, 0.0),
                Pair.of(1.0, 0.0),
                Pair.of(1.0, 0.0)
        );
        assertEquals(expectedRoots, roots);
    }

    @Test
    void find_whenQuarticHasTwoPairsOfEqualRealRoots_thenReturnsCorrectRoots() {
        ImmutableList<Complex> roots = QuarticRootFinder.find(1, 0, -2, 0, 1);

        ImmutableList<Pair<Double, Double>> expectedRoots = ImmutableList.of(
                Pair.of(1.0, 0.0),
                Pair.of(1.0, 0.0),
                Pair.of(-1.0, 0.0),
                Pair.of(-1.0, 0.0)
        );
        assertEquals(expectedRoots, roots);
    }

    @Test
    void find_whenQuarticHasTwoComplexAndTwoRealRoots_thenReturnsCorrectRoots() {
        ImmutableList<Complex> roots = QuarticRootFinder.find(-20, 5, 17, -29, 87);

        ImmutableList<Pair<Double, Double>> expectedRoots = ImmutableList.of(
                Pair.of(1.4876, 0.0),
                Pair.of(0.2222, 1.2996),
                Pair.of(0.2222, -1.2996),
                Pair.of(-1.6820, 0.0)
        );
        assertEquals(expectedRoots, roots);
    }

    @Test
    void find_whenQuarticHasFourComplexRoots_thenReturnsCorrectRoots() {
        ImmutableList<Complex> roots = QuarticRootFinder.find(1, 1, 3, -1, 12);

        ImmutableList<Pair<Double, Double>> expectedRoots = ImmutableList.of(
                Pair.of(-1.3122, 1.7320),
                Pair.of(0.8122, 1.3717),
                Pair.of(0.8122, -1.3717),
                Pair.of(-1.3122, -1.7320)
        );
        assertEquals(expectedRoots, roots);
    }

    @Test
    void find_whenGivenCubicEquation_thenReturnsCorrectRoots() {
        ImmutableList<Complex> roots = CubicRootFinder.find(1, 6, 12, 8);

        ImmutableList<Pair<Double, Double>> expectedRoots = ImmutableList.of(
                Pair.of(-2.0, 0.0),
                Pair.of(-2.0, 0.0),
                Pair.of(-2.0, 0.0)
        );
        assertEquals(expectedRoots, roots);
    }

    @Test
    void find_whenGivenQuadraticEquation_thenReturnsCorrectRoots() {
        ImmutableList<Complex> roots = QuarticRootFinder.find(0, 0, 1, 2, 1);

        ImmutableList<Pair<Double, Double>> expectedRoots = ImmutableList.of(
                Pair.of(-1.0, 0.0),
                Pair.of(-1.0, 0.0)
        );
        assertEquals(expectedRoots, roots);
    }

    @Test
    void find_whenGivenLinearEquation_thenReturnsCorrectRoot() {
        ImmutableList<Complex> roots = QuarticRootFinder.find(0, 0, 0, 2, 1);

        ImmutableList<Pair<Double, Double>> expectedRoots = ImmutableList.of(
                Pair.of(-0.5, 0.0)
        );
        assertEquals(expectedRoots, roots);
    }
}
