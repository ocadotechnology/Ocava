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

import java.io.BufferedReader;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BufferedReaderBuilderTest {
    private BufferedReaderBuilder bufferedReaderBuilder = BufferedReaderBuilder.create();

    private String firstTestString = "Test";
    private String secondTestString = "AnotherTest";
    private String thirdTestString = "A Third String to Test";
    private String expectedStringsWithoutNewLine = firstTestString + secondTestString + thirdTestString;

    @Test
    void testAppend() throws IOException {
        String actualString = bufferedReaderBuilder
                .append(firstTestString)
                .append(secondTestString)
                .append(thirdTestString)
                .build().readLine();
        Assertions.assertEquals(expectedStringsWithoutNewLine, actualString);
    }

    @Test
    void testAppendOrderMatters() throws IOException {
        String actualString = bufferedReaderBuilder
                .append(thirdTestString)
                .append(firstTestString)
                .append(secondTestString)
                .build().readLine();
        Assertions.assertNotEquals(expectedStringsWithoutNewLine, actualString);
    }

    @Test
    void testAppendLine() throws IOException {
        BufferedReader reader = bufferedReaderBuilder
                .appendLine(firstTestString)
                .appendLine(secondTestString)
                .appendLine(thirdTestString)
                .build();
        Assertions.assertEquals(reader.readLine(), firstTestString);
        Assertions.assertEquals(reader.readLine(), secondTestString);
        Assertions.assertEquals(reader.readLine(), thirdTestString);
    }

    @Test
    void testAppendLineOrderMatters() throws IOException {
        BufferedReader reader = bufferedReaderBuilder
                .appendLine(firstTestString)
                .appendLine(thirdTestString)
                .appendLine(secondTestString)
                .build();
        Assertions.assertEquals(reader.readLine(), firstTestString);
        Assertions.assertNotEquals(reader.readLine(), secondTestString);
        Assertions.assertNotEquals(reader.readLine(), thirdTestString);
    }
}
