/*
 * Copyright © 2017 Ocado (Ocava)
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

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.ToDoubleFunction;

import com.google.common.collect.Iterables;
import com.ocadotechnology.validation.Failer;

public class SegmentGraphUtilities {
    private static final double ERROR_TOLERANCE = 10E-9;

    private SegmentGraphUtilities() {}

    /**
     * Utility to calculate the interpolated 'y' value on an x/y graph with multiple segments with independent y values
     * and cumulative x values (eg speed at a given time/distance).
     *
     * @param segments The ordered list of segments defining the graph
     * @param segmentToXExtent Function giving the range of x values covered by a given region.
     * @param yValueInterpolator Interpolation function taking a region and the x-distance into that region and
     *                            returning the interpolated y value at that distance
     * @param xValue The value to calculate the y result
     * @return the value of the y-variable at the given x value
     *
     * @throws IllegalStateException if the target x value is not reached with the list of segments provided.
     */
    public static <T> double getValueAt(List<T> segments, ToDoubleFunction<T> segmentToXExtent, BiFunction<T, Double, Double> yValueInterpolator, double xValue) {
        double xValueAtBoundary = 0;
        for (T segment : segments) {
            double xExtent = segmentToXExtent.applyAsDouble(segment);
            double xDistanceIntoSegment = Math.max(xValue - xValueAtBoundary, 0); //protect against a rounding error
            if (xExtent >= xDistanceIntoSegment) {
                return yValueInterpolator.apply(segment, xDistanceIntoSegment);
            }

            xValueAtBoundary += xExtent;
        }

        if (xValue - xValueAtBoundary < xValue * ERROR_TOLERANCE) {
            T lastRegion = Iterables.getLast(segments, null);
            return yValueInterpolator.apply(lastRegion, segmentToXExtent.applyAsDouble(lastRegion));
        }

        throw Failer.fail("Should have found an answer in loop");
    }

    /**
     * Utility to calculate the interpolated 'y' value on an x/y graph with multiple segments with cumulative x and y
     * values (eg time at a given distance or distance at a given time)
     *
     * @param segments The ordered list of segments defining the graph
     * @param segmentToXExtent Function giving the range of x values covered by a given region.
     * @param segmentToYExtent Function giving the range of y values covered by a given region.
     * @param yValueInterpolator Interpolation function taking a region and the x-distance into that region and
     *                            returning the interpolated y value at that distance
     * @param xValue The value to calculate the y result
     * @return the accumulated y-extent of all segments below the chosen x value plus the interpolated y-extent up to the
     *          target x-value of the region around the target.
     *
     * @throws IllegalStateException if the target x value is not reached with the list of segments provided.
     */
    public static <T> double accumulateValueTo(List<T> segments, ToDoubleFunction<T> segmentToXExtent, ToDoubleFunction<T> segmentToYExtent, BiFunction<T, Double, Double> yValueInterpolator, double xValue) {
        double xValueAtBoundary = 0;
        double yValueAtBoundary = 0;

        for (T segment : segments) {
            double xExtent = segmentToXExtent.applyAsDouble(segment);
            double xDistanceIntoSegment = Math.max(xValue - xValueAtBoundary, 0); //protect against a rounding error
            if (xExtent >= xDistanceIntoSegment) {
                return yValueAtBoundary + yValueInterpolator.apply(segment, xDistanceIntoSegment);
            }

            xValueAtBoundary += xExtent;
            yValueAtBoundary += segmentToYExtent.applyAsDouble(segment);
        }

        if (xValue - xValueAtBoundary < xValue * ERROR_TOLERANCE) {
            return yValueAtBoundary;
        }

        throw Failer.fail("Should have found an answer in loop");
    }
}
