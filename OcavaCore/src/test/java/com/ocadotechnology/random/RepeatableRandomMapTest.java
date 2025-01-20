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
package com.ocadotechnology.random;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.id.StringId;
import com.ocadotechnology.utils.ImmutableMapFactory;
import com.ocadotechnology.wrappers.Pair;

class RepeatableRandomMapTest {
    enum Color {
        RED, GREEN, BLUE
    }

    private static class Vehicle {
        private final StringId<Vehicle> id;
        private final Color color;
        private final int numberOfSeats;

        public Vehicle(StringId<Vehicle> id, Color color, int numberOfSeats) {
            this.id = id;
            this.color = color;
            this.numberOfSeats = numberOfSeats;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Vehicle vehicle = (Vehicle) o;
            return numberOfSeats == vehicle.numberOfSeats && Objects.equals(id, vehicle.id) && color == vehicle.color;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, color, numberOfSeats);
        }
    }

    @Test
    void theResultsForAGivenKeyShouldNotBeAffectedByTheNumbersOfKeys() {
        RepeatableRandom.initialiseWithSeed(1);
        Set<StringId<Vehicle>> vehiclesInBothScenarios = generateVehicleIds(1000);
        List<StringId<Vehicle>> vehiclesInFirstScenario = new ArrayList<>();
        vehiclesInFirstScenario.addAll(vehiclesInBothScenarios);
        vehiclesInFirstScenario.addAll(generateVehicleIds(250));
        List<StringId<Vehicle>> vehiclesInSecondScenario = new ArrayList<>();
        vehiclesInSecondScenario.addAll(vehiclesInBothScenarios);
        vehiclesInSecondScenario.addAll(generateVehicleIds(1250));
        var resultOfFirstScenario = runScenario(vehiclesInFirstScenario);
        var resultOfSecondScenario = runScenario(vehiclesInSecondScenario);

        assertThat(vehiclesInBothScenarios)
                .allSatisfy(vehicleId -> {
                    var firstVehicleFromLocalRandom = resultOfFirstScenario.get(vehicleId).a;
                    var secondVehicleFromLocalRandom = resultOfSecondScenario.get(vehicleId).a;
                    assertThat(firstVehicleFromLocalRandom)
                            .isNotNull()
                            .isEqualTo(secondVehicleFromLocalRandom);
                })
                // It is entirely possible for the vehicles created with the global random to be coincidentally the same,
                // but we would expect most of them to be different.
                .anySatisfy(vehicleId -> {
                    var firstVehicleFromGlobalRandom = resultOfFirstScenario.get(vehicleId).b;
                    var secondVehicleFromGlobalRandom = resultOfSecondScenario.get(vehicleId).b;
                    assertThat(firstVehicleFromGlobalRandom)
                            .isNotNull()
                            .isNotEqualTo(secondVehicleFromGlobalRandom);
                });
    }

    private static ImmutableMap<StringId<Vehicle>, Pair<Vehicle, Vehicle>> runScenario(List<StringId<Vehicle>> vehicles) {
        RepeatableRandom.initialiseWithSeed(1);
        // the creation of the repeatable randoms must be done *before* shuffling the vehicles,
        // as the vehicle shuffling will move on the repeatable random, possibly by a different
        // margin (especially if the scenarios don't have the same number of vehicles).
        var singletonRandom = RepeatableRandom.newInstance();
        var repeatableRandomMap = RepeatableRandomMap.<StringId<Vehicle>>createFromMasterRepeatableRandomAndString(id -> id.id);
        RepeatableRandom.shuffle(vehicles);
        return ImmutableMapFactory.createFromKeys(
                vehicles,
                k -> Pair.of(
                        randomlyGenerateVehicle(repeatableRandomMap.get(k), k),
                        randomlyGenerateVehicle(singletonRandom, k)
                )
        );
    }

    private static Vehicle randomlyGenerateVehicle(InstancedRepeatableRandom instancedRepeatableRandom, StringId<Vehicle> vehicleId) {
        return new Vehicle(
                vehicleId,
                instancedRepeatableRandom.randomElementOf(Arrays.asList(Color.values())),
                instancedRepeatableRandom.nextInt(7) + 2
        );
    }

    private static Set<StringId<Vehicle>> generateVehicleIds(int numIdsToGenerate) {
        return Stream.generate(RepeatableRandomMapTest::generateVehicleId)
                .limit(numIdsToGenerate)
                .collect(Collectors.toSet());
    }

    private static StringId<Vehicle> generateVehicleId() {
        return StringId.create(RepeatableRandom.nextUUID().toString());
    }

    @Nested
    class CreateFromMasterRepeatableRandomTest {

        @Test
        void testDeterminism() {
            RepeatableRandom.initialiseWithSeed(0);
            RepeatableRandomMap<Long> firstMasterSeedMap =
                    RepeatableRandomMap.createFromMasterRepeatableRandomAndLong(Long::longValue);

            RepeatableRandom.initialiseWithSeed(0);
            RepeatableRandomMap<Long> secondMasterSeedMap =
                    RepeatableRandomMap.createFromMasterRepeatableRandomAndLong(Long::longValue);

            Assertions.assertEquals(firstMasterSeedMap.get(1L).nextLong(), secondMasterSeedMap.get(1L).nextLong());
        }

        @Test
        void testDependenceOnMasterSeed() {
            RepeatableRandom.initialiseWithSeed(0);
            RepeatableRandomMap<Long> firstMasterSeedMap =
                    RepeatableRandomMap.createFromMasterRepeatableRandomAndLong(Long::longValue);

            RepeatableRandom.initialiseWithSeed(1);
            RepeatableRandomMap<Long> secondMasterSeedMap =
                    RepeatableRandomMap.createFromMasterRepeatableRandomAndLong(Long::longValue);

            Assertions.assertNotEquals(
                    firstMasterSeedMap.get(1L).nextLong(),
                    secondMasterSeedMap.get(1L).nextLong(),
                    "Two different master seeds should produce different results"
            );
        }

        @Test
        void testIndependenceOfElements() {
            RepeatableRandom.initialiseWithSeed(0);
            RepeatableRandomMap<Long> repeatableRandomMap =
                    RepeatableRandomMap.createFromMasterRepeatableRandomAndLong(Long::longValue);

            Assertions.assertNotEquals(
                    repeatableRandomMap.get(1L),
                    repeatableRandomMap.get(2L),
                    "Two distinct elements of the map should have distinct random generators"
            );
            Assertions.assertNotEquals(
                    repeatableRandomMap.get(1L).nextLong(),
                    repeatableRandomMap.get(2L).nextLong(),
                    "Two distinct elements of the map should not be initialised with the same seed"
            );
        }

        @Test
        void testIndependenceFromAdditionalIds() {
            RepeatableRandom.initialiseWithSeed(0);
            RepeatableRandomMap<Long> firstMasterSeedMap =
                    RepeatableRandomMap.createFromMasterRepeatableRandomAndLong(Long::longValue);
            long valueWithoutOtherIdCall = firstMasterSeedMap.get(2L).nextLong();

            RepeatableRandom.initialiseWithSeed(0);
            RepeatableRandomMap<Long> secondMasterSeedMap =
                    RepeatableRandomMap.createFromMasterRepeatableRandomAndLong(Long::longValue);

            secondMasterSeedMap.get(1L).nextLong(); // Testing that this does not affect the result
            long valueAfterOtherIdCall = secondMasterSeedMap.get(2L).nextLong();

            Assertions.assertEquals(
                    valueWithoutOtherIdCall,
                    valueAfterOtherIdCall,
                    "The order and the number of IDs initialised in the map should "
                            + "not affect the individual behaviours"
            );
        }

        @Test
        void testIndependenceFromRepeatableRandomNextCalls() {
            RepeatableRandom.initialiseWithSeed(0);
            RepeatableRandomMap<Long> firstMasterSeedMap =
                    RepeatableRandomMap.createFromMasterRepeatableRandomAndLong(Long::longValue);

            long valueWithoutRepeatableRandomCall = firstMasterSeedMap.get(1L).nextLong();

            RepeatableRandom.initialiseWithSeed(0);
            RepeatableRandomMap<Long> secondMasterSeedMap =
                    RepeatableRandomMap.createFromMasterRepeatableRandomAndLong(Long::longValue);

            RepeatableRandom.nextLong(); // Testing that this does not affect the result

            long valueAfterRepeatableRandomCall = secondMasterSeedMap.get(1L).nextLong();

            Assertions.assertEquals(
                    valueWithoutRepeatableRandomCall,
                    valueAfterRepeatableRandomCall,
                    "Calls to RepeatableRandom after RepeatableRandomMap construction "
                            + "should not affect the behaviour of the elements"
            );
        }
    }

    @Test
    void testDeterminismOfCreateFromMasterRepeatableRandomAndString() {
        RepeatableRandom.initialiseWithSeed(0);
        RepeatableRandomMap<String> firstMasterSeedMap =
                RepeatableRandomMap.createFromMasterRepeatableRandomAndString(Function.identity());

        RepeatableRandom.initialiseWithSeed(0);
        RepeatableRandomMap<String> secondMasterSeedMap =
                RepeatableRandomMap.createFromMasterRepeatableRandomAndString(Function.identity());

        Assertions.assertEquals(firstMasterSeedMap.get("Test").nextLong(), secondMasterSeedMap.get("Test").nextLong());
    }
}
