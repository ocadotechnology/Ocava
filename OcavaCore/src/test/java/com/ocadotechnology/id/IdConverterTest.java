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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class IdConverterTest {

    private static final IdConverter<?> ID_CONVERTER = new IdConverter<>();
    private static final long[] RAW_IDS = new long[]{0, 1, -1, Long.MIN_VALUE, Long.MAX_VALUE, 25, 903, 41985810065818L, -120995018985L};
    private static final Object[] INVALID_IDS = new Object[]{null, new Object(), ID_CONVERTER, LongStream.of(99), "foo", Long.class};

    /**
     * Verifies that the underlying conversion logic works when supplied with a valid ID.
     */
    @DisplayName("Convert valid ID by calling convert() directly")
    @ParameterizedTest(name = "Raw ID = {0}")
    @MethodSource("rawIdsAsLongs")
    void convertManually(long rawId) {
        String rawIdStr = String.valueOf(rawId);
        Id<?> id = ID_CONVERTER.convert(rawIdStr, null);
        assertNotNull(id);
        assertEquals(rawId, id.id);
    }

    private static LongStream rawIdsAsLongs() {
        return Arrays.stream(RAW_IDS);
    }
    
    /**
     * Verifies that the converter is invoked correctly via {@link IdValue} when given a valid ID.
     */
    @DisplayName("Convert valid ID automatically via @IdValue")
    @ParameterizedTest(name = "Raw ID = {0}")
    @MethodSource("rawIdsAsStrings")
    void convertUsingAnnotation(long rawId, @IdValue Id<?> id) {
        assertNotNull(id);
        assertEquals(rawId, id.id);
    }
    
    private static Stream<Arguments> rawIdsAsStrings() {
        return Arrays.stream(RAW_IDS)
                .mapToObj(id -> Arguments.of(id, String.valueOf(id)));
    }

    /**
     * Verifies that the underlying conversion logic throws an exception when supplied with an invalid ID.
     */
    @DisplayName("Convert invalid ID by calling convert() directly")
    @ParameterizedTest(name = "Invalid ID = {0}")
    @MethodSource("invalidIds")
    void convertManually(Object invalidId) {
        String invalidIdStr = String.valueOf(invalidId);
        assertThrows(ArgumentConversionException.class, () -> ID_CONVERTER.convert(invalidIdStr, null));
    }

    private static Stream<Object> invalidIds() {
        return Arrays.stream(INVALID_IDS);
    }

    // Note that it is not possible to test that the conversion invoked via @IdValue fails, as the test
    // body will never be reached and thus there is no opportunity to verify that the conversion failed
}
