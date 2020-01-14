package com.ocadotechnology.trafficlightsimulation.steps;

import org.slf4j.LoggerFactory;

import com.ocadotechnology.scenario.AbstractThenSteps;
import com.ocadotechnology.scenario.NotificationCache;
import com.ocadotechnology.scenario.StepManager;
import com.ocadotechnology.scenario.StepManager.CheckStepExecutionType;
import com.ocadotechnology.trafficlightsimulation.SimulationEndedNotification;

public class SimulationThenSteps extends AbstractThenSteps<SimulationThenSteps> {

    private SimulationThenSteps(StepManager stepManager, NotificationCache notificationCache, CheckStepExecutionType checkStepExecutionType, boolean isFailingStep) {
        super(stepManager, notificationCache, checkStepExecutionType, isFailingStep);
    }

    public SimulationThenSteps(StepManager stepManager, NotificationCache notificationCache) {
        this(stepManager, notificationCache, CheckStepExecutionType.ordered(), false);
    }

    @Override
    protected SimulationThenSteps create(StepManager stepManager, NotificationCache notificationCache, CheckStepExecutionType checkStepExecutionType, boolean isFailingStep) {
        return new SimulationThenSteps(stepManager, notificationCache, checkStepExecutionType, isFailingStep);
    }

    /**
     * Generally, we should avoid using this step. We should mostly be stopping on meeting
     * some test condition (i.e. physical outcome, message sent). This way, the test is more robust, and runs faster.
     * Some tests do require the simulation to run to the end, such as tests that graph over a specific time period etc.
     * These sorts of tests can use this step, specifying the reason that the simulation must run to completion.
     */
    public void hasFinished(String reason) {
        addCheckStep(SimulationEndedNotification.class, notification -> {
            LoggerFactory.getLogger(SimulationThenSteps.class).info("Simulation ended, {}", reason);
            return true;
        });
    }
}
