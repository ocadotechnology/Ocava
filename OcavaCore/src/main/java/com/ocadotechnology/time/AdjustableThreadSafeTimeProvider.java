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
package com.ocadotechnology.time;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.util.concurrent.AtomicDouble;

@ThreadSafe
public class AdjustableThreadSafeTimeProvider implements ModifiableTimeProvider {
    private AtomicDouble currTime;

    public AdjustableThreadSafeTimeProvider(double initialTime) {
        currTime = new AtomicDouble(initialTime);
    }

    @Override
    public double getTime() {
        return currTime.get();
    }

    @Override
    public void setTime(double time) {
        currTime.set(time);
    }

    @Override
    public void advanceTime(double periodMs) {
        currTime.getAndAdd(periodMs);
    }
}
