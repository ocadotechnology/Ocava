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

import org.apache.commons.math.complex.Complex;

import com.google.common.collect.ImmutableList;

/**
 * Class to calculate all roots for a given quartic equation.
 */
public final class QuarticRootFinder {
    private QuarticRootFinder() {
        throw new UnsupportedOperationException("Should not be instantiating a QuarticRootFinder");
    }

    private static final double EPSILON = 1e-14;

    /**
     * Calculates the solutions to an equation of the form ax^4 + bx^3 + cx^2 + dx + e = 0
     *
     * @param a the coefficient of the x^4 term
     * @param b the coefficient of the x^3 term
     * @param c the coefficient of the x^2 term
     * @param d the coefficient of the x term
     * @param e the coefficient of the constant term
     * @return {@link Complex} objects representing all solutions to the given equation.
     *          The number of roots will be appropriate to the order of the polynomial equation:
     *          four if {@code a} is non-zero, else three if {@code c} is non-zero etc.
     *          Note that in the case of a repeated root, multiple equal {@link Complex} objects
     *          will be returned.
     *
     * @throws IllegalArgumentException if a, b, c and d are zero, as the equation is insoluble.
     */
    public static ImmutableList<Complex> find(double a, double b, double c, double d, double e) {
        if (a == 0) {
            return CubicRootFinder.find(b, c, d, e);
        }

        //See https://en.wikipedia.org/wiki/Quartic_function#Solution_methods - subsection: Ferrari's Solution

        //Simplify equation so its of the form x^4 + bx^3 + cx^2 + dx + e = 0
        b = b / a;
        c = c / a;
        d = d / a;
        e = e / a;

        //Define y^4 + py^2 + qy + r = 0 (where x = y - b/4)
        Complex yToXAdjustment = new Complex(-b / 4, 0);
        double p = c - 3 * Math.pow(b, 2) / 8;
        double q = d + (Math.pow(b, 3) / 8) - (b * c / 2);
        double r = e - (3 * Math.pow(b, 4) / 256) + (Math.pow(b, 2) * c / 16) - (b * d / 4);

        //Special case of y^4 = 0
        if (p == 0 && q == 0 && r == 0) {
            return ImmutableList.of(yToXAdjustment, yToXAdjustment, yToXAdjustment, yToXAdjustment);
        }

        //Solve the following cubic for m
        ImmutableList<Complex> rootsOfCubic = CubicRootFinder.find(1,  p, (Math.pow(p, 2) / 4) - r, -Math.pow(q, 2) / 8);
        Complex m = rootsOfCubic.stream()
                .filter(complex -> Math.abs(complex.getImaginary()) >  EPSILON || Math.abs(complex.getReal()) > EPSILON)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No non-zero solution for m found"));

        Complex yRoot1 = calculateRoot(p, q, m, true, true);
        Complex yRoot2 = calculateRoot(p, q, m, true, false);
        Complex yRoot3 = calculateRoot(p, q, m, false, true);
        Complex yRoot4 = calculateRoot(p, q, m, false, false);

        return ImmutableList.of(
                yRoot1.add(yToXAdjustment),
                yRoot2.add(yToXAdjustment),
                yRoot3.add(yToXAdjustment),
                yRoot4.add(yToXAdjustment));
    }

    private static Complex calculateRoot(double p, double q, Complex m, boolean firstIsPositive, boolean secondIsPositive) {
        Complex workingResult = new Complex(Math.sqrt(2) * q, 0).divide(m.sqrt());
        if (!firstIsPositive) {
            workingResult = workingResult.multiply(-1);
        }
        workingResult = workingResult.add(m.multiply(2)).add(new Complex(2 * p, 0)).multiply(-1).sqrt();
        if (!secondIsPositive) {
            workingResult = workingResult.multiply(-1);
        }

        Complex sqrt2m = m.multiply(2).sqrt();
        if (firstIsPositive) {
            workingResult = workingResult.add(sqrt2m);
        } else {
            workingResult = workingResult.subtract(sqrt2m);
        }
        return workingResult.multiply(0.5);
    }
}
