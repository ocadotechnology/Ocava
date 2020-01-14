package com.ocadotechnology.trafficlightsimulation.glue;

import com.ocadotechnology.scenario.NotificationCache;
import com.ocadotechnology.scenario.ScenarioNotificationListener;
import com.ocadotechnology.scenario.StepManager;
import com.ocadotechnology.scenario.TimeThenSteps;
import com.ocadotechnology.scenario.UnorderedSteps;
import com.ocadotechnology.trafficlightsimulation.TrafficSimulationApi;
import com.ocadotechnology.trafficlightsimulation.steps.CarThenSteps;
import com.ocadotechnology.trafficlightsimulation.steps.SimulationThenSteps;
import com.ocadotechnology.trafficlightsimulation.steps.TrafficLightThenSteps;

public class Then {

    //generic steps
    public final SimulationThenSteps simulation;
    public final TimeThenSteps time;
    public final UnorderedSteps unordered;

    //simulation specific steps
    public final CarThenSteps car;
    public final TrafficLightThenSteps trafficLight;

    public Then(StepManager stepManager, TrafficSimulationApi simulationApi, ScenarioNotificationListener listener, NotificationCache notificationCache) {

        this.simulation = new SimulationThenSteps(stepManager, notificationCache);
        this.time = new TimeThenSteps(simulationApi, stepManager, listener);

        this.unordered = new UnorderedSteps(stepManager);
        this.car = new CarThenSteps(stepManager, notificationCache);
        trafficLight = new TrafficLightThenSteps(stepManager, notificationCache);
    }
}
