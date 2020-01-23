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
package com.ocadotechnology.physics.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

class SegmentGraphUtilitiesTest {

    private ImmutableList<Section> sections = ImmutableList.of(
            new Section(0, 4, 4),
            new Section(4, 7, 4),
            new Section(7, 8, 2)
    );

    @Test
    void getValueAt_whenXWithinRange_thenReturnsExpectedValue() {
        double result = SegmentGraphUtilities.getValueAt(sections, Section::xExtent, Section::extrapolateValue, 7.7);
        assertThat(result).isCloseTo(1.4, within(1e-6));
    }

    @Test
    void getValueAt_whenXBeyondRange_thenThrowsException() {
        assertThatThrownBy(() -> SegmentGraphUtilities.getValueAt(sections, Section::xExtent, Section::extrapolateValue, 100000))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void accumulateValueTo_whenXWithinRange_thenReturnsExpectedValue() {
        double result = SegmentGraphUtilities.accumulateValueTo(sections, Section::xExtent, s -> s.y, Section::extrapolateValue, 7.7);
        assertThat(result).isCloseTo(9.4, within(1e-6));
    }

    @Test
    void accumulateValueTo_whenXBeyondRange_thenThrowsException() {
        assertThatThrownBy(() -> SegmentGraphUtilities.accumulateValueTo(sections, Section::xExtent, s -> s.y, Section::extrapolateValue, 100000))
                .isInstanceOf(IllegalStateException.class);
    }

    private static class Section {
        private final double x1;
        private final double x2;
        private final double y;

        private Section(double x1, double x2, double y) {
            this.x1 = x1;
            this.x2 = x2;
            this.y = y;
        }

        double xExtent() {
            return x2 - x1;
        }

        private double extrapolateValue(double xValue) {
            return y * xValue / xExtent();
        }
    }
}
