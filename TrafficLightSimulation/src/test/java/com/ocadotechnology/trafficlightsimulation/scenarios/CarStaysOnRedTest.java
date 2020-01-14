package com.ocadotechnology.trafficlightsimulation.scenarios;

import static com.ocadotechnology.trafficlightsimulation.controller.TrafficLightController.State.GREEN;
import static com.ocadotechnology.trafficlightsimulation.controller.TrafficLightController.State.RED;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.ocadotechnology.scenario.Story;
import com.ocadotechnology.trafficlightsimulation.TrafficConfig.TrafficLight;
import com.ocadotechnology.trafficlightsimulation.TrafficSimulationStory;

@Story
class CarStaysOnRedTest extends TrafficSimulationStory {

    private static final String YOU_SHALL_NOT_PASS = "YOU_SHALL_NOT_PASS";

    @Test
    void scenario() {
        //we set traffic light to be RED initially
        given.config.set(TrafficLight.INITIAL_STATE, RED);
        //making sure traffic light stays RED unless we tell it to change
        given.config.set(TrafficLight.ENABLE_AUTOMATIC_CHANGE, false);

        when.simulation.starts();
        when.car.arrives();

        //car should not pass on RED
        then.car.never(YOU_SHALL_NOT_PASS).leaves();

        //waiting for a while, so we are sure car is not passing
        then.time.waitForDuration(2, TimeUnit.MINUTES);

        //change traffic light to green, car can now leave
        when.trafficLight.isChangedTo(GREEN);

        //remove the YOU_SHALL_NOT_PASS restriction, as we now want the car to move
        then.unordered.removesUnorderedSteps(YOU_SHALL_NOT_PASS);

        //car leaves
        then.car.leaves();
    }
}