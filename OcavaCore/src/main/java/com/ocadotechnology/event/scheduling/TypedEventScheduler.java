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

import com.google.common.base.Preconditions;

/**
 * Concretes EventScheduler's `getType` method using the type passed to the constructor.
 */
public abstract class TypedEventScheduler implements EventSchedulerWithCanceling {
    protected final EventSchedulerType type;

    public TypedEventScheduler(EventSchedulerType type) {
        Preconditions.checkNotNull(type, "Type can't be null");
        this.type = type;
    }

    @Override
    public final EventSchedulerType getType() {
        return type;
    }
}
