/*
 * Copyright © 2017-2019 Ocado (Ocava)
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
package com.ocadotechnology.scenario;

import java.util.function.Consumer;

import com.ocadotechnology.id.StringIdGenerator;
import com.ocadotechnology.notification.Notification;

public abstract class AbstractGivenSteps {
    protected final StepManager stepManager;
    private final NotificationCache notificationCache;

    protected AbstractGivenSteps(StepManager stepManager, NotificationCache notificationCache) {
        this.stepManager = stepManager;
        this.notificationCache = notificationCache;
    }

    protected <N extends Notification> void addValidationStep(Class<N> notificationType, Consumer<N> consumer) {
        addValidationStep(nextId(), notificationType, consumer);
    }

    protected <N extends Notification> void addValidationStep(String name, Class<N> notificationType, Consumer<N> consumer) {
        stepManager.add(name, new ValidationStep<>(notificationType, notificationCache, consumer));
    }

    private String nextId() {
        return StringIdGenerator.getId(ValidationStep.class).id;
    }

}
