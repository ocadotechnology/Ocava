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
package com.ocadotechnology.event.scheduling;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Custom unckecked exception to indicate that an event scheduler was called in a time-unit-aware manner without being
 * provided with a unit-aware time provider.
 */
@ParametersAreNonnullByDefault
public class TimeUnitNotSpecifiedException extends RuntimeException {
    public TimeUnitNotSpecifiedException() {
        super("Event Scheduler was not created with a TimeProviderWithTimeUnit, but was called in a time-unit-aware manner. "
                + "Please provide a TimeProviderWithTimeUnit to the EventScheduler constructor.");
    }
}
