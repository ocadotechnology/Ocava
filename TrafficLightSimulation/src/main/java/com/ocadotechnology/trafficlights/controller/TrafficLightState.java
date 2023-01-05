/*
 * Copyright © 2017-2023 Ocado (Ocava)
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
package com.ocadotechnology.trafficlights.controller;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.validation.Failer;

public class TrafficLightState {

    public enum LightType {
        TRAFFIC,
        PEDESTRIAN;

        public static LightType getInverse(LightType type) {
            switch (type) {
                case TRAFFIC:
                    return PEDESTRIAN;
                case PEDESTRIAN:
                    return TRAFFIC;
                default:
                    throw Failer.fail("No inverse for type %s", type);
            }
        }
    }

    private final ImmutableMap<LightType, LightColour> lightTypeLightColourMap;
    private final boolean pedestrianCrossingRequested;

    public TrafficLightState(LightColour trafficColour, LightColour pedestrianColour, boolean pedestrianCrossingRequested) {
        this(ImmutableMap.of(LightType.TRAFFIC, trafficColour, LightType.PEDESTRIAN, pedestrianColour), pedestrianCrossingRequested);
    }

    public TrafficLightState(ImmutableMap<LightType, LightColour> lightTypeLightColourMap, boolean pedestrianCrossingRequested) {
        this.lightTypeLightColourMap = lightTypeLightColourMap;
        this.pedestrianCrossingRequested = pedestrianCrossingRequested;

        long numberOfGreenLights = lightTypeLightColourMap.values().stream()
                .filter(colour -> colour.equals(LightColour.GREEN))
                .count();
        Preconditions.checkState(numberOfGreenLights <= 1, "Only one type of light can be green at a time! %s", lightTypeLightColourMap);
    }

    public LightColour getTrafficColour() {
        return lightTypeLightColourMap.get(LightType.TRAFFIC);
    }

    public LightColour getPedestrianColour() {
        return lightTypeLightColourMap.get(LightType.PEDESTRIAN);
    }

    public boolean canPedestrianCross() {
        return getPedestrianColour().equals(LightColour.GREEN);
    }

    public boolean canCarMove() {
        return getTrafficColour().equals(LightColour.GREEN);
    }

    public boolean isPedestrianCrossingRequested() {
        return pedestrianCrossingRequested;
    }

    /**
     * Since at least one light is always red, there will always be at least one light that can turn green (at some point).
     * In the case of both lights being red (at startup) the iteration order of the {@link ImmutableMap} will be used.
     */
    public LightType nextTypeToTurnGreen() {
        return lightTypeLightColourMap.entrySet().stream()
                .filter(e -> e.getValue().equals(LightColour.RED))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElseThrow(Failer::valueExpected);
    }

    public Builder builder() {
        return new Builder(this);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("lightTypeLightColourMap", lightTypeLightColourMap)
                .add("pedestrianCrossingRequested", pedestrianCrossingRequested)
                .toString();
    }

    public static class Builder {

        private final HashMap<LightType, LightColour> lightTypeLightColourMap;
        private boolean pedestrianCrossingRequested;

        Builder(TrafficLightState state) {
            lightTypeLightColourMap = new HashMap<>(state.lightTypeLightColourMap);
            pedestrianCrossingRequested = state.pedestrianCrossingRequested;
        }

        public Builder cycleLights() {
            setLightColour(LightColour.getInverse(lightTypeLightColourMap.get(LightType.TRAFFIC)));
            return setPedestrianColour(LightColour.getInverse(lightTypeLightColourMap.get(LightType.PEDESTRIAN)));
        }

        public Builder setLightColour(LightColour lightColour) {
            this.lightTypeLightColourMap.put(LightType.TRAFFIC, lightColour);
            return this;
        }

        public Builder setPedestrianColour(LightColour pedestrianColour) {
            this.lightTypeLightColourMap.put(LightType.PEDESTRIAN, pedestrianColour);
            return this;
        }

        public Builder setColourForType(LightType type, LightColour colour) {
            this.lightTypeLightColourMap.put(type, colour);
            return this;
        }

        public Builder setCrossingRequested(boolean crossingRequested) {
            this.pedestrianCrossingRequested = crossingRequested;
            return this;
        }

        public TrafficLightState build() {
            return new TrafficLightState(ImmutableMap.copyOf(lightTypeLightColourMap), pedestrianCrossingRequested);
        }

    }
}
