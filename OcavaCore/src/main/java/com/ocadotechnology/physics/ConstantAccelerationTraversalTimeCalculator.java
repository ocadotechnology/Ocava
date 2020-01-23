/*
 * Copyright © 2017-2020 Ocado (Ocava)
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
package com.ocadotechnology.physics;

import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

public class ConstantAccelerationTraversalTimeCalculator {
    public static final double ROUNDING_ERROR_MARGIN = 1E-9;

    /**
     * Don't instantiate this static utility class
     */
    private ConstantAccelerationTraversalTimeCalculator() {
    }

    /**
     * @throws TraversalCalculationException
     */
    static ImmutableList<TraversalSection> calcTraversalTime(double length, double initialSpeed, double finalSpeed, VehicleMotionProperties vehicleProperties) {
        return calcTraversalTime(length, initialSpeed, finalSpeed, vehicleProperties.acceleration,
                vehicleProperties.deceleration, vehicleProperties.maxSpeed);
    }

    /**
     * @throws TraversalCalculationException in the following cases: negative distance, negative initialSpeed, negative maxSpeed.
     */
    public static ImmutableList<TraversalSection> calcAccelerationTraversal(double distance, double initialSpeed, double acceleration, double maxSpeed) {
        if (initialSpeed < 0) {
            throw new TraversalCalculationException("Invalid initial speed " + initialSpeed);
        }
        if (maxSpeed < 0) {
            throw new TraversalCalculationException("Invalid max speed " + maxSpeed);
        }
        if (distance < 0) {
            throw new TraversalCalculationException("Invalid distance " + distance);
        }

        /*
         * There are potentially 2 parts to a journey along a Leg: acceleration,
         * then travel at max speed. One might not reach max speed before reaching
         * the target distance
         *
         * First, check whether max speed is reachable: v^2 = u^2 + 2 * a * d
         *
         * d1 = v1^2 - u1^2 / (2 * a1) distance required to reach max speed
         */
        double d1 = (Math.pow(maxSpeed, 2) - Math.pow(initialSpeed, 2)) / (2 * acceleration);

        /*
         * Max speed cannot be reached in given distance, so traversal is just one part.
         */
        if (d1 >= distance) {
            d1 = distance;
            double t1 = (Math.sqrt(2 * acceleration * d1 + Math.pow(initialSpeed, 2)) - initialSpeed) / acceleration;
            double v1 = initialSpeed + acceleration * t1;
            return ImmutableList.of(new ConstantAccelerationTraversalSection(d1, acceleration, initialSpeed, v1, t1));
        }

        /*
         * Max speed can be reached, so calculate traversal in two parts
         */

        double d2 = distance - d1;
        double t1 = (maxSpeed - initialSpeed) / acceleration;
        double t2 = d2 / maxSpeed;

        return ImmutableList.of(
                new ConstantAccelerationTraversalSection(d1, acceleration, initialSpeed, maxSpeed, t1),
                new ConstantSpeedTraversalSection(d2, maxSpeed, t2));
    }

    /**
     * Calculates how long it will take to travel some distance, given the
     * specified motion parameters.
     */
    public static ImmutableList<TraversalSection> calcTraversalTime(double distance, double initialSpeed, double finalSpeed, double acceleration, double deceleration, double maxSpeed) {
        if (DoubleMath.fuzzyEquals(distance, 0, ROUNDING_ERROR_MARGIN)) {
            return ImmutableList.of();
        }

        // NOTE : Currently, finalSpeed == 0 whenever this method is called
        // thus the calculations could be simplified

        /*
         * There are potentially 3 parts to a journey along a Leg: acceleration,
         * travel at max speed, deceleration. One might not reach max speed
         * before needing to decelerate.
         *
         * First, check whether max speed is reachable: v^2 = u^2 + 2 * a * d
         *
         * d1 = v1^2 - u1^2 / (2 * a1) distance required to reach max speed
         * d3 = v3^2 - u3^2 / (2 * a3) distance required to stop from max speed
         */
        double d1 = (Math.pow(maxSpeed, 2) - Math.pow(initialSpeed, 2)) / (2 * acceleration);
        double d3 = (Math.pow(finalSpeed, 2) - Math.pow(maxSpeed, 2)) / (2 * deceleration);

        // if d1 + d3 <= distance, the vehicle can reach max speed, so calculate t
        if (d1 + d3 <= distance) {
            /*
             * First, calculate time accelerating and decelerating:
             * v = u + a * t
             * t1 = v1-u1/a1
             * t3 = v3-u3/a3
             */
            double t1 = (maxSpeed - initialSpeed) / acceleration;
            double t3 = (finalSpeed - maxSpeed) / deceleration;

            /*
             * Then calculate time at max speed:
             * d = d1 + d2 + d3
             * d2 = d - (d1 + d3)
             */
            // NOTE: using "distance - (d1 + d3)" can introduce rounding errors; we MUST have distance == (d1 + d2 + d3)
            double d2 = distance - d1 - d3;

            /*
             * d = u * t + 1/2 * a * t^2
             * t2 = d2 / u2
             */
            double t2 = d2 / maxSpeed;

            ImmutableList.Builder<TraversalSection> partListBuilder = ImmutableList.builder();

            if (!DoubleMath.fuzzyEquals(d1, 0, ROUNDING_ERROR_MARGIN)) {
                ConstantAccelerationTraversalSection accelerationPart = new ConstantAccelerationTraversalSection(d1, acceleration, initialSpeed, maxSpeed, t1);
                partListBuilder.add(accelerationPart);
            }

            if (!DoubleMath.fuzzyEquals(d2, 0, ROUNDING_ERROR_MARGIN)) {
                ConstantSpeedTraversalSection maxSpeedPart = new ConstantSpeedTraversalSection(d2, maxSpeed, t2);
                partListBuilder.add(maxSpeedPart);
            }

            if (!DoubleMath.fuzzyEquals(d3, 0, ROUNDING_ERROR_MARGIN)) {
                ConstantAccelerationTraversalSection decelerationPart = new ConstantAccelerationTraversalSection(d3, deceleration, maxSpeed, finalSpeed, t3);
                partListBuilder.add(decelerationPart);
            }

            return partListBuilder.build();
        }

        /*
         * Max speed not reachable. Calculate the time to traverse the Leg in
         * two parts; acceleration and deceleration:
         *
         * d = d1 + d2    total distance equals sum of distances for each leg
         * v1 = u2        initial speed for second leg = final speed for first leg
         *
         * v1^2 = u1^2 + 2 * a1 * d1
         * v2^2 = u2^2 + 2 * a2 * d2
         *
         * v2^2 = u2^2 + 2 * a2 * (d - d1)
         * v2^2 - (2 * a2 * (d - d1)) = u2^2
         * v2^2 - (2 * a2 * (d - d1)) = u1^2 + 2 * a1 * d1
         * d1 = (2*a2*d + u1^2 - v2^2)/(2*(a2-a1))
         */
        d1 = (2 * deceleration * distance + Math.pow(initialSpeed, 2) - Math.pow(finalSpeed, 2))
                / (2 * (deceleration - acceleration));

        double d2 = distance - d1;

        /*
         * Then, given d1, find t1
         * d = u * t + 1/2 * a * t^2
         * t1 = ((2 * a1 * d1 + u1^2)^0.5 - u1)/a1
         */
        double t1 = Math.max((Math.sqrt(2 * acceleration * d1 + Math.pow(initialSpeed, 2)) - initialSpeed), 0d) / acceleration;
        // (nb. The Math.max() call is to get around floating point rounding
        // errors which can return negative values for the above)

        /*
         * Then, find v1 to determine u2, to determine t2 v1 = u1 + a1 * t1
         */
        double v1 = initialSpeed + acceleration * t1;

        /*
         * u2 = v1
         * v2 = u2 + a2 * t2
         * t2 = v2-v1/a2
         */
        double t2 = (finalSpeed - v1) / deceleration;

        ImmutableList.Builder<TraversalSection> partListBuilder = ImmutableList.builder();

        if (!DoubleMath.fuzzyEquals(d1, 0, ROUNDING_ERROR_MARGIN)) {
            ConstantAccelerationTraversalSection accelerationPart = new ConstantAccelerationTraversalSection(d1, acceleration, initialSpeed, v1, t1);
            partListBuilder.add(accelerationPart);
        }

        if (!DoubleMath.fuzzyEquals(d2, 0, ROUNDING_ERROR_MARGIN)) {
            ConstantAccelerationTraversalSection decelerationPart = new ConstantAccelerationTraversalSection(d2, deceleration, v1, finalSpeed, t2);
            partListBuilder.add(decelerationPart);
        }

        return partListBuilder.build();
    }
}
