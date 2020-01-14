package com.ocadotechnology.trafficlightsimulation;

import java.util.concurrent.TimeUnit;

import com.ocadotechnology.scenario.AbstractStory;
import com.ocadotechnology.trafficlightsimulation.TrafficConfig.Vehicles;
import com.ocadotechnology.trafficlightsimulation.glue.Given;
import com.ocadotechnology.trafficlightsimulation.glue.Then;
import com.ocadotechnology.trafficlightsimulation.glue.When;

public class TrafficSimulationStory extends AbstractStory {

    protected final Given given; // Allows stories to use given steps
    protected final When when; // Allows stories to use when steps
    protected final Then then; // Allows stories to use then steps

    private TrafficSimulationStory(TrafficSimulationApi simulationApi) {
        super(simulationApi);
        given = new Given(stepManager, simulationApi, listener, notificationCache);
        when = new When(stepManager, simulationApi, listener, notificationCache);
        then = new Then(stepManager, simulationApi, listener, notificationCache);

        //continue simulation for a short time to check we are not about to fail
        stepsRunner.setPostStepsRunTime(10, TimeUnit.MILLISECONDS);
    }

    @Override
    public void init() {
        super.init();

        defaultConfiguration();
    }

    private void defaultConfiguration() {
        given.config.set(Vehicles.ENABLE_RANDOM_ARRIVAL, false);
        given.config.set(Vehicles.INITIAL_VEHICLES, 0);
    }

    protected TrafficSimulationStory() {
        this(new TrafficSimulationApi());
    }
}
