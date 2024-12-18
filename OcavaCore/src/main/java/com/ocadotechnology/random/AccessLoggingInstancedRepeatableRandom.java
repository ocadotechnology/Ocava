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
package com.ocadotechnology.random;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper around {@link InstancedRepeatableRandom} that logs all calls to the public methods.
 */
class AccessLoggingInstancedRepeatableRandom extends InstancedRepeatableRandom {
    private static Logger logger = LoggerFactory.getLogger(AccessLoggingInstancedRepeatableRandom.class);

    private final InstancedRepeatableRandom wrapped;

    public AccessLoggingInstancedRepeatableRandom(InstancedRepeatableRandom wrapped) {
        super(null, null, null, null, null, null, null);
        this.wrapped = wrapped;
    }

    InstancedRepeatableRandom getWrapped() {
        return wrapped;
    }

    @Override
    public double nextDouble() {
        logAccess();
        return wrapped.nextDouble();
    }

    @Override
    public double nextDouble(double origin, double bound) {
        logAccess();
        return wrapped.nextDouble(origin, bound);
    }

    @Override
    public int nextInt(int bound) {
        logAccess();
        return wrapped.nextInt(bound);
    }

    @Override
    public boolean nextBoolean() {
        logAccess();
        return wrapped.nextBoolean();
    }

    @Override
    public void nextBytes(byte[] bytes) {
        logAccess();
        wrapped.nextBytes(bytes);
    }

    @Override
    public UUID nextUUID() {
        logAccess();
        return wrapped.nextUUID();
    }

    @Override
    public long nextLong() {
        logAccess();
        return wrapped.nextLong();
    }

    @Override
    public double nextGaussian() {
        logAccess();
        return wrapped.nextGaussian();
    }

    @Override
    public <T> void shuffle(List<T> list) {
        logAccess();
        wrapped.shuffle(list);
    }

    @Override
    public <T> void shuffle(T[] array) {
        logAccess();
        wrapped.shuffle(array);
    }

    @Override
    public <T> T randomElementOf(Collection<T> collection) {
        logAccess();
        return wrapped.randomElementOf(collection);
    }

    @Override
    public <T> T randomElementOf(List<T> list) {
        logAccess();
        return wrapped.randomElementOf(list);
    }

    @Override
    public int nextInt() {
        logAccess();
        return wrapped.nextInt();
    }

    @Override
    public float nextFloat() {
        logAccess();
        return wrapped.nextFloat();
    }

    @Override
    @Deprecated
    public void setSeed(int i) {
        logAccess();
        wrapped.setSeed(i);
    }

    @Override
    @Deprecated
    public void setSeed(int[] ints) {
        logAccess();
        wrapped.setSeed(ints);
    }

    @Override
    @Deprecated
    public void setSeed(long l) {
        logAccess();
        wrapped.setSeed(l);
    }

    static void logAccess() {
        if (!logger.isTraceEnabled()) {
            return;
        }

        StringBuilder sb = new StringBuilder("Call to RepeatableRandom methods from:\n");
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        //Skips the first two elements as they are the getStackTrace method and this method
        for (int i = 2; i < stackTrace.length; i++) {
            sb.append(stackTrace[i]).append("\n");
        }
        //Remove the last newline
        sb.deleteCharAt(sb.length() - 1);
        //Checkstyle insists that I can't use sb.toString() directly in the logger call
        logger.trace("{}", sb);
    }
}
