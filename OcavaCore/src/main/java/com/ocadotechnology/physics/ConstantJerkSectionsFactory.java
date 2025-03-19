/*
 * Copyright © 2017-2025 Ocado (Ocava)
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.math3.complex.Complex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.ocadotechnology.maths.PolynomialRootUtils;
import com.ocadotechnology.maths.QuadraticRootFinder;
import com.ocadotechnology.maths.QuarticRootFinder;
import com.ocadotechnology.validation.Failer;

/**
 * See the README.md for derivations
 * for the equations used by this class
 */
class ConstantJerkSectionsFactory {
    private static final double EPSILON = Math.pow(10, -9);

    /**
     * Don't instantiate this static utility class
     */
    private ConstantJerkSectionsFactory() {
    }

    static Optional<ImmutableList<TraversalSection>> neitherMaxAccelerationNorMaxDecelerationNorMaxSpeedReached(double totalDistance, VehicleMotionProperties vehicleProperties) {

        double j1 = vehicleProperties.jerkAccelerationUp;
        double j2 = vehicleProperties.jerkAccelerationDown;
        double j3 = vehicleProperties.jerkDecelerationUp;
        double j4 = vehicleProperties.jerkDecelerationDown;

        double a = 1 / j1 - 1 / j2;
        double b = 1 / j4 - 1 / j3;
        double c = 1 / (6 * Math.pow(j1, 2)) - 1 / (2 * j1 * j2) + 1 / (3 * Math.pow(j2, 2));
        double d = -1 / (6 * Math.pow(j4, 2)) + 1 / (2 * j3 * j4) - 1 / (3 * Math.pow(j3, 2));

        double acceleration = Math.cbrt(totalDistance / (c + d * Math.pow(-Math.sqrt(a / b), 3)));
        double deceleration = Math.cbrt(totalDistance / (d + c * Math.pow(-Math.sqrt(b / a), 3)));
        double maxSpeedReached = Math.pow(acceleration, 2) * a / 2;
        if (acceleration > vehicleProperties.acceleration || deceleration < vehicleProperties.deceleration || maxSpeedReached >= vehicleProperties.maxSpeed) {
            return Optional.empty();
        }

        ConstantJerkTraversalSection section1 = ConstantJerkSectionFactory.jerkAccelerationUp(acceleration, j1);
        ConstantJerkTraversalSection section2 = ConstantJerkSectionFactory.jerkAccelerationDown(acceleration, section1.finalSpeed, j2);
        ConstantJerkTraversalSection section3 = ConstantJerkSectionFactory.jerkDecelerationUp(0, section2.finalSpeed, deceleration, j3).get();
        ConstantJerkTraversalSection section4 = ConstantJerkSectionFactory.jerkDecelerationDown(deceleration, j4);

        return Optional.of(ImmutableList.of(section1, section2, section3, section4));
    }

