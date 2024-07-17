/*
 * Copyright © 2017-2024 Ocado (Ocava)
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

import java.util.List;

import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.notification.NotificationRouter;

public class TestEventWhenSteps extends AbstractWhenSteps<FrameworkTestSimulation, TestEventWhenSteps>{

    public TestEventWhenSteps(StepManager<FrameworkTestSimulation> stepManager) {
        this(stepManager, NamedStepExecutionType.ordered());
    }

    private TestEventWhenSteps(StepManager<FrameworkTestSimulation> stepManager, NamedStepExecutionType namedStepExecutionType) {
        super(stepManager, namedStepExecutionType);
    }

    @Override
    protected TestEventWhenSteps create(StepManager<FrameworkTestSimulation> stepManager, NamedStepExecutionType executionType) {
        return new TestEventWhenSteps(stepManager, executionType);
    }

    public <T> StepFuture<T> populateFuture(T value) {
        MutableStepFuture<T> future = new MutableStepFuture<>();
        addExecuteStep(() -> future.populate(value));
        return future;
    }

    public StepFuture<List<String>> broadcastThenPopulateFuture(String... values) {
        MutableStepFuture<List<String>> future = new MutableStepFuture<>();
        addExecuteStep(() -> {
            List<String> valuesList = List.of(values);
            valuesList.forEach(v -> NotificationRouter.get().broadcast(new TestEventNotification(v)));
            future.populate(valuesList);
        });
        return future;
    }

    public StepFuture<Double> scheduled(double time, String name) {
        return scheduled(time, new TestEventNotification(name));
    }

    public StepFuture<Double> scheduled(double time, TestEventNotification notification) {
        MutableStepFuture<Double> scheduleTime = new MutableStepFuture<>();
        addExecuteStep(() -> {
            EventScheduler eventScheduler = getSimulation().getEventScheduler();
            scheduleTime.populate(time - eventScheduler.getTimeProvider().getTime());
            eventScheduler.doAt(
                        time,
                        () -> NotificationRouter.get().broadcast(notification),
                        "scheduled(" + time + ", \"" + notification.name + "\")");
        });
        return scheduleTime;
    }

    public StepFuture<Double> scheduledIn(double delay, String name) {
        MutableStepFuture<Double> scheduleTime = new MutableStepFuture<>();
        addExecuteStep(() -> {
            EventScheduler eventScheduler = getSimulation().getEventScheduler();
            double eventTime = eventScheduler.getTimeProvider().getTime() + delay;
            scheduleTime.populate(delay - eventScheduler.getTimeProvider().getTime());
            eventScheduler.doAt(
                        eventTime,
                        () -> NotificationRouter.get().broadcast(new TestEventNotification(name)),
                        "scheduledIn(" + delay + ", \"" + name + "\")");
        });
        return scheduleTime;
    }
}
