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

import java.util.concurrent.TimeUnit;

/**
 * Steps class presenting a mechanism to define a simulation or wall clock timeout through the steps interface
 */
public class TimeoutGivenSteps {
    private final StepsRunner runner;
    private final ScenarioSimulationApi simulation;

    public TimeoutGivenSteps(StepsRunner runner, ScenarioSimulationApi simulation) {
        this.runner = runner;
        this.simulation = simulation;
    }

    /**
     * Adds a time limit to the test which is checked against the scheduler time of the system.  By default this will be
     * implemented as a scheduled event on the scenario scheduler.
     */
    public void addSimulationTimeout(double duration, TimeUnit unit) {
        simulation.setDiscreteEventTimeout(duration, unit);
    }

    /**
     * Adds a time limit to the test which is checked against the System time of the system.  This is implemented as a
     * check for timeout every time a new notification is processed by the scenario test.  This guarantees that the test
     * will fail if it takes too long, but does not present a mechanism to interrupt the test if it gets stuck in a long
     * loop.
     */
    public void addWallClockTimeout(long duration, TimeUnit unit) {
        runner.setWallClockTimeout(duration, unit);
    }
}