    /**
     * Handles 4 different acceleration profiles, where we hit the max acceleration or not, and where we hit max
     * deceleration or not. Usually returns 5, 6 or 7 sections but can return 4 sections in the edge case where we
     * reach max speed but need to start decelerating immediately and thus have no constant speed section.
     */
    static Optional<ImmutableList<TraversalSection>> maxSpeedReached(double distance, VehicleMotionProperties vehicleProperties) {
        double j1 = vehicleProperties.jerkAccelerationUp;
        double j2 = vehicleProperties.jerkAccelerationDown;
        double j3 = vehicleProperties.jerkDecelerationUp;
        double j4 = vehicleProperties.jerkDecelerationDown;
        double maxSpeed = vehicleProperties.maxSpeed;
        double maxAcc = vehicleProperties.acceleration;
        double maxDec = vehicleProperties.deceleration;
        List<TraversalSection> acceleratingSections = new ArrayList<>();
        List<TraversalSection> deceleratingSections = new ArrayList<>();

        // Start with accelerating part.
        // Check if accelerating like /\ is possible, i.e. we don't reach max acc
        double denom = 1 / j1 - 1 / j2;
        double a = Math.sqrt(2 * maxSpeed / denom);
        if (a <= maxAcc) {
            // Success, we can have a /\
            acceleratingSections.add(ConstantJerkSectionFactory.jerkAccelerationUp(a, j1));
            acceleratingSections.add(ConstantJerkSectionFactory.jerkAccelerationDownToV(a, maxSpeed, j2));
        } else {
            // We will exceed the max acc so we need to accelerate like /-\ instead
            ConstantJerkTraversalSection jerkAccUpSection = ConstantJerkSectionFactory.jerkAccelerationUp(maxAcc, j1);
            double speedReachedByMaxAccSection = maxSpeed + (1/2d * Math.pow(maxAcc, 2) / j2);
            ConstantAccelerationTraversalSection maxAccSection = ConstantJerkSectionFactory.constantAcceleration(jerkAccUpSection.finalSpeed, speedReachedByMaxAccSection, maxAcc);
            ConstantJerkTraversalSection jerkAccDownSection = ConstantJerkSectionFactory.jerkAccelerationDownToV(maxAcc, maxSpeed, j2);
            acceleratingSections.addAll(List.of(jerkAccUpSection, maxAccSection, jerkAccDownSection));
        }

        denom = 1 / j4 - 1 / j3;
        double d = -Math.sqrt(2 * maxSpeed / denom);
        if (d >= maxDec) {
            // We can have a \/
            deceleratingSections.add(ConstantJerkSectionFactory.jerkDecelerationUp(0, maxSpeed, d, j3).get());
            deceleratingSections.add(ConstantJerkSectionFactory.jerkDecelerationDown(d, j4));
        } else {
            // We need \_/
            ConstantJerkTraversalSection jerkDecUpSection = ConstantJerkSectionFactory.jerkDecelerationUp(0, maxSpeed, maxDec, j3).get();
            double speedReachedByMaxDecSection = 1/2d * Math.pow(maxDec, 2) / j4;
            ConstantAccelerationTraversalSection maxDecSection = ConstantJerkSectionFactory.constantAcceleration(jerkDecUpSection.finalSpeed, speedReachedByMaxDecSection, maxDec);
            ConstantJerkTraversalSection jerkDecDownSection = ConstantJerkSectionFactory.jerkDecelerationDown(maxDec, j4);
            deceleratingSections.addAll(List.of(jerkDecUpSection, maxDecSection, jerkDecDownSection));
        }

        double distanceTraveledDuringAcceleratingSections = acceleratingSections.stream()
                .map(TraversalSection::getTotalDistance)
                .reduce(0d, Double::sum);
        double distanceTraveledDuringDeceleratingSections = deceleratingSections.stream()
                .map(TraversalSection::getTotalDistance)
                .reduce(0d, Double::sum);
        double distanceToTravelAtMaxSpeed = distance - distanceTraveledDuringAcceleratingSections - distanceTraveledDuringDeceleratingSections;

        Builder<TraversalSection> traversalSectionBuilder = new Builder<TraversalSection>()
                .addAll(acceleratingSections);

        if (distanceToTravelAtMaxSpeed <= -EPSILON) {
            return Optional.empty();
        }
        // in the edge case where the constant speed section would be infinitesimally small, we don't add it.
        if (distanceToTravelAtMaxSpeed >= EPSILON) {
            traversalSectionBuilder.add(ConstantJerkSectionFactory.constantSpeed(distanceToTravelAtMaxSpeed, maxSpeed));
        }

        return Optional.of(traversalSectionBuilder
                .addAll(deceleratingSections)
                .build());
    }

