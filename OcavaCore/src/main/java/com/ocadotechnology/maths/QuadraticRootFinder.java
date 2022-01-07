/*
 * Copyright © 2017-2022 Ocado (Ocava)
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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * Class to calculate all roots for a given quadratic equation.
 */
public final class QuadraticRootFinder {

    private QuadraticRootFinder() {
        throw new UnsupportedOperationException("Should not be instantiating a QuadraticRootFinder");
    }

    /**
     * Calculates the solutions to an equation of the form ax^2 + bx + c = 0
     *
     * @param a the coefficient of the x^2 term
     * @param b the coefficient of the x term
     * @param c the coefficient of the constant term
     * @return {@link Complex} objects representing all solutions to the given equation.
     *          The number of roots will be appropriate to the order of the polynomial equation:
     *          two if {@code a} is non-zero, else one.  Note that in the case of a repeated root, multiple
     *          equal {@link Complex} objects will be returned.
     *
     * @throws IllegalArgumentException if a and b are both zero, as the equation is insoluble.
     */
    public static ImmutableList<Complex> find(double a, double b, double c) {
        if (a == 0) {
            Preconditions.checkArgument(b != 0, "No solutions can be found for an equation of the form c = 0");
            return ImmutableList.of(new Complex(-c / b, 0));
        }

        double discriminant = Math.pow(b, 2) - 4 * a * c;

        if (discriminant < 0) {
            Complex rootOne = new Complex(-b / (2 * a), Math.sqrt(-discriminant) / (2 * a));
            Complex rootTwo = new Complex(rootOne.getReal(), -rootOne.getImaginary());
            return ImmutableList.of(rootOne, rootTwo);
        }

        Complex rootOne =  new Complex((-b + Math.sqrt(discriminant)) / (2 * a), 0d);
        Complex rootTwo =  new Complex((-b - Math.sqrt(discriminant)) / (2 * a), 0d);
        return ImmutableList.of(rootOne, rootTwo);
    }
}
