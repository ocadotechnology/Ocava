/*
 * Copyright © 2017-2020 Ocado (Ocava)
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
package com.ocadotechnology.trafficlightsimulation;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Runnables;
import com.ocadotechnology.config.Config;
import com.ocadotechnology.config.ConfigManager;
import com.ocadotechnology.config.ConfigManager.Builder;
import com.ocadotechnology.event.scheduling.BusyLoopEventScheduler;
import com.ocadotechnology.event.scheduling.EventExecutor;
import com.ocadotechnology.event.scheduling.EventScheduler;
import com.ocadotechnology.event.scheduling.SimpleDiscreteEventScheduler;
import com.ocadotechnology.event.scheduling.SourceSchedulerTracker;
import com.ocadotechnology.event.scheduling.SourceTrackingEventScheduler;
import com.ocadotechnology.notification.DefaultBus;
import com.ocadotechnology.notification.NotificationRouter;
import com.ocadotechnology.random.RepeatableRandom;
import com.ocadotechnology.time.AdjustableTimeProvider;
import com.ocadotechnology.time.UtcTimeProvider;
import com.ocadotechnology.trafficlightsimulation.controller.TrafficLightController;
import com.ocadotechnology.trafficlightsimulation.simulation.VehicleCache;
import com.ocadotechnology.trafficlightsimulation.simulation.VehicleSimulation;
import com.ocadotechnology.validation.Failer;

public class TrafficSimulation {
    private static final Logger logger = LoggerFactory.getLogger(TrafficSimulation.class);
    private static final String CONFIG_FILE = "trafficSimulation.properties";

    private final EventScheduler scheduler;

    private Config<TrafficConfig> trafficConfig;

    private TrafficLightController trafficLightController;
    private VehicleSimulation vehicleSimulation;

    private TrafficSimulation(Config<TrafficConfig> trafficConfig) {
        this.trafficConfig = trafficConfig;

        this.scheduler = createScheduler(trafficConfig);
        Logging.configure(scheduler.getTimeProvider());
    }

    public static TrafficSimulation create(String... args) {
        ConfigManager config = createConfigManager(args);
        Config<TrafficConfig> trafficConfig  = config.getConfig(TrafficConfig.class);

        RepeatableRandom.initialiseWithSeed(123);

        return new TrafficSimulation(trafficConfig);
    }

    public void go() {
        scheduler.doNow(() -> start(scheduler));
    }

    public static void main(String[] args) {
        TrafficSimulation trafficSimulation = create(args);
        trafficSimulation.go();
    }

    public EventScheduler getScheduler() {
        return scheduler;
    }

    public TrafficLightController getTrafficLightController() {
        return trafficLightController;
    }

    public VehicleSimulation getVehicleSimulation() {
        return vehicleSimulation;
    }

    private void start(EventScheduler scheduler) {
        this.trafficLightController = new TrafficLightController(scheduler, trafficConfig);
        this.vehicleSimulation = VehicleSimulation.createAndSubscribe(scheduler, new VehicleCache(), trafficConfig);

        String terminationReason = "Max sim time reached";
        long maxSimTime = trafficConfig.getTime(TrafficConfig.MAX_SIM_TIME);
        scheduler.doInDaemon(maxSimTime + EventScheduler.ONE_CLOCK_CYCLE, () -> shutdown(scheduler, terminationReason), terminationReason);
    }

    private void shutdown(EventScheduler scheduler, String terminationReason) {
        logger.info("{}. Stopping schedulers", terminationReason);
        NotificationRouter.get().broadcast(new SimulationEndedNotification());
        scheduler.doNow(scheduler::stop, terminationReason);
    }

    private static EventScheduler createScheduler(Config<TrafficConfig> config) {
        SchedulerType schedulerType = config.getEnum(TrafficConfig.SCHEDULER_TYPE, SchedulerType.class);
        switch (schedulerType) {
            case SIMULATION:
                SimpleDiscreteEventScheduler simpleDiscreteEventScheduler = new SimpleDiscreteEventScheduler(
                        new EventExecutor(),
                        Runnables::doNothing,
                        SchedulerLayerType.SIMULATION,
                        new AdjustableTimeProvider(0),
                        true);

                SourceTrackingEventScheduler eventScheduler = new SourceTrackingEventScheduler(new SourceSchedulerTracker(), SchedulerLayerType.SIMULATION, simpleDiscreteEventScheduler);
                NotificationRouter.get().registerExecutionLayer(eventScheduler, DefaultBus.get());

                return eventScheduler;
            case REALTIME:
                BusyLoopEventScheduler busyLoopEventScheduler = new BusyLoopEventScheduler(new UtcTimeProvider(TimeUnit.SECONDS), "Realtime scheduler", SchedulerLayerType.SIMULATION);
                busyLoopEventScheduler.start();
                return busyLoopEventScheduler;
            default:
                throw Failer.fail("Missing switch case for schedulerType %s", schedulerType);
        }
    }

    private static ConfigManager createConfigManager(String[] args) {
        ImmutableList<String> resourceFiles = ImmutableList.of(CONFIG_FILE);
        ImmutableSet<Class<? extends Enum<?>>> configKeys = ImmutableSet.of(TrafficConfig.class);
        try {
            return new Builder(args)
                    .loadConfigFromLocalResources(resourceFiles, configKeys)
                    .setTimeUnit(TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

}
