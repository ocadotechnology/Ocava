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
package com.ocadotechnology.wrappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

public class PairTest {
    @Test
    public void mapAll() {
        String expectedResult = "1a";
        Optional<Pair<Integer, String>> boxedPair = Optional.of(Pair.of(1, "a"));

        String result = boxedPair.map(Pair.map((a, b) -> "" + a + b)).get();

        assertEquals(expectedResult, result);
    }

    @Test
    public void mapA() {
        Pair<Double, String> expectedResult = Pair.of(2.5, "a");
        Optional<Pair<Integer, String>> boxedPair = Optional.of(Pair.of(1, "a"));

        Pair<Double, String> result = boxedPair.map(Pair.mapA(a -> a + 1.5)).get();

        assertEquals(expectedResult, result);
    }

    @Test
    public void mapB() {
        Pair<Integer, Character> expectedResult = Pair.of(1, 'a');
        Optional<Pair<Integer, String>> boxedPair = Optional.of(Pair.of(1, "a"));

        Pair<Integer, Character> result = boxedPair.map(Pair.mapB(b -> b.charAt(0))).get();

        assertEquals(expectedResult, result);
    }
}
