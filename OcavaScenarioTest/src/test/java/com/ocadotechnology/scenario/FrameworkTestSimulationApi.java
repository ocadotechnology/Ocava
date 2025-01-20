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
package com.ocadotechnology.scenario;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.ocadotechnology.event.scheduling.EventExecutor;
import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.event.scheduling.SimpleDiscreteEventScheduler;
import com.ocadotechnology.notification.Notification;
import com.ocadotechnology.notification.NotificationBus;
import com.ocadotechnology.notification.NotificationRouter;
import com.ocadotechnology.time.AdjustableTimeProvider;

public class FrameworkTestSimulationApi extends AbstractScenarioSimulationApi<FrameworkTestSimulation> {
    private static final Logger logger = LoggerFactory.getLogger(FrameworkTestSimulationApi.class);

    private SimpleDiscreteEventScheduler eventScheduler;
    private double schedulerStartTime = 0.0;

    @Override
    public void clean() {
        eventScheduler = null;
        super.clean();
    }

    public void setSchedulerStartTime(double startTime) {
        schedulerStartTime = startTime;
    }

    private void createCleanScheduler() {
        eventScheduler = new SimpleDiscreteEventScheduler(
            new EventExecutor(),
            () -> logger.info("Simulation Terminated"),
            ScenarioTestSchedulerType.INSTANCE,
            new AdjustableTimeProvider(schedulerStartTime));
    }

    @Override
    public SimpleDiscreteEventScheduler getEventScheduler() {
        return eventScheduler;
    }

    @Override
    protected void startSimulation() {
        eventScheduler.doNow(() -> NotificationRouter.get().broadcast(TestSimulationStarts.INSTANCE), "Startup complete event");
        eventScheduler.unPause();
    }

    @Override
    protected void exitSimulation() {
        // nop
    }

    @Override
    protected EventScheduler createScheduler() {
        createCleanScheduler();
        return eventScheduler;
    }

    @Override
    protected NotificationBus<Notification> createNotificationBus() {
        return new ScenarioBus();
    }

    @Override
    public double getSchedulerStartTime() {
        return schedulerStartTime;
    }

    @Override
    public TimeUnit getSchedulerTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

    @Override
    public FrameworkTestSimulation getSimulation() {
        return new FrameworkTestSimulation(Preconditions.checkNotNull(eventScheduler));
    }
}
