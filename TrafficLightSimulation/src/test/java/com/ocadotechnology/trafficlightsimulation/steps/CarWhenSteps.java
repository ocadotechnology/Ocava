package com.ocadotechnology.trafficlightsimulation.steps;

import com.ocadotechnology.scenario.AbstractWhenSteps;
import com.ocadotechnology.scenario.StepManager;
import com.ocadotechnology.trafficlightsimulation.TrafficSimulationApi;

public class CarWhenSteps extends AbstractWhenSteps {
    private final StepManager stepManager;
    private final TrafficSimulationApi simulationApi;

    public CarWhenSteps(StepManager stepManager, TrafficSimulationApi simulationApi) {
        super(stepManager);
        this.stepManager = stepManager;
        this.simulationApi = simulationApi;
    }

    public void arrives() {
        stepManager.addExecuteStep(() -> simulationApi.getTrafficSimulation().getVehicleSimulation().carArrivesAtJunction());
    }
}
