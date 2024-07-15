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
package com.ocadotechnology.scenario;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

/**
 * Tests for the ListStepFuture class
 */
@ParametersAreNonnullByDefault
public class ListStepFutureTest {

    @Test
    public void testGetSucceedsWhenAllPopulated() {
        MutableStepFuture<String> future1 = new MutableStepFuture<>();
        MutableStepFuture<String> future2 = new MutableStepFuture<>();
        ListStepFuture<String> listStepFuture = ListStepFuture.of(future1, future2);

        future1.populate("hi");
        future2.populate("hello");

        ImmutableList<String> stepFutureResults = listStepFuture.get();
        Assertions.assertEquals(stepFutureResults, ImmutableList.of("hi", "hello"));
    }

    @Test
    public void testGetFailsWhenNotAllPopulated() {
        MutableStepFuture<String> future1 = new MutableStepFuture<>();
        MutableStepFuture<String> future2 = new MutableStepFuture<>();
        ListStepFuture<String> listStepFuture = ListStepFuture.of(future1, future2);

        future1.populate("hi");

        Assertions.assertThrows(IllegalStateException.class, listStepFuture::get);
    }

    @Test
    public void testGetAnyPopulatedSucceeds() {
        MutableStepFuture<String> future1 = new MutableStepFuture<>();
        MutableStepFuture<String> future2 = new MutableStepFuture<>();
        ListStepFuture<String> listStepFuture = ListStepFuture.of(future1, future2);

        ImmutableList<String> stepFutureResults = listStepFuture.getAnyPopulated();
        Assertions.assertEquals(stepFutureResults, ImmutableList.of());

        future1.populate("hi");
        stepFutureResults = listStepFuture.getAnyPopulated();
        Assertions.assertEquals(stepFutureResults, ImmutableList.of("hi"));

        future2.populate("hello");
        stepFutureResults = listStepFuture.getAnyPopulated();
        Assertions.assertEquals(stepFutureResults, ImmutableList.of("hi", "hello"));
    }

    @Test
    public void testHasBeenPopulated() {
        MutableStepFuture<String> future1 = new MutableStepFuture<>();
        MutableStepFuture<String> future2 = new MutableStepFuture<>();
        ListStepFuture<String> listStepFuture = ListStepFuture.of(future1, future2);

        Assertions.assertFalse(listStepFuture.hasBeenPopulated());

        future1.populate("hi");
        Assertions.assertFalse(listStepFuture.hasBeenPopulated());

        future2.populate("hello");
        Assertions.assertTrue(listStepFuture.hasBeenPopulated());
    }
}
