/*
 * Copyright © 2017-2024 Ocado (Ocava)
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

public class RepeatableRandomTest {
    private static final long FIXED_SEED = 12345;
    private static final int TEST_ITERATIONS = 50;
    private static final int TEST_ITERATIONS_FOR_UNIQUENESS_CHECK = 1_000_000;

    @AfterEach
    public void clearRepeatableRandom() {
        RepeatableRandom.clear();
    }

    @Test
    public void whenUninitialised_thenThrows() {
        Assertions.assertThrows(IllegalStateException.class, RepeatableRandom::newInstance);
    }

    @Test
    public void whenInitialisedSeed_thenDoNoThrow() {
        RepeatableRandom.initialiseWithSeed(1234L);
        Assertions.assertNotNull(RepeatableRandom.newInstance(), "New instance should return an object if initialised.");
    }

    @Test
    public void whenInitialisedFixed_thenDoNoThrow() {
        RepeatableRandom.initialiseWithFixedValue(0.65);
        Assertions.assertNotNull(RepeatableRandom.newInstance(), "New instance should return an object if initialised.");
    }

    @Test
    public void seededRandom_canReturnExpectedBoolean() {
        testFunctionDeterminism(RepeatableRandom::nextBoolean, "nextBoolean");
    }

    @Test
    public void seededRandom_canReturnExpectedLong() {
        testFunctionDeterminism(RepeatableRandom::nextBoolean, "nextBoolean");
    }

    @Test
    public void seededRandom_spawnsDeterministicInstance_returningDeterministicLong() {
        RepeatableRandom.initialiseWithSeed(FIXED_SEED);
        InstancedRepeatableRandom firstInstance = RepeatableRandom.newInstance();
        RepeatableRandom.initialiseWithSeed(FIXED_SEED);
        InstancedRepeatableRandom secondInstance = RepeatableRandom.newInstance();

        ImmutableList<Long> firstPass = IntStream.range(0, TEST_ITERATIONS).mapToObj(i -> firstInstance.nextLong()).collect(ImmutableList.toImmutableList());
        ImmutableList<Long> secondPass = IntStream.range(0, TEST_ITERATIONS).mapToObj(i -> secondInstance.nextLong()).collect(ImmutableList.toImmutableList());

        for (int i = 0; i < TEST_ITERATIONS; ++i) {
            Assertions.assertEquals(firstPass.get(i), secondPass.get(i), "Difference detected at iteration " + i + " of nextLong on a spawned instance of InstancedRepeatableRandom");
        }
    }

    @Test
    public void seededRandom_canReturnExpectedGaussian() {
        testFunctionDeterminism(RepeatableRandom::nextGaussian, "nextGaussian");
    }

    @Test
    public void seededRandom_shufflesListDeterministically() {
        testFunctionDeterminism(() -> {
            List<Integer> mutableInput = Arrays.asList(1, 2, 3, 4, 5);
            RepeatableRandom.shuffle(mutableInput);
            return mutableInput;
        }, "shuffleLists");
    }

    @Test
    public void seededRandom_shufflesArrayDeterministically() {
        testFunctionDeterminism(() -> {
            Integer[] mutableInput = new Integer[]{1, 2, 3, 4, 5};
            RepeatableRandom.shuffle(mutableInput);
            return Arrays.asList(mutableInput); // Array.equals falls back to Object.equals, so we need to convert to a list
        }, "shuffleArrays");
    }

    @Test
    public void seededRandom_throwsException_selectsItemFromEmptyList() {
        RepeatableRandom.initialiseWithSeed(FIXED_SEED);
        List<Integer> input = Collections.emptyList();
        Assertions.assertThrows(IllegalStateException.class, () -> RepeatableRandom.randomElementOf(input));
    }

    @Test
    public void seededRandom_throwsException_selectsItemFromNullList() {
        RepeatableRandom.initialiseWithSeed(FIXED_SEED);
        Assertions.assertThrows(IllegalStateException.class, () -> RepeatableRandom.randomElementOf(null));
    }

    @Test
    public void seededRandom_selectsItemFromListDeterministically() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        testFunctionDeterminism(() -> RepeatableRandom.randomElementOf(input), "randomElementOfList");
    }

    @Test
    public void seededRandom_selectsItemFromCollectionDeterministically() {
        Set<Integer> input = new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4, 5));
        testFunctionDeterminism(() -> RepeatableRandom.randomElementOf(input), "randomElementOfList");
    }

    @Test
    public void randomlyGeneratedUUIDs_AdhereToUUIDSpec4122RFC() {
        RepeatableRandom.initialiseWithSeed(FIXED_SEED);
        String pattern = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$";

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            String uuid = RepeatableRandom.nextUUID().toString();
            Assertions.assertTrue(uuid.matches(pattern), "UUID:" + uuid + " does not match pattern:" + pattern);
        }
    }

    @Test
    public void seededRandom_getDoubleInRange() {
        RepeatableRandom.initialiseWithSeed(FIXED_SEED);
        double origin = 1, bound = 1;
        Assertions.assertEquals(origin, RepeatableRandom.nextDouble(origin, bound));
    }

    @Test
    void testUuid_withRandomSeed() {
        Assertions.assertTrue(verifyUuidUniqueness(System.currentTimeMillis()));
    }

    @Test
    void testUuid_withRepeatableSeed() {
        Assertions.assertTrue(verifyUuidUniqueness(42L));
    }

    private Boolean verifyUuidUniqueness(long seed) {
        System.out.println("Seed: " + seed);
        RepeatableRandom.initialiseWithSeed(seed);

        HashSet<UUID> set = new HashSet<>();
        int i = 0;
        while (i < TEST_ITERATIONS_FOR_UNIQUENESS_CHECK) {
            UUID current = RepeatableRandom.nextUUID();
            if (set.contains(current)) {
                return false;
            }
            set.add(current);
            i++;
        }
        return true;
    }

    private <T> void testFunctionDeterminism(Supplier<T> itemSupplier, String functionName) {
        RepeatableRandom.initialiseWithSeed(FIXED_SEED);
        ImmutableList<T> firstPass = IntStream.range(0, TEST_ITERATIONS).mapToObj(i -> itemSupplier.get()).collect(ImmutableList.toImmutableList());
        RepeatableRandom.initialiseWithSeed(FIXED_SEED);
        ImmutableList<T> secondPass = IntStream.range(0, TEST_ITERATIONS).mapToObj(i -> itemSupplier.get()).collect(ImmutableList.toImmutableList());

        Assertions.assertIterableEquals(firstPass, secondPass, "Mismatch in " + functionName);
    }
}
