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
package com.ocadotechnology.time;

public class ScalableOffsetUtcTimeProvider extends OffsetUtcTimeProvider {

    private final double delta;

    public ScalableOffsetUtcTimeProvider(double simulationStartTime, double delta) {
        //Divide the given simulation start time by delta as it will be multiplied by delta when `getTime()` is called
        super(simulationStartTime / delta);

        this.delta = delta;
    }

    @Override
    public double getTime() {
        return super.getTime() * delta;
    }
}
