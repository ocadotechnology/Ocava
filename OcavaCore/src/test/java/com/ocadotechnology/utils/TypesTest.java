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

import java.util.List;
import java.util.stream.Stream;

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

    @Test
    public void testSafeStreamInstancesOfType_whenGivenAnEmptyCollection_noResultsAreReturned() {
        Assertions.assertEquals(ImmutableList.of(), Types.safeStreamInstancesOfType(Lists.newArrayList(), Object.class).collect(ImmutableList.toImmutableList()));
        Assertions.assertEquals(ImmutableList.of(), Types.safeStreamInstancesOfType(Stream.of(), Object.class).collect(ImmutableList.toImmutableList()));
    }

    @Test
    public void testSafeStreamInstancesOfType_whenGivenSomeValuesOfType_resultsCorrectlyFiltered() {
        Assertions.assertEquals(ImmutableList.of(1, 3), Types.safeStreamInstancesOfType(List.of(1, 2.1, 3), Integer.class).collect(ImmutableList.toImmutableList()));
        Assertions.assertEquals(ImmutableList.of(1, 3), Types.safeStreamInstancesOfType(Stream.of(1, 2.1, 3), Integer.class).collect(ImmutableList.toImmutableList()));
    }

    @Test
    public void testSafeGetInstancesOfType_whenGivenAnEmptyCollection_noResultsAreReturned() {
        Assertions.assertEquals(ImmutableList.of(), Types.safeGetInstancesOfType(Lists.newArrayList(), Object.class));
        Assertions.assertEquals(ImmutableList.of(), Types.safeGetInstancesOfType(Stream.of(), Object.class));
    }

    @Test
    public void testSafeGetInstancesOfType_whenGivenSomeValuesOfType_resultsCorrectlyFiltered() {
        Assertions.assertEquals(ImmutableList.of(1, 3), Types.safeGetInstancesOfType(List.of(1, 2.1, 3), Integer.class));
        Assertions.assertEquals(ImmutableList.of(1, 3), Types.safeGetInstancesOfType(Stream.of(1, 2.1, 3), Integer.class));
    }

    @Test
    public void testSafeStreamInstancesOfTypeOrFail_whenGivenAnEmptyCollection_noResultsAreReturned() {
        Assertions.assertEquals(ImmutableList.of(), Types.safeStreamInstancesOfTypeOrFail(Lists.newArrayList(), Object.class).collect(ImmutableList.toImmutableList()));
        Assertions.assertEquals(ImmutableList.of(), Types.safeStreamInstancesOfTypeOrFail(Stream.of(), Object.class).collect(ImmutableList.toImmutableList()));
    }

    @Test
    public void testSafeStreamInstancesOfTypeOrFail_whenGivenSomeValuesOfType_thenThrows() {
        Assertions.assertThrows(IllegalStateException.class, () -> Types.safeStreamInstancesOfTypeOrFail(List.of(1, 2.1, 3), Integer.class).collect(ImmutableList.toImmutableList()));
        Assertions.assertThrows(IllegalStateException.class, () -> Types.safeStreamInstancesOfTypeOrFail(Stream.of(1, 2.1, 3), Integer.class).collect(ImmutableList.toImmutableList()));
    }

    @Test
    public void testSafeStreamInstancesOfTypeOrFail_whenGivenAllValuesOfType_thenResultsCorrectlyReturned() {
        Assertions.assertEquals(ImmutableList.of(1, 2, 3), Types.safeStreamInstancesOfTypeOrFail(List.of(1, 2, 3), Integer.class).collect(ImmutableList.toImmutableList()));
        Assertions.assertEquals(ImmutableList.of(1, 2, 3), Types.safeStreamInstancesOfTypeOrFail(Stream.of(1, 2, 3), Integer.class).collect(ImmutableList.toImmutableList()));
    }

    @Test
    public void testSafeGetInstancesOfTypeOrFail_whenGivenAnEmptyCollection_noResultsAreReturned() {
        Assertions.assertEquals(ImmutableList.of(), Types.safeGetInstancesOfTypeOrFail(Lists.newArrayList(), Object.class));
        Assertions.assertEquals(ImmutableList.of(), Types.safeGetInstancesOfTypeOrFail(Stream.of(), Object.class));
    }

    @Test
    public void testSafeGetInstancesOfTypeOrFail_whenGivenSomeValuesOfType_thenThrows() {
        Assertions.assertThrows(IllegalStateException.class, () -> Types.safeGetInstancesOfTypeOrFail(List.of(1, 2.1, 3), Integer.class));
        Assertions.assertThrows(IllegalStateException.class, () -> Types.safeGetInstancesOfTypeOrFail(Stream.of(1, 2.1, 3), Integer.class));
    }

    @Test
    public void testSafeGetInstancesOfTypeOrFail_whenGivenAllValuesOfType_thenResultsCorrectlyReturned() {
        Assertions.assertEquals(ImmutableList.of(1, 2, 3), Types.safeGetInstancesOfTypeOrFail(List.of(1, 2, 3), Integer.class));
        Assertions.assertEquals(ImmutableList.of(1, 2, 3), Types.safeGetInstancesOfTypeOrFail(Stream.of(1, 2, 3), Integer.class));
    }

    @Test
    public void testUnunsafeStreamInstancesOfType_whenGivenAnEmptyCollection_noResultsAreReturned() {
        Assertions.assertEquals(ImmutableList.of(), Types.unsafeStreamInstancesOfType(Lists.newArrayList(), Object.class).collect(ImmutableList.toImmutableList()));
        Assertions.assertEquals(ImmutableList.of(), Types.unsafeStreamInstancesOfType(Stream.of(), Object.class).collect(ImmutableList.toImmutableList()));
    }

    @Test
    public void testUnunsafeStreamInstancesOfType_whenGivenSomeValuesOfType_resultsCorrectlyFiltered() {
        Assertions.assertEquals(ImmutableList.of(1, 3), Types.unsafeStreamInstancesOfType(List.of(1, 2.1, 3), Integer.class).collect(ImmutableList.toImmutableList()));
        Assertions.assertEquals(ImmutableList.of(1, 3), Types.unsafeStreamInstancesOfType(Stream.of(1, 2.1, 3), Integer.class).collect(ImmutableList.toImmutableList()));
    }

    @Test
    public void testUnunsafeGetInstancesOfType_whenGivenAnEmptyCollection_noResultsAreReturned() {
        Assertions.assertEquals(ImmutableList.of(), Types.unsafeGetInstancesOfType(Lists.newArrayList(), Object.class));
        Assertions.assertEquals(ImmutableList.of(), Types.unsafeGetInstancesOfType(Stream.of(), Object.class));
    }

    @Test
    public void testUnunsafeGetInstancesOfType_whenGivenSomeValuesOfType_resultsCorrectlyFiltered() {
        Assertions.assertEquals(ImmutableList.of(1, 3), Types.unsafeGetInstancesOfType(List.of(1, 2.1, 3), Integer.class));
        Assertions.assertEquals(ImmutableList.of(1, 3), Types.unsafeGetInstancesOfType(Stream.of(1, 2.1, 3), Integer.class));
    }

    @Test
    public void testUnunsafeStreamInstancesOfTypeOrFail_whenGivenAnEmptyCollection_noResultsAreReturned() {
        Assertions.assertEquals(ImmutableList.of(), Types.unsafeStreamInstancesOfTypeOrFail(Lists.newArrayList(), Object.class).collect(ImmutableList.toImmutableList()));
        Assertions.assertEquals(ImmutableList.of(), Types.unsafeStreamInstancesOfTypeOrFail(Stream.of(), Object.class).collect(ImmutableList.toImmutableList()));
    }

    @Test
    public void testUnunsafeStreamInstancesOfTypeOrFail_whenGivenSomeValuesOfType_thenThrows() {
        Assertions.assertThrows(IllegalStateException.class, () -> Types.unsafeStreamInstancesOfTypeOrFail(List.of(1, 2.1, 3), Integer.class).collect(ImmutableList.toImmutableList()));
        Assertions.assertThrows(IllegalStateException.class, () -> Types.unsafeStreamInstancesOfTypeOrFail(Stream.of(1, 2.1, 3), Integer.class).collect(ImmutableList.toImmutableList()));
    }

    @Test
    public void testUnunsafeStreamInstancesOfTypeOrFail_whenGivenAllValuesOfType_thenResultsCorrectlyReturned() {
        Assertions.assertEquals(ImmutableList.of(1, 2, 3), Types.unsafeStreamInstancesOfTypeOrFail(List.of(1, 2, 3), Integer.class).collect(ImmutableList.toImmutableList()));
        Assertions.assertEquals(ImmutableList.of(1, 2, 3), Types.unsafeStreamInstancesOfTypeOrFail(Stream.of(1, 2, 3), Integer.class).collect(ImmutableList.toImmutableList()));
    }

    @Test
    public void testUnunsafeGetInstancesOfTypeOrFail_whenGivenAnEmptyCollection_noResultsAreReturned() {
        Assertions.assertEquals(ImmutableList.of(), Types.unsafeGetInstancesOfTypeOrFail(Lists.newArrayList(), Object.class));
        Assertions.assertEquals(ImmutableList.of(), Types.unsafeGetInstancesOfTypeOrFail(Stream.of(), Object.class));
    }

    @Test
    public void testUnunsafeGetInstancesOfTypeOrFail_whenGivenSomeValuesOfType_thenThrows() {
        Assertions.assertThrows(IllegalStateException.class, () -> Types.unsafeGetInstancesOfTypeOrFail(List.of(1, 2.1, 3), Integer.class));
        Assertions.assertThrows(IllegalStateException.class, () -> Types.unsafeGetInstancesOfTypeOrFail(Stream.of(1, 2.1, 3), Integer.class));
    }

    @Test
    public void testUnunsafeGetInstancesOfTypeOrFail_whenGivenAllValuesOfType_thenResultsCorrectlyReturned() {
        Assertions.assertEquals(ImmutableList.of(1, 2, 3), Types.unsafeGetInstancesOfTypeOrFail(List.of(1, 2, 3), Integer.class));
        Assertions.assertEquals(ImmutableList.of(1, 2, 3), Types.unsafeGetInstancesOfTypeOrFail(Stream.of(1, 2, 3), Integer.class));
    }
}