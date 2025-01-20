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
package com.ocadotechnology.notification;

import com.ocadotechnology.event.scheduling.EventSchedulerType;
import com.ocadotechnology.event.scheduling.SimpleEventSchedulerType;

/**
 * Convenience extension of Subscriber interface intended for use with a fully single-threaded simulation
 * (that uses only one scheduler with the type {@link SimpleEventSchedulerType#SIMULATION}).
 */
public interface SimpleSubscriber extends Subscriber {
    default EventSchedulerType getSchedulerType() {
        return SimpleEventSchedulerType.SIMULATION;
    }
}
