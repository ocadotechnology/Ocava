package com.ocadotechnology.trafficlightsimulation.glue;

import com.ocadotechnology.scenario.CoreSimulationWhenSteps;
import com.ocadotechnology.scenario.NotificationCache;
import com.ocadotechnology.scenario.ScenarioNotificationListener;
import com.ocadotechnology.scenario.StepManager;
import com.ocadotechnology.trafficlightsimulation.TrafficSimulationApi;
import com.ocadotechnology.trafficlightsimulation.steps.CarWhenSteps;
import com.ocadotechnology.trafficlightsimulation.steps.SimulationWhenSteps;
import com.ocadotechnology.trafficlightsimulation.steps.TrafficLightWhenSteps;

public class When {

    //generic steps
    public final SimulationWhenSteps simulation;

    //simulation specific steps
    public final CarWhenSteps car;
    public final TrafficLightWhenSteps trafficLight;

    public When(StepManager stepManager, TrafficSimulationApi simulationApi, ScenarioNotificationListener listener, NotificationCache notificationCache) {
        CoreSimulationWhenSteps coreSteps = new CoreSimulationWhenSteps(stepManager, simulationApi, listener, notificationCache);

        simulation = new SimulationWhenSteps(stepManager, coreSteps);
        car = new CarWhenSteps(stepManager, simulationApi);
        trafficLight = new TrafficLightWhenSteps(stepManager, simulationApi);
    }
}
