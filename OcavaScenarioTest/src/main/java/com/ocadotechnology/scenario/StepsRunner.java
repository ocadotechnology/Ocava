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

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StepsRunner extends Cleanable {
    private static final Logger logger = LoggerFactory.getLogger(StepsRunner.class);

    private StepCache stepsCache;
    private ScenarioSimulationApi simulation;

    /**
     * Required to latch the stopping of the simulation otherwise a notification will prompt another stop
     */
    private boolean stopped = false;

    private Executable currentStep;
    private Executable currentUnorderedStep;

    private double postStepsRunTime = 100;

    private boolean hasWallClockTimeout;
    private long trackedWallClockStartTime;
    private long wallClockTimeoutMillis;

    public StepsRunner(StepCache stepsCache, ScenarioSimulationApi simulation) {
        this.stepsCache = stepsCache;
        this.simulation = simulation;
    }

    public void setPostStepsRunTime(long duration, TimeUnit timeUnit) {
        this.postStepsRunTime = simulation.getSchedulerTimeUnit().convert(duration, timeUnit);
    }

    public void setWallClockTimeout(long duration, TimeUnit unit) {
        hasWallClockTimeout = true;
        trackedWallClockStartTime = System.currentTimeMillis();
        wallClockTimeoutMillis = unit.toMillis(duration);
    }

    public boolean isFinished() {
        return stepsCache.isFinished() && isCurrentStepFinished();
    }

    private boolean isCurrentStepFinished() {
        return currentStep == null || currentStep.isFinished();
    }

    public Executable getUnfinishedStep() {
        if (currentUnorderedStep != null) {
            return currentUnorderedStep;
        }
        if (currentStep != null) {
            return currentStep;
        }
        return stepsCache.getUnfinishedUnorderedStep();
    }

    public void tryToExecuteNextStep(boolean onlyUnordered) {
        checkForTimeout();
        if (simulation.isStarted()) {
            executeUnorderedSteps();
        }
        if (onlyUnordered) {
            return;
        }

        while (true) {
            progressStepIfAble();

            if (currentStep == null) {
                break;
            }

            currentStep.executeAndLog();

            if (!isNextStep()) {
                break;
            }

            // Check steps should only be executed off the back of a notification, which will call tryToExecuteNextStep
            // Otherwise, they may pick up on old notifications in the NotificationCache.
            // Advance to the next step anyway, otherwise if the test fails, the wrong step would be reported as the problem.
            progressStepIfAble();
            if (currentStep instanceof CheckStep) {
                break;
            }
        }

        if (isFinished() && !stopped) {
            // The test has finished - stop the simulation, but give it 5s to catch any failures immediately following the test
            stopped = true;

            logger.info("Last Step finished. Continuing simulation for {} {} to check that we aren't about to fail", postStepsRunTime, simulation.getSchedulerTimeUnit());

            simulation.getEventScheduler().doIn(
                    postStepsRunTime,
                    () -> {
                        logger.info("Scenario test complete");
                        simulation.getEventScheduler().stop();
                    },
                    "Scenario test stop event");
        }
    }

    private void checkForTimeout() {
        if (hasWallClockTimeout) {
            Assertions.assertFalse(System.currentTimeMillis() - trackedWallClockStartTime > wallClockTimeoutMillis, "Wall clock timeout exceeded");
        }
    }

    private void progressStepIfAble() {
        if (isNextStep()) {
            currentStep = stepsCache.getNextStep();
        }
    }

    private void executeUnorderedSteps() {
        Iterator<Executable> iterator = stepsCache.getUnorderedSteps().iterator();
        while (iterator.hasNext()) {
            currentUnorderedStep = iterator.next();
            currentUnorderedStep.executeAndLog();
            if (currentUnorderedStep.isFinished()) {
                iterator.remove();
            }
        }
        currentUnorderedStep = null;
    }

    private boolean isNextStep() {
        return currentStep == null || currentStep.isFinished();
    }

    @Override
    public void clean() {
        stopped = false;
        currentStep = null;
        currentUnorderedStep = null;
    }

    public boolean validateException(Throwable exception) {
        return stepsCache.getExceptionChecker().test(exception);
    }
}
