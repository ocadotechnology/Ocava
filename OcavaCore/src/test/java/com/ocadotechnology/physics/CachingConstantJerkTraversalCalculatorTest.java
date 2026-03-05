/*
 * Copyright © 2017-2026 Ocado (Ocava)
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Comprehensive test suite for {@link CachingConstantJerkTraversalCalculator} to verify that caching works correctly.
 */
class CachingConstantJerkTraversalCalculatorTest {

    private CachingConstantJerkTraversalCalculator calculator;
    private VehicleMotionProperties vehicleProperties;

    @BeforeEach
    void setUp() {
        calculator = CachingConstantJerkTraversalCalculator.INSTANCE;
        vehicleProperties = new VehicleMotionProperties(
                2.0,  // acceleration
                0.1,  // accelerationAbsoluteTolerance
                -2.0, // deceleration
                0.1,  // decelerationAbsoluteTolerance
                5.0,  // maxSpeed
                0.1,  // maxSpeedAbsoluteTolerance
                3.0,  // jerkAccelerationUp
                -3.0, // jerkAccelerationDown
                -3.0, // jerkDecelerationUp
                3.0   // jerkDecelerationDown
        );
    }

    @AfterEach
    void tearDown() {
        // Clear caches after each test to ensure test isolation
        calculator.clearCaches();
    }

    @Test
    void create_whenCalledWithSameParameters_thenReturnsCachedTraversal() {
        // First call - should calculate and cache
        Traversal firstResult = calculator.create(10.0, vehicleProperties);
        int initialCacheSize = calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.TRAVERSAL_CACHE_LABEL);
        assertThat(initialCacheSize).isEqualTo(1);

