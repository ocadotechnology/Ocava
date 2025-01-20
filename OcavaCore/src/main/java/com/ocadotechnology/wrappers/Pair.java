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
package com.ocadotechnology.wrappers;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.common.base.MoreObjects;

public class Pair<A, B> implements Serializable {
    private static final long serialVersionUID = 1L;
    public final A a;
    public final B b;

    private Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public static <A, B> Pair<A, B> of(A a, B b) {
        return new Pair<>(a, b);
    }

    /**
     * Converts a Map.Entry into a Pair
     */
    public static <A, B> Pair<A, B> fromEntry(Map.Entry<A, B> entry) {
        return Pair.of(entry.getKey(), entry.getValue());
    }

    /**
     * To be used when Pair is boxed in a monad (Stream/Optional), Map
     * the two pair values using the function provided.
     * @param f The function to map from the two pair values
     * @param <A> Pair&lt;A,?&gt;
     * @param <B> Pair&lt;?,B&gt;
     * @param <C> Resulting type
     * @return New value of type C
     */
    public static <A, B, C> Function<Pair<A, B>, C> map(BiFunction<A, B, C> f) {
        return p -> f.apply(p.a, p.b);
    }

    /**
     * To be used when Pair is boxed in a monad (Stream/Optional), Map
     * the 'a' value while keeping the 'b' value in place.
     * @param f The mapping function to apply to side 'a'
     * @param <A> Original Pair&lt;A, ?&gt;
     * @param <B> Pair&lt;?, B&gt;
     * @param <C> New Pair&lt;C, ?&gt;
     * @return New pair with side 'a' type changed from A -&gt; C
     */
    public static <A, B, C> Function<Pair<A, B>, Pair<C, B>> mapA(Function<A, C> f) {
        return p -> Pair.of(f.apply(p.a), p.b);
    }

    /**
     * To be used when Pair is boxed in a monad (Stream/Optional), Map
     * the 'b' value while keeping the 'a' value in place.
     * @param f The mapping function to apply to side 'a'
     * @param <A> Pair&lt;A, ?&gt;
     * @param <B> Original Pair&lt;?, B&gt;
     * @param <C> New Pair&lt;?, C&gt;
     * @return New pair with side 'b' type changed from B -&gt; C
     */
    public static <A, B, C> Function<Pair<A, B>, Pair<A, C>> mapB(Function<B, C> f) {
        return p -> Pair.of(p.a, f.apply(p.b));
    }

    /**
     * Return the first element of the pair (a)
     * @return first element of the pair
     */
    public A a() {
        return a;
    }

    /**
     * Return the second element of the pair (b)
     * @return second element of the pair
     */
    public B b() {
        return b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(a, pair.a) && Objects.equals(b, pair.b);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("a", a)
                .add("b", b)
                .toString();
    }
}
