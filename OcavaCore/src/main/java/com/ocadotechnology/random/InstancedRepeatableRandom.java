/*
 * Copyright © 2017-2021 Ocado (Ocava)
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.LongSupplier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.google.common.base.Preconditions;

/**
 * A static utility providing deterministic "randomness" (via either seeding or fixing the value).
 *
 * NOTE: this class does not provide any guarantees of determinism in a multi-threaded application.  It is possible for
 * applications which use concepts such as Stream.parallelStream to retain deterministic behaviour, but each thread must
 * be passed an independent instance, created from a deterministic seed.
 */
@SuppressFBWarnings(value = "DMI_RANDOM_USED_ONLY_ONCE", justification = "Random object is stored in the instantiated class")
public class InstancedRepeatableRandom {
    private final Random randomInstance;

    private final DoubleSupplier repeatableDoubleSupplier;
    private final IntUnaryOperator repeatableIntSupplier;
    private final LongSupplier repeatableLongSupplier;
    private final BooleanSupplier repeatableBooleanSupplier;
    private final DoubleSupplier repeatableGaussianSupplier;
    private final ByteArraySupplier repeatableByteArraySupplier;

    private InstancedRepeatableRandom(
            Random randomInstance,
            DoubleSupplier repeatableDoubleSupplier,
            IntUnaryOperator repeatableIntSupplier,
            LongSupplier repeatableLongSupplier,
            BooleanSupplier repeatableBooleanSupplier,
            DoubleSupplier repeatableGaussianSupplier,
            ByteArraySupplier repeatableByteArraySupplier) {
        this.randomInstance = randomInstance;
        this.repeatableDoubleSupplier = repeatableDoubleSupplier;
        this.repeatableIntSupplier = repeatableIntSupplier;
        this.repeatableLongSupplier = repeatableLongSupplier;
        this.repeatableBooleanSupplier = repeatableBooleanSupplier;
        this.repeatableGaussianSupplier = repeatableGaussianSupplier;
        this.repeatableByteArraySupplier = repeatableByteArraySupplier;
    }

    public static InstancedRepeatableRandom fromSeed(long masterSeed) {
        Random random = new Random(masterSeed);
        return new InstancedRepeatableRandom(
                random,
                random::nextDouble,
                random::nextInt,
                random::nextLong,
                random::nextBoolean,
                random::nextGaussian,
                sizeOfArr -> {
                    byte[] ret = new byte[sizeOfArr];
                    random.nextBytes(ret);
                    return ret;
                }
        );
    }

    public static InstancedRepeatableRandom fromFixedValue(double fixedValue) {
        Preconditions.checkState(RepeatableRandom.MIN_FIXED_VALUE <= fixedValue && RepeatableRandom.MAX_FIXED_VALUE >= fixedValue,
                "Selected fixed value %s is invalid", fixedValue);

        return new InstancedRepeatableRandom(
                new Random((long) fixedValue),
                () -> fixedValue,
                range -> (int) (fixedValue * range),
                () -> (long) fixedValue,
                () -> fixedValue > 0,
                () -> fixedValue,
                sizeOfArr -> {
                    byte[] ret = new byte[sizeOfArr];
                    Arrays.fill(ret, Double.valueOf(fixedValue).byteValue());
                    return ret;
                }
        );
    }

    public double nextDouble() {
        return repeatableDoubleSupplier.getAsDouble();
    }

    /**
     * The form of nextDouble used by DoubleStream Spliterators.
     *
     * @param origin the least value, unless greater than bound
     * @param bound the upper bound (inclusive)
     * @return a pseudorandom value
     * @throws IllegalStateException if {@code origin} is greater than {@code bound}
     */
    public double nextDouble(double origin, double bound) {
        Preconditions.checkState(origin <= bound, "bound must be greater than or equal to origin");

        double random = nextDouble() * (bound - origin) + origin;

        if (random > bound) { // correct for rounding
            random = Double.longBitsToDouble(Double.doubleToLongBits(bound) - 1);
        }
        return random;
    }

    public int nextInt(int bound) {
        return repeatableIntSupplier.applyAsInt(bound);
    }

    public boolean nextBoolean() {
        return repeatableBooleanSupplier.getAsBoolean();
    }

    public UUID nextUUID() {
        return UUID.nameUUIDFromBytes(repeatableByteArraySupplier.getBytes(16));
    }

    public long nextLong() {
        return repeatableLongSupplier.getAsLong();
    }

    public double nextGaussian() {
        return repeatableGaussianSupplier.getAsDouble();
    }

    public <T> void shuffle(List<T> list) {
        Collections.shuffle(list, randomInstance);
    }

    public <T> void shuffle(T[] array) {
        for (int i = array.length; i > 1; --i) {
            int swapPosition = repeatableIntSupplier.applyAsInt(i);
            T t = array[i - 1];
            array[i - 1] = array[swapPosition];
            array[swapPosition] = t;
        }
    }

    public <T> T randomElementOf(Collection<T> collection) {
        Preconditions.checkState(collection != null && !collection.isEmpty(),
                "Collection is null or has no elements");
        List<T> queryable = new ArrayList<>(collection);
        int indexToReturn = nextInt(queryable.size());
        return queryable.get(indexToReturn);
    }

    public <T> T randomElementOf(List<T> list) {
        Preconditions.checkState(list != null && !list.isEmpty(),
                "Input is null or has no elements");
        int indexToReturn = nextInt(list.size());
        return list.get(indexToReturn);
    }
}
