/*
 * Copyright © 2017 Ocado (Ocava)
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
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/** A utility for dealing with polymorphic types. */
public class Types {
    /** This is a static utility and should not be constructed. */
    private Types() {
        throw new UnsupportedOperationException("Static utility class that shouldn't be instantiated");
    }

    @SuppressWarnings("unchecked") // This is actually checked
    public static <T> Optional<T> fromType(Object o, Class<T> clazz) {
        if (o == null) {
            return Optional.empty();
        }
        return clazz.isAssignableFrom(o.getClass()) ? Optional.of((T) o) : Optional.empty();
    }

    @SuppressWarnings("unchecked") // This is actually checked
    public static <T> T fromTypeOrFail(Object o, Class<T> clazz) {
        Preconditions.checkNotNull(o, "Cannot cast null object");
        Preconditions.checkState(clazz.isAssignableFrom(o.getClass()), "Class %s cannot be cast to %s", o.getClass(), clazz);
        return (T) o;
    }

    public static <T> Stream<T> streamInstancesOfType(Collection<?> collection, Class<T> clazz) {
        return streamInstancesOfType(collection.stream(), clazz);
    }

    @SuppressWarnings("unchecked") // This is actually checked
    public static <T> Stream<T> streamInstancesOfType(Stream<?> stream, Class<T> clazz) {
        return stream.filter(o -> clazz.isAssignableFrom(o.getClass()))
                .map(o -> (T) o);
    }

    public static <T> ImmutableList<T> getInstancesOfType(Collection<?> collection, Class<T> clazz) {
        return getInstancesOfType(collection.stream(), clazz);
    }

    public static <T> ImmutableList<T> getInstancesOfType(Stream<?> stream, Class<T> clazz) {
        return streamInstancesOfType(stream, clazz).collect(ImmutableList.toImmutableList());
    }

    public static <T> Stream<T> streamInstancesOfTypeOrFail(Collection<?> collection, Class<T> clazz) {
        return streamInstancesOfTypeOrFail(collection.stream(), clazz);
    }

    public static <T> Stream<T> streamInstancesOfTypeOrFail(Stream<?> stream, Class<T> clazz) {
        return stream.map(i -> Types.fromTypeOrFail(i, clazz));
    }

    public static <T> ImmutableList<T> getInstancesOfTypeOrFail(Collection<?> collection, Class<T> clazz) {
        return getInstancesOfTypeOrFail(collection.stream(), clazz);
    }

    public static <T> ImmutableList<T> getInstancesOfTypeOrFail(Stream<?> stream, Class<T> clazz) {
        return streamInstancesOfTypeOrFail(stream, clazz).collect(ImmutableList.toImmutableList());
    }

    @SuppressWarnings("unchecked") // This is actually checked
    public static <O, T extends O> ImmutableList<T> getInstancesOfTypeUntil(Collection<O> collection, Class<T> clazz, Predicate<O> condition) {
        Builder<T> resultBuilder = ImmutableList.builder();

        for (O o : collection) {
            if (!condition.test(o)) {
                break;
            }

            if (clazz.isAssignableFrom(o.getClass())) {
                resultBuilder.add((T) o);
            }
        }

        return resultBuilder.build();
    }

    public static <O, T extends O> ImmutableList<T> getInstancesOfTypeUntil(Stream<O> stream, Class<T> clazz, Predicate<O> condition) {
        // TODO: JDK9 will not need to do Stream.collect(), as it will have Stream.takeWhile()
        return getInstancesOfTypeUntil(stream.collect(Collectors.toList()), clazz, condition);
    }

    public static <O, T extends O> Stream<T> streamInstancesOfTypeUntil(Collection<O> collection, Class<T> clazz, Predicate<O> condition) {
        // TODO: Remove intermediate collection generated here before value returned
        return getInstancesOfTypeUntil(collection, clazz, condition).stream();
    }

    public static <O, T extends O> Stream<T> streamInstancesOfTypeUntil(Stream<O> stream, Class<T> clazz, Predicate<O> condition) {
        // TODO: Avoid intermediate ImmutableList generation
        return getInstancesOfTypeUntil(stream, clazz, condition).stream();
    }
}
