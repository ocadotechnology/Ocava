/*
 * Copyright © 2017 Ocado (Ocava)
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

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.ocadotechnology.validation.Failer;

/**
 * A static utility providing deterministic "randomness" (via either seeding or fixing the value).  It should be
 * initialised at start of application before use via either `initialiseWithSeed` or `initialiseWithFixedValue`.
 *
 * NOTE: this class does not provide any guarantees of determinism in a multi-threaded application.  It is possible for
 * applications which use concepts such as Stream.parallelStream to retain deterministic behaviour, but each thread must
 * be passed an independent random instance.  For these use cases, we provide {@link InstancedRepeatableRandom} which
 * is the backing class for this static interface.
 */
public class RepeatableRandom {
    public static final double MIN_FIXED_VALUE = 0.0;
    public static final double MAX_FIXED_VALUE = 0.999999999;

    private static InstancedRepeatableRandom instancedRepeatableRandom;

    private RepeatableRandom() {
        throw Failer.fail("Do not instantiate static utility class");
    }

    public static void initialiseWithSeed(long masterSeed) {
        instancedRepeatableRandom = InstancedRepeatableRandom.fromSeed(masterSeed);
    }

    public static void initialiseWithFixedValue(double fixedValue) {
        instancedRepeatableRandom = InstancedRepeatableRandom.fromFixedValue(fixedValue);
    }

    public static void clear() {
        instancedRepeatableRandom = null;
    }

    public static InstancedRepeatableRandom newInstance() {
        return InstancedRepeatableRandom.fromSeed(instancedRepeatableRandom.nextLong());
    }

    public static double nextDouble() {
        return instancedRepeatableRandom.nextDouble();
    }

    public static int nextInt(int bound) {
        return instancedRepeatableRandom.nextInt(bound);
    }

    public static boolean nextBoolean() {
        return instancedRepeatableRandom.nextBoolean();
    }

    public static UUID nextUUID() {
        return instancedRepeatableRandom.nextUUID();
    }

    public static long nextLong() {
        return instancedRepeatableRandom.nextLong();
    }

    public static double nextGaussian() {
        return instancedRepeatableRandom.nextGaussian();
    }

    public static <T> void shuffle(List<T> list) {
        instancedRepeatableRandom.shuffle(list);
    }

    public static <T> void shuffle(T[] array) {
        instancedRepeatableRandom.shuffle(array);
    }

    public static <T> T randomElementOf(Collection<T> collection) {
        return instancedRepeatableRandom.randomElementOf(collection);
    }

    public static <T> T randomElementOf(List<T> list) {
        return instancedRepeatableRandom.randomElementOf(list);
    }
}
