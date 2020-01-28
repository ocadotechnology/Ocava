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
package com.ocadotechnology.trafficlightsimulation.simulation;

import java.util.Comparator;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.indexedcache.CachedSort;
import com.ocadotechnology.indexedcache.IndexedImmutableObjectCache;
import com.ocadotechnology.indexedcache.OneToManyIndex;
import com.ocadotechnology.trafficlightsimulation.simulation.Vehicle.Colour;

public class VehicleCache {

    private final IndexedImmutableObjectCache<Vehicle, Vehicle> cache = IndexedImmutableObjectCache.createHashMapBackedCache();
    private final CachedSort<Vehicle> byArrivalTime;
    private final OneToManyIndex<Colour, Vehicle> byColour;

    public VehicleCache() {
        this.byArrivalTime = cache.addCacheSort(Comparator.comparingDouble(Vehicle::getArrivalTime).thenComparing(Vehicle::getId));
        this.byColour = cache.addOneToManyIndex(Vehicle::getColour);
    }

    public void add(Vehicle vehicle) {
        cache.add(vehicle);
    }

    public void update(Vehicle oldState, Vehicle newState) {
        cache.update(oldState, newState);
    }

    public Vehicle delete(Id<Vehicle> vehicleId) {
        return cache.delete(vehicleId);
    }

    public int size() {
        return cache.size();
    }

    public Optional<Vehicle> getStationaryEarliestArrival() {
        return Streams.stream(byArrivalTime.iterator())
                .filter(Vehicle::isStationary)
                .findFirst();
    }

    public ImmutableList<Vehicle> getVehiclesOfColour(Colour colour) {
        return byColour.getCopy(colour);
    }

}
