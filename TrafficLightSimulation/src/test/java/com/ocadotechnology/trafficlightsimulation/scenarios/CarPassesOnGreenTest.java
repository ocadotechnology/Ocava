package com.ocadotechnology.trafficlightsimulation.scenarios;

import static com.ocadotechnology.trafficlightsimulation.controller.TrafficLightController.State.GREEN;

import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.Story;
import com.ocadotechnology.trafficlightsimulation.TrafficConfig.TrafficLight;
import com.ocadotechnology.trafficlightsimulation.TrafficSimulationStory;

@Story
class CarPassesOnGreenTest extends TrafficSimulationStory {

    @Test
    void scenario() {
        //we set traffic light to be GREEN initially
        given.config.set(TrafficLight.INITIAL_STATE, GREEN);

        when.simulation.starts();

        //when we add a car, it should leave
        when.car.arrives();
        then.car.leaves();
    }
}
