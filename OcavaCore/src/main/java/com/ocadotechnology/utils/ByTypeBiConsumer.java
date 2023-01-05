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

import java.util.function.BiConsumer;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * Maps classes to Consumers of instances of that class, such that you can use this component to add specific handling for
 * instances of polymorphic types.
 */
@ParametersAreNonnullByDefault
public class ByTypeBiConsumer<T, U> implements BiConsumer<T, U> {
    private final ImmutableMap<Class<? extends T>, BiConsumer<? extends T, U>> consumers;
    @CheckForNull
    private final BiConsumer<T, U> defaultConsumer;

    private ByTypeBiConsumer(ImmutableMap<Class<? extends T>, BiConsumer<? extends T, U>> consumers, @CheckForNull BiConsumer<T, U> defaultConsumer) {
        this.consumers = consumers;
        this.defaultConsumer = defaultConsumer;
    }

    /** Behaviour of method when no consumer for type 't' exists depends on the builder:<br>
     *    If {@link ByTypeBiConsumerBuilder#withPartialCoverage} was called, then missing types are ignored;<br>
     *    If {@link ByTypeBiConsumerBuilder#withDeadLetterConsumer(BiConsumer)} was called, the provided consumer will be called instead;<br>
     *    Otherwise, the method will throw a NullPointerException
     *
     * @throws NullPointerException if missing consumers are not permitted and no consumer is found for the class of parameter 't'.
     */
    @Override
    public void accept(T t, U u) {
        Preconditions.checkNotNull(getConsumer(t), "No consumer defined for class %s", t.getClass()).accept(t, u);
    }

    @SuppressWarnings({"unchecked", "SuspiciousMethodCalls"})
    @CheckForNull
    protected BiConsumer<T, U> getConsumer(T t) {
        return (BiConsumer) consumers.getOrDefault(t.getClass(), defaultConsumer);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("consumers", consumers)
                .add("defaultConsumer", defaultConsumer)
                .toString();
    }

    public static <P, U> ByTypeBiConsumerBuilder<P, U> builder() {
        return new ByTypeBiConsumerBuilder<>();
    }

    public static class ByTypeBiConsumerBuilder<T, U> {
        private static final BiConsumer PARTIAL_COVERAGE_CONSUMER = (t, u) -> {};

        private final ImmutableMap.Builder<Class<? extends T>, BiConsumer<? extends T, U>> consumersBuilder;
        private BiConsumer<T, U> defaultConsumer = null;

        private ByTypeBiConsumerBuilder() {
            consumersBuilder = ImmutableMap.builder();
        }

        public <Q extends P, P extends T> ByTypeBiConsumerBuilder<T, U> withBiConsumerFor(Class<Q> clazz, BiConsumer<P, U> consumer) {
            consumersBuilder.put(clazz, consumer);
            return this;
        }

        /** Set a consumer to call if no other consumers match.<br>
         *  If a <em>deadLetterConsumer</em> is not provided, then a missing Class entry will throw a NullPointerException on calling
         *  {@link ByTypeBiConsumer#accept(Object, Object)}, unless <em>partialCoverage</em> has been set.
         *
         *  @throws IllegalStateException if {@link ByTypeBiConsumerBuilder#withPartialCoverage} has already been called on this builder
         */
        public ByTypeBiConsumerBuilder<T, U> withDeadLetterConsumer(BiConsumer<T, U> deadLetterConsumer) {
            Preconditions.checkArgument(deadLetterConsumer != null, "Null deadLetterConsumer makes no sense");
            Preconditions.checkState(defaultConsumer != PARTIAL_COVERAGE_CONSUMER, "DeadLetterConsumer is incompatible with partial coverage.  Do not use both.");
            defaultConsumer = deadLetterConsumer;
            return this;
        }

        /** Permits missing Class entries<br>
         *  If <em>partialCoverage</em> is not set, then a missing Class entry will throw a NullPointerException on calling
         *  {@link ByTypeBiConsumer#accept(Object, Object)}, unless a <em>deadLetterConsumer</em> has been provided.
         *
         *  @throws IllegalStateException if {@link ByTypeBiConsumerBuilder#withDeadLetterConsumer} has already been called on this builder
         */
        @SuppressWarnings("unchecked")
        public ByTypeBiConsumerBuilder<T, U> withPartialCoverage() {
            Preconditions.checkState(defaultConsumer == null || defaultConsumer == PARTIAL_COVERAGE_CONSUMER, "Partial coverage is incompatible with deadLetterConsumer.  Do not use both.");
            defaultConsumer = (BiConsumer<T, U>) PARTIAL_COVERAGE_CONSUMER;
            return this;
        }

        public BiConsumer<T, U> build() {
            ImmutableMap<Class<? extends T>, BiConsumer<? extends T, U>> classToConsumer = consumersBuilder.build();
            return new ByTypeBiConsumer<>(classToConsumer, defaultConsumer);
        }
    }
}