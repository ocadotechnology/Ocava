package com.ocadotechnology.trafficlightsimulation.steps;

import org.slf4j.LoggerFactory;

import com.ocadotechnology.scenario.AbstractThenSteps;
import com.ocadotechnology.scenario.NotificationCache;
import com.ocadotechnology.scenario.StepManager;
import com.ocadotechnology.scenario.StepManager.CheckStepExecutionType;
import com.ocadotechnology.trafficlightsimulation.simulation.CarLeavesNotification;

public class CarThenSteps extends AbstractThenSteps<CarThenSteps> {

    public CarThenSteps(StepManager stepManager, NotificationCache notificationCache) {
        this(stepManager, notificationCache, CheckStepExecutionType.ordered(), false);
    }

    private CarThenSteps(StepManager stepManager, NotificationCache notificationCache, CheckStepExecutionType checkStepExecutionType, boolean isFailingStep) {
        super(stepManager, notificationCache, checkStepExecutionType, isFailingStep);
    }

    @Override
    protected CarThenSteps create(StepManager stepManager, NotificationCache notificationCache, CheckStepExecutionType checkStepExecutionType, boolean isFailingStep) {
        return new CarThenSteps(stepManager, notificationCache, checkStepExecutionType, isFailingStep);
    }

    public void leaves() {
        addCheckStep(CarLeavesNotification.class, notification -> {
            LoggerFactory.getLogger(CarThenSteps.class).info("Car left, {}", notification.vehicle);
            return true;
        });
    }
}
