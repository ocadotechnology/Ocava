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
package com.ocadotechnology.tableio.csv;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.ocadotechnology.tableio.ExampleEnum;

class CSVRowBuilderTest {

    @Test
    void testRowWasCreatedCorrectly() {
        CSVRow<TestCSVColumn> rowToTest = CSVRowBuilder.create(TestCSVColumn.class)
                .withValue(TestCSVColumn.INTEGER, 1)
                .withValue(TestCSVColumn.DOUBLE, 2d)
                .withValue(TestCSVColumn.BIG_DECIMAL, 3d)
                .withValue(TestCSVColumn.BOOLEAN, "True")
                .withValue(TestCSVColumn.ENUM, "EXAMPLE")
                .build();
        ImmutableMap<String, String> expectedLineByHeaderMap = ImmutableMap.of(
                "INTEGER", "1",
                "DOUBLE", "2.0",
                "BIG_DECIMAL", "3.0",
                "BOOLEAN", "True",
                "ENUM", "EXAMPLE"
        );
        Assertions.assertEquals(expectedLineByHeaderMap, rowToTest.lineByHeader);
        Assertions.assertEquals(2d, rowToTest.parseDouble(TestCSVColumn.DOUBLE));
        Assertions.assertEquals(new BigDecimal("3.0"), rowToTest.parseBigDecimal(TestCSVColumn.BIG_DECIMAL));
        Assertions.assertEquals(true, rowToTest.parseBoolean(TestCSVColumn.BOOLEAN));
        Assertions.assertEquals(ExampleEnum.EXAMPLE, rowToTest.parseEnum(TestCSVColumn.ENUM, ExampleEnum.class));
    }
}
