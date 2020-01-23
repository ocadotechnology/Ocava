/*
 * Copyright © 2017-2020 Ocado (Ocava)
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

import org.junit.jupiter.api.Assertions;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.event.scheduling.EventSchedulerType;
import com.ocadotechnology.notification.Notification;
import com.ocadotechnology.notification.NotificationBus;
import com.ocadotechnology.notification.NotificationRouter;

public abstract class AbstractScenarioSimulationApi extends Cleanable implements ScenarioSimulationApi {

    private boolean started = false;

    protected ImmutableMap<EventSchedulerType, EventScheduler> schedulers;
    private double timeout = -1;

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public EventScheduler getEventScheduler() {
        return schedulers.get(ScenarioTestSchedulerType.INSTANCE);
    }

    @Override
    public void setDiscreteEventTimeout(double duration, TimeUnit unit) {
        Preconditions.checkState(!started, "Attempted to set timeout after startup");
        timeout = TimeThenSteps.convertToUnit(duration, unit, getSchedulerTimeUnit());
    }

    @Override
    public void start(ScenarioNotificationListener listener) {
        started = true;

        schedulers = createSchedulers();

        Preconditions.checkState(schedulers.containsKey(ScenarioTestSchedulerType.INSTANCE),
                "Must create ScenarioTestSchedulerType");
        Preconditions.checkState(schedulers.get(ScenarioTestSchedulerType.INSTANCE).getType() == ScenarioTestSchedulerType.INSTANCE,
                "Scenario scheduler created with the wrong type (%s) - it wouldn't receive messages",
                schedulers.get(ScenarioTestSchedulerType.INSTANCE).getType());

        NotificationRouter.get().registerExecutionLayer(schedulers.get(ScenarioTestSchedulerType.INSTANCE), createNotificationBus());

        //This call should actually start the scheduler and is not expected to return as startSimulation should trigger the then step which continues the process.
        //See com.ocadotechnology.scenario.CoreSimulationWhenSteps.starts
        simulationExecutor(() -> {
            startListener(listener);
            if (timeout > 0) {
                getEventScheduler().doAt(getSchedulerStartTime() + timeout, () -> Assertions.fail("Discrete event timeout reached"));
            }
            startSimulation();
        });
    }

    /**
     * Can be overridden to one that does not use scheduler.doNow()
     */
    protected void simulationExecutor(Runnable execute)  {
        getEventScheduler().doNow(execute);
    }

    protected void startListener(ScenarioNotificationListener listener) {
        getEventScheduler().doNow(listener::subscribeForNotifications);
    }

    protected NotificationBus<Notification> createNotificationBus() {
        return new ScenarioBus();
    }

    /**
     * Create the schedulers used in the simulation, plus a ScenarioTestSchedulerType.INSTANCE scheduler.
     */
    protected abstract ImmutableMap<EventSchedulerType, EventScheduler> createSchedulers();

    protected abstract void startSimulation();

    @Override
    public void clean() {
        started = false;
        schedulers = null;
    }
}
