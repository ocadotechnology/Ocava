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

import java.util.Comparator;

import org.apache.commons.math.complex.Complex;

import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

/**
 * Utility class for processing sets of roots of polynomial equations.
 */
public final class PolynomialRootUtils {
    private PolynomialRootUtils() {
        throw new UnsupportedOperationException("Should not be instantiating a PolynomialRootUtils");
    }

    /**
     * @return the minimum real component of the supplied complex numbers with a strictly positive real component, and
     *         an imaginary component equal to zero within 1e-9.
     *
     * @throws IllegalArgumentException if no positive real root is supplied
     */
    public static double getMinimumPositiveRealRoot(ImmutableList<Complex> roots) {
        return getMinimumPositiveRealRoot(roots, 1e-9);
    }

    /**
     * @return the minimum real component of the supplied complex numbers with a strictly positive real component, and
     *         an imaginary component equal to zero within the provided epsilon.
     *
     * @throws IllegalArgumentException if no positive real root is supplied
     */
    public static double getMinimumPositiveRealRoot(ImmutableList<Complex> roots, double epsilon) {
        return roots
                .stream()
                .filter(c -> DoubleMath.fuzzyEquals(c.getImaginary(), 0, epsilon) && c.getReal() > 0d)
                .min(Comparator.comparingDouble(Complex::getReal))
                .orElseThrow(() -> new IllegalArgumentException("No positive, real root provided"))
                .getReal();
    }

    /**
     * @return the maximum real component of the supplied complex number with a strictly negative real component, and
     *          an imaginary component equal to zero within 1e-9.
     *
     * @throws IllegalArgumentException if no negative real root is supplied
     */
    public static double getMaximumNegativeRealRoot(ImmutableList<Complex> roots) {
        return getMaximumNegativeRealRoot(roots, 1e-9);
    }

    /**
     * @return the maximum real component of the supplied complex number with a strictly negative real component, and
     *          an imaginary component equal to zero within the provided epsilon.
     *
     * @throws IllegalArgumentException if no negative real root is supplied
     */
    public static double getMaximumNegativeRealRoot(ImmutableList<Complex> roots, double epsilon) {
        return roots
                .stream()
                .filter(c -> DoubleMath.fuzzyEquals(c.getImaginary(), 0, epsilon) && c.getReal() < 0d)
                .max(Comparator.comparingDouble(Complex::getReal))
                .orElseThrow(() -> new IllegalArgumentException("No negative, real root provided"))
                .getReal();
    }
}
