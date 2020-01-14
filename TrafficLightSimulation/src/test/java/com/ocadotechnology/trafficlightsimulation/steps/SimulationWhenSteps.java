package com.ocadotechnology.trafficlightsimulation.steps;

import com.ocadotechnology.scenario.AbstractWhenSteps;
import com.ocadotechnology.scenario.CoreSimulationWhenSteps;
import com.ocadotechnology.scenario.StepManager;
import com.ocadotechnology.trafficlightsimulation.TrafficSimulationStartedNotification;

public class SimulationWhenSteps extends AbstractWhenSteps {
    private CoreSimulationWhenSteps coreSteps;

    public SimulationWhenSteps(StepManager stepManager, CoreSimulationWhenSteps coreSteps) {
        super(stepManager);
        this.coreSteps = coreSteps;
    }

    public void starts() {
        coreSteps.starts(TrafficSimulationStartedNotification.class);
    }
}
