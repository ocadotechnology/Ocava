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
package com.ocadotechnology.trafficlights.simulation;

import java.util.Comparator;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.indexedcache.CachedSort;
import com.ocadotechnology.indexedcache.IndexedImmutableObjectCache;
import com.ocadotechnology.indexedcache.OneToManyIndex;
import com.ocadotechnology.trafficlights.simulation.entities.SimulatedCar;
import com.ocadotechnology.trafficlights.simulation.entities.SimulatedCar.Colour;

public class SimulatedCarCache {

    private final IndexedImmutableObjectCache<SimulatedCar, SimulatedCar> cache = IndexedImmutableObjectCache.createHashMapBackedCache();
    private final CachedSort<SimulatedCar> byArrivalTime;
    private final OneToManyIndex<Colour, SimulatedCar> byColour;

    public SimulatedCarCache() {
        this.byArrivalTime = cache.addCacheSort(Comparator.comparingDouble(SimulatedCar::getArrivalTime).thenComparing(SimulatedCar::getId));
        this.byColour = cache.addOneToManyIndex(SimulatedCar::getColour);
    }

    public void add(SimulatedCar simulatedCar) {
        cache.add(simulatedCar);
    }

    public void update(SimulatedCar oldState, SimulatedCar newState) {
        cache.update(oldState, newState);
    }

    public SimulatedCar delete(Id<SimulatedCar> vehicleId) {
        return cache.delete(vehicleId);
    }

    public int size() {
        return cache.size();
    }

    public Optional<SimulatedCar> getStationaryEarliestArrival() {
        return Streams.stream(byArrivalTime.iterator())
                .filter(SimulatedCar::isStationary)
                .findFirst();
    }

    public ImmutableList<SimulatedCar> getCarsOfColour(Colour colour) {
        return byColour.getCopy(colour);
    }

}
