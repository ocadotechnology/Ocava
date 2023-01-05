/*
 * Copyright © 2017-2023 Ocado (Ocava)
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.Iterator;

import org.apache.commons.math.complex.Complex;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.ocadotechnology.testing.UtilityClassTest;
import com.ocadotechnology.wrappers.Pair;

class PolynomialRootUtilsTest implements UtilityClassTest {
    private static final double EPSILON = 0.01;

    @Override
    public Class<?> getTestSubject() {
        return PolynomialRootUtils.class;
    }

    @Test
    void getMinimumPositiveRealRoot_whenMultiplePositiveRootsProvided_thenReturnsCorrectValue() {
        ImmutableList<Complex> roots = ImmutableList.of(
                new Complex(2, 0),
                new Complex(1, 0),
                new Complex(3, 0)
        );
        Assertions.assertEquals(1, PolynomialRootUtils.getMinimumPositiveRealRoot(roots));
    }

    @Test
    void getMinimumPositiveRealRoot_whenComplexRootHasSmallImaginaryPart_thenReturnsRealValue() {
        ImmutableList<Complex> roots = ImmutableList.of(
                new Complex(1, 0.01)
        );
        Assertions.assertEquals(1, PolynomialRootUtils.getMinimumPositiveRealRoot(roots, 0.1));
    }

    @Test
    void getMinimumPositiveRealRoot_whenUnsuitableRootsProvided_thenReturnsCorrectValue() {
        ImmutableList<Complex> roots = ImmutableList.of(
                new Complex(-1, 0),
                new Complex(1, 1),
                new Complex(1, -1),
                new Complex(53, 0)
        );
        Assertions.assertEquals(53, PolynomialRootUtils.getMinimumPositiveRealRoot(roots));
    }

    @Test
    void getMinimumPositiveRealRoot_whenNoSuitableRootProvided_thenThrowsException() {
            ImmutableList<Complex> roots = ImmutableList.of(
                    new Complex(0, 0),
                    new Complex(0, 100),
                    new Complex(-100, 0)
            );
        Assertions.assertThrows(IllegalArgumentException.class, () -> PolynomialRootUtils.getMinimumPositiveRealRoot(roots));
    }

    @Test
    void getMinimumPositiveRealRoot_whenNoRootsProvided_thenThrowsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> PolynomialRootUtils.getMinimumPositiveRealRoot(ImmutableList.of()));
    }

    @Test
    void getMaximumNegativeRealRoot_whenMultipleNegativeRootsProvided_thenReturnsCorrectValue() {
        ImmutableList<Complex> roots = ImmutableList.of(
                new Complex(-2, 0),
                new Complex(-1, 0),
                new Complex(-3, 0)
        );
        Assertions.assertEquals(-1, PolynomialRootUtils.getMaximumNegativeRealRoot(roots));
    }

    @Test
    void getMaximumNegativeRealRoot_whenComplexRootHasSmallImaginaryPart_thenReturnsRealValue() {
        ImmutableList<Complex> roots = ImmutableList.of(
                new Complex(-1, 0.01)
        );
        Assertions.assertEquals(-1, PolynomialRootUtils.getMaximumNegativeRealRoot(roots, 0.1));
    }

    @Test
    void getMaximumNegativeRealRoot_whenUnsuitableRootsProvided_thenReturnsCorrectValue() {
        ImmutableList<Complex> roots = ImmutableList.of(
                new Complex(1, 0),
                new Complex(-1, 1),
                new Complex(-1, -1),
                new Complex(-53, 0)
        );
        Assertions.assertEquals(-53, PolynomialRootUtils.getMaximumNegativeRealRoot(roots));
    }

    @Test
    void getMaximumNegativeRealRoot_whenNoSuitableRootProvided_thenThrowsException() {
            ImmutableList<Complex> roots = ImmutableList.of(
                    new Complex(0, 0),
                    new Complex(0, 100),
                    new Complex(100, 0)
            );
        Assertions.assertThrows(IllegalArgumentException.class, () -> PolynomialRootUtils.getMaximumNegativeRealRoot(roots));
    }

    @Test
    void getMaximumNegativeRealRoot_whenNoRootsProvided_thenThrowsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> PolynomialRootUtils.getMaximumNegativeRealRoot(ImmutableList.of()));
    }

    static void assertEquals(ImmutableCollection<Pair<Double, Double>> expectedRoots, ImmutableCollection<Complex> actualRoots) {
        Assertions.assertEquals(expectedRoots.size(), actualRoots.size(), "Unexpected number of roots provided");

        Iterator<Pair<Double, Double>> sortedExpectedRoots = expectedRoots.stream()
                .sorted(Comparator.<Pair<Double, Double>>comparingDouble(p -> p.a).thenComparingDouble(p -> p.b))
                .iterator();
        Iterator<Complex> sortedActualRoots = actualRoots.stream()
                .sorted(Comparator.comparingDouble(Complex::getReal).thenComparingDouble(Complex::getImaginary))
                .iterator();

        while (sortedActualRoots.hasNext()) {
            assertEquals(sortedExpectedRoots.next(), sortedActualRoots.next());
        }
    }

    private static void assertEquals(Pair<Double, Double> expectedRoot, Complex actualRoot) {
        assertThat(actualRoot.getReal()).isCloseTo(expectedRoot.a, AssertionsForClassTypes.within(EPSILON));
        assertThat(actualRoot.getImaginary()).isCloseTo(expectedRoot.b, AssertionsForClassTypes.within(EPSILON));
    }
}