    static ImmutableList<TraversalSection> maxAccelerationAndMaxDecelerationReachedButMaxSpeedNotReached(double targetDistance, VehicleMotionProperties vehicleProperties) {

        double j1 = vehicleProperties.jerkAccelerationUp;
        double j2 = vehicleProperties.jerkAccelerationDown;
        double j3 = vehicleProperties.jerkDecelerationUp;
        double j4 = vehicleProperties.jerkDecelerationDown;

        double a = (1 / 2d) * (1 / vehicleProperties.acceleration - 1 / vehicleProperties.deceleration);
        double b = (1 / 2d) * (vehicleProperties.deceleration / j3 - vehicleProperties.acceleration / j2);

        double v1 = (1 / 2d) * Math.pow(vehicleProperties.acceleration, 2) / j1;
        double v5 = (1 / 2d) * Math.pow(vehicleProperties.deceleration, 2) / j4;

        double c1 = Math.pow(vehicleProperties.acceleration, 3) / (6 * Math.pow(j1, 2)) - Math.pow(v1, 2) / (2 * vehicleProperties.acceleration)
                + Math.pow(vehicleProperties.acceleration, 3) / (3 * Math.pow(j2, 2));

        double c2 = -Math.pow(vehicleProperties.deceleration, 3) / (6 * Math.pow(j4, 2)) + Math.pow(v5, 2) / (2 * vehicleProperties.deceleration)
                - Math.pow(vehicleProperties.deceleration, 3) / (3 * Math.pow(j3, 2));

        double cprime = c1 + c2;

        double c3 = Math.pow(vehicleProperties.acceleration, 3) / (8 * Math.pow(j2, 2)) - Math.pow(vehicleProperties.deceleration, 3) / (8 * Math.pow(j3, 2)) -
                (1 / 2d) * Math.pow(vehicleProperties.acceleration, 3) / Math.pow(j2, 2) + (1 / 2d) * Math.pow(vehicleProperties.deceleration, 3) / Math.pow(j3, 2);

        double c = cprime + c3 - targetDistance;

        ImmutableList<Complex> roots = QuadraticRootFinder.find(a, b, c);

        double v3 = PolynomialRootUtils.getMinimumPositiveRealRoot(roots);
        double v2 = v3 + ((1 / 2d) * Math.pow(vehicleProperties.acceleration, 2)) / j2;
        double v4 = v3 + ((1 / 2d) * Math.pow(vehicleProperties.deceleration, 2)) / j3;

        if (v3 > vehicleProperties.maxSpeed + EPSILON ||
                v1 >= v2 + EPSILON ||
                v5 >= v4 + EPSILON) {
            throw new TraversalCalculationException("Should never reach this point.");
        }

        Builder<TraversalSection> listBuilder = ImmutableList.builder();

        ConstantJerkTraversalSection accUpSection = ConstantJerkSectionFactory.jerkAccelerationUp(vehicleProperties.acceleration, vehicleProperties.jerkAccelerationUp);
        listBuilder.add(accUpSection);

        // No need to add the const acc section if it would be infinitesmally small.
        if (Math.abs(v2 - accUpSection.finalSpeed) > EPSILON) {
            TraversalSection constAccSection = ConstantJerkSectionFactory.constantAcceleration(accUpSection.finalSpeed, v2, vehicleProperties.acceleration);
            listBuilder.add(constAccSection);
        }
        ConstantJerkTraversalSection accDownSection = ConstantJerkSectionFactory.jerkAccelerationDownToV(vehicleProperties.acceleration, v3, vehicleProperties.jerkAccelerationDown);
        ConstantJerkTraversalSection decUpSection = ConstantJerkSectionFactory.jerkDecelerationUp(0, v3, vehicleProperties.deceleration, vehicleProperties.jerkDecelerationUp).get();
        listBuilder.add(accDownSection, decUpSection);

        // No need to add the const acc section if it would be infinitesmally small.
        if (Math.abs(decUpSection.finalSpeed - v5) > EPSILON) {
            TraversalSection constDecSection = ConstantJerkSectionFactory.constantAcceleration(v4, v5, vehicleProperties.deceleration);
            listBuilder.add(constDecSection);
        }

        ConstantJerkTraversalSection decDownSection = ConstantJerkSectionFactory.jerkDecelerationDown(vehicleProperties.deceleration, vehicleProperties.jerkDecelerationDown);
        listBuilder.add(decDownSection);

        return listBuilder.build();
    }

