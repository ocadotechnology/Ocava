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
import java.util.function.DoubleFunction;
import java.util.function.ToDoubleFunction;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.math.DoubleMath;
import com.ocadotechnology.physics.utils.BinarySearch;

/**
 * Builds an optimal jerk-limited S-curve traversal using ConstantJerk/ConstantAcceleration/ConstantSpeed sections.
 * The resulting traversals will end at rest, that is both acceleration and velocity will be 0.
 */
public final class ConstantJerkTraversalCalculator implements TraversalCalculator {
    private static final double ROUNDING_ERROR_MARGIN = 1E-9;
    public static final ConstantJerkTraversalCalculator INSTANCE = new ConstantJerkTraversalCalculator();

    private ConstantJerkTraversalCalculator() {
    }

    /**
     * @param distance          distance to traverse
     * @param vehicleProperties vehicle properties
     * @return Traversal that starts and ends with speed and acceleration equal to zero and traverses the distance.
     * If the distance is less than the braking distance, then the braking traversal will instead be returned.
     */
    public Traversal create(double distance, VehicleMotionProperties vehicleProperties) {
        return create(distance, 0, 0, vehicleProperties);
    }

    /**
     * @param distance            distance to traverse
     * @param initialSpeed        initial speed
     * @param initialAcceleration initial acceleration
     * @param vehicleProperties   vehicle properties
     * @return Traversal that starts with the given speed and acceleration and ends at rest after traversing distance.
     * If the distance is less than the braking distance, then the braking traversal will instead be returned.
     */
    public Traversal create(double distance, double initialSpeed, double initialAcceleration, VehicleMotionProperties vehicleProperties) {
        validateInputs(distance, initialSpeed, initialAcceleration, vehicleProperties);
        Traversal brakingTraversal = getBrakingTraversal(initialSpeed, initialAcceleration, vehicleProperties);
        double minimalDistanceToBrake = brakingTraversal.getTotalDistance();
        int comparisonResult = DoubleMath.fuzzyCompare(minimalDistanceToBrake, distance, ROUNDING_ERROR_MARGIN * distance);
        if (comparisonResult >= 0) {
            // If the target distance is smaller than the minimal braking distance, then we simply return the braking traversal
            return brakingTraversal;
        }
        DoubleFunction<Traversal> speedToTraversalCalculator = (vPeak) -> maxSpeedToTraversal(initialSpeed, initialAcceleration, vPeak, distance, vehicleProperties);
        // First try if reaching the max speed works as this is often the solution.
        Traversal traversalReachingMaxSpeed = speedToTraversalCalculator.apply(vehicleProperties.maxSpeed);
        if (DoubleMath.fuzzyEquals(traversalReachingMaxSpeed.getTotalDistance(), distance, ROUNDING_ERROR_MARGIN * distance)) {
            return traversalReachingMaxSpeed;
        }

        // For the following function, the distance of the traversal increases monotonically with the speed reached
        // after the first section so we can binary search within the range to find the correct max speed.
        return BinarySearch.find(
                speedToTraversalCalculator,
                getDistanceComp(distance),
                lowerBoundSpeedReachedAfterFirstHalf(initialAcceleration, initialSpeed, vehicleProperties),
                vehicleProperties.maxSpeed
        );
    }

    private ToDoubleFunction<Traversal> getDistanceComp(double distance) {
        return t -> {
            if (DoubleMath.fuzzyEquals(t.getTotalDistance(), distance, distance * ROUNDING_ERROR_MARGIN)) {
                return 0;
            }
            return t.getTotalDistance() - distance;
        };
    }

    /**
     * Sometimes we can find a better lower bound that 0 for the speed reached after the first half for the purpose of
     * the binary search. This is a small optimisation that reduces the initial search range. In the cases where we can't
     * we can simply use 0 as the lower bound.
     */
    private double lowerBoundSpeedReachedAfterFirstHalf(double initialAcceleration, double initialSpeed, VehicleMotionProperties vehicleProperties) {
        if (willInevitablyExceedMaxSpeed(initialSpeed, initialAcceleration, vehicleProperties)) {
            return 0.0;
        }
        double jerk = initialAcceleration < 0 ? vehicleProperties.jerkDecelerationDown : vehicleProperties.jerkAccelerationDown;
        double timeToReachZeroAcc = -initialAcceleration / jerk;
        return Math.min(vehicleProperties.maxSpeed, JerkKinematics.getFinalVelocity(initialSpeed, initialAcceleration, jerk, timeToReachZeroAcc));
    }

