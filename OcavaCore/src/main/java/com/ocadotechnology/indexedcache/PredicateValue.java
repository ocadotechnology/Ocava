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
package com.ocadotechnology.indexedcache;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.CheckForNull;

import com.ocadotechnology.id.Identified;

public final class PredicateValue<C extends Identified<?>, R, T> extends AbstractIndex<C> {
    private final Predicate<? super C> predicate;
    private final Function<? super C, R> extract;
    private final BiFunction<T, R, T> leftAccumulate;
    private final BiFunction<T, R, T> leftDecumulate;
    private T current;

    PredicateValue(Predicate<? super C> predicate, Function<? super C, R> extract, BiFunction<T, R, T> leftAccumulate, BiFunction<T, R, T> leftDecumulate, T initial) {
        this(null, predicate, extract, leftAccumulate, leftDecumulate, initial);
    }

    PredicateValue(@CheckForNull String name, Predicate<? super C> predicate, Function<? super C, R> extract, BiFunction<T, R, T> leftAccumulate, BiFunction<T, R, T> leftDecumulate, T initial) {
        super(name);
        this.predicate = predicate;
        this.extract = extract;
        this.leftAccumulate = leftAccumulate;
        this.leftDecumulate = leftDecumulate;
        this.current = initial;
    }

    public T getValue() {
        return current;
    }

    @Override
    protected void add(C object) {
        if (predicate.test(object)) {
            current = leftAccumulate.apply(current, extract.apply(object));
        }
    }

    @Override
    protected void remove(C object) {
        if (predicate.test(object)) {
            current = leftDecumulate.apply(current, extract.apply(object));
        }
    }
}
