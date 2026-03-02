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
package com.ocadotechnology.physics;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ImmutableMap;

/**
 * A caching wrapper for {@link ConstantJerkTraversalCalculator}.
 * Caches traversals based on their input parameters, so that repeated calls with the same parameters will be faster.
 * This class is thread-safe.
 */
public final class CachingConstantJerkTraversalCalculator implements TraversalCalculator {
    public static final CachingConstantJerkTraversalCalculator INSTANCE = new CachingConstantJerkTraversalCalculator();

    public static final String TRAVERSAL_CACHE_LABEL = "traversalCache";
    public static final String BRAKING_TRAVERSAL_CACHE_LABEL = "brakingTraversalCache";

    private final ConcurrentHashMap<TraversalCacheKey, Traversal> traversalCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BrakingTraversalCacheKey, Traversal> brakingTraversalCache = new ConcurrentHashMap<>();

    private CachingConstantJerkTraversalCalculator() {
    }

    /**
     * Provides access to the current size of caches for monitoring purposes.
     * @return a map of cache names to their current size.
     */
    public ImmutableMap<String, Integer> getCacheSizeStats() {
        return ImmutableMap.of(
                TRAVERSAL_CACHE_LABEL, traversalCache.size(),
                BRAKING_TRAVERSAL_CACHE_LABEL, brakingTraversalCache.size());
    }

    /**
     * @param distance          distance to traverse
     * @param vehicleProperties vehicle properties
     * @return Traversal that starts and ends with speed and acceleration equal to zero and traverses the distance.
     * If the distance is less than the braking distance, then the braking traversal will instead be returned, covering
     * the full braking distance.
     */
    public Traversal create(double distance, VehicleMotionProperties vehicleProperties) {
        return create(distance, 0, 0, vehicleProperties);
    }

    /**
     * @param distance            distance to traverse
     * @param initialSpeed        initial speed
     * @param initialAcceleration initial acceleration
     * @param vehicleProperties   vehicle properties
     * @return Traversal that starts with the given speed and acceleration and ends at rest after traversing distance.
     * If the distance is less than the braking distance, then the braking traversal will instead be returned.
     */
    public Traversal create(double distance, double initialSpeed, double initialAcceleration, VehicleMotionProperties vehicleProperties) {
        return traversalCache.computeIfAbsent(
                new TraversalCacheKey(distance, initialSpeed, initialAcceleration, vehicleProperties),
                key -> ConstantJerkTraversalCalculator.INSTANCE.create(distance, initialSpeed, initialAcceleration, vehicleProperties));
    }

    /**
     * @param initialSpeed        initial speed
     * @param initialAcceleration initial acceleration
     * @param vehicleProperties   vehicle properties
     * @return the minimal distance traversal to come to rest
     * @throws TraversalCalculationException if the speed would go negative.
     */
    public Traversal getBrakingTraversal(double initialSpeed, double initialAcceleration, VehicleMotionProperties vehicleProperties) {
        return brakingTraversalCache.computeIfAbsent(
                new BrakingTraversalCacheKey(initialSpeed, initialAcceleration, vehicleProperties),
                key -> ConstantJerkTraversalCalculator.INSTANCE.getBrakingTraversal(initialSpeed, initialAcceleration, vehicleProperties));
    }

    public void clearCaches() {
        traversalCache.clear();
        brakingTraversalCache.clear();
    }

    /**
     * Cache key for the traversal cache. Contains all parameters needed to uniquely identify a traversal calculation.
     */
    private static final class TraversalCacheKey {
        private final double distance;
        private final double initialSpeed;
        private final double initialAcceleration;
        private final VehicleMotionProperties vehicleProperties;

        private final transient int hashCode;

        TraversalCacheKey(double distance, double initialSpeed, double initialAcceleration, VehicleMotionProperties vehicleProperties) {
            this.distance = distance;
            this.initialSpeed = initialSpeed;
            this.initialAcceleration = initialAcceleration;
            this.vehicleProperties = vehicleProperties;

            // Caching a manual hashCode calculation - this does the same thing as Objects.hash, but avoids creating an array for the varargs.
            // The nature of the caches means we will always need the hashCode at least once, and existing use cases will continually create a huge number of keys.
            int result = Double.hashCode(distance);
            result = 31 * result + Double.hashCode(initialSpeed);
            result = 31 * result + Double.hashCode(initialAcceleration);
            result = 31 * result + Objects.hashCode(vehicleProperties);
            this.hashCode = result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TraversalCacheKey that = (TraversalCacheKey) o;
            return Double.compare(that.distance, distance) == 0
                    && Double.compare(that.initialSpeed, initialSpeed) == 0
                    && Double.compare(that.initialAcceleration, initialAcceleration) == 0
                    && Objects.equals(that.vehicleProperties, vehicleProperties);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    /**
     * Cache key for the braking traversal cache. Contains all parameters needed to uniquely identify a braking traversal calculation.
     */
    private static final class BrakingTraversalCacheKey {
        private final double initialSpeed;
        private final double initialAcceleration;
        private final VehicleMotionProperties vehicleProperties;

        private final transient int hashCode;

        BrakingTraversalCacheKey(double initialSpeed, double initialAcceleration, VehicleMotionProperties vehicleProperties) {
            this.initialSpeed = initialSpeed;
            this.initialAcceleration = initialAcceleration;
            this.vehicleProperties = vehicleProperties;

            // Caching a manual hashCode calculation - this does the same thing as Objects.hash, but avoids creating an array for the varargs.
            // The nature of the caches means we will always need the hashCode at least once, and existing use cases will continually create a huge number of keys.
            int result = Double.hashCode(initialSpeed);
            result = 31 * result + Double.hashCode(initialAcceleration);
            result = 31 * result + Objects.hashCode(vehicleProperties);
            this.hashCode = result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BrakingTraversalCacheKey that = (BrakingTraversalCacheKey) o;
            return Double.compare(that.initialSpeed, initialSpeed) == 0
                    && Double.compare(that.initialAcceleration, initialAcceleration) == 0
                    && Objects.equals(that.vehicleProperties, vehicleProperties);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