    static Optional<ImmutableList<TraversalSection>> oneMaxAccelReachedButNotOtherAndMaxSpeedNotReached(double totalDistance, VehicleMotionProperties vehicleProperties) {
        double j1 = vehicleProperties.jerkAccelerationUp;
        double j2 = vehicleProperties.jerkAccelerationDown;
        double j3 = vehicleProperties.jerkDecelerationUp;
        double j4 = vehicleProperties.jerkDecelerationDown;

        if (willHitMaxAccelFirst(vehicleProperties)) {
            if (willExceedMaxSpeedTryingToReachMaxAccel(vehicleProperties)) {
                return Optional.empty();
            }
            double a = vehicleProperties.acceleration;

            //TODO: strip out commonality between this ABCDE and the below
            double A = (1 / (4 * (-a))) * (1 / (j4 * j3) - 1 / (2 * Math.pow(j4, 2)) - 1 / (2 * Math.pow(j3, 2)));

            double B = -(1 / (3 * Math.pow(j3, 2)) - 1 / (2 * j4 * j3) + 1 / (6 * Math.pow(j4, 2)));

            double C = ((-a) / 4d) * (1 / (j3 * j2) - 1 / (j4 * j2)) + (-a) / (2 * j2) * (1 / j4 - 1 / j3);

            double D = 0;

            double E = (Math.pow((-a), 3) / 8d) * (1 / Math.pow(j1, 2) - 1 / Math.pow(j2, 2)) - Math.pow((-a), 3) / (6d * Math.pow(j1, 2)) + Math.pow((-a), 3) / (6 * Math.pow(j2, 2)) - totalDistance;

            ImmutableList<Complex> roots = QuarticRootFinder.find(A, B, C, D, E);

            double d = PolynomialRootUtils.getMaximumNegativeRealRoot(roots);

            ConstantJerkTraversalSection jerkAccelUp = ConstantJerkSectionFactory.jerkAccelerationUp(a, j1);
            ConstantJerkTraversalSection jerkDecelDown = ConstantJerkSectionFactory.jerkDecelerationDown(d, j4);
            Optional<ConstantJerkTraversalSection> maybeJerkDecelUp = ConstantJerkSectionFactory.jerkDecelerationUpToV(0, jerkDecelDown.initialSpeed, d, j3);
            if (maybeJerkDecelUp.isEmpty() ||
                    d < vehicleProperties.deceleration) {
                return Optional.empty();
            }
            ConstantJerkTraversalSection jerkDecelUp = maybeJerkDecelUp.get();
            ConstantJerkTraversalSection jerkAccelDown = ConstantJerkSectionFactory.jerkAccelerationDownToV(a, jerkDecelUp.initialSpeed, j2);
            double maxSpeedReached = jerkAccelDown.finalSpeed;
            if (maxSpeedReached > vehicleProperties.maxSpeed) {
                return Optional.empty();
            }
            TraversalSection accelerate = ConstantJerkSectionFactory.constantAcceleration(jerkAccelUp.finalSpeed, jerkAccelDown.initialSpeed, a);
            return Optional.of(ImmutableList.of(jerkAccelUp, accelerate, jerkAccelDown, jerkDecelUp, jerkDecelDown));
        }

        double d = vehicleProperties.deceleration;

        double A = (1 / (4 * d)) * (1 / (j1 * j2) - 1 / (2 * Math.pow(j1, 2)) - 1 / (2 * Math.pow(j2, 2)));

        double B = 1 / (3 * Math.pow(j2, 2)) - 1 / (2 * j1 * j2) + 1 / (6 * Math.pow(j1, 2));

        double C = (d / 4d) * (1 / (j2 * j3) - 1 / (j1 * j3)) + d / (2 * j3) * (1 / j1 - 1 / j2);

        double D = 0;

        double E = (Math.pow(d, 3) / 8d) * (1 / Math.pow(j4, 2) - 1 / Math.pow(j3, 2)) - Math.pow(d, 3) / (6d * Math.pow(j4, 2)) + Math.pow(d, 3) / (6 * Math.pow(j3, 2)) - totalDistance;

        ImmutableList<Complex> roots = QuarticRootFinder.find(A, B, C, D, E);

        double a = PolynomialRootUtils.getMinimumPositiveRealRoot(roots);
        ConstantJerkTraversalSection jerkDecelDown = ConstantJerkSectionFactory.jerkDecelerationDown(d, j4);
        ConstantJerkTraversalSection jerkAccelUp = ConstantJerkSectionFactory.jerkAccelerationUp(a, j1);
        ConstantJerkTraversalSection jerkAccelDown = ConstantJerkSectionFactory.jerkAccelerationDown(a, jerkAccelUp.finalSpeed, j2);
        Optional<ConstantJerkTraversalSection> maybeJerkDecelUp = ConstantJerkSectionFactory.jerkDecelerationUp(0, jerkAccelDown.finalSpeed, d, j3);
        if (maybeJerkDecelUp.isEmpty() ||
                jerkAccelDown.finalSpeed > vehicleProperties.maxSpeed ||
                willStopMovingWhileTryingToReachMaxDecel(vehicleProperties, jerkAccelDown.finalSpeed) ||
                a > vehicleProperties.acceleration) {
            return Optional.empty();
        }
        ConstantJerkTraversalSection jerkDecelUp = maybeJerkDecelUp.get();
        TraversalSection decelerate = ConstantJerkSectionFactory.constantAcceleration(jerkDecelUp.finalSpeed, jerkDecelDown.initialSpeed, d);
        return Optional.of(ImmutableList.of(jerkAccelUp, jerkAccelDown, jerkDecelUp, decelerate, jerkDecelDown));
    }

