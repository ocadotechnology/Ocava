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

import java.util.concurrent.TimeUnit;

@StoryContent
public abstract class AbstractFrameworkTestStory extends AbstractStory {

    public final TestWhen when;
    public final TestThen then;
    public final TestGiven given;

    private AbstractFrameworkTestStory(FrameworkTestSimulationApi simulationApi) {
        super(simulationApi);

        when = new TestWhen(stepManager, simulationApi, listener, notificationCache);
        then = new TestThen(stepManager, notificationCache, simulationApi, listener);
        given = new TestGiven(stepsRunner, simulationApi);
        stepsRunner.setPostStepsRunTime(10, TimeUnit.MILLISECONDS);
    }

    protected AbstractFrameworkTestStory() {
        this(new FrameworkTestSimulationApi());
    }
}
