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
package com.ocadotechnology.trafficlights;

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
import com.ocadotechnology.notification.NotificationRouter;
import com.ocadotechnology.notification.SimpleBus;
import com.ocadotechnology.random.RepeatableRandom;
import com.ocadotechnology.simulation.Simulation;
import com.ocadotechnology.time.AdjustableTimeProvider;
import com.ocadotechnology.time.UtcTimeProvider;
import com.ocadotechnology.trafficlights.controller.LightColour;
import com.ocadotechnology.trafficlights.controller.TrafficLightController;
import com.ocadotechnology.trafficlights.simulation.CarSimulation;
import com.ocadotechnology.trafficlights.simulation.CarSpawner;
import com.ocadotechnology.trafficlights.simulation.PedestrianSimulation;
import com.ocadotechnology.trafficlights.simulation.PedestrianSpawner;
import com.ocadotechnology.trafficlights.simulation.SimulatedCarCache;
import com.ocadotechnology.trafficlights.simulation.SimulatedPedestrianCache;
import com.ocadotechnology.trafficlights.simulation.comms.SimulatedRestForwarder;
import com.ocadotechnology.trafficlights.simulation.comms.SimulatedRestSender;
import com.ocadotechnology.trafficlights.simulation.entities.SimulatedTrafficLight;
import com.ocadotechnology.validation.Failer;

public class TrafficSimulation implements Simulation {
    private static final Logger logger = LoggerFactory.getLogger(TrafficSimulation.class);
    private static final String CONFIG_FILE = "trafficSimulation.properties";

    private final EventScheduler scheduler;

    private Config<TrafficConfig> trafficConfig;

    private TrafficLightController trafficLightController;
    private SimulatedTrafficLight simulatedTrafficLight;
    private CarSimulation carSimulation;
    private CarSpawner carSpawner;
    private PedestrianSimulation pedestrianSimulation;
    private PedestrianSpawner pedestrianSpawner;

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

    public CarSimulation getCarSimulation() {
        return carSimulation;
    }

    public CarSpawner getCarSpawner() {
        return carSpawner;
    }

    public PedestrianSimulation getPedestrianSimulation() {
        return pedestrianSimulation;
    }

    public PedestrianSpawner getPedestrianSpawner() {
        return pedestrianSpawner;
    }

    private void start(EventScheduler scheduler) {
        LightColour startingTrafficLightColour = trafficConfig.getValue(TrafficConfig.TrafficLight.INITIAL_TRAFFIC_STATE).asEnum(LightColour.class);
        LightColour startingPedestrianLightColour = trafficConfig.getIfKeyAndValueDefined(TrafficConfig.TrafficLight.INITIAL_PEDESTRIAN_STATE)
                .asEnum(LightColour.class)
                .orElse(LightColour.getInverse(startingTrafficLightColour));

        this.trafficLightController = new TrafficLightController(new SimulatedRestSender(), scheduler, trafficConfig, startingTrafficLightColour, startingPedestrianLightColour);
        this.simulatedTrafficLight = SimulatedTrafficLight.createAndSubscribe(startingTrafficLightColour, startingPedestrianLightColour);

        SimulatedCarCache simulatedCarCache = new SimulatedCarCache();
        this.carSimulation = CarSimulation.createAndSubscribe(scheduler, simulatedCarCache, trafficConfig, simulatedTrafficLight);
        this.carSpawner = new CarSpawner(scheduler, simulatedCarCache, carSimulation, trafficConfig);

        SimulatedPedestrianCache simulatedPedestrianCache = new SimulatedPedestrianCache();
        this.pedestrianSimulation = PedestrianSimulation.createAndSubscribe(scheduler, simulatedPedestrianCache, trafficConfig, simulatedTrafficLight);
        this.pedestrianSpawner = new PedestrianSpawner(scheduler, simulatedPedestrianCache, pedestrianSimulation, trafficConfig);

        SimulatedRestForwarder.createAndSubscribe(trafficLightController);

        String terminationReason = "Max sim time reached";
        long maxSimTime = trafficConfig.getValue(TrafficConfig.MAX_SIM_TIME).asTime();
        scheduler.doInDaemon(maxSimTime + EventScheduler.ONE_CLOCK_CYCLE, () -> shutdown(scheduler, terminationReason), terminationReason);
    }

    private void shutdown(EventScheduler scheduler, String terminationReason) {
        logger.info("{}. Stopping schedulers", terminationReason);
        NotificationRouter.get().broadcast(new SimulationEndedNotification());
        scheduler.doNow(scheduler::stop, terminationReason);
    }

    private static EventScheduler createScheduler(Config<TrafficConfig> config) {
        SchedulerType schedulerType = config.getValue(TrafficConfig.SCHEDULER_TYPE).asEnum(SchedulerType.class);
        switch (schedulerType) {
            case SIMULATION:
                SimpleDiscreteEventScheduler simpleDiscreteEventScheduler = new SimpleDiscreteEventScheduler(
                        new EventExecutor(),
                        Runnables::doNothing,
                        SchedulerLayerType.SIMULATION,
                        new AdjustableTimeProvider(0),
                        true);

                SourceTrackingEventScheduler eventScheduler = new SourceTrackingEventScheduler(new SourceSchedulerTracker(), SchedulerLayerType.SIMULATION, simpleDiscreteEventScheduler);
                NotificationRouter.get().registerExecutionLayer(eventScheduler, SimpleBus.create());

                return eventScheduler;
            case REALTIME:
                BusyLoopEventScheduler busyLoopEventScheduler = new BusyLoopEventScheduler(new UtcTimeProvider(TimeUnit.MILLISECONDS), "Realtime scheduler", SchedulerLayerType.SIMULATION);
                NotificationRouter.get().registerExecutionLayer(busyLoopEventScheduler, SimpleBus.create());
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