    /**
     * It's possible to exceed the max speed if the initial speed was already higher than the maximum or if
     * the initial speed and initial acceleration are both high enough that we would exceed the max speed even if we
     * jerked down as fast as possible.
     */
    private boolean willInevitablyExceedMaxSpeed(
            double initialSpeed,
            double initialAcceleration,
            VehicleMotionProperties props) {
        final double vMax = props.maxSpeed;
        if (DoubleMath.fuzzyCompare(initialAcceleration, 0.0, props.accelerationAbsoluteTolerance) <= 0) {
            return DoubleMath.fuzzyCompare(initialSpeed, vMax, ROUNDING_ERROR_MARGIN * vMax) > 0;
        }
        final double jDownAcc = props.jerkAccelerationDown;
        final double jMag = Math.abs(jDownAcc);

        final double dvMin = (initialAcceleration * initialAcceleration) / (2.0 * jMag);
        final double vPeakMin = initialSpeed + dvMin;

        return DoubleMath.fuzzyCompare(vPeakMin, vMax, ROUNDING_ERROR_MARGIN * vMax) > 0;
    }

    /**
     * @param initialSpeed        initial speed
     * @param initialAcceleration initial acceleration
     * @param vehicleProperties   vehicle properties
     * @return the minimal distance traversal to come to rest
     * @throws TraversalCalculationException if the speed would go negative.
     */
    public Traversal getBrakingTraversal(double initialSpeed, double initialAcceleration, VehicleMotionProperties vehicleProperties) {
        validateInputs(0, initialSpeed, initialAcceleration, vehicleProperties);
        List<TraversalSection> sections = new ArrayList<>();

        // Add a correcting section if the bot begins outside of its acceleration constraints.
        Optional<TraversalSection> maybeInitialCorrectingSection = ConstantJerkSectionsFactory.buildSectionToBringAccelerationWithinBounds(initialAcceleration, initialSpeed, vehicleProperties);
        if (maybeInitialCorrectingSection.isPresent()) {
            TraversalSection section = maybeInitialCorrectingSection.get();
            sections.add(section);
            initialSpeed = section.getSpeedAtTime(section.getDuration());
            initialAcceleration = section.getAccelerationAtTime(section.getDuration());
        }

        double jerkAccelerationDown = vehicleProperties.jerkAccelerationDown;
        int comparisonResult = DoubleMath.fuzzyCompare(initialAcceleration, 0, ROUNDING_ERROR_MARGIN * vehicleProperties.acceleration);
        if (comparisonResult == -1) {
            // Acceleration is already negative, so continue to brake
            sections.addAll(ConstantJerkSectionsFactory.solveTriangleOrTrapezoidSections(initialSpeed, initialAcceleration, 0, vehicleProperties));
        } else if (comparisonResult == 1) {
            // Slow to zero acc as fast as possible.
            // a_t = initialAcceleration + jt = 0
            // t = -initialAcceleration / j
            double timeToReachZeroAcc = -initialAcceleration / jerkAccelerationDown;
            double distanceToReachZeroAcc = JerkKinematics.getDisplacement(initialSpeed, initialAcceleration, jerkAccelerationDown, timeToReachZeroAcc);
            double speedReachedAtZeroAcc = JerkKinematics.getFinalVelocity(initialSpeed, initialAcceleration, jerkAccelerationDown, timeToReachZeroAcc);
            sections.add(new ConstantJerkTraversalSection(timeToReachZeroAcc, distanceToReachZeroAcc, initialSpeed, speedReachedAtZeroAcc, initialAcceleration, 0, jerkAccelerationDown));
            sections.addAll(ConstantJerkSectionsFactory.buildDeceleratingPhaseOfTraversal(speedReachedAtZeroAcc, vehicleProperties));
        } else {
            // Acceleration is already 0
            sections.addAll(ConstantJerkSectionsFactory.buildDeceleratingPhaseOfTraversal(initialSpeed, vehicleProperties));
        }
        return new Traversal(ImmutableList.copyOf(sections));
    }

