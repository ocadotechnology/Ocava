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
package com.ocadotechnology.physics.units;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LengthUnitTest {
    @Test
    void convert_whenTargetIsSmallerThanValue_thenGivesCorrectAnswer() {
        assertThat(LengthUnit.CENTIMETERS.convert(13, LengthUnit.KILOMETERS)).isEqualTo(1_300_000);
    }

    @Test
    void convert_whenTargetIsLargerThanValue_thenGivesCorrectAnswer() {
        assertThat(LengthUnit.KILOMETERS.convert(1_300_000, LengthUnit.CENTIMETERS)).isEqualTo(13);
    }

    @Test
    void convert_whenTargetIsLargerThanValue_thenTruncatesPrecision() {
        assertThat(LengthUnit.KILOMETERS.convert(1_311_513, LengthUnit.CENTIMETERS)).isEqualTo(13);
    }

    @Test
    void convert_whenTargetIsSmallerThanValueAndDistanceIsLargeAndPositive_thenSaturatesLength() {
        assertThat(LengthUnit.NANOMETERS.convert(Long.MAX_VALUE / 100, LengthUnit.KILOMETERS)).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void convert_whenTargetIsSmallerThanValueAndDistanceIsLargeAndNegative_thenSaturatesLength() {
        assertThat(LengthUnit.NANOMETERS.convert(Long.MIN_VALUE / 100, LengthUnit.KILOMETERS)).isEqualTo(Long.MIN_VALUE);
    }

    @Test
    void getUnitsIn_gives_correctValue() {
        assertThat(LengthUnit.NANOMETERS.getUnitsIn(LengthUnit.METERS)).isEqualTo(1e9);
    }
}
