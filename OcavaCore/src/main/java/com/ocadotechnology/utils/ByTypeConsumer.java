/*
 * Copyright © 2017-2022 Ocado (Ocava)
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

import java.util.function.Consumer;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * Maps classes to Consumers of that class, such that you can use this component to add specific handling for
 * instances of polymorphic types.
 */
@ParametersAreNonnullByDefault
public class ByTypeConsumer<T> implements Consumer<T> {
    public static <P> ByTypeConsumerBuilder<P> builder() {
        return new ByTypeConsumerBuilder<>();
    }

    private final ImmutableMap<Class<? extends T>, Consumer<? extends T>> consumers;
    @CheckForNull
    private final Consumer<T> defaultConsumer;

    /** Only instantiable through ByTypeConsumer.builder() */
    private ByTypeConsumer(ImmutableMap<Class<? extends T>, Consumer<? extends T>> consumers, @CheckForNull Consumer<T> defaultConsumer) {
        this.consumers = consumers;
        this.defaultConsumer = defaultConsumer;
    }

    /** Behaviour of method when no consumer for type 't' exists depends on the builder:<br>
     *    If {@link ByTypeConsumerBuilder#withPartialCoverage} was called, then missing types are ignored;<br>
     *    If {@link ByTypeConsumerBuilder#withDeadLetterConsumer(Consumer)} was called, the provided consumer will be called instead;<br>
     *    Otherwise, the method will throw a NullPointerException
     *
     * @throws NullPointerException if missing consumers are not permitted and no consumer is found for the class of parameter 't'.
     */
    @Override
    public void accept(T t) {
        Preconditions.checkNotNull(getConsumer(t), "No consumer defined for class %s", t.getClass()).accept(t);
    }

    /**
     * Returns {@code true} if and only if a consumer exists for the specified class.
     */
    @SuppressWarnings("SuspiciousMethodCalls")
    public boolean hasSpecificConsumerForType(T t) {
        return consumers.containsKey(t.getClass());
    }

    /**
     * Returns the handler which has been registered for the specified object, or {@link #defaultConsumer} if no such
     * handler exists.
     */
    @SuppressWarnings("unchecked")
    @CheckForNull
    private <Q extends T> Consumer<Q> getConsumer(Q q) {
        return (Consumer<Q>) consumers.getOrDefault(q.getClass(), defaultConsumer);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("consumers", consumers)
                .add("defaultConsumer", defaultConsumer)
                .toString();
    }

    public static class ByTypeConsumerBuilder<T> {
        private static final Consumer PARTIAL_COVERAGE_CONSUMER = t -> {};

        private Consumer<T> defaultConsumer = null;

        private final ImmutableMap.Builder<Class<? extends T>, Consumer<? extends T>> consumersBuilder = ImmutableMap.builder();

        /** Only instantiable through ByTypeConsumer.builder() */
        private ByTypeConsumerBuilder() {}

        /**
         * Add consumer for instances of the specified class.
         */
        // Generics guarantee at compile time that we haven't constructed an invalid map of consumers (i.e. having a consumer keyed by a class that it doesn't consume)
        public <P extends Q, Q extends T> ByTypeConsumerBuilder<T> withConsumerFor(Class<P> clazz, Consumer<Q> consumer) {
            consumersBuilder.put(clazz, consumer);
            return this;
        }

        /**
         * Copies all of the consumers from the source instance into this builder.
         *
         * Does not copy the deadLetter/defaultConsumer
         */
        public <Q extends T> ByTypeConsumerBuilder<T> withAllConsumers(ByTypeConsumer<Q> byTypeConsumer) {
            consumersBuilder.putAll(byTypeConsumer.consumers);
            return this;
        }

        /**
         * Copies the deadLetter/defaultConsumer from the source instance into this builder.
         *
         * Does not copy any of the type-specific consumers
         */
        public ByTypeConsumerBuilder<T> withDeadLetterConsumer(ByTypeConsumer<T> byTypeConsumer) {
            Preconditions.checkState(defaultConsumer == null || defaultConsumer == byTypeConsumer.defaultConsumer,
                    "Overriding an already configured dead letter consumer with one from an existing instance is not supported.");
            defaultConsumer = byTypeConsumer.defaultConsumer;
            return this;
        }

        /** Set a consumer to call if no other consumers match.<br>
         *  If a <em>deadLetterConsumer</em> is not provided, then a missing Class entry will throw a NullPointerException on calling
         *  {@link ByTypeConsumer#accept(Object)}, unless <em>partialCoverage</em> has been set.
         *
         *  @throws IllegalStateException if {@link ByTypeConsumerBuilder#withPartialCoverage} has already been called on this builder
         */
        public ByTypeConsumerBuilder<T> withDeadLetterConsumer(Consumer<T> deadLetterConsumer) {
            Preconditions.checkArgument(deadLetterConsumer != null, "Null deadLetterConsumer makes no sense");
            Preconditions.checkState(defaultConsumer != PARTIAL_COVERAGE_CONSUMER, "DeadLetterConsumer is incompatible with partial coverage.  Do not use both.");
            defaultConsumer = deadLetterConsumer;
            return this;
        }

        /** Permits missing Class entries<br>
         *  If <em>partialCoverage</em> is not set, then a missing Class entry will throw a NullPointerException on calling
         *  {@link ByTypeConsumer#accept(Object)}, unless a <em>deadLetterConsumer</em> has been provided.
         *
         *  @throws IllegalStateException if {@link ByTypeConsumerBuilder#withDeadLetterConsumer} has already been called on this builder
         */
        @SuppressWarnings("unchecked")
        public ByTypeConsumerBuilder<T> withPartialCoverage() {
            Preconditions.checkState(defaultConsumer == null || defaultConsumer == PARTIAL_COVERAGE_CONSUMER, "Partial coverage is incompatible with deadLetterConsumer.  Do not use both.");
            defaultConsumer = (Consumer<T>) PARTIAL_COVERAGE_CONSUMER;
            return this;
        }

        /**
         * Construct the {@link ByTypeConsumer}.
         *
         * @throws IllegalArgumentException If multiple consumers have been added for the same class. This exception
         * will be thrown even if all consumers for that class are the same object.
         */
        public ByTypeConsumer<T> build() {
            return new ByTypeConsumer<>(consumersBuilder.build(), defaultConsumer);
        }
    }
}
