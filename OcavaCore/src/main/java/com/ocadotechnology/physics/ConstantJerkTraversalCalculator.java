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

import java.util.function.DoubleFunction;
import java.util.function.ToDoubleFunction;

import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;
import com.ocadotechnology.physics.utils.BinarySearch;

public class ConstantJerkTraversalCalculator implements TraversalCalculator {
    private static final double ROUNDING_ERROR_MARGIN = 1E-9;
    public static final ConstantJerkTraversalCalculator INSTANCE = new ConstantJerkTraversalCalculator();

    private ConstantJerkTraversalCalculator() {
    }

    public Traversal create(double distance, VehicleMotionProperties vehicleProperties) {
        if (DoubleMath.fuzzyEquals(distance, 0d, getDistanceToReachMaxSpeed(vehicleProperties) * ROUNDING_ERROR_MARGIN)) {
            return Traversal.EMPTY_TRAVERSAL;
        }

        ImmutableList<TraversalSection> sections =
                ConstantJerkSectionsFactory.neitherMaxAccelerationNorMaxDecelerationNorMaxSpeedReached(distance, vehicleProperties)
                        .or(() -> ConstantJerkSectionsFactory.maxSpeedReached(distance, vehicleProperties))
                        .or(() -> ConstantJerkSectionsFactory.oneMaxAccelReachedButNotOtherAndMaxSpeedNotReached(distance, vehicleProperties))
                        .orElseGet(() -> ConstantJerkSectionsFactory.maxAccelerationAndMaxDecelerationReachedButMaxSpeedNotReached(distance, vehicleProperties));

        return new Traversal(sections);
    }

    /**
     * @throws TraversalCalculationException
     */
    @Override
    public Traversal create(double distance, double initialSpeed, double initialAcceleration, VehicleMotionProperties vehicleProperties) {
        if (DoubleMath.fuzzyEquals(distance, 0d, vehicleProperties.maxSpeed * ROUNDING_ERROR_MARGIN)) {
            return Traversal.EMPTY_TRAVERSAL;
        }

        if (DoubleMath.fuzzyEquals(initialSpeed, 0d, vehicleProperties.maxSpeed * ROUNDING_ERROR_MARGIN)) {
            return create(distance, vehicleProperties);
        }

        initialSpeed = Math.min(vehicleProperties.maxSpeed, initialSpeed);

        Traversal brakingTraversal = getBrakingTraversal(initialSpeed, initialAcceleration, vehicleProperties);
        double brakingDistance = brakingTraversal.getTotalDistance();
        if (brakingDistance >= distance) {
            return brakingTraversal;
        }

        if (brakingTraversal.getAccelerationAtDistance(brakingDistance) < 0) {
            Traversal traversalAfterBraking = create(distance - brakingDistance, vehicleProperties);

            ImmutableList<TraversalSection> sections = ImmutableList.<TraversalSection>builder()
                    .addAll(brakingTraversal.getSections())
                    .addAll(traversalAfterBraking.getSections()).build();

            return new Traversal(sections);
        }

        if (initialAcceleration < vehicleProperties.acceleration) {
            ImmutableList<TraversalSection> sections = ConstantJerkSectionsFactory.jerkUpToAmaxConstrainedByVmaxThenBrake(initialSpeed, initialAcceleration, vehicleProperties);
            Traversal traversal = new Traversal(sections);
            if (traversal.getTotalDistance() > distance) {
                return BinarySearch.find(
                        calculateTraversalGivenFixedJerkUpTimeAssumingNeitherConstantAccelerationOrSpeed(initialSpeed, initialAcceleration, vehicleProperties),
                        getDistanceComp(distance),
                        0,
                        sections.get(0).getDuration());
            }
        }

        ImmutableList<TraversalSection> sections = ConstantJerkSectionsFactory.getToVmaxThenBrake(initialSpeed, initialAcceleration, vehicleProperties);
        Traversal traversal = new Traversal(sections);
        if (traversal.getTotalDistance() > distance) {
            if (sections.stream().anyMatch(s -> s.getAccelerationAtTime(0) >= vehicleProperties.acceleration)) {
                return BinarySearch.find(
                        getTraversalForAmaxT(initialSpeed, initialAcceleration, vehicleProperties),
                        getDistanceComp(distance),
                        0,
                        getConstantAccelerationTime(sections));
            }

            return BinarySearch.find(
                    getTraversalForInitialAT(initialSpeed, initialAcceleration, vehicleProperties),
                    getDistanceComp(distance),
                    0,
                    getConstantAccelerationTime(sections));
        }

        return new Traversal(ConstantJerkSectionsFactory.getToVmaxAndStayAtVMaxToReachDistance(distance, initialSpeed, initialAcceleration, vehicleProperties));
    }

