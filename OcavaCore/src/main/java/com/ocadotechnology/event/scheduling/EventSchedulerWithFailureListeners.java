/*
 * Copyright © 2017-2026 Ocado (Ocava)
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

import java.util.function.Consumer;

import com.ocadotechnology.event.RecoverableException;

/**
 * An {@link EventScheduler} which supports registering listeners to be notified when an event throws an exception
 * that stops the scheduler, or an exception from which the scheduler is able to recover.
 */
public interface EventSchedulerWithFailureListeners extends EventScheduler {
    /**
     * Registers a listener to be notified whenever an event executed by this scheduler throws an exception which
     * is not a {@link RecoverableException}, causing the scheduler to stop.
     */
    void registerFailureListener(Consumer<Throwable> failureListener);

    /**
     * Registers a listener to be notified whenever an event executed by this scheduler throws a
     * {@link RecoverableException}, allowing the scheduler to continue running.
     */
    void registerRecoverableFailureListener(Consumer<RecoverableException> failureListener);
}
