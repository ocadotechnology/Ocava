/*
 * Copyright © 2017-2025 Ocado (Ocava)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ocadotechnology.trafficlights;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.event.scheduling.SourceTrackingEventScheduler;
import com.ocadotechnology.notification.NotificationRouter;
import com.ocadotechnology.scenario.AbstractScenarioSimulationApi;
import com.ocadotechnology.scenario.ScenarioTestSchedulerType;
import com.ocadotechnology.utils.Types;

/**
 * Used to interface the scenario testing framework with traffic simulation.
 */
public class TrafficSimulationApi extends AbstractScenarioSimulationApi<TrafficSimulation> {
    private EventScheduler eventScheduler;
    private Map<String, String> configMap = new LinkedHashMap<>();

    private TrafficSimulation trafficSimulation;

    TrafficSimulationApi() {}

    @Override
    protected EventScheduler createScheduler() {

        String[] configArgs = configMap.entrySet().stream()
                .map(entry -> "-O" + entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);

        //create simulation
        trafficSimulation = TrafficSimulation.create(configArgs);

        //get scheduler from simulation
        eventScheduler = trafficSimulation.getScheduler();

        SourceTrackingEventScheduler simulationScheduler = Types.fromTypeOrFail(trafficSimulation.getScheduler(), SourceTrackingEventScheduler.class);
        return simulationScheduler.createSibling(ScenarioTestSchedulerType.INSTANCE);
    }

    @Override
    protected void startSimulation() {
        //run simulation
        trafficSimulation.go();

        eventScheduler.doNow(() -> NotificationRouter.get().broadcast(new TrafficSimulationStartedNotification()), "Simulation start");
    }

    @Override
    protected void exitSimulation() {
        eventScheduler.stop();
    }

    @Override
    public double getSchedulerStartTime() {
        return 0.0;
    }

    @Override
    public TimeUnit getSchedulerTimeUnit() {
        return TimeUnit.SECONDS;
    }

    @Override
    public TrafficSimulation getSimulation() {
        return trafficSimulation;
    }

    public Map<String, String> getConfigMap() {
        return configMap;
    }
}
