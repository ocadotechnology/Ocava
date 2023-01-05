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

import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * Maps classes to Functions of that class, such that you can use this component to add specific handling for
 * instances of polymorphic types.
 */
@ParametersAreNonnullByDefault
public class ByTypeFunction<T, R> implements Function<T, R> {
    private final ImmutableMap<Class<? extends T>, Function<? extends T, R>> functions;

    private ByTypeFunction(ImmutableMap<Class<? extends T>, Function<? extends T, R>> functions) {
        this.functions = functions;
    }

    /**
     * Behaviour of method when no function for type 't' exists depends on the builder:<br>
     *    If {@link ByTypeFunctionBuilder#returningNullWhenNoFunctionFound} was called, then missing types return null,<br>
     *    Otherwise, the method will throw a NullPointerException
     *
     * @throws NullPointerException if missing functions are not permitted and no function is found for the class of parameter 't'.
     */
    @Override
    public R apply(T t) {
        return Preconditions.checkNotNull(getFunction(t), "No function defined for class %s", t.getClass()).apply(t);
    }

    @SuppressWarnings({"unchecked", "SuspiciousMethodCalls"})
    protected Function<T, R> getFunction(T t) {
        return (Function) functions.get(t.getClass());
    }

    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("functions", functions)
                .toString();
    }

    public static <P, R> ByTypeFunctionBuilder<P, R> builder() {
        return new ByTypeFunctionBuilder<>();
    }

    public static class ByTypeFunctionBuilder<T, R> {
        private final ImmutableMap.Builder<Class<? extends T>, Function<? extends T, R>> functions;
        private boolean returnNull = false;

        private ByTypeFunctionBuilder() {
            functions = ImmutableMap.builder();
        }

        public <Q extends T> ByTypeFunctionBuilder<T, R> withFunctionFor(Class<Q> clazz, Function<Q, R> function) {
            functions.put(clazz, function);
            return this;
        }

        /**
         * Permits missing Class entries<br>
         *  If <em>returnNull</em> is set, then a missing Class entry will return null on calling
         *  {@link ByTypeFunction#apply(Object)}, otherwise a NullPointerException will be thrown.
         */
        public <Q extends T> ByTypeFunctionBuilder<T, R> returningNullWhenNoFunctionFound() {
            returnNull = true;
            return this;
        }

        /**
         * Builds a ByTypeFunction with the defined properties. Does not prevent the builder from being used further
         * without affecting any previously built objects.
         *
         * @return a ByTypeFunction with the behaviour currently defined in the builder.
         *
         * @throws IllegalArgumentException if two functions have been defined for the same class.
         */
        public ByTypeFunction<T, R> build() {
            if (returnNull) {
                return new NullReturningByTypeFunction<>(functions.build());
            }
            return new ByTypeFunction<>(functions.build());
        }
    }

    private static class NullReturningByTypeFunction<T, R> extends ByTypeFunction<T, R> {
        private NullReturningByTypeFunction(ImmutableMap<Class<? extends T>, Function<? extends T, R>> functions) {
            super(functions);
        }

        @Override
        public R apply(T t) {
            Function<T, R> function = getFunction(t);
            if (function == null) {
                return null;
            }
            return function.apply(t);
        }
    }
}