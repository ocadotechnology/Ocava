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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.base.Preconditions;
import com.ocadotechnology.utils.ByTypeConsumer.ByTypeConsumerBuilder;

public class ByTypeConsumerTest {

    private static final String TEST_STRING = "Test String";
    private static final int TEST_INT = 37;
    private static final IOException TEST_IO_EXCEPTION = new IOException();
    private static final RuntimeException TEST_RUNTIME_EXCEPTION = new RuntimeException();

    private static final Consumer<Object> FAILING_CONSUMER = o -> Preconditions.checkState(false);

    private TestConsumer<Integer> integerConsumer;
    private TestConsumer<String> stringConsumer;
    private TestConsumer<Exception> exceptionConsumer;

    private ByTypeConsumer<Object> byTypeConsumer;

    @BeforeEach
    public void setup() {
        integerConsumer = new TestConsumer<>();
        stringConsumer = new TestConsumer<>();
        exceptionConsumer = new TestConsumer<>();

        // Do not add Double.class or Exception.class -- used in test
        byTypeConsumer = ByTypeConsumer.builder()
                .withConsumerFor(Integer.class, integerConsumer)
                .withConsumerFor(String.class, stringConsumer)
                .withConsumerFor(IOException.class, exceptionConsumer)
                .withConsumerFor(RuntimeException.class, exceptionConsumer)
                .withDeadLetterConsumer(FAILING_CONSUMER)
                .build();
    }

    @Test
    public void testThatNoExceptionOccursWhenDefinedPartial() {
        ByTypeConsumer.builder()
                .withPartialCoverage()
                .build()
                .accept("Some arbitrary object");
    }

    @Test
    public void testThatExceptionOccursWhenNotDefinedPartialAndNoConsumers() {
        assertThrows(NullPointerException.class, () -> ByTypeConsumer.builder().build().accept("Some arbitrary object"));
    }

    @Test
    public void testThatCorrectConsumersAreCalled() {
        byTypeConsumer.accept(TEST_STRING);
        assertEquals(TEST_STRING, stringConsumer.heldObject);

        byTypeConsumer.accept(TEST_INT);
        assertEquals(TEST_INT, integerConsumer.heldObject);

        byTypeConsumer.accept(TEST_IO_EXCEPTION);
        assertEquals(TEST_IO_EXCEPTION, exceptionConsumer.heldObject);

        byTypeConsumer.accept(TEST_RUNTIME_EXCEPTION);
        assertEquals(TEST_RUNTIME_EXCEPTION, exceptionConsumer.heldObject);
    }

    @Test
    public void testDeadLetterConsumer() {
        assertThrows(IllegalStateException.class, () -> byTypeConsumer.accept(4.5));
    }

    @Test
    public void testDeadLetterConsumerWhenHandlerExistsButOnlyRegisteredToSubclasses() {
        assertThrows(IllegalStateException.class, () -> byTypeConsumer.accept(new Exception()));
    }

    @Test
    public void testDeadLetterConsumerWhenHandlerExistsButOnlyRegisteredToSuperclasses() {
        Consumer<Object> exceptionConsumer = ByTypeConsumer.builder()
                .withConsumerFor(Exception.class, this.exceptionConsumer)
                .withDeadLetterConsumer(FAILING_CONSUMER)
                .build();
        assertThrows(IllegalStateException.class, () -> exceptionConsumer.accept(new IOException()));
    }

    @Test
    public void testWithAllConsumers() {
        ByTypeConsumer<Object> byTypeAllConsumers = ByTypeConsumer.builder()
                .withAllConsumers(byTypeConsumer)
                .withDeadLetterConsumer(FAILING_CONSUMER)
                .build();

        byTypeAllConsumers.accept(TEST_STRING);
        assertEquals(TEST_STRING, stringConsumer.heldObject);

        byTypeAllConsumers.accept(TEST_INT);
        assertEquals(TEST_INT, integerConsumer.heldObject);

        byTypeAllConsumers.accept(TEST_IO_EXCEPTION);
        assertEquals(TEST_IO_EXCEPTION, exceptionConsumer.heldObject);

        byTypeAllConsumers.accept(TEST_RUNTIME_EXCEPTION);
        assertEquals(TEST_RUNTIME_EXCEPTION, exceptionConsumer.heldObject);
    }

    @Test
    public void testWithDeadLetterConsumerFromInstance_whenDeadLetterConsumerNotAlreadyConfiguredOnBuilder_thenDeadLetterConsumerAccepted() {
        ByTypeConsumer<Object> byTypeCopiedDeadLetter = ByTypeConsumer.builder()
                .withDeadLetterConsumer(byTypeConsumer)
                .build();

        assertThrows(IllegalStateException.class, () -> byTypeCopiedDeadLetter.accept("Some test object"));
    }

    @Test
    public void testWithDeadLetterConsumerFromInstance_whenDeadLetterConsumerAlreadyConfiguredOnBuilder_thenThrowsException() {
        ByTypeConsumerBuilder<Object> builder = ByTypeConsumer.builder()
                .withDeadLetterConsumer(System.err::println);

        assertThrows(IllegalStateException.class, () -> builder.withDeadLetterConsumer(byTypeConsumer));
    }

    @Test
    public void testAddingConsumerWhenConsumerForSuperclassExists() {
        ByTypeConsumer.builder()
                .withConsumerFor(Object.class, o -> {})
                .withConsumerFor(Integer.class, i -> {})
                .build();
    }

    @Test
    public void testAddingConsumerWhenConsumerForSubclassExists() {
        ByTypeConsumer.builder()
                .withConsumerFor(Integer.class, i -> {})
                .withConsumerFor(Object.class, o -> {})
                .build();
    }

    @Test
    public void testAddingClassTwiceWithDifferentConsumers() {
        assertThrows(IllegalArgumentException.class, () -> ByTypeConsumer.builder()
                .withConsumerFor(Integer.class, i -> {})
                .withConsumerFor(Integer.class, i -> {})
                .build());
    }

    @Test
    public void testAddingClassTwiceWithSameConsumer() {
        Consumer<Integer> consumer = i -> {};
        assertThrows(IllegalArgumentException.class, () -> ByTypeConsumer.builder()
                .withConsumerFor(Integer.class, consumer)
                .withConsumerFor(Integer.class, consumer)
                .build());
    }

    @Test
    public void testHasConsumerForType() {
        assertTrue(byTypeConsumer.hasSpecificConsumerForType(TEST_INT));
        assertTrue(byTypeConsumer.hasSpecificConsumerForType(TEST_STRING));
        assertTrue(byTypeConsumer.hasSpecificConsumerForType(TEST_IO_EXCEPTION));
        assertTrue(byTypeConsumer.hasSpecificConsumerForType(TEST_RUNTIME_EXCEPTION));

        assertFalse(byTypeConsumer.hasSpecificConsumerForType(new Exception()));
        assertFalse(byTypeConsumer.hasSpecificConsumerForType(new AtomicInteger()));
        assertFalse(byTypeConsumer.hasSpecificConsumerForType(new Object()));
    }

    private static final class TestConsumer<T> implements Consumer<T> {
        T heldObject;

        @Override
        public void accept(T t) {
            this.heldObject = t;
        }
    }
}
