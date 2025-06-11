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

/**
 * @deprecated - Calling cancel on a scheduler can have significant performance implications, as it requires
 * iterating through all events in the queue to find the one to cancel. Consider calling Event::cancel,
 * or writing your event such that it knows if it needs to run or not.
 */
@Deprecated
public interface EventSchedulerWithCanceling extends EventScheduler {
    /**
     * @deprecated - Calling cancel on a scheduler can have significant performance implications, as it requires
     * iterating through all events in the queue to find the one to cancel. Consider calling Event::cancel,
     * or writing your event such that it knows if it needs to run or not.
     */
    @Deprecated
    void cancel(Event e);
}
