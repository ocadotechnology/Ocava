/*
 * Copyright © 2017-2023 Ocado (Ocava)
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

import java.util.Collection;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

public class CollectionUtils {
    private CollectionUtils() {
        // this class cannot be instantiated
    }
    public static <T> int sumInts(Collection<T> values, ToIntFunction<T> toInt) {
        return values.stream().mapToInt(toInt).sum();
    }

    public static int sumInts(Collection<Integer> ints) {
        return sumInts(ints, Integer::intValue);
    }

    public static <T> double sumDoubles(Collection<T> values, ToDoubleFunction<T> toDouble) {
        return values.stream().mapToDouble(toDouble).sum();
    }

    public static double sumDoubles(Collection<Double> values) {
        return sumDoubles(values, Double::doubleValue);
    }
}
