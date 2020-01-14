package com.ocadotechnology.trafficlightsimulation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.event.scheduling.EventSchedulerType;
import com.ocadotechnology.event.scheduling.SourceTrackingEventScheduler;
import com.ocadotechnology.notification.NotificationRouter;
import com.ocadotechnology.scenario.AbstractScenarioSimulationApi;
import com.ocadotechnology.scenario.ScenarioTestSchedulerType;
import com.ocadotechnology.utils.Types;

/**
 * Used to interface the scenario testing framework with traffic simulation.
 */
public class TrafficSimulationApi extends AbstractScenarioSimulationApi {
    private EventScheduler eventScheduler;
    private Map<String, String> configMap = new LinkedHashMap<>();

    private TrafficSimulation trafficSimulation;

    TrafficSimulationApi() {}

    @Override
    protected ImmutableMap<EventSchedulerType, EventScheduler> createSchedulers() {

        String[] configArgs = configMap.entrySet().stream()
                .map(entry -> "-O" + entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);

        //create simulation
        trafficSimulation = TrafficSimulation.create(configArgs);

        //get scheduler from simulation
        eventScheduler = trafficSimulation.getScheduler();

        SourceTrackingEventScheduler simulationScheduler = Types.fromTypeOrFail(trafficSimulation.getScheduler(), SourceTrackingEventScheduler.class);
        return ImmutableMap.of(ScenarioTestSchedulerType.INSTANCE, simulationScheduler.createSibling(ScenarioTestSchedulerType.INSTANCE));
    }

    @Override
    protected void startSimulation() {
        //run simulation
        trafficSimulation.go();

        eventScheduler.doNow(() -> NotificationRouter.get().broadcast(new TrafficSimulationStartedNotification()), "Simulation start");
    }

    @Override
    public double getSchedulerStartTime() {
        return 0.0;
    }

    @Override
    public TimeUnit getSchedulerTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

    public Map<String, String> getConfigMap() {
        return configMap;
    }

    public TrafficSimulation getTrafficSimulation() {
        return trafficSimulation;
    }
}
