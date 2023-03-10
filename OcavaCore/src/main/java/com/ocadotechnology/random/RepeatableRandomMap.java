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
package com.ocadotechnology.random;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToLongFunction;

import com.ocadotechnology.maths.stats.Distributions;
import com.ocadotechnology.maths.stats.RandomGeneratorDistributions;

/**
 * A class that wraps a generic to InstancedRepeatableRandom map and a corresponding mapping function.
 * Every time get is called, the mapping function is called and the resulting InstancedRepeatableRandom is cached
 * and reused for future get calls with the same argument.
 */
public class RepeatableRandomMap<T> {
    private final Map<T, InstancedRepeatableRandom> repeatableRandomByGeneric = new HashMap<>();
    private final Function<T, InstancedRepeatableRandom> keyValueMapper;

    public RepeatableRandomMap(Function<T, InstancedRepeatableRandom> mapper) {
        this.keyValueMapper = mapper;
    }

    private static long hashSeed(long a, long b) {
        return 13 * a + 31 * b;
    }

    // Note that String::hashCode is deterministic.
    private static long hashString(String s) {
        return s.hashCode();
    }

    /**
     * The repeatable randoms are instantiated from a seed dependent on a Long representation of the Generic
     * instance (such as an ID) and a (master) random value (the same value for all instances).
     * This guarantees the independence from successive calls to RepeatableRandom and amongst the different
     * instances in the map. However, it makes all instances dependent on the master seed.
     */
    public static <T> RepeatableRandomMap<T> createFromMasterRepeatableRandomAndLong(
            ToLongFunction<T> keyToLongMapper
    ) {
        long masterRepeatableRandomDependentValue = RepeatableRandom.nextLong();

        return new RepeatableRandomMap<>(
                t -> InstancedRepeatableRandom.fromSeed(
                        RepeatableRandomMap.hashSeed(
                                keyToLongMapper.applyAsLong(t),
                                masterRepeatableRandomDependentValue
                        )
                )
        );
    }

    public static <T> RepeatableRandomMap<T> createFromMasterRepeatableRandomAndString(
            Function<T, String> keyToStringMapper
    ) {
        return createFromMasterRepeatableRandomAndLong(t -> hashString(keyToStringMapper.apply(t)));
    }

    public InstancedRepeatableRandom get(T t) {
        return repeatableRandomByGeneric.computeIfAbsent(t, keyValueMapper);
    }

    public Distributions getDistributions(T t) {
        return new RandomGeneratorDistributions(get(t));
    }
}
