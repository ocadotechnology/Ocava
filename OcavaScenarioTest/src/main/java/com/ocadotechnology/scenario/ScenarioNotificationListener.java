/*
 * Copyright © 2017-2023 Ocado (Ocava)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.ocadotechnology.event.scheduling.EventSchedulerType;
import com.ocadotechnology.notification.Notification;
import com.ocadotechnology.notification.Subscriber;

public class ScenarioNotificationListener extends Cleanable implements Subscriber {
    private final Logger logger = LoggerFactory.getLogger(ScenarioNotificationListener.class);
    private final NotificationCache notificationCache;
    private final StepsRunner stepsRunner;

    private boolean onlyUnordered = false;

    public ScenarioNotificationListener(NotificationCache notificationCache, StepsRunner stepsRunner) {
        this.notificationCache = notificationCache;
        this.stepsRunner = stepsRunner;
    }

    public void suspend() {
        onlyUnordered = true;
    }

    public void resume() {
        notificationCache.resetUnorderedNotification();
        notificationCache.getNotificationAndReset();
        onlyUnordered = false;
    }

    @Override
    public EventSchedulerType getSchedulerType() {
        return ScenarioTestSchedulerType.INSTANCE;
    }

    @Subscribe
    public void handleNotification(Notification notification) {
        if (notificationCache.knownNotification(notification)) {
            logger.debug("Received notification {}", notification);
            notificationCache.set(notification);

            tryToExecuteNextStep(onlyUnordered);
        }
    }

    @Override
    public void clean() {
        onlyUnordered = false;
    }

    public void tryToExecuteNextStep(boolean onlyUnordered) {
        stepsRunner.tryToExecuteNextStep(onlyUnordered);
    }
}
