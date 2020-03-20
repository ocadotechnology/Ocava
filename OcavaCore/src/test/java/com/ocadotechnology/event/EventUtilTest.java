/*
 * Copyright © 2017-2020 Ocado (Ocava)
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
package com.ocadotechnology.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EventUtilTest {

    // region eventTimeToString(double time)
    @Test
    void testEventTimeToString_PrimitiveDoubleNegativeTime() {
        assertEquals("1969-12-31 23:59:59.979", EventUtil.eventTimeToString(-20.5d));
    }

    @Test
    void testEventTimeToString_PrimitiveDouble_ZeroTime() {
        assertEquals("1970-01-01 00:00:00.000", EventUtil.eventTimeToString(0d));
    }

    @Test
    void testEventTimeToString_PrimitiveDouble_PositiveTime() {
        assertEquals("1970-01-01 00:00:01.500", EventUtil.eventTimeToString(1500d));
    }

    @Test
    void testEventTimeToString_PrimitiveDouble_MinValue() {
        assertThrows(IllegalArgumentException.class, () -> EventUtil.eventTimeToString(-Double.MAX_VALUE));
    }

    @Test
    void testEventTimeToString_PrimitiveDouble_MaxValue() {
        assertThrows(IllegalArgumentException.class, () -> EventUtil.eventTimeToString(Double.MAX_VALUE));
    }

    @Test
    void testEventTimeToString_PrimitiveDouble_MinSupportedValue() {
        // Use long here to avoid precision errors; value will be cast to double at point of use
        long minSupportedTime = -62135596800000L;
        assertEquals("0001-01-01 00:00:00.000", EventUtil.eventTimeToString((double) minSupportedTime));
        assertThrows(IllegalArgumentException.class, () -> EventUtil.eventTimeToString((double) (minSupportedTime - 1)));
    }

    @Test
    void testEventTimeToString_PrimitiveDouble_MaxSupportedTime() {
        // Max supported time is constrained due to use of DoubleMath.roundToLong()
        // Use long here to avoid precision errors; value will be cast to double at point of use
        long maxSupportedTime = 9223372036854775295L;
        // Years greater than 9999 are prefixed by "+", since the year format is "yyyy" - see SignStyle.EXCEEDS_PAD
        assertEquals("+292278994-08-17 07:12:54.784", EventUtil.eventTimeToString((double) maxSupportedTime));
        assertThrows(IllegalArgumentException.class, () -> EventUtil.eventTimeToString((double) (maxSupportedTime + 1)));
    }

    @Test
    void testEventTimeToString_PrimitiveDouble_MaxUnpaddedTime() {
        assertEquals("9999-12-31 23:59:59.999", EventUtil.eventTimeToString(253402300799999d));
    }

    @Test
    void testEventTimeToString_PrimitiveDouble_MinPaddedTime() {
        assertEquals("+10000-01-01 00:00:00.000", EventUtil.eventTimeToString(253402300800000d));
    }

    @Test
    void testEventTimeToString_PrimitiveDouble_NegativeInfinity() {
        assertEquals("-Infinity", EventUtil.eventTimeToString(Double.NEGATIVE_INFINITY));
    }

    @Test
    void testEventTimeToString_PrimitiveDouble_PositiveInfinity() {
        assertEquals("Infinity", EventUtil.eventTimeToString(Double.POSITIVE_INFINITY));
    }

    @Test
    void testEventTimeToString_PrimitiveDouble_Nan() {
        assertEquals("NaN", EventUtil.eventTimeToString(Double.NaN));
    }
    // endregion

    // region eventTimeToString(Double time)
    @Test
    void testEventTimeToString_Double_NullTime() {
        assertEquals("null", EventUtil.eventTimeToString((Double) null));
    }

    @Test
    void testEventTimeToString_Double_NegativeTime() {
        assertEquals("1969-12-31 23:59:59.979", EventUtil.eventTimeToString(Double.valueOf(-20.5)));
    }

    @Test
    void testEventTimeToString_Double_ZeroTime() {
        assertEquals("1970-01-01 00:00:00.000", EventUtil.eventTimeToString(Double.valueOf(0)));
    }

    @Test
    void testEventTimeToString_Double_PositiveTime() {
        assertEquals("1970-01-01 00:00:01.500", EventUtil.eventTimeToString(Double.valueOf(1500)));
    }

    @Test
    void testEventTimeToString_Double_MinValue() {
        assertThrows(IllegalArgumentException.class, () -> EventUtil.eventTimeToString(Double.valueOf(-Double.MAX_VALUE)));
    }

    @Test
    void testEventTimeToString_Double_MaxValue() {
        assertThrows(IllegalArgumentException.class, () -> EventUtil.eventTimeToString(Double.valueOf(Double.MAX_VALUE)));
    }

    @Test
    void testEventTimeToString_Double_MinSupportedTime() {
        // Use long here to avoid precision errors; value will be converted to Double at point of use
        long minSupportedTime = -62135596800000L;
        assertEquals("0001-01-01 00:00:00.000", EventUtil.eventTimeToString(Double.valueOf(minSupportedTime)));
        assertThrows(IllegalArgumentException.class, () -> EventUtil.eventTimeToString(Double.valueOf(minSupportedTime - 1)));
    }

    @Test
    void testEventTimeToString_Double_MaxSupportedTime() {
        // Max supported time is constrained due to use of DoubleMath.roundToLong()
        // Use long here to avoid precision errors; value will be converted to Double at point of use
        long maxSupportedTime = 9223372036854775295L;
        // Years greater than 9999 are prefixed by "+", since the year format is "yyyy" - see SignStyle.EXCEEDS_PAD
        assertEquals("+292278994-08-17 07:12:54.784", EventUtil.eventTimeToString(Double.valueOf(maxSupportedTime)));
        assertThrows(IllegalArgumentException.class, () -> EventUtil.eventTimeToString(Double.valueOf(maxSupportedTime + 1)));
    }

    @Test
    void testEventTimeToString_Double_MaxUnpaddedTime() {
        assertEquals("9999-12-31 23:59:59.999", EventUtil.eventTimeToString(Double.valueOf(253402300799999d)));
    }

    @Test
    void testEventTimeToString_Double_MinPaddedTime() {
        assertEquals("+10000-01-01 00:00:00.000", EventUtil.eventTimeToString(Double.valueOf(253402300800000d)));
    }

    @Test
    void testEventTimeToString_Double_NegativeInfinity() {
        assertEquals("-Infinity", EventUtil.eventTimeToString(Double.valueOf(Double.NEGATIVE_INFINITY)));
    }

    @Test
    void testEventTimeToString_Double_PositiveInfinity() {
        assertEquals("Infinity", EventUtil.eventTimeToString(Double.valueOf(Double.POSITIVE_INFINITY)));
    }

    @Test
    void testEventTimeToString_Double_Nan() {
        assertEquals("NaN", EventUtil.eventTimeToString(Double.valueOf(Double.NaN)));
    }
    // endregion

    // region eventTimeToString(long time)
    @Test
    void testEventTimeToString_PrimitiveLong_NegativeTime() {
        assertEquals("1969-12-31 23:59:59.975", EventUtil.eventTimeToString(-25L));
    }

    @Test
    void testEventTimeToString_PrimitiveLong_ZeroTime() {
        assertEquals("1970-01-01 00:00:00.000", EventUtil.eventTimeToString(0L));
    }

    @Test
    void testEventTimeToString_PrimitiveLong_PositiveTime() {
        assertEquals("1970-01-01 00:00:01.500", EventUtil.eventTimeToString(1500L));
    }

    @Test
    void testEventTimeToString_PrimitiveLong_MinValue() {
        assertThrows(IllegalArgumentException.class, () -> EventUtil.eventTimeToString(Long.MIN_VALUE));
    }

    @Test
    void testEventTimeToString_PrimitiveLong_MaxValue() {
        // Years greater than 9999 are prefixed by "+", since the year format is "yyyy" - see SignStyle.EXCEEDS_PAD
        assertEquals("+292278994-08-17 07:12:55.807", EventUtil.eventTimeToString(Long.MAX_VALUE));
    }

    @Test
    void testEventTimeToString_PrimitiveLong_MinSupportedTime() {
        long minSupportedTime = -62135596800000L;
        assertEquals("0001-01-01 00:00:00.000", EventUtil.eventTimeToString(minSupportedTime));
        assertThrows(IllegalArgumentException.class, () -> EventUtil.eventTimeToString(minSupportedTime - 1));
    }

    @Test
    void testEventTimeToString_PrimitiveLong_MaxUnpaddedTime() {
        assertEquals("9999-12-31 23:59:59.999", EventUtil.eventTimeToString(253402300799999L));
    }

    @Test
    void testEventTimeToString_PrimitiveLong_MinPaddedTime() {
        assertEquals("+10000-01-01 00:00:00.000", EventUtil.eventTimeToString(253402300800000L));
    }
    // endregion

    // region eventTimeToString(Long time)
    @Test
    void testEventTimeToString_Long_NullTime() {
        assertEquals("null", EventUtil.eventTimeToString((Long) null));
    }

    @Test
    void testEventTimeToString_Long_NegativeTime() {
        assertEquals("1969-12-31 23:59:59.975", EventUtil.eventTimeToString(Long.valueOf(-25)));
    }

    @Test
    void testEventTimeToString_Long_ZeroTime() {
        assertEquals("1970-01-01 00:00:00.000", EventUtil.eventTimeToString(Long.valueOf(0)));
    }

    @Test
    void testEventTimeToString_Long_PositiveTime() {
        assertEquals("1970-01-01 00:00:01.500", EventUtil.eventTimeToString(Long.valueOf(1500)));
    }

    @Test
    void testEventTimeToString_Long_MinValue() {
        assertThrows(IllegalArgumentException.class, () -> EventUtil.eventTimeToString(Long.valueOf(Long.MIN_VALUE)));
    }

    @Test
    void testEventTimeToString_Long_MaxValue() {
        // Years greater than 9999 are prefixed by "+", since the year format is "yyyy" - see SignStyle.EXCEEDS_PAD
        assertEquals("+292278994-08-17 07:12:55.807", EventUtil.eventTimeToString(Long.valueOf(Long.MAX_VALUE)));
    }

    @Test
    void testEventTimeToString_Long_MinSupportedTime() {
        long minSupportedTime = -62135596800000L;
        assertEquals("0001-01-01 00:00:00.000", EventUtil.eventTimeToString(Long.valueOf(minSupportedTime)));
        assertThrows(IllegalArgumentException.class, () -> EventUtil.eventTimeToString(Long.valueOf(minSupportedTime - 1)));
    }

    @Test
    void testEventTimeToString_Long_MaxUnpaddedTime() {
        assertEquals("9999-12-31 23:59:59.999", EventUtil.eventTimeToString(Long.valueOf(253402300799999L)));
    }

    @Test
    void testEventTimeToString_Long_MinPaddedTime() {
        assertEquals("+10000-01-01 00:00:00.000", EventUtil.eventTimeToString(Long.valueOf(253402300800000L)));
    }
    // endregion
}