    private static boolean willExceedMaxSpeedTryingToReachMaxAccel(VehicleMotionProperties vehicleProperties) {
        double j1 = vehicleProperties.jerkAccelerationUp;
        double j2 = vehicleProperties.jerkAccelerationDown;
        double maxSpeed = vehicleProperties.maxSpeed;
        double maxAcc = vehicleProperties.acceleration;

        double minSpeedReached = 0.5 * Math.pow(maxAcc, 2) * (1 / j1 - 1 / j2);
        return minSpeedReached > maxSpeed;
    }

    private static boolean willStopMovingWhileTryingToReachMaxDecel(VehicleMotionProperties vehicleProperties, double initialSpeed) {
        double j3 = vehicleProperties.jerkDecelerationUp;
        double j4 = vehicleProperties.jerkDecelerationDown;
        double maxDec = vehicleProperties.deceleration;

        double maxSpeedReached = initialSpeed + (0.5 * Math.pow(maxDec, 2) * (1 / j3 - 1 / j4));
        return maxSpeedReached < 0;
    }

    /**
     * TODO: cache this on vehicle properties once instead
     */
    private static boolean willHitMaxAccelFirst(VehicleMotionProperties vehicleProperties) {
        ConstantJerkTraversalSection section1 = ConstantJerkSectionFactory.jerkAccelerationUp(vehicleProperties.acceleration, vehicleProperties.jerkAccelerationUp);
        ConstantJerkTraversalSection section2 = ConstantJerkSectionFactory.jerkAccelerationDown(vehicleProperties.acceleration, section1.finalSpeed, vehicleProperties.jerkAccelerationDown);

        ConstantJerkTraversalSection section4 = ConstantJerkSectionFactory.jerkDecelerationDown(vehicleProperties.deceleration, vehicleProperties.jerkDecelerationDown);
        ConstantJerkTraversalSection section3 = ConstantJerkSectionFactory.jerkDecelerationUpToV(0, section4.initialSpeed, vehicleProperties.deceleration, vehicleProperties.jerkDecelerationUp).orElseThrow(Failer::valueExpected);

        return section2.finalSpeed < section3.initialSpeed;
    }

    static ImmutableList<TraversalSection> findBrakingTraversal(double initialSpeed, double initialAcceleration, VehicleMotionProperties vehicleProperties) {
        Builder<TraversalSection> sections = ImmutableList.builder();

        double v3;
        double accelerationAtV3;
        if (initialAcceleration > 0) {
            accelerationAtV3 = 0;

            ConstantJerkTraversalSection jerkAccelerationDownSection = ConstantJerkSectionFactory.jerkAccelerationDown(
                    initialAcceleration, initialSpeed, vehicleProperties.jerkAccelerationDown);

            if (jerkAccelerationDownSection.finalSpeed > vehicleProperties.maxSpeed) {
                jerkAccelerationDownSection = ConstantJerkSectionFactory.jerkAccelerationFromUToV(
                        initialAcceleration, initialSpeed, vehicleProperties.maxSpeed, vehicleProperties.jerkAccelerationDown);
            }
            v3 = jerkAccelerationDownSection.finalSpeed;
            sections.add(jerkAccelerationDownSection);
        } else {
            v3 = initialSpeed;
            accelerationAtV3 = initialAcceleration;
        }

        return sections.addAll(findBrakingTraversalFromHighestSpeed(v3, accelerationAtV3, vehicleProperties)).build();
    }

