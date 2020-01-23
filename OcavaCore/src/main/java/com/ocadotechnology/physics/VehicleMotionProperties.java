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

import java.io.Serializable;

/**
 * Wrapper class describing an objects physical motion properties.
 */
public class VehicleMotionProperties implements Serializable {

    public final double maxSpeed;
    public final double acceleration;
    public final double deceleration;
    public final double jerkAccelerationUp;
    public final double jerkAccelerationDown;
    public final double jerkDecelerationUp;
    public final double jerkDecelerationDown;

    public VehicleMotionProperties(
            double acceleration,
            double deceleration,
            double maxSpeed,
            double jerkAccelerationUp,
            double jerkAccelerationDown,
            double jerkDecelerationUp,
            double jerkDecelerationDown) {
        this.acceleration = acceleration;
        this.deceleration = deceleration;
        this.maxSpeed = maxSpeed;
        this.jerkAccelerationUp = jerkAccelerationUp;
        this.jerkAccelerationDown = jerkAccelerationDown;
        this.jerkDecelerationUp = jerkDecelerationUp;
        this.jerkDecelerationDown = jerkDecelerationDown;
    }
}
