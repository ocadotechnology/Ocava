/*
 * Copyright © 2017 Ocado (Ocava)
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

import com.ocadotechnology.scenario.StepManager.CheckStepExecutionType;

public class TestThen {

    private StepManager stepManager;
    private NotificationCache notificationCache;
    private TimeThenSteps timeThenSteps;

    public final UnorderedSteps unordered;

    public TestThen(StepManager stepManager, NotificationCache notificationCache, FrameworkTestSimulationApi simulationApi, ScenarioNotificationListener listener) {
        this.stepManager = stepManager;
        this.notificationCache = notificationCache;
        this.timeThenSteps = new TimeThenSteps(simulationApi, stepManager, listener);
        this.unordered = new UnorderedSteps(stepManager.getStepsCache(), stepManager);
    }

    public TestEventThenSteps testEvent() {
        return new TestEventThenSteps(stepManager, notificationCache,
                CheckStepExecutionType.ordered());
    }

    public TimeThenSteps timeSteps() {
        return timeThenSteps;
    }

    public static class TestEventThenSteps extends AbstractThenSteps<TestEventThenSteps> {

        private TestEventThenSteps(StepManager stepManager, NotificationCache notificationCache, CheckStepExecutionType checkStepExecutionType) {
            super(stepManager, notificationCache, checkStepExecutionType);
        }

        @Override
        protected TestEventThenSteps create(StepManager stepManager, NotificationCache notificationCache, CheckStepExecutionType executionType) {
            return new TestEventThenSteps(stepManager, notificationCache, executionType);
        }

        public void occurs(String name) {
            addCheckStep(TestEventNotification.class, n -> n.name.equals(name));
        }

        public void doesNotOccurInCaches(String name) {
            addCheckStep(new SingleCheckStep<>(
                    TestEventNotification.class,
                    stepManager.notificationCache, n -> {
                        if (n.name.equals(name)) {
                            throw new RuntimeException("We should not receive TestEventNotification with message: " + name);
                        }
                        return true;
                    }));
        }
    }

    public ExceptionThenSteps exception() {
        return new ExceptionThenSteps(stepManager);
    }
}
