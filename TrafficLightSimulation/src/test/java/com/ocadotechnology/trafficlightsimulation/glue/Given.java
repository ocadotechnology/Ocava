package com.ocadotechnology.trafficlightsimulation.glue;

import com.ocadotechnology.scenario.NotificationCache;
import com.ocadotechnology.scenario.ScenarioNotificationListener;
import com.ocadotechnology.scenario.StepManager;
import com.ocadotechnology.trafficlightsimulation.TrafficSimulationApi;
import com.ocadotechnology.trafficlightsimulation.steps.ConfigGivenSteps;

public class Given {

    public ConfigGivenSteps config;

    public Given(StepManager stepManager, TrafficSimulationApi simulationApi, ScenarioNotificationListener listener, NotificationCache notificationCache) {
        config = new ConfigGivenSteps(stepManager, simulationApi.getConfigMap());
    }
}
