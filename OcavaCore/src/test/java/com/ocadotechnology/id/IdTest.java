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
package com.ocadotechnology.id;

import java.util.stream.LongStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IdTest {
    private static final long RAW_ID = 532;

    @Test
    void cachedIdReturnsTheSameInstance() {
        Assertions.assertSame(Id.createCached(RAW_ID), Id.createCached(RAW_ID));
    }

    @Test
    void idDoesNotReturnTheSameInstance() {
        Assertions.assertNotSame(Id.create(RAW_ID), Id.create(RAW_ID));
    }

    @Test
    void reachedMaxOfYourCache() {
        Id<Object> cached = Id.createCached(RAW_ID);
        LongStream.range(0, 6_000_000).forEach(Id::createCached);
        Assertions.assertNotSame(cached, Id.createCached(RAW_ID));
    }

    @Test
    void cachedIdAndIdDoesNotReturnTheSameInstance() {
        Assertions.assertNotSame(Id.createCached(RAW_ID), Id.create(RAW_ID));
    }

}
