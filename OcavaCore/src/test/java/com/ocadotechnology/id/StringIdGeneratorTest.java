/*
 * Copyright © 2017-2022 Ocado (Ocava)
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
package com.ocadotechnology.id;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringIdGeneratorTest {

    @Test
    public void generateTwoStringIds() {
        StringId<StringIdGeneratorTest> id1 = StringIdGenerator.getId(StringIdGeneratorTest.class);
        StringId<StringIdGeneratorTest> id2 = StringIdGenerator.getId(StringIdGeneratorTest.class);
        Assertions.assertNotNull(id1);
        Assertions.assertNotNull(id2);
        Assertions.assertNotEquals(id1, id2);
    }
}
