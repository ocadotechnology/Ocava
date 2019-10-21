/*
 * Copyright © 2017-2019 Ocado (Ocava)
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

class CubicRootFinderTest implements UtilityClassTest {

    @Override
    public Class<?> getTestSubject() {
        return CubicRootFinder.class;
    }

    @Test
    void find_whenAllThreeRootsAreReal_thenReturnsCorrectRoots() {
        ImmutableList<Complex> roots = CubicRootFinder.find(2, -4, -22, 24);

        ImmutableList<Pair<Double, Double>> expectedRoots = ImmutableList.of(
                Pair.of(4.0, 0.0),
                Pair.of(1.0, 0.0),
                Pair.of(-3.0, 0.0)
        );
        assertEquals(expectedRoots, roots);
    }

    @Test
    void find_whenAllRootsAreRealAndEqual_thenReturnsCorrectRoots() {
        ImmutableList<Complex> roots = CubicRootFinder.find(1, 6, 12, 8);

        ImmutableList<Pair<Double, Double>> expectedRoots = ImmutableList.of(
                Pair.of(-2.0, 0.0),
                Pair.of(-2.0, 0.0),
                Pair.of(-2.0, 0.0)
                );
        assertEquals(expectedRoots, roots);
    }

    @Test
    void find_whenOnlyOneRootIsReal_thenReturnsCorrectRoots() {
        ImmutableList<Complex> roots = CubicRootFinder.find(3, -10, 14, 27);

        ImmutableList<Pair<Double, Double>> expectedRoots = ImmutableList.of(
                Pair.of(-1.0, 0.0),
                Pair.of(2.1667, 2.075),
                Pair.of(2.1667, -2.075)
                );
        assertEquals(expectedRoots, roots);
    }

    @Test
    void find_whenGivenQuadraticEquation_thenReturnsCorrectRoots() {
        ImmutableList<Complex> roots = CubicRootFinder.find(0, 1, 2, 1);

        ImmutableList<Pair<Double, Double>> expectedRoots = ImmutableList.of(
                Pair.of(-1.0, 0.0),
                Pair.of(-1.0, 0.0)
        );
        assertEquals(expectedRoots, roots);
    }

    @Test
    void find_whenGivenLinearEquation_thenReturnsCorrectRoot() {
        ImmutableList<Complex> roots = CubicRootFinder.find(0, 0, 2, 1);

        ImmutableList<Pair<Double, Double>> expectedRoots = ImmutableList.of(
                Pair.of(-0.5, 0.0)
        );
        assertEquals(expectedRoots, roots);
    }
}
