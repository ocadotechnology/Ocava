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
package com.ocadotechnology.time;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import javax.annotation.CheckForNull;

public class AdjustableTimeProviderWithUnit extends AdjustableTimeProvider implements TimeProviderWithUnit {
    private final TimeConverter converter;
    private @CheckForNull Instant cachedInstant;

    public AdjustableTimeProviderWithUnit(double initialTime, TimeUnit timeUnit) {
        super(initialTime);
        this.converter = new TimeConverter(timeUnit);
    }

    @Override
    public TimeConverter getConverter() {
        return converter;
    }

    @Override
    public Instant getInstant() {
        if (cachedInstant == null) {
            cachedInstant = TimeProviderWithUnit.super.getInstant();
        }
        return cachedInstant;
    }

    @Override
    public void setTime(double time) {
        if (time != getTime()) {
            cachedInstant = null;
        }
        super.setTime(time);
    }
}