    private double getConstantAccelerationTime(ImmutableList<TraversalSection> sections) {
        return sections.stream()
                .filter(s -> !s.isConstantSpeed() && s.isConstantAcceleration())
                .findFirst()
                .map(TraversalSection::getDuration)
                .orElse(0d);
    }

    @Override
    public Traversal getBrakingTraversal(double initialSpeed, double initialAcceleration, VehicleMotionProperties vehicleProperties) {
        if (DoubleMath.fuzzyEquals(initialSpeed, 0d, vehicleProperties.maxSpeed * ROUNDING_ERROR_MARGIN)) {
            return Traversal.EMPTY_TRAVERSAL;
        }
        return new Traversal(ConstantJerkSectionsFactory.findBrakingTraversal(initialSpeed, initialAcceleration, vehicleProperties));
    }

    private ToDoubleFunction<Traversal> getDistanceComp(double distance) {
        return t -> {
            if (DoubleMath.fuzzyEquals(t.getTotalDistance(), distance, distance * ROUNDING_ERROR_MARGIN)) {
                return 0;
            }

            return t.getTotalDistance() - distance;
        };
    }

    private double getDistanceToReachMaxSpeed(VehicleMotionProperties vehicleProperties) {
        double maxSpeed = vehicleProperties.maxSpeed;
        double maxAcc = vehicleProperties.acceleration;
        double jerk = vehicleProperties.jerkAccelerationUp;
        // Assuming vehicle starts from rest
        // a = jt
        // v = (1/2) jt^2
        // s = (1/6) jt^3
        double timeToReachMaxSpeedIfJerkConstant = Math.sqrt(2 * maxSpeed / jerk);
        double impliedAccelerationWhenMaxSpeedReached = timeToReachMaxSpeedIfJerkConstant * jerk;
        if (impliedAccelerationWhenMaxSpeedReached <= maxAcc) {
            return 1/6 * jerk * Math.pow(timeToReachMaxSpeedIfJerkConstant, 3);
        } else {
            // Max acceleration is reached before max speed
            double timeToReachMaxAcceleration = maxAcc / jerk;
            double speedReachedAtPointOfReachingMaxAcceleration = 1/2d * jerk * Math.pow(timeToReachMaxAcceleration, 2);
            double distanceTraveledAtPointOfReachingMaxAcceleration = 1/6d * jerk * Math.pow(timeToReachMaxAcceleration, 3);
            // constant acceleration so s = (v^2 - u^2) / 2a
            double distanceNeededToReachMaxSpeed = (Math.pow(maxSpeed, 2) - Math.pow(speedReachedAtPointOfReachingMaxAcceleration, 2)) / (2 * maxAcc);
            return distanceTraveledAtPointOfReachingMaxAcceleration + distanceNeededToReachMaxSpeed;
        }
    }

    private DoubleFunction<Traversal> calculateTraversalGivenFixedJerkUpTimeAssumingNeitherConstantAccelerationOrSpeed(double u, double a, VehicleMotionProperties vehicleProperties) {
        return (double t) -> new Traversal(ConstantJerkSectionsFactory.calculateTraversalGivenFixedJerkUpTimeAssumingNeitherConstantAccelerationOrSpeed(t, u, a, vehicleProperties));
    }

    private DoubleFunction<Traversal> getTraversalForAmaxT(double u, double a, VehicleMotionProperties vehicleProperties) {
        return (double t) -> new Traversal(ConstantJerkSectionsFactory.calculateTraversalGivenFixedMaximumAccelerationTimeAssumingNoMaximumSpeedSection(t, u, a, vehicleProperties));
    }

    private DoubleFunction<Traversal> getTraversalForInitialAT(double u, double a, VehicleMotionProperties vehicleProperties) {
        return (double t) -> new Traversal(ConstantJerkSectionsFactory.calculateTraversalGivenFixedConstantAccelerationTimeAssumingNoMaximumSpeedSection(t, u, a, vehicleProperties));
    }
}
