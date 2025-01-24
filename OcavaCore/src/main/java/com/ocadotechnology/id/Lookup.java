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
package com.ocadotechnology.id;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Mixin allowing instances of implementing classes to look themselves up
 * in {@link java.util.Map}s and {@link Set}s.
 *
 * <p>The intent behind this twofold:</p>
 * <ol>
 *     <li>provide a means of expressing <q>is l in m</q>, or <q>l&rsquo;s value in m</q>
 *     rather than need to reword the expression to <q>does m contain l</q> or <q>get s from m</q></li>
 *     <li>leveraging the fluent language of {@link Optional} notably its concepts of presence and emptiness</li>
 *     <li>helping to design out <code>null</code></li>
 * </ol>
 *
 * <p>Inspiration comes from
 * <a href="https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/ILookup.java">Clojure&rsquo;s <code>ILookup</code></a></p>
 *
 * @param <T> the class&rsquo; own type
 */
@ParametersAreNonnullByDefault
public interface Lookup<T extends Lookup<T>> {
    /**
     * Returns an {@link Optional} containing {@code this} if {@code this} is
     * contained in <var>set</var>.
     *
     * <p>This performs as well as {@link Set#contains(Object)} for <i>set</i>.</p>
     *
     * @param set the {@link Set} to be tested for contents
     * @return an {@link Optional} containing {@code this} if it is in <i>set</i>,
     *         empty otherwise
     */
    default Optional<T> getIn(@CheckForNull Set<? extends Lookup<T>> set) {
        if (set == null) {
            return Optional.empty();
        }
        if (set.contains(this)) {
            return Optional.of((T)this);
        }
        return Optional.empty();
    }

    /**
     * Returns an {@link Optional} containing the value of {@code this} in
     * <i>map</i> if {@code this} is a key of <i>map</i>, empty otherwise.
     *
     * <p>This performs as well as {@link Map#get(Object)} for <i>map</i>.</p>
     *
     * @param map the {@link Map} to be looked up
     * @param <X> the value type of the map
     * @return an {@link Optional} containing: the mapped value of {@code this} if map is non-null and {@code this} is in <var>map</var>, empty otherwise
     */
    default <X> Optional<X> getIn(@CheckForNull Map<? extends Lookup<T>, X> map) {
        return Optional.ofNullable(map == null ? null : map.get(this));
    }

    /** Returns an {@link Optional} containing the value of {@code this} in
     * <i>map</i> if {@code this} is a key of <i>map</i>, with a (possibly
     * {@code null}) default value use if <i>map</i> is <code>null</code>,
     * or does not contain {@code this}.
     *
     * <p>This performs as well as {@link Map#getOrDefault(Object, Object)} for <i>map</i>.</p>
     *
     * @param map the {@link Map} to be looked up
     * @param defaultValue the default value to be supplied if <i>map</i> is <code>null</code>
     *             or <code>this</code> is not a key of <i>map</i>
     * @param <X> the value type of <i>map</i>
     * @return an empty {@link Optional} if <i>map</i> is null, or {@code this} was mapped to null.
     *         An optional containing the mapped value of {@code this} if it was mapped in <i>map</i>,
     *         or <i>defaultValue</i> otherwise
     */
    default <X> Optional<X> getIn(@CheckForNull Map<? extends Lookup<T>, X> map, @CheckForNull X defaultValue) {
        if (map == null) {
            return Optional.ofNullable(defaultValue);
        }
        X value = map.get(this);
        if (value != null) {
            return Optional.of(value);
        }
        // If it matters enough to someone to be able to put null in a map,
        // then it matters that we can detect it through this API.
        if (map.containsKey(this)) {
            return Optional.empty();
        }
        return Optional.ofNullable(defaultValue);
    }

    /** Tests whether {@code this} is in <i>set</i>
     *
     * @param set the set to test for {@code this}&rsquo; presence
     * @return true iff <i>set</i> is not {@code null} and contains {@code this}
     */
    default boolean isIn(@CheckForNull Set<? extends Lookup<T>> set) {
        return set != null && set.contains(this);
    }

    /** Tests whether {@code this} is a key of <i>map</i>
     *
     * @param map the map to test for {@code this}&rsquo; presence as a key
     * @return true iff <i>map</i> is not {@code null} and has {@code this} as a key
     */
    default boolean isKeyIn(@CheckForNull Map<? extends Lookup<T>, ?> map) {
        return map != null && map.containsKey(this);
    }

    /** Tests whether {@code this} is a value of <i>map</i>
     *
     * @param map the map to test for {@code this}&rsquo; presence as a value
     * @return true iff <i>map</i> is not {@code null} and has {@code this} as a value
     */
    default boolean isValueIn(@CheckForNull Map<?, ? extends Lookup<T>> map) {
        return map != null && map.containsValue(this);
    }
}
