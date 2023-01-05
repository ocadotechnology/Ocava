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
package com.ocadotechnology.physics;

import java.io.Serializable;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.math.DoubleMath;
import com.ocadotechnology.physics.utils.SegmentGraphUtilities;

/**
 * Class describing physical motion in one direction along a straight line.
 */
public class Traversal implements Serializable {
    private static final double ROUNDING_ERROR_FRACTION = 1E-9;

    /**
     * A zero-distance traversal
     */
    public static final Traversal EMPTY_TRAVERSAL = new Traversal(ImmutableList.of());

    private final ImmutableList<TraversalSection> sections;
    private final double totalDuration;
    private final double totalDistance;

    public Traversal(ImmutableList<TraversalSection> sections) {
        double time = 0;
        double dist = 0;
        for (TraversalSection section : sections) {
            time += section.getDuration();
            dist += section.getTotalDistance();
        }
        this.sections = sections;
        this.totalDuration = time;
        this.totalDistance = dist;
    }

    /**
     * @return the list of TraversalSections defining this traversal.
     */
    public ImmutableList<TraversalSection> getSections() {
        return sections;
    }

    /**
     * @return the total duration of all the sections of this traversal.
     */
    public double getTotalDuration() {
        return totalDuration;
    }

    /**
     * @return the total distance of this traversal.
     */
    public double getTotalDistance() {
        return totalDistance;
    }

    /**
     * @return the distance reached in the specified time with this traversal.  Returns the total distance if the
     *          specified time is greater than the total time of the traversal
     *
     * @throws TraversalCalculationException if the specified time is negative.
     */
    public double getDistanceAtTime(double time) {
        if (time > totalDuration || DoubleMath.fuzzyEquals(time, totalDuration, totalDuration * ROUNDING_ERROR_FRACTION)) {
            return totalDistance;
        }

        if (time < 0) {
            throw new TraversalCalculationException("Negative time provided");
        }

        return SegmentGraphUtilities.accumulateValueTo(
                sections,
                TraversalSection::getDuration,
                TraversalSection::getTotalDistance,
                TraversalSection::getDistanceAtTime,
                time);
    }

    /**
     * @return the time it will take to reach the specified distance with this traversal.
     *
     * @throws TraversalCalculationException if the provided distance is negative, or greater than the total length of the
     *          Traversal
     */
    public double getTimeAtDistance(double distance) {
        if (DoubleMath.fuzzyEquals(distance, totalDistance, totalDistance * ROUNDING_ERROR_FRACTION)) {
            return totalDuration;
        }

        if (distance < 0) {
            throw new TraversalCalculationException("Negative distance provided.");
        }
        if (distance >= totalDistance) {
            throw new TraversalCalculationException("Distance provided " + distance + " is beyond total traversal distance " + totalDistance);
        }

        return SegmentGraphUtilities.accumulateValueTo(
                sections,
                TraversalSection::getTotalDistance,
                TraversalSection::getDuration,
                TraversalSection::getTimeAtDistance,
                distance);
    }

    /**
     * @return the speed of the object at the specified time.  Returns zero if the specified time is greater than the
     *          total time of the traversal
     *
     * @throws TraversalCalculationException if the specified time is negative.
     */
    public double getSpeedAtTime(double time) {
        if (DoubleMath.fuzzyEquals(time, totalDuration, totalDuration * ROUNDING_ERROR_FRACTION)) {
            TraversalSection lastSection = Iterables.getLast(sections);
            return lastSection.getSpeedAtTime(lastSection.getDuration());
        }
        if (time > totalDuration) {
            return 0;
        }

        if (time < 0) {
            throw new TraversalCalculationException("Negative time provided");
        }

        return SegmentGraphUtilities.getValueAt(
                sections,
                TraversalSection::getDuration,
                TraversalSection::getSpeedAtTime,
                time);
    }

    /**
     * @return the speed of the object at the specified distance.
     *
     * @throws TraversalCalculationException if the provided distance is negative, or greater than the total length of the
     *          Traversal
     */
    public double getSpeedAtDistance(double distance) {
        if (DoubleMath.fuzzyEquals(distance, totalDistance, totalDistance * ROUNDING_ERROR_FRACTION)) {
            TraversalSection lastSection = Iterables.getLast(sections);
            return lastSection.getSpeedAtTime(lastSection.getDuration());
        }

        if (distance < 0) {
            throw new TraversalCalculationException("Negative distance provided.");
        }
        if (distance >= totalDistance) {
            throw new TraversalCalculationException("Distance provided " + distance + " is beyond total traversal distance " + totalDistance);
        }

        return SegmentGraphUtilities.getValueAt(
                sections,
                TraversalSection::getTotalDistance,
                TraversalSection::getSpeedAtDistance,
                distance);
    }

    /**
     * @return the acceleration of the object at the specified time.  Returns zero if the specified time is greater than
     *          the total time of the traversal
     *
     * @throws TraversalCalculationException if the specified time is negative.
     */
    public double getAccelerationAtTime(double time) {
        if (DoubleMath.fuzzyEquals(time, totalDuration, totalDuration * ROUNDING_ERROR_FRACTION)) {
            TraversalSection lastSection = Iterables.getLast(sections);
            return lastSection.getAccelerationAtTime(lastSection.getDuration());
        }
        if (time > totalDuration) {
            return 0;
        }

        if (time < 0) {
            throw new TraversalCalculationException("Negative time provided");
        }

        return SegmentGraphUtilities.getValueAt(
                sections,
                TraversalSection::getDuration,
                TraversalSection::getAccelerationAtTime,
                time);
    }

    /**
     * @return the acceleration of the object at the specified distance.
     *
     * @throws TraversalCalculationException if the provided distance is negative, or greater than the total length of the
     *          Traversal
     */
    public double getAccelerationAtDistance(double distance) {
        if (DoubleMath.fuzzyEquals(distance, totalDistance, totalDistance * ROUNDING_ERROR_FRACTION)) {
            TraversalSection lastSection = Iterables.getLast(sections);
            return lastSection.getAccelerationAtTime(lastSection.getDuration());
        }

        if (distance < 0) {
            throw new TraversalCalculationException("Negative distance provided.");
        }
        if (distance >= totalDistance) {
            throw new TraversalCalculationException("Distance provided " + distance + " is beyond total traversal distance " + totalDistance);
        }

        return SegmentGraphUtilities.getValueAt(
                sections,
                TraversalSection::getTotalDistance,
                TraversalSection::getAccelerationAtDistance,
                distance);
    }

    /**
     * @return the total time during which the object's speed is increasing.
     */
    public double getDurationAccelerating() {
        return getDurationAt(TraversalSection::isAccelerating);
    }

    /**
     * @return the total time during which the object's speed is constant.
     */
    public double getDurationAtConstantSpeed() {
        return getDurationAt(TraversalSection::isConstantSpeed);
    }

    /**
     * @return the total time during which the object's speed is decreasing.
     */
    public double getDurationDecelerating() {
        return getDurationAt(TraversalSection::isDecelerating);
    }

    private double getDurationAt(Predicate<TraversalSection> condition) {
        return sections.stream().filter(condition).mapToDouble(TraversalSection::getDuration).sum();
    }
}
