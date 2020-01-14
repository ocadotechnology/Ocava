package com.ocadotechnology.trafficlightsimulation.steps;

import com.ocadotechnology.scenario.AbstractWhenSteps;
import com.ocadotechnology.scenario.StepManager;
import com.ocadotechnology.trafficlightsimulation.TrafficSimulationApi;
import com.ocadotechnology.trafficlightsimulation.controller.TrafficLightController.State;

public class TrafficLightWhenSteps extends AbstractWhenSteps {
    private final TrafficSimulationApi simulationApi;

    public TrafficLightWhenSteps(StepManager stepManager, TrafficSimulationApi simulationApi) {
        super(stepManager);
        this.simulationApi = simulationApi;
    }

    public void isChangedTo(State state) {
        addExecuteStep(() -> simulationApi.getTrafficSimulation().getTrafficLightController().changeState(state));
    }
}
