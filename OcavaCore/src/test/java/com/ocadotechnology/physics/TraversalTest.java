/*
 * Copyright © 2017-2024 Ocado (Ocava)
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

class TraversalTest {
    private final ImmutableList<TraversalSection> sections = ImmutableList.of(
            new ConstantAccelerationTraversalSection(4, 2, 0, 4, 2),
            new ConstantSpeedTraversalSection(4, 4, 1),
            new ConstantAccelerationTraversalSection(8, -1, 4, 0, 4));
    private final Traversal constantAccelerationTraversal = new Traversal(sections);

    @Test
    void getTotalTime_returnsExpectedValue() {
        assertThat(constantAccelerationTraversal.getTotalDuration()).isCloseTo(7, within(0.000001));
    }

    @Test
    void getTotalDistance_returnsExpectedValue() {
        assertThat(constantAccelerationTraversal.getTotalDistance()).isCloseTo(16, within(0.000001));
    }

    @Test
    void getDurationAccelerating_returnsExpectedValue() {
        assertThat(constantAccelerationTraversal.getDurationAccelerating()).isCloseTo(2, within(0.0000001));
    }

    @Test
    void getDurationAtConstantSpeed_returnsExpectedValue() {
        assertThat(constantAccelerationTraversal.getDurationAtConstantSpeed()).isCloseTo(1, within(0.0000001));
    }

    @Test
    void getDurationDecelerating_returnsExpectedValue() {
        assertThat(constantAccelerationTraversal.getDurationDecelerating()).isCloseTo(4, within(0.0000001));
    }

    @Test
    void getTimeAtDistance_returnsExpectedValue() {
        assertThat(constantAccelerationTraversal.getTimeAtDistance(4.4)).isCloseTo(2.1, within(0.000001));
    }

    @Test
    void getDistanceAtTime_returnsExpectedValue() {
        assertThat(constantAccelerationTraversal.getDistanceAtTime(2.4)).isCloseTo(5.6, within(0.000001));
    }

    @Test
    void getSpeedAtDistance_returnsExpectedValue() {
        assertThat(constantAccelerationTraversal.getSpeedAtDistance(1)).isCloseTo(2, within(0.000001));
        assertThat(constantAccelerationTraversal.getSpeedAtDistance(4.4)).isCloseTo(4, within(0.000001));
    }

    @Test
    void getSpeedAtTime_returnsExpectedValue() {
        assertThat(constantAccelerationTraversal.getSpeedAtTime(1.2)).isCloseTo(2.4, within(0.000001));
        assertThat(constantAccelerationTraversal.getSpeedAtTime(2.4)).isCloseTo(4, within(0.000001));
    }

    @Test
    void getAccelerationAtDistance_returnsExpectedValue() {
        assertThat(constantAccelerationTraversal.getAccelerationAtDistance(2.4)).isCloseTo(2, within(0.000001));
        assertThat(constantAccelerationTraversal.getAccelerationAtDistance(4.4)).isCloseTo(0, within(0.000001));
    }

    @Test
    void getAccelerationAtTime_returnsExpectedValue() {
        assertThat(constantAccelerationTraversal.getAccelerationAtTime(1.2)).isCloseTo(2, within(0.000001));
        assertThat(constantAccelerationTraversal.getAccelerationAtTime(2.4)).isCloseTo(0, within(0.000001));
    }

    @Test
    void getTimeAtDistance_whenInputNegative_thenThrowsException() {
        assertThatThrownBy(() -> constantAccelerationTraversal.getTimeAtDistance(-1)).isInstanceOf(TraversalCalculationException.class);
    }

    @Test
    void getDistanceAtTime_whenInputNegative_thenThrowsException() {
        assertThatThrownBy(() -> constantAccelerationTraversal.getDistanceAtTime(-1)).isInstanceOf(TraversalCalculationException.class);
    }

    @Test
    void getSpeedAtDistance_whenInputNegative_thenThrowsException() {
        assertThatThrownBy(() -> constantAccelerationTraversal.getSpeedAtDistance(-1)).isInstanceOf(TraversalCalculationException.class);
    }

    @Test
    void getSpeedAtTime_whenInputNegative_thenThrowsException() {
        assertThatThrownBy(() -> constantAccelerationTraversal.getSpeedAtTime(-1)).isInstanceOf(TraversalCalculationException.class);
    }

    @Test
    void getAccelerationAtDistance_whenInputNegative_thenThrowsException() {
        assertThatThrownBy(() -> constantAccelerationTraversal.getAccelerationAtDistance(-1)).isInstanceOf(TraversalCalculationException.class);
    }

    @Test
    void getAccelerationAtTime_whenInputNegative_thenThrowsException() {
        assertThatThrownBy(() -> constantAccelerationTraversal.getAccelerationAtTime(-1)).isInstanceOf(TraversalCalculationException.class);
    }

    @Test
    void getTimeAtDistance_whenInputBeyondMax_thenThrowsException() {
        assertThatThrownBy(() -> constantAccelerationTraversal.getTimeAtDistance(1000)).isInstanceOf(TraversalCalculationException.class);
    }

    @Test
    void getDistanceAtTime_whenInputBeyondMax_thenReturnsTotalDistance() {
        assertThat(constantAccelerationTraversal.getDistanceAtTime(1000)).isCloseTo(16, within(1e-9));
    }

    @Test
    void getSpeedAtDistance_whenInputBeyondMax_thenThrowsException() {
        assertThatThrownBy(() -> constantAccelerationTraversal.getSpeedAtDistance(1000)).isInstanceOf(TraversalCalculationException.class);
    }

    @Test
    void getSpeedAtTime_whenInputBeyondMax_thenReturnsZero() {
        assertThat(constantAccelerationTraversal.getSpeedAtTime(1000)).isCloseTo(0, within(1e-9));
    }

    @Test
    void getAccelerationAtDistance_whenInputBeyondMax_thenThrowsException() {
        assertThatThrownBy(() -> constantAccelerationTraversal.getAccelerationAtDistance(1000)).isInstanceOf(TraversalCalculationException.class);
    }

    @Test
    void getAccelerationAtTime_whenInputBeyondMax_thenReturnsZero() {
        assertThat(constantAccelerationTraversal.getAccelerationAtTime(1000)).isCloseTo(0, within(1e-9));
    }

    private final ImmutableList<TraversalSection> realExampleSections = ImmutableList.of(
        new ConstantAccelerationTraversalSection(0.014932585323793109, 2.7993232561717623E-6, 8.000102181418545E-5, 3.000040503637245E-4, 78.59150530918177),
        new ConstantSpeedTraversalSection(1.434959353209076, 3.000040503637245E-4, 4783.133265932021),
        new ConstantAccelerationTraversalSection(0.014932585323793107, -2.7993232561717623E-6, 3.000040503637245E-4, 8.000102181418545E-5, 78.59150530918177),
        new ConstantSpeedTraversalSection(0.1988110026007896, 8.000102181418545E-5, 2485.1057910554996),
        new ConstantAccelerationTraversalSection(0.0011431626335406458, -2.7993232561717623E-6, 8.000102181418545E-5, 0.0, 28.578700811992505));

    private final Traversal realTraversal = new Traversal(realExampleSections);

    @Test
    void getSpeedAtTime_realExample() {
        assertThat(realTraversal.getSpeedAtTime(4968.894977362377)).isCloseTo(8.000102181418545E-5, within(1e-9));
    }

    @Test
    void getDistanceAtTime_realExample() {
        assertThat(realTraversal.getDistanceAtTime(4968.894977362377)).isCloseTo(1.4671108491237437, within(1e-9));
    }
}
