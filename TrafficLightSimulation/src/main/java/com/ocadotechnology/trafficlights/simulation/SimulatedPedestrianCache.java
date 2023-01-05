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

import java.util.Collection;

import com.google.common.collect.ImmutableSet;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.indexedcache.IndexedImmutableObjectCache;
import com.ocadotechnology.indexedcache.PredicateIndex;
import com.ocadotechnology.trafficlights.simulation.entities.SimulatedPedestrian;

public class SimulatedPedestrianCache {

    private final IndexedImmutableObjectCache<SimulatedPedestrian, SimulatedPedestrian> cache = IndexedImmutableObjectCache.createHashMapBackedCache();
    private final PredicateIndex<SimulatedPedestrian> isStationary;

    public SimulatedPedestrianCache() {
        this.isStationary = cache.addPredicateIndex(SimulatedPedestrian::isStationary);
    }

    public SimulatedPedestrian get(Id<SimulatedPedestrian> pedestrianId) {
        return cache.get(pedestrianId);
    }

    public void add(SimulatedPedestrian simulatedPedestrian) {
        cache.add(simulatedPedestrian);
    }

    public void update(SimulatedPedestrian oldState, SimulatedPedestrian newState) {
        cache.update(oldState, newState);
    }

    public SimulatedPedestrian delete(Id<SimulatedPedestrian> pedestrianId) {
        return cache.delete(pedestrianId);
    }

    public int size() {
        return cache.size();
    }

    public Collection<Id<SimulatedPedestrian>> getAllStationary() {
        return isStationary.stream()
                .map(SimulatedPedestrian::getId)
                .collect(ImmutableSet.toImmutableSet());
    }

}
