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
package com.ocadotechnology.physics.utils;

import java.util.function.DoubleFunction;
import java.util.function.ToDoubleFunction;

import com.google.common.base.Preconditions;

public class BinarySearch {
    private BinarySearch(){}

    /**
     * Searches over the double range between lowerBound and upperBound for an instance of T specified by the
     * comparator.
     *
     * Any precision constraints should be taken account of in comparator.
     *
     * @param pointAtValue should supply the T instance which corresponds with a given double value
     * @param comparator should take a T and return 0 if T is being searched for, less than 0 for if T maps to a lower
     *                   value than the object being searched for, and greater than 0 for if T maps to a higher value
     *                   than the object being searched for.
     * @param lowerBound the lower bound of the region to be searched over
     * @param upperBound the upper bound of the region to be searched over
     * @return the first encountered T instance provided by pointAtValue for which the comparator returns zero.
     *
     * @throws IllegalArgumentException if the lower bound is greater than the upper bound, or if the comparator returns
     *                                  greater than zero for the T instance at the lower bound, or less than zero for
     *                                  the T instance at the upper bound.
     */
    public static <T> T find(DoubleFunction<T> pointAtValue, ToDoubleFunction<T> comparator, double lowerBound, double upperBound) {
        Preconditions.checkArgument(lowerBound <= upperBound, "lower bound " + lowerBound + " must not be greater than upper bound " + upperBound);
        
        T lowerBoundPoint = pointAtValue.apply(lowerBound);
        double lowerBoundComp = comparator.applyAsDouble(lowerBoundPoint);
        if (lowerBoundComp == 0) {
            return lowerBoundPoint;
        }
        Preconditions.checkArgument(lowerBoundComp < 0, "Invalid search parameter");

        T upperBoundPoint = pointAtValue.apply(upperBound);
        double upperBoundComp = comparator.applyAsDouble(upperBoundPoint);
        if (upperBoundComp == 0) {
            return upperBoundPoint;
        }
        Preconditions.checkArgument(upperBoundComp > 0, "Invalid search parameter");

        while (true) {
            double mid = (lowerBound + upperBound) / 2.0;
            T newPoint = pointAtValue.apply(mid);
            double comp = comparator.applyAsDouble(newPoint);
            Preconditions.checkState(mid > lowerBound && mid < upperBound);

            if (comp == 0) {
                return newPoint;
            } else if (comp < 0) {
                lowerBound = mid;
            } else {
                upperBound = mid;
            }
        }
    }
}
