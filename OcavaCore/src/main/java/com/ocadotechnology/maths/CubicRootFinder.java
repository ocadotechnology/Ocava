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

import org.apache.commons.math.complex.Complex;

import com.google.common.collect.ImmutableList;

/**
 * Class to calculate all roots for a given cubic equation.
 */
public final class CubicRootFinder {
    private CubicRootFinder() {
        throw new UnsupportedOperationException("Should not be instantiating a CubicRootFinder");
    }

    /**
     * Calculates the solutions to an equation of the form ax^3 + bx^2 + cx + d = 0
     *
     * @param a the coefficient of the x^3 term
     * @param b the coefficient of the x^2 term
     * @param c the coefficient of the x term
     * @param d the coefficient of the constant term
     * @return {@link Complex} objects representing all solutions to the given equation.
     *          The number of roots will be appropriate to the order of the polynomial equation:
     *          three if {@code a} is non-zero, else two if {@code b} is non-zero else one.
     *          Note that in the case of a repeated root, multiple equal {@link Complex} objects
     *          will be returned.
     *
     * @throws IllegalArgumentException if a, b and c are zero, as the equation is insoluble.
     */
    public static ImmutableList<Complex> find(double a, double b, double c, double d) {
        if (a == 0) {
            return QuadraticRootFinder.find(b, c, d);
        }

        ImmutableList.Builder<Complex> builder = ImmutableList.builder();

        double f = ((3 * c / a) - (Math.pow(b, 2) / Math.pow(a, 2))) / 3d;
        double g = ((2 * Math.pow(b, 3))/ Math.pow(a, 3) - (9 * b * c) / Math.pow(a, 2) + (27 * d / a)) / 27;
        double h = (Math.pow(g, 2)/4) + (Math.pow(f, 3)/27);

        if ( h > 0 ) {

            double R = -(g/2) + Math.sqrt(h);
            double S = Math.cbrt(R);
            double T = -(g/2) - Math.sqrt(h);
            double U = Math.cbrt(T);

            Complex root1 = new Complex(S + U - (b / (3 * a)), 0d);
            Complex root2 = new Complex(-(S + U)/2 - (b / (3 * a)), ((S-U) * Math.sqrt(3))/2d);
            Complex root3 = new Complex(-(S + U)/2 - (b / (3 * a)), - ((S - U) * Math.sqrt(3))/2d);
            return builder.add(root1).add(root2).add(root3).build();
        }

        if (f == 0 && g == 0 && h == 0) {
            Complex root = new Complex(-Math.cbrt(d/a), 0d);
            return builder.add(root).add(root).add(root).build();
        }

        double i = Math.sqrt(((Math.pow(g, 2)/4) - h));
        double j = Math.cbrt(i);
        double k = Math.acos(- (g / (2 * i)));
        double l = - j;
        double m = Math.cos(k /3);
        double n = Math.sqrt(3) * Math.sin(k / 3);
        double p =  -(b / (3 * a));

        double rootOne = 2 * j * Math.cos(k/3) - (b/ (3 *a));
        double rootTwo = l * (m + n) + p;
        double rootThree = l * (m - n) + p;
        return builder.add(new Complex(rootOne, 0d)).add(new Complex(rootTwo, 0d)).add(new Complex(rootThree, 0d)).build();
    }
}
