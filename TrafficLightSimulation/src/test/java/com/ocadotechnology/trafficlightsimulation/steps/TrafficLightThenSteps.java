package com.ocadotechnology.trafficlightsimulation.steps;

import com.ocadotechnology.scenario.AbstractThenSteps;
import com.ocadotechnology.scenario.NotificationCache;
import com.ocadotechnology.scenario.StepManager;
import com.ocadotechnology.scenario.StepManager.CheckStepExecutionType;
import com.ocadotechnology.trafficlightsimulation.controller.TrafficLightChangedNotification;
import com.ocadotechnology.trafficlightsimulation.controller.TrafficLightController.State;

public class TrafficLightThenSteps extends AbstractThenSteps<TrafficLightThenSteps> {

    private TrafficLightThenSteps(StepManager stepManager, NotificationCache notificationCache, CheckStepExecutionType checkStepExecutionType, boolean isFailingStep) {
        super(stepManager, notificationCache, checkStepExecutionType, isFailingStep);
    }

    public TrafficLightThenSteps(StepManager stepManager, NotificationCache notificationCache) {
        this(stepManager, notificationCache, CheckStepExecutionType.ordered(), false);
    }

    @Override
    protected TrafficLightThenSteps create(StepManager stepManager, NotificationCache notificationCache, CheckStepExecutionType checkStepExecutionType, boolean isFailingStep) {
        return new TrafficLightThenSteps(stepManager, notificationCache, checkStepExecutionType, isFailingStep);
    }

    public void changesStateTo(State state) {
        addCheckStep(TrafficLightChangedNotification.class, notification -> state.equals(notification.newState));
    }
}
