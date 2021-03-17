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
package com.ocadotechnology.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class TypesTest {
    @Test
    public void testGetInstancesOfTypeUntil_whenGivenAnEmptyCollection_noResultsAreReturned() throws Exception {
        Assertions.assertEquals(ImmutableList.of(), Types.getInstancesOfTypeUntil(Lists.newArrayList(), Object.class, n -> true));
    }

    @Test
    public void testGetInstancesOfTypeUntil_whenPredicateAlwaysFalse_noResultsAreReturned() throws Exception {
        Assertions.assertEquals(ImmutableList.of(), Types.getInstancesOfTypeUntil(Lists.newArrayList(1, 2, 3), Integer.class, n -> false));
    }

    @Test
    public void testGetInstancesOfTypeUntil_whenPredicateAlwaysTrue_allResultsAreReturned() throws Exception {
        Assertions.assertEquals(ImmutableList.of(1, 2, 3), Types.getInstancesOfTypeUntil(Lists.newArrayList(1, 2, 3), Integer.class, n -> true));
    }

    @Test
    public void testGetInstancesOfTypeUntil_whenPredicateAlwaysTrue_resultsAreFilteredByClass() throws Exception {
        Assertions.assertEquals(ImmutableList.of(1, 3), Types.getInstancesOfTypeUntil(Lists.newArrayList(1, 2.0, 3), Integer.class, n -> true));
    }

    @Test
    public void testGetInstancesOfTypeUntil_negativeNumberOccurs() throws Exception {
        Assertions.assertEquals(ImmutableList.of(1.0, 3.0), Types.getInstancesOfTypeUntil(Lists.newArrayList(1.0, 2, 3.0, -4.0, 5.0), Double.class, number -> number.intValue() >=  0));
    }
}