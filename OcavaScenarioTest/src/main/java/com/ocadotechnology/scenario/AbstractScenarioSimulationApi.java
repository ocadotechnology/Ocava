/*
 * Copyright © 2017-2022 Ocado (Ocava)
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
import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.notification.Notification;
import com.ocadotechnology.notification.NotificationBus;
import com.ocadotechnology.notification.NotificationRouter;
import com.ocadotechnology.simulation.Simulation;

public abstract class AbstractScenarioSimulationApi<S extends Simulation> extends Cleanable implements ScenarioSimulationApi<S> {

    private boolean started = false;

    protected EventScheduler scheduler;
    private double timeout = -1;

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public EventScheduler getEventScheduler() {
        return scheduler;
    }

    @Override
    public void setDiscreteEventTimeout(double duration, TimeUnit unit) {
        Preconditions.checkState(!started, "Attempted to set timeout after startup");
        timeout = TimeThenSteps.convertToUnit(duration, unit, getSchedulerTimeUnit());
    }

    @Override
    public void start(ScenarioNotificationListener listener) {
        started = true;

        scheduler = createScheduler();

        Preconditions.checkState(
                ScenarioTestSchedulerType.INSTANCE.equals(scheduler.getType()),
                "Scenario scheduler created with the wrong type (%s) - it wouldn't receive messages",
                scheduler.getType());

        NotificationRouter.get().registerExecutionLayer(scheduler, createNotificationBus());

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
     * Creates a scheduler of type ScenarioTestSchedulerType.INSTANCE for use by the framework.
     */
    protected abstract EventScheduler createScheduler();

    protected abstract void startSimulation();

    @Override
    public void clean() {
        started = false;
        scheduler = null;
    }
}
