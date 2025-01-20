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
package com.ocadotechnology.utils;

import java.util.function.BiFunction;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * Maps classes to Functions of that class, such that you can use this component to add specific handling for
 * instances of polymorphic types.
 */
@ParametersAreNonnullByDefault
public class ByTypeBiFunction<T, U, R> implements BiFunction<T, U, R> {
    private final ImmutableMap<Class<? extends T>, BiFunction<? extends T, U, R>> functions;

    private ByTypeBiFunction(ImmutableMap<Class<? extends T>, BiFunction<? extends T, U, R>> functions) {
        this.functions = functions;
    }

    /**
     * Behaviour of method when no function for type 't' exists depends on the builder:<br>
     *    If {@link ByTypeBiFunctionBuilder#returningNullWhenNoBiFunctionFound} was called, then missing types return null,<br>
     *    Otherwise, the method will throw a NullPointerException
     *
     * @throws NullPointerException if missing functions are not permitted and no function is found for the class of parameter 't'.
     */
    @Override
    public R apply(T t, U u) {
        return Preconditions.checkNotNull(getFunction(t), "No function defined for class %s", t.getClass()).apply(t, u);
    }

    @SuppressWarnings({"unchecked", "SuspiciousMethodCalls"})
    protected BiFunction<T, U, R> getFunction(T t) {
        return (BiFunction) functions.get(t.getClass());
    }

    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("functions", functions)
                .toString();
    }

    public static <P, U, R> ByTypeBiFunctionBuilder<P, U, R> builder() {
        return new ByTypeBiFunctionBuilder<>();
    }

    public static class ByTypeBiFunctionBuilder<T, U, R> {
        private final ImmutableMap.Builder<Class<? extends T>, BiFunction<? extends T, U, R>> functions;
        private boolean returnNull = false;

        private ByTypeBiFunctionBuilder() {
            functions = ImmutableMap.builder();
        }

        public <Q extends T> ByTypeBiFunctionBuilder<T, U, R> withBiFunctionFor(Class<Q> clazz, BiFunction<Q, U, R> function) {
            functions.put(clazz, function);
            return this;
        }

        /**
         * Permits missing Class entries<br>
         *  If <em>returnNull</em> is set, then a missing Class entry will return null on calling
         *  {@link ByTypeBiFunction#apply(Object, Object)}, otherwise a NullPointerException will be thrown.
         */
        public <Q extends T> ByTypeBiFunctionBuilder<T, U, R> returningNullWhenNoBiFunctionFound() {
            returnNull = true;
            return this;
        }

        public ByTypeBiFunction<T, U, R> build() {
            if (returnNull) {
                return new NullReturningByTypeBiFunction<>(functions.build());
            }
            return new ByTypeBiFunction<>(functions.build());
        }
    }

    private static class NullReturningByTypeBiFunction<T, U, R> extends ByTypeBiFunction<T, U, R> {
        private NullReturningByTypeBiFunction(ImmutableMap<Class<? extends T>, BiFunction<? extends T, U, R>> functions) {
            super(functions);
        }

        @Override
        public R apply(T t, U u) {
            BiFunction<T, U, R> function = getFunction(t);
            if (function == null) {
                return null;
            }
            return function.apply(t, u);
        }
    }
}