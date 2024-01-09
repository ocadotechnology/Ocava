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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VehicleMotionPropertiesTest {

    @Test
    public void constructor_whenAllArgumentsAreValid_thenPasses () {
        Assertions.assertDoesNotThrow(
                () -> new VehicleMotionProperties(1, 0, -1, 0, 1, 0, 1, -1, -1, 1));
    }

    @Test
    public void constructor_whenAccelerationIsNegative_thenFails () {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new VehicleMotionProperties(-1, 0, -1, 0, 1, 0, 1, -1, -1, 1));
    }

    @Test
    public void constructor_whenDecelerationIsPositive_thenFails () {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new VehicleMotionProperties(1, 0, 1, 0, 1, 0, 1, -1, -1, 1));
    }

    @Test
    public void constructor_whenMaxSpeedIsNegative_thenFails () {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new VehicleMotionProperties(1, 0, -1, 0, -1, 0, 1, -1, -1, 1));
    }

    @Test
    public void constructor_whenJerkAccelerationUpIsNegative_thenFails () {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new VehicleMotionProperties(1, 0, -1, 0, 1, 0, -1, -1, -1, 1));
    }

    @Test
    public void constructor_whenJerkAccelerationDownIsPositive_thenFails () {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new VehicleMotionProperties(1, 0, -1, 0, 1, 0, 1, 1, -1, 1));
    }

    @Test
    public void constructor_whenJerkDecelerationUpIsPositive_thenFails () {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new VehicleMotionProperties(1, 0, -1, 0, 1, 0, 1, -1, 1, 1));
    }

    @Test
    public void constructor_whenJerkDecelerationDownIsNegative_thenFails () {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new VehicleMotionProperties(1, 0, -1, 0, 1, 0, 1, -1, -1, -1));
    }

}
