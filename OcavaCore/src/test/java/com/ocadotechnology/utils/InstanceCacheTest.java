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
package com.ocadotechnology.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

class InstanceCacheTest {
    private static final Instant INSTANCE_1 = Instant.EPOCH;
    private static final Instant INSTANCE_2 = Instant.EPOCH.plus(1, ChronoUnit.DAYS);
    private static final Instant INSTANCE_2B = Instant.EPOCH.plus(1, ChronoUnit.DAYS);

    private final InstanceCache<Instant> cache = new InstanceCache<>();

    @Test
    void getEqual_whenCacheEmpty_returnsSelfsameObject() {
        Instant instance = cache.getEqual(INSTANCE_1);
        assertThat(instance).isSameAs(INSTANCE_1);
    }

    @Test
    void getEqual_whenPassedObjectAlreadyInCache_returnsPreviousObjectNotNewOne() {
        cache.getEqual(INSTANCE_2);

        Instant instance = cache.getEqual(INSTANCE_2B);
        assertThat(instance).isSameAs(INSTANCE_2).isNotSameAs(INSTANCE_2B);
    }

    @Test
    void getEqual_whenWrongObjectInCache_returnsSelfsameObject() {
        cache.getEqual(INSTANCE_1);

        Instant instance = cache.getEqual(INSTANCE_2);
        assertThat(instance).isSameAs(INSTANCE_2);
    }
}