    /**
     * @param initialSpeed                initial speed
     * @param initialAcceleration                initial acceleration
     * @param vTarget           target max speed
     * @param distanceTarget    target distance for traversal
     * @param vehicleProperties vehicle properties
     * @return Traversal that reaches vTarget as its max speed. It will only add a constant accelerations section
     * if that speed happens to correspond to the vehicle's max speed. The distance of traversal increases monotonically with
     * vTarget within valid bounds.
     */
    @VisibleForTesting
    Traversal maxSpeedToTraversal(double initialSpeed, double initialAcceleration, double vTarget, double distanceTarget, VehicleMotionProperties vehicleProperties) {
        Builder<TraversalSection> traversalSectionBuilder = ImmutableList.builder();
        double currentSpeed = initialSpeed;
        double currentAcceleration = initialAcceleration;
        // Add a correcting section if the vehicle begins out of its acceleration bounds.
        Optional<TraversalSection> maybeInitialCorrectingSection = ConstantJerkSectionsFactory.buildSectionToBringAccelerationWithinBounds(currentAcceleration, currentSpeed, vehicleProperties);
        if (maybeInitialCorrectingSection.isPresent()) {
            TraversalSection section = maybeInitialCorrectingSection.get();
            traversalSectionBuilder.add(section);
            currentSpeed = section.getSpeedAtTime(section.getDuration());
            currentAcceleration = section.getAccelerationAtTime(section.getDuration());
        }

        List<TraversalSection> acceleratingPhaseSections = ConstantJerkSectionsFactory.buildAcceleratingPhaseOfTraversal(currentAcceleration, currentSpeed, vTarget, vehicleProperties);
        currentSpeed = vTarget;
        List<TraversalSection> deceleratingPhaseSections = ConstantJerkSectionsFactory.buildDeceleratingPhaseOfTraversal(currentSpeed, vehicleProperties);

        traversalSectionBuilder.addAll(acceleratingPhaseSections);
        double distanceSoFar = maybeInitialCorrectingSection.stream().mapToDouble(TraversalSection::getTotalDistance).sum()
                + acceleratingPhaseSections.stream().mapToDouble(TraversalSection::getTotalDistance).sum()
                + deceleratingPhaseSections.stream().mapToDouble(TraversalSection::getTotalDistance).sum();
        // If we have reached the max speed then we can add a cruise section.
        if (DoubleMath.fuzzyEquals(vTarget, vehicleProperties.maxSpeed, ROUNDING_ERROR_MARGIN * vehicleProperties.maxSpeed)
                && distanceSoFar < distanceTarget) {
            double distanceDelta = distanceTarget - distanceSoFar;
            double timeAtConstantSpeed = distanceDelta / vTarget;
            ConstantSpeedTraversalSection cruiseSection = new ConstantSpeedTraversalSection(distanceDelta, vTarget, timeAtConstantSpeed);
            traversalSectionBuilder.add(cruiseSection);
        }

        ImmutableList<TraversalSection> sections = traversalSectionBuilder
                .addAll(deceleratingPhaseSections)
                .build();
        return new Traversal(ImmutableList.copyOf(sections));
    }

    private void validateInputs(double distance, double initialSpeed, double initialAcceleration, VehicleMotionProperties props) throws TraversalCalculationException {
        Preconditions.checkArgument(distance >= 0.0, "Distance must be non-negative");
        Preconditions.checkArgument(props.acceleration > 0.0, "Max acceleration must be > 0");
        Preconditions.checkArgument(props.deceleration < 0.0, "Max deceleration must be < 0 (expects negative)");
        Preconditions.checkArgument(props.maxSpeed > 0.0, "Max speed must be > 0");
        Preconditions.checkArgument(props.jerkAccelerationUp > 0.0, "jerkAccelerationUp must be > 0");
        Preconditions.checkArgument(props.jerkAccelerationDown < 0.0, "jerkAccelerationDown must be < 0");
        Preconditions.checkArgument(props.jerkDecelerationUp < 0.0, "jerkDecelerationUp must be < 0");
        Preconditions.checkArgument(props.jerkDecelerationDown > 0.0, "jerkDecelerationDown must be > 0");
        Preconditions.checkArgument(
                DoubleMath.fuzzyCompare(initialSpeed, 0.0, ROUNDING_ERROR_MARGIN * props.maxSpeed) >= 0,
                "Initial speed cannot be negative (no reverse)"
        );
        if (willSpeedGoNegative(initialSpeed, initialAcceleration, props)) {
            String errorMsg = String.format("Speed would eventually go negative given the initial acceleration %s and initial speed %s, given the vehicle properties: %s", initialAcceleration, initialSpeed, props);
            throw new TraversalCalculationException(errorMsg);
        }
    }

    /**
     * @return true if the initial conditions would result in the speed going negative due to initial acceleration being
     * too negative. Returns false, otherwise
     */
    private boolean willSpeedGoNegative(double initialSpeed, double initialAcceleration, VehicleMotionProperties vehicleProperties) {
        if (initialAcceleration >= 0) {
            return false;
        }
        double jerkDecelerationDown = vehicleProperties.jerkDecelerationDown;
        double timeToReachZeroAcc = -initialAcceleration / jerkDecelerationDown;
        double speedReached = JerkKinematics.getFinalVelocity(initialSpeed, initialAcceleration, jerkDecelerationDown, timeToReachZeroAcc);
        return DoubleMath.fuzzyCompare(speedReached, 0, ROUNDING_ERROR_MARGIN * vehicleProperties.maxSpeed) < 0;
    }
}
