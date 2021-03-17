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
package com.ocadotechnology.physics;

import java.util.Optional;
import java.util.function.DoubleFunction;
import java.util.function.ToDoubleFunction;

import com.google.common.collect.ImmutableList;
import com.ocadotechnology.physics.utils.BinarySearch;

public class ConstantJerkTraversalCalculator implements TraversalCalculator {
    private static final double ERROR_MARGIN = 0.000000001;

    public static final ConstantJerkTraversalCalculator INSTANCE = new ConstantJerkTraversalCalculator();

    private ConstantJerkTraversalCalculator() {
    }

    public Traversal create(double distance, VehicleMotionProperties vehicleProperties) {
        if (distance == 0) {
            return Traversal.EMPTY_TRAVERSAL;
        }

        Optional<ImmutableList<TraversalSection>> sections = ConstantJerkSectionsFactory.neitherMaxAccelerationNorMaxDecelerationReached(distance, vehicleProperties);
        if (sections.isPresent()) {
            return new Traversal(sections.get());
        }

        sections = ConstantJerkSectionsFactory.maxAccelerationDecelerationAndSpeedReached(distance, vehicleProperties);
        if (sections.isPresent()) {
            return new Traversal(sections.get());
        }

        sections = ConstantJerkSectionsFactory.maxAccelerationAndMaxDecelerationReachedButNotMaxSpeed(distance, vehicleProperties);
        if (sections.isPresent()) {
            return new Traversal(sections.get());
        }

        return new Traversal(ConstantJerkSectionsFactory.oneMaxAccelReachedButNotOther(distance, vehicleProperties));
    }

    /**
     * @throws TraversalCalculationException
     */
    @Override
    public Traversal create(double distance, double initialSpeed, double initialAcceleration, VehicleMotionProperties vehicleProperties) {
        if (distance == 0) {
            return Traversal.EMPTY_TRAVERSAL;
        }

        if (initialSpeed == 0) {
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
        return new Traversal(ConstantJerkSectionsFactory.findBrakingTraversal(initialSpeed, initialAcceleration, vehicleProperties));
    }

    private ToDoubleFunction<Traversal> getDistanceComp(double distance) {
        return t -> {
            double difference = t.getTotalDistance() - distance;
            if (Math.abs(difference) < ERROR_MARGIN) {
                return 0;
            }

            return difference;
        };
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
