package com.ocadotechnology.trafficlightsimulation.scenarios;

import static com.ocadotechnology.trafficlightsimulation.controller.TrafficLightController.State.GREEN;
import static com.ocadotechnology.trafficlightsimulation.controller.TrafficLightController.State.RED;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.ocadotechnology.scenario.Story;
import com.ocadotechnology.trafficlightsimulation.TrafficConfig.TrafficLight;
import com.ocadotechnology.trafficlightsimulation.TrafficSimulationStory;
import com.ocadotechnology.trafficlightsimulation.controller.TrafficLightController;
import com.ocadotechnology.trafficlightsimulation.controller.TrafficLightController.State;

@Story
class TrafficLightsChangeTest extends TrafficSimulationStory {

    private static final String CHANGED = "CHANGED";

    @ParameterizedTest
    @EnumSource(TrafficLightController.State.class) //will be run with RED and GREEN
    void scenarioWithWaitForSteps(State initialState) {
        //setting the initial state - RED/GREEN
        given.config.set(TrafficLight.INITIAL_STATE, initialState);

        //setting traffic light to switch between RED and GREEN automatically
        given.config.set(TrafficLight.ENABLE_AUTOMATIC_CHANGE, true);

        when.simulation.starts();

        //define two steps to be executed in any order
        then.trafficLight.unordered(CHANGED).changesStateTo(RED);
        then.trafficLight.unordered(CHANGED).changesStateTo(GREEN);

        //wait for all steps labelled "CHANGED" to finish
        then.unordered.waitForSteps(CHANGED);

        //add other steps here that will be executed specifically after the `then.unordered.waitForSteps` step above
    }

    @ParameterizedTest
    @EnumSource(TrafficLightController.State.class) //will be run with RED and GREEN
    void scenarioWithoutWaitForSteps(State initialState) {
        //setting the initial state - RED/GREEN
        given.config.set(TrafficLight.INITIAL_STATE, initialState);

        //setting traffic light to switch between RED and GREEN automatically
        given.config.set(TrafficLight.ENABLE_AUTOMATIC_CHANGE, true);

        when.simulation.starts();

        //define two steps to be executed in any order
        then.trafficLight.unordered(CHANGED).changesStateTo(RED);
        then.trafficLight.unordered(CHANGED).changesStateTo(GREEN);

        //if the unordered steps are the last two steps in the test, we do not need waitForSteps,
        //scenario test framework will wait for outstanding unordered steps by default
    }
}