    private static ImmutableList<TraversalSection> findBrakingTraversalFromHighestSpeed(double initialSpeed, double initialAcceleration, VehicleMotionProperties vehicleProperties) {
        // The following will be valid in the case that we are able to hit / are already at maximum deceleration
        Optional<ConstantJerkTraversalSection> optionalFirstSection;
        if (initialAcceleration < vehicleProperties.deceleration) {
            optionalFirstSection = ConstantJerkSectionFactory.jerkAccelerationDownToAcceleration(initialAcceleration, vehicleProperties.deceleration, initialSpeed, vehicleProperties.jerkDecelerationDown);
        } else {
            optionalFirstSection = ConstantJerkSectionFactory.jerkDecelerationUp(initialAcceleration, initialSpeed, vehicleProperties.deceleration, vehicleProperties.jerkDecelerationUp);
        }

        if (optionalFirstSection.isPresent()) {
            ConstantJerkTraversalSection firstSection = optionalFirstSection.get();
            ConstantJerkTraversalSection jerkDecelDownFromMaxDecel = ConstantJerkSectionFactory.jerkDecelerationDown(vehicleProperties.deceleration, vehicleProperties.jerkDecelerationDown);

            double changeInVDuringJerkDecelUp = firstSection.initialSpeed - firstSection.finalSpeed;
            double changeInVDuringJerkDecelDown = jerkDecelDownFromMaxDecel.initialSpeed - jerkDecelDownFromMaxDecel.finalSpeed;
            if (changeInVDuringJerkDecelUp + changeInVDuringJerkDecelDown <= initialSpeed) {
                TraversalSection decelerationSection = ConstantJerkSectionFactory.constantAcceleration(firstSection.finalSpeed, jerkDecelDownFromMaxDecel.initialSpeed, vehicleProperties.deceleration);
                return ImmutableList.of(firstSection, decelerationSection, jerkDecelDownFromMaxDecel);
            }
        }

        return findBrakingTraversalFromHighestSpeedWithNoConstantDecelerationSection(initialSpeed, initialAcceleration, vehicleProperties);
    }

    private static ImmutableList<TraversalSection> findBrakingTraversalFromHighestSpeedWithNoConstantDecelerationSection(
            double initialSpeed, double initialAcceleration, VehicleMotionProperties vehicleProperties) {

        if (initialSpeed == 0d && initialAcceleration == 0d) {
            return ImmutableList.of();
        }

        // Constraints:
        // v1 = u2
        // a1 = a.2
        // a2 = 0
        // v2 = 0

        double j1 = vehicleProperties.jerkDecelerationUp;
        double j2 = vehicleProperties.jerkDecelerationDown;

        double a = (1 / 2d) * (1 / j1 - 1 / j2);
        double c = initialSpeed - (Math.pow(initialAcceleration, 2)) / (2 * j1);

        double acceleration = -Math.sqrt(-c / a);

        ConstantJerkTraversalSection jerkDecelerationDown = ConstantJerkSectionFactory.jerkDecelerationDown(acceleration, vehicleProperties.jerkDecelerationDown);

        if (jerkDecelerationDown.initialSpeed > initialSpeed + EPSILON) {
            return ImmutableList.of(ConstantJerkSectionFactory.jerkDecelerationDownToZeroV(initialSpeed, initialAcceleration, vehicleProperties.jerkDecelerationDown));
        }

        ConstantJerkTraversalSection jerkDecelerationUpSection = ConstantJerkSectionFactory.jerkDecelerationUp(initialAcceleration, initialSpeed, acceleration, vehicleProperties.jerkDecelerationUp).orElseThrow(Failer::valueExpected);
        if (jerkDecelerationUpSection.duration > 0 + EPSILON) {
            return ImmutableList.of(jerkDecelerationUpSection, jerkDecelerationDown);
        }

        return ImmutableList.of(jerkDecelerationDown);
    }

    static ImmutableList<TraversalSection> calculateTraversalGivenFixedJerkUpTimeAssumingNeitherConstantAccelerationOrSpeed(double timeToJerkUp, double initialSpeed, double initialAcceleration, VehicleMotionProperties vehicleProperties) {
        Builder<TraversalSection> sectionsBuilder = ImmutableList.builder();
        ConstantJerkTraversalSection sectionOne = ConstantJerkSectionFactory.jerkAccelerationUpFrom(initialAcceleration, initialSpeed, vehicleProperties.jerkAccelerationUp, timeToJerkUp);
        ImmutableList<TraversalSection> breakingTraversal = findBrakingTraversal(sectionOne.finalSpeed, sectionOne.finalAcceleration, vehicleProperties);
        return sectionsBuilder.add(sectionOne).addAll(breakingTraversal).build();
    }