        // Second call with same parameters - should return cached result
        Traversal secondResult = calculator.create(10.0, vehicleProperties);
        int finalCacheSize = calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.TRAVERSAL_CACHE_LABEL);

        // Cache size should not increase
        assertThat(finalCacheSize).isEqualTo(initialCacheSize);

        // Both results should be the exact same instance (proving it came from cache)
        assertThat(secondResult).isSameAs(firstResult);
    }

    @Test
    void create_withInitialConditions_whenCalledWithSameParameters_thenReturnsCachedTraversal() {
        // First call with initial conditions
        Traversal firstResult = calculator.create(10.0, 2.0, 1.0, vehicleProperties);
        int initialCacheSize = calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.TRAVERSAL_CACHE_LABEL);
        assertThat(initialCacheSize).isEqualTo(1);

        // Second call with same parameters
        Traversal secondResult = calculator.create(10.0, 2.0, 1.0, vehicleProperties);
        int finalCacheSize = calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.TRAVERSAL_CACHE_LABEL);

        assertThat(finalCacheSize).isEqualTo(initialCacheSize);
        assertThat(secondResult).isSameAs(firstResult);
    }

    @Test
    void create_whenCalledWithDifferentDistances_thenCreatesSeparateCacheEntries() {
        Traversal result1 = calculator.create(10.0, vehicleProperties);
        Traversal result2 = calculator.create(20.0, vehicleProperties);
        Traversal result3 = calculator.create(30.0, vehicleProperties);

        assertThat(calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.TRAVERSAL_CACHE_LABEL)).isEqualTo(3);
        assertThat(result1).isNotSameAs(result2);
        assertThat(result2).isNotSameAs(result3);
        assertThat(result1).isNotSameAs(result3);
    }

    @Test
    void create_whenCalledWithDifferentInitialSpeeds_thenCreatesSeparateCacheEntries() {
        Traversal result1 = calculator.create(10.0, 1.0, 0.0, vehicleProperties);
        Traversal result2 = calculator.create(10.0, 2.0, 0.0, vehicleProperties);

        assertThat(calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.TRAVERSAL_CACHE_LABEL)).isEqualTo(2);
        assertThat(result1).isNotSameAs(result2);
    }

    @Test
    void create_whenCalledWithDifferentInitialAccelerations_thenCreatesSeparateCacheEntries() {
        Traversal result1 = calculator.create(10.0, 1.0, 0.5, vehicleProperties);
        Traversal result2 = calculator.create(10.0, 1.0, 1.0, vehicleProperties);

        assertThat(calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.TRAVERSAL_CACHE_LABEL)).isEqualTo(2);
        assertThat(result1).isNotSameAs(result2);
    }

    @Test
    void create_whenCalledWithDifferentVehicleProperties_thenCreatesSeparateCacheEntries() {
        VehicleMotionProperties differentProperties = new VehicleMotionProperties(
                3.0,  // different acceleration
                0.1,
                -2.0,
                0.1,
                5.0,
                0.1,
                3.0,
                -3.0,
                -3.0,
                3.0
        );

        Traversal result1 = calculator.create(10.0, vehicleProperties);
        Traversal result2 = calculator.create(10.0, differentProperties);

        assertThat(calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.TRAVERSAL_CACHE_LABEL)).isEqualTo(2);
        assertThat(result1).isNotSameAs(result2);
    }

    @Test
    void getBrakingTraversal_whenCalledWithSameParameters_thenReturnsCachedTraversal() {
        // First call - should calculate and cache
        Traversal firstResult = calculator.getBrakingTraversal(3.0, 1.0, vehicleProperties);
        int initialCacheSize = calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.BRAKING_TRAVERSAL_CACHE_LABEL);
        assertThat(initialCacheSize).isEqualTo(1);

        // Second call with same parameters - should return cached result
        Traversal secondResult = calculator.getBrakingTraversal(3.0, 1.0, vehicleProperties);
        int finalCacheSize = calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.BRAKING_TRAVERSAL_CACHE_LABEL);

        assertThat(finalCacheSize).isEqualTo(initialCacheSize);
        assertThat(secondResult).isSameAs(firstResult);
    }

    @Test
    void getBrakingTraversal_whenCalledWithDifferentInitialSpeeds_thenCreatesSeparateCacheEntries() {
        Traversal result1 = calculator.getBrakingTraversal(2.0, 0.0, vehicleProperties);
        Traversal result2 = calculator.getBrakingTraversal(3.0, 0.0, vehicleProperties);

        assertThat(calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.BRAKING_TRAVERSAL_CACHE_LABEL)).isEqualTo(2);
        assertThat(result1).isNotSameAs(result2);
    }

    @Test
    void getBrakingTraversal_whenCalledWithDifferentInitialAccelerations_thenCreatesSeparateCacheEntries() {
        Traversal result1 = calculator.getBrakingTraversal(3.0, 0.5, vehicleProperties);
        Traversal result2 = calculator.getBrakingTraversal(3.0, 1.0, vehicleProperties);

        assertThat(calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.BRAKING_TRAVERSAL_CACHE_LABEL)).isEqualTo(2);
        assertThat(result1).isNotSameAs(result2);
    }

    @Test
    void getBrakingTraversal_whenCalledWithDifferentVehicleProperties_thenCreatesSeparateCacheEntries() {
        VehicleMotionProperties differentProperties = new VehicleMotionProperties(
                2.0,
                0.1,
                -3.0, // different deceleration
                0.1,
                5.0,
                0.1,
                3.0,
                -3.0,
                -3.0,
                3.0
        );

        Traversal result1 = calculator.getBrakingTraversal(3.0, 1.0, vehicleProperties);
        Traversal result2 = calculator.getBrakingTraversal(3.0, 1.0, differentProperties);

        assertThat(calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.BRAKING_TRAVERSAL_CACHE_LABEL)).isEqualTo(2);
        assertThat(result1).isNotSameAs(result2);
    }

    @Test
    void caches_areIndependent() {
        // Create a regular traversal
        calculator.create(10.0, vehicleProperties);
        assertThat(calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.TRAVERSAL_CACHE_LABEL)).isEqualTo(1);
        assertThat(calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.BRAKING_TRAVERSAL_CACHE_LABEL)).isEqualTo(0);

        // Create a braking traversal
        calculator.getBrakingTraversal(3.0, 1.0, vehicleProperties);
        assertThat(calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.TRAVERSAL_CACHE_LABEL)).isEqualTo(1);
        assertThat(calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.BRAKING_TRAVERSAL_CACHE_LABEL)).isEqualTo(1);
    }

    @Test
    void create_withMultipleCallPatterns_thenCachesCorrectly() {
        // Call with different parameters
        Traversal t1 = calculator.create(10.0, vehicleProperties);
        Traversal t2 = calculator.create(20.0, vehicleProperties);
        Traversal t3 = calculator.create(10.0, 1.0, 0.0, vehicleProperties);

        assertThat(calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.TRAVERSAL_CACHE_LABEL)).isEqualTo(3);

        // Call again with same parameters in different order
        Traversal t1Again = calculator.create(10.0, vehicleProperties);
        Traversal t3Again = calculator.create(10.0, 1.0, 0.0, vehicleProperties);
        Traversal t2Again = calculator.create(20.0, vehicleProperties);

        // Cache should not grow
        assertThat(calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.TRAVERSAL_CACHE_LABEL)).isEqualTo(3);

        // All repeated calls should return same instances
        assertThat(t1Again).isSameAs(t1);
        assertThat(t2Again).isSameAs(t2);
        assertThat(t3Again).isSameAs(t3);
    }

    @Test
    void getCacheSizeStats_returnsCorrectCacheSizes() {
        assertThat(calculator.getCacheSizeStats().get("traversalCache")).isEqualTo(0);
        assertThat(calculator.getCacheSizeStats().get("brakingTraversalCache")).isEqualTo(0);

        // Add some entries to traversal cache
        calculator.create(10.0, vehicleProperties);
        calculator.create(20.0, vehicleProperties);

        ImmutableMap<String, Integer> stats = calculator.getCacheSizeStats();
        assertThat(stats.get("traversalCache")).isEqualTo(2);
        assertThat(stats.get("brakingTraversalCache")).isEqualTo(0);

        // Add some entries to braking traversal cache
        calculator.getBrakingTraversal(3.0, 1.0, vehicleProperties);
        calculator.getBrakingTraversal(4.0, 1.0, vehicleProperties);
        calculator.getBrakingTraversal(5.0, 1.0, vehicleProperties);

        stats = calculator.getCacheSizeStats();
        assertThat(stats.get("traversalCache")).isEqualTo(2);
        assertThat(stats.get("brakingTraversalCache")).isEqualTo(3);
    }

    @Test
    void clearCaches_clearsAllCaches() {
        // Populate both caches
        calculator.create(10.0, vehicleProperties);
        calculator.create(20.0, vehicleProperties);
        calculator.getBrakingTraversal(3.0, 1.0, vehicleProperties);
        calculator.getBrakingTraversal(4.0, 1.0, vehicleProperties);

        // Verify caches are populated
        assertThat(calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.TRAVERSAL_CACHE_LABEL)).isEqualTo(2);
        assertThat(calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.BRAKING_TRAVERSAL_CACHE_LABEL)).isEqualTo(2);

        // Clear caches
        calculator.clearCaches();

        // Verify caches are empty
        assertThat(calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.TRAVERSAL_CACHE_LABEL)).isEqualTo(0);
        assertThat(calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.BRAKING_TRAVERSAL_CACHE_LABEL)).isEqualTo(0);
    }

    @Test
    void clearCaches_afterClear_cacheStillWorks() {
        // Create and cache a traversal
        Traversal firstResult = calculator.create(10.0, vehicleProperties);
        assertThat(calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.TRAVERSAL_CACHE_LABEL)).isEqualTo(1);

        // Clear caches
        calculator.clearCaches();
        assertThat(calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.TRAVERSAL_CACHE_LABEL)).isEqualTo(0);

        // Create the same traversal again - should be recalculated and cached
        Traversal secondResult = calculator.create(10.0, vehicleProperties);
        assertThat(calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.TRAVERSAL_CACHE_LABEL)).isEqualTo(1);

        // The results should be different instances (since cache was cleared)
        assertThat(secondResult).isNotSameAs(firstResult);

        // But calling again should return the newly cached instance
        Traversal thirdResult = calculator.create(10.0, vehicleProperties);
        assertThat(thirdResult).isSameAs(secondResult);
        assertThat(calculator.getCacheSizeStats().get(CachingConstantJerkTraversalCalculator.TRAVERSAL_CACHE_LABEL)).isEqualTo(1);
    }

    @Test
    void cachedTraversals_produceCorrectResults() {
        // First call - calculates and caches
        Traversal firstResult = calculator.create(10.0, vehicleProperties);
        double firstDistance = firstResult.getTotalDistance();
        double firstDuration = firstResult.getTotalDuration();

        // Second call - from cache
        Traversal secondResult = calculator.create(10.0, vehicleProperties);
        double secondDistance = secondResult.getTotalDistance();
        double secondDuration = secondResult.getTotalDuration();

        // Results should be identical
        assertThat(secondDistance).isCloseTo(firstDistance, within(1e-10));
        assertThat(secondDuration).isCloseTo(firstDuration, within(1e-10));

        // Verify they're the same object
        assertThat(secondResult).isSameAs(firstResult);
    }

    @Test
    void cachedBrakingTraversals_produceCorrectResults() {
        // First call - calculates and caches
        Traversal firstResult = calculator.getBrakingTraversal(3.0, 1.0, vehicleProperties);
        double firstDistance = firstResult.getTotalDistance();
        double firstDuration = firstResult.getTotalDuration();

        // Second call - from cache
        Traversal secondResult = calculator.getBrakingTraversal(3.0, 1.0, vehicleProperties);
        double secondDistance = secondResult.getTotalDistance();
        double secondDuration = secondResult.getTotalDuration();

        // Results should be identical
        assertThat(secondDistance).isCloseTo(firstDistance, within(1e-10));
        assertThat(secondDuration).isCloseTo(firstDuration, within(1e-10));

        // Verify they're the same object
        assertThat(secondResult).isSameAs(firstResult);
    }
}
