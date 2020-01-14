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