    public static ImmutableList<TraversalSection> calculateTraversalGivenFixedMaximumAccelerationTimeAssumingNoMaximumSpeedSection(double t, double u, double a, VehicleMotionProperties vehicleProperties) {
        Builder<TraversalSection> sectionsBuilder = ImmutableList.builder();

        TraversalSection constantAcceleration;
        double finalSpeed;
        if (a < vehicleProperties.acceleration) {
            ConstantJerkTraversalSection jerkUpSection = ConstantJerkSectionFactory.jerkAccelerationUpFromTo(a, u, vehicleProperties.jerkAccelerationUp, vehicleProperties.acceleration);
            sectionsBuilder.add(jerkUpSection);

            finalSpeed = jerkUpSection.finalSpeed + vehicleProperties.acceleration * t;

            constantAcceleration = ConstantJerkSectionFactory.constantAcceleration(jerkUpSection.finalSpeed, finalSpeed, vehicleProperties.acceleration);
            sectionsBuilder.add(constantAcceleration);
        } else {
            finalSpeed = u + vehicleProperties.acceleration * t;
            constantAcceleration = ConstantJerkSectionFactory.constantAcceleration(u, finalSpeed, vehicleProperties.acceleration);
            sectionsBuilder.add(constantAcceleration);
        }

        ImmutableList<TraversalSection> breakingTraversal = findBrakingTraversal(finalSpeed, vehicleProperties.acceleration, vehicleProperties);
        sectionsBuilder.addAll(breakingTraversal);

        return sectionsBuilder.build();
    }

    static ImmutableList<TraversalSection> jerkUpToAmaxConstrainedByVmaxThenBrake(
            double initialSpeed, double initialAcceleration, VehicleMotionProperties vehicleProperties) {

        if (initialSpeed >= vehicleProperties.maxSpeed || initialAcceleration > vehicleProperties.acceleration) {
            return findBrakingTraversal(initialSpeed, initialAcceleration, vehicleProperties);
        }

        // Try getting to/from Amax
        ConstantJerkTraversalSection jerkAccelerationUp = ConstantJerkSectionFactory.jerkAccelerationUp(
                initialSpeed, initialAcceleration, vehicleProperties.acceleration, vehicleProperties.jerkAccelerationUp);

        ConstantJerkTraversalSection jerkAccelerationDown = ConstantJerkSectionFactory.jerkAccelerationDown(
                vehicleProperties.acceleration, jerkAccelerationUp.finalSpeed, vehicleProperties.jerkAccelerationDown);

        Builder<TraversalSection> builder = ImmutableList.builder();
        if (jerkAccelerationDown.finalSpeed > vehicleProperties.maxSpeed) {
            // We can't jerk to amax because of vmax. Figure out the highest a we can jerk to and do that instead.

            double j1 = vehicleProperties.jerkAccelerationUp;
            double j2 = vehicleProperties.jerkAccelerationDown;

            //TODO: strip out commonality between this and another place in this file that does the same thing (it's got doube a and double c)
            double a = (1 / 2d) * (1 / j1 - 1 / j2);
            double c = initialSpeed - (Math.pow(initialAcceleration, 2)) / (2 * j1) - vehicleProperties.maxSpeed;

            double a1 = Math.sqrt(-c / a);

            if (a1 < initialAcceleration) {
                return findBrakingTraversal(initialSpeed, initialAcceleration, vehicleProperties);
            }

            jerkAccelerationUp = ConstantJerkSectionFactory.jerkAccelerationUp(
                    initialSpeed, initialAcceleration, a1, vehicleProperties.jerkAccelerationUp);

            jerkAccelerationDown = ConstantJerkSectionFactory.jerkAccelerationDown(
                    a1, jerkAccelerationUp.finalSpeed, vehicleProperties.jerkAccelerationDown);
        }

        builder.add(jerkAccelerationUp);
        builder.add(jerkAccelerationDown);

        return builder.addAll(findBrakingTraversalFromHighestSpeed(jerkAccelerationDown.finalSpeed, 0, vehicleProperties)).build();
    }

