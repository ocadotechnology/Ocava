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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StepsRunner extends Cleanable {
    private static final Logger logger = LoggerFactory.getLogger(StepsRunner.class);

    private final StepCache stepsCache;
    private final ScenarioSimulationApi<?> simulation;

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

    private boolean isExecutingStepCycle = false;
    private Queue<Runnable> runnableQueue = new LinkedList<>();

    public StepsRunner(StepCache stepsCache, ScenarioSimulationApi simulation) {
        this.stepsCache = stepsCache;
        this.simulation = simulation;
    }

    public void setPostStepsRunTime(double duration, TimeUnit timeUnit) {
        this.postStepsRunTime = TimeThenSteps.convertToUnit(duration, timeUnit, simulation.getSchedulerTimeUnit());
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

    /**
     * Executes as many steps as possible. Does not prevent steps from being interrupted by another invocation.
     * This is deliberate since the code which starts a simulation is not expected to return until the simulation (and
     * thus test) has run to completion.
     */
    public void executeNextStepsUnblocking() {
        executeNextSteps();
    }

    /**
     * Attempts to execute as many steps as possible. If steps are already executing, this will queue another execution
     * of the cycle to occur after this one is complete.
     *
     * @param runnable - A task to execute before entering this cycle. Typically used to populate the notification cache
     */
    public void tryToExecuteNextSteps(Runnable runnable) {
        runnableQueue.add(runnable);
        if (isExecutingStepCycle) {
            return;
        }

        while (!runnableQueue.isEmpty()) {
            isExecutingStepCycle = true;
            try {
                runnableQueue.poll().run();
                executeNextSteps();
            } finally {
                isExecutingStepCycle = false;
            }
        }
    }

    private void executeNextSteps() {
        checkForTimeout();
        executeStepCycle();

        if (isFinished() && !stopped) {
            stopSimulation();
        }
    }

    private void executeStepCycle() {
        if (simulation.isStarted()) {
            executeUnorderedSteps();
        }

        while (true) {
            progressStepIfAble();

            if (currentStep == null) {
                break;
            }

            currentStep.executeAndLog();

            if (!isCurrentStepComplete()) {
                break;
            }

            progressStepIfAble();
            // Check steps should only be executed off the back of a notification, which will call tryToExecuteNextStep
            // Otherwise, they may pick up on old notifications in the NotificationCache.
            if (currentStep instanceof CheckStep) {
                break;
            }
        }
    }

    private void stopSimulation() {
        // The test has finished - stop the simulation, but give it 5s to catch any failures immediately following the test
        stopped = true;

        logger.info("Last Step finished. Continuing simulation for {} {} to check that we aren't about to fail", postStepsRunTime, simulation.getSchedulerTimeUnit());

        simulation.getEventScheduler().doIn(
                postStepsRunTime,
                () -> {
                    // The scenario test hasn't included "end simulation" step(s),
                    // so we're effectively hard-stopping at a (random) point in time.
                    // We can/should leave "proper" shutdown to post-test cleanup.
                    logger.info("Scenario test complete");
                    simulation.getEventScheduler().stop();
                },
                "Scenario test stop event");
    }

    private void checkForTimeout() {
        if (hasWallClockTimeout) {
            Assertions.assertFalse(System.currentTimeMillis() - trackedWallClockStartTime > wallClockTimeoutMillis, "Wall clock timeout exceeded");
        }
    }

    private void progressStepIfAble() {
        if (isCurrentStepComplete()) {
            currentStep = stepsCache.getNextStep();
            if (currentStep != null) {
                currentStep.setActive();
            }
        }
    }

    private void executeUnorderedSteps() {
        Iterator<Executable> iterator = stepsCache.getUnorderedStepsIterator();
        while (iterator.hasNext()) {
            currentUnorderedStep = iterator.next();
            currentUnorderedStep.executeAndLog();
            if (currentUnorderedStep.isFinished()) {
                iterator.remove();
            }
        }
        currentUnorderedStep = null;
    }

    private boolean isCurrentStepComplete() {
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
