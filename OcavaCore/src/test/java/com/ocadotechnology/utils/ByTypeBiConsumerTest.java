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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ByTypeBiConsumerTest {

    private static final String ANY_STRING = null;
    private static final String TEST_STRING = "Test String";
    private static final int TEST_INT = 37;
    private static final IOException TEST_IO_EXCEPTION = new IOException();
    private static final RuntimeException TEST_RUNTIME_EXCEPTION = new RuntimeException();

    private static final RuntimeException FAILING_CONSUMER_EXCEPTION = new RuntimeException() {};
    private static final BiConsumer<Object, String> FAILING_CONSUMER = (o, s) -> { throw FAILING_CONSUMER_EXCEPTION; };

    private TestConsumer<Integer, String> integerConsumer;
    private TestConsumer<String, String> stringConsumer;
    private TestConsumer<Exception, String> exceptionConsumer;

    private BiConsumer<Object, String> byTypeConsumerWithFailingDeadLetter;
    private BiConsumer<Object, String> byTypePartialConsumer;

    @BeforeEach
    public void setup() {
        integerConsumer = new TestConsumer<>();
        stringConsumer = new TestConsumer<>();
        exceptionConsumer = new TestConsumer<>();

        byTypeConsumerWithFailingDeadLetter = ByTypeBiConsumer.<Object, String>builder()
                .withBiConsumerFor(Integer.class, integerConsumer)
                .withBiConsumerFor(String.class, stringConsumer)
                // Do not add Double.class or Exception.class -- used in test
                .withBiConsumerFor(IOException.class, exceptionConsumer)
                .withBiConsumerFor(RuntimeException.class, exceptionConsumer)
                .withDeadLetterConsumer(FAILING_CONSUMER)
                .build();

        byTypePartialConsumer = ByTypeBiConsumer.<Object, String>builder()
                .withBiConsumerFor(Integer.class, integerConsumer)
                .withBiConsumerFor(String.class, stringConsumer)
                // Do not add Double.class -- used in test
                .withPartialCoverage()
                .build();
    }

    @Test
    public void testThatNoConsumersCausesExceptionOnAccept() {
        assertThrows(NullPointerException.class, () -> ByTypeBiConsumer.builder().build().accept(1, null));
    }

    @Test
    public void testThatNoConsumersAndPartialCoverageDoesNothingOnAccept() {
        ByTypeBiConsumer.builder().withPartialCoverage().build().accept(1, null);
    }

    @Test
    public void testThatNoConsumersAndDeadLetterConsumerCallsDeadLetterConsumerOnAccept() {
        assertThrows(FAILING_CONSUMER_EXCEPTION.getClass(), () -> ByTypeBiConsumer.<Object, String>builder().withDeadLetterConsumer(FAILING_CONSUMER).build().accept(1, null));
    }

    @Test
    public void testThatBuildingPartialCoverageAfterDeadLetterConsumerFails() {
        assertThrows(IllegalStateException.class, () -> ByTypeBiConsumer.<Object, String>builder().withDeadLetterConsumer(FAILING_CONSUMER).withPartialCoverage().build());
    }

    @Test
    public void testThatAddingDeadLetterConsumerAfterPartialCoverageSetFails() {
        assertThrows(IllegalStateException.class, () -> ByTypeBiConsumer.<Object, String>builder().withPartialCoverage().withDeadLetterConsumer(FAILING_CONSUMER).build());
    }

    @Test
    public void testThatReplacingDeadLetterConsumerUsesLastCall() {
        assertThrows(FAILING_CONSUMER_EXCEPTION.getClass(), () -> ByTypeBiConsumer.<Object, String>builder().withDeadLetterConsumer((t, u) -> {}).withDeadLetterConsumer(FAILING_CONSUMER).build().accept(1, ANY_STRING));
    }

    @Test
    public void testThatCorrectConsumersAreCalled() {
        byTypeConsumerWithFailingDeadLetter.accept(TEST_STRING, ANY_STRING);
        assertEquals(TEST_STRING, stringConsumer.heldObject);

        byTypeConsumerWithFailingDeadLetter.accept(TEST_INT, ANY_STRING);
        assertEquals(TEST_INT, integerConsumer.heldObject);

        byTypeConsumerWithFailingDeadLetter.accept(TEST_IO_EXCEPTION, ANY_STRING);
        assertEquals(TEST_IO_EXCEPTION, exceptionConsumer.heldObject);

        byTypeConsumerWithFailingDeadLetter.accept(TEST_RUNTIME_EXCEPTION, ANY_STRING);
        assertEquals(TEST_RUNTIME_EXCEPTION, exceptionConsumer.heldObject);
    }

    @Test
    public void testDeadLetterConsumer() {
        assertThrows(FAILING_CONSUMER_EXCEPTION.getClass(), () -> byTypeConsumerWithFailingDeadLetter.accept(4.5, ANY_STRING));
    }

    @Test
    public void testDeadLetterConsumerWhenHandlerExistsButOnlyRegisteredToSubclasses() {
        assertThrows(FAILING_CONSUMER_EXCEPTION.getClass(), () -> byTypeConsumerWithFailingDeadLetter.accept(new Exception(), ANY_STRING));
    }

    @Test
    public void testDeadLetterConsumerWhenHandlerExistsButOnlyRegisteredToSuperclasses() {
        BiConsumer<Object, String> exceptionConsumer = ByTypeBiConsumer.<Object, String>builder()
                .withBiConsumerFor(Exception.class, this.exceptionConsumer)
                .withDeadLetterConsumer(FAILING_CONSUMER)
                .build();
        assertThrows(FAILING_CONSUMER_EXCEPTION.getClass(), () -> exceptionConsumer.accept(new IOException(), ANY_STRING));
    }

    @Test
    public void testPartialConsumerDoNotThrowForUnregisteredClasses() {
        byTypePartialConsumer.accept(4.5, ANY_STRING);
    }

    @Test
    public void testPartialConsumerConsumes() {
        byTypePartialConsumer.accept(TEST_INT, ANY_STRING);
        assertEquals(TEST_INT, integerConsumer.heldObject);

        byTypePartialConsumer.accept(TEST_STRING, ANY_STRING);
        assertEquals(TEST_STRING, stringConsumer.heldObject);
    }

    @Test
    public void testAddingConsumerWhenConsumerForSuperclassExists() {
        ByTypeBiConsumer.builder()
                .withBiConsumerFor(Object.class, (o, s) -> {})
                .withBiConsumerFor(Integer.class, (i, s) -> {})
                .build();
    }

    @Test
    public void testAddingConsumerWhenConsumerForSubclassExists() {
        ByTypeBiConsumer.builder()
                .withBiConsumerFor(Integer.class, (i, s) -> {})
                .withBiConsumerFor(Object.class, (o, s) -> {})
                .build();
    }

    @Test
    public void testAddingClassTwiceWithDifferentConsumers() {
        assertThrows(IllegalArgumentException.class, () -> ByTypeBiConsumer.builder()
                .withBiConsumerFor(Integer.class, (i, s) -> {})
                .withBiConsumerFor(Integer.class, (i, s) -> {})
                .build());
    }

    @Test
    public void testAddingClassTwiceWithSameConsumer() {
        BiConsumer<Integer, String> consumer = (i, s) -> {};
        assertThrows(IllegalArgumentException.class, () -> ByTypeBiConsumer.<Object, String>builder()
                .withBiConsumerFor(Integer.class, consumer)
                .withBiConsumerFor(Integer.class, consumer)
                .build());
    }

    private static final class TestConsumer<T, U> implements BiConsumer<T, U> {
        T heldObject;

        @Override
        public void accept(T t, U u) {
            this.heldObject = t;
        }
    }
}