    static ImmutableList<TraversalSection> getToVmaxThenBrake(
            double initialSpeed, double initialAcceleration, VehicleMotionProperties vehicleProperties) {

        ImmutableList<TraversalSection> sections = jerkUpToAmaxConstrainedByVmaxThenBrake(initialSpeed, initialAcceleration, vehicleProperties);
        if (sections.stream().anyMatch(s -> s.getSpeedAtTime(0) + EPSILON >= vehicleProperties.maxSpeed)) {
            return sections;
        }

        Builder<TraversalSection> builder = ImmutableList.builder();

        double startOfConstantAccelerationSpeed = initialSpeed;

        ConstantJerkTraversalSection jerkAccelerationDown = ConstantJerkSectionFactory.jerkAccelerationDownToV(
                vehicleProperties.acceleration, vehicleProperties.maxSpeed, vehicleProperties.jerkAccelerationDown);

        if (initialAcceleration < vehicleProperties.acceleration) {
            ConstantJerkTraversalSection jerkAccelerationUp = ConstantJerkSectionFactory.jerkAccelerationUp(
                    initialSpeed, initialAcceleration, vehicleProperties.acceleration, vehicleProperties.jerkAccelerationUp);

            startOfConstantAccelerationSpeed = jerkAccelerationUp.finalSpeed;

            if (startOfConstantAccelerationSpeed > jerkAccelerationDown.initialSpeed) {
                // Undershoot case
                jerkAccelerationDown = ConstantJerkSectionFactory.jerkAccelerationDownToV(
                        initialAcceleration, vehicleProperties.maxSpeed, vehicleProperties.jerkAccelerationDown);

                TraversalSection constantAcceleration = ConstantJerkSectionFactory.constantAcceleration(
                        initialSpeed, jerkAccelerationDown.initialSpeed, initialAcceleration);

                builder.add(constantAcceleration);
                builder.add(jerkAccelerationDown);

                builder.addAll(findBrakingTraversalFromHighestSpeed(vehicleProperties.maxSpeed, 0, vehicleProperties));

                return builder.build();
            }

            builder.add(jerkAccelerationUp);
        }

        if (jerkAccelerationDown.initialSpeed < initialSpeed) {
            return findBrakingTraversal(initialSpeed, initialAcceleration, vehicleProperties);
        }

        TraversalSection constantAcceleration = ConstantJerkSectionFactory.constantAcceleration(
                startOfConstantAccelerationSpeed, jerkAccelerationDown.initialSpeed, vehicleProperties.acceleration);

        builder.add(constantAcceleration);
        builder.add(jerkAccelerationDown);

        builder.addAll(findBrakingTraversalFromHighestSpeed(vehicleProperties.maxSpeed, 0, vehicleProperties));

        return builder.build();
    }

    /**
     * TODO: Use this for the 0 start case too
     * FIXME: What if we are going to overshoot vmax? Do? Cut a to 0 at vmax? Use some other j that reaches vmax?
     * FIXME: What if we are going to undershoot vmax? Do? if so, brake short of distance, or go at constant (sub-max) V? Stay at current a some more? Jerk up more?
     */
    static ImmutableList<TraversalSection> getToVmaxAndStayAtVMaxToReachDistance(
            double targetDistance, double initialSpeed, double initialAcceleration, VehicleMotionProperties vehicleProperties) {

        ImmutableList<TraversalSection> sections = findBrakingTraversal(initialSpeed, initialAcceleration, vehicleProperties);
        if (sections.stream().noneMatch(s -> s.getSpeedAtTime(0) >= vehicleProperties.maxSpeed)) {
            sections = getToVmaxThenBrake(initialSpeed, initialAcceleration, vehicleProperties);
        }

        Traversal traversal = new Traversal(sections);

        TraversalSection maxV = ConstantJerkSectionFactory.constantSpeed(
                targetDistance - traversal.getTotalDistance(), vehicleProperties.maxSpeed);

        Builder<TraversalSection> newSections = ImmutableList.builder();

        boolean first = true;
        for (TraversalSection section : sections) {
            if (section.getSpeedAtTime(0) == vehicleProperties.maxSpeed && first) {
                newSections.add(maxV);
                first = false;
            }

            newSections.add(section);
        }

        return newSections.build();
    }

    public static ImmutableList<TraversalSection> calculateTraversalGivenFixedConstantAccelerationTimeAssumingNoMaximumSpeedSection(
            double t, double u, double a, VehicleMotionProperties vehicleProperties) {
        Builder<TraversalSection> sectionsBuilder = ImmutableList.builder();

        double finalSpeed = u + a * t;
        TraversalSection constantAcceleration = ConstantJerkSectionFactory.constantAcceleration(u, finalSpeed, a);
        sectionsBuilder.add(constantAcceleration);

        ImmutableList<TraversalSection> breakingTraversal = findBrakingTraversal(finalSpeed, a, vehicleProperties);
        sectionsBuilder.addAll(breakingTraversal);

        return sectionsBuilder.build();
    }
}
