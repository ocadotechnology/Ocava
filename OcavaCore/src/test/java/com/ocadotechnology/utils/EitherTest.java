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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

public class EitherTest {
    @Test
    public void exactly_sameButEmpty() {
        assertThrows(IllegalStateException.class, () -> Either.exactly(Optional.empty(), Optional.empty()));
    }

    @Test
    public void exactly_sameButPresent() {
        assertThrows(IllegalStateException.class, () -> Either.exactly(Optional.of(1L), Optional.of(1L)));
    }

    @Test
    public void exactly_differentOneEmpty() {
        Either.exactly(Optional.empty(), Optional.of(1L));
    }

    @Test
    public void firstOf_nothing() {
        assertThrows(IllegalStateException.class, () -> Either.firstOf(Optional.empty(), Optional.empty()));
    }

    @Test
    public void firstOf_first() {
        Either<Long, Long> either = Either.firstOf(Optional.of(1L), Optional.empty());
        assertTrue(either.isLeft(), "Expected first choice");
        assertFalse(either.isRight(), "Did not expect second choice");
    }

    @Test
    public void firstOf_second() {
        Either<Long, Long> either = Either.firstOf(Optional.empty(), Optional.of(2L));
        assertFalse(either.isLeft(), "Did not expect first choice");
        assertTrue(either.isRight(), "Expected second choice");
    }

    @Test
    public void firstOf_firstOfTwo() {
        Either<Long, Long> either = Either.firstOf(Optional.of(1L), Optional.of(2L));
        assertTrue(either.isLeft(), "Expected first choice");
        assertFalse(either.isRight(), "Did not expect second choice");
    }

    @Test
    public void firstNonNull_nothing() {
        assertThrows(IllegalArgumentException.class, () -> Either.firstNonNull(null, null));
    }

    @Test
    public void firstNonNull_first() {
        Either<Long, Long> either = Either.firstNonNull(1L, null);
        assertTrue(either.isLeft(), "Expected first choice");
        assertFalse(either.isRight(), "Did not expect second choice");
    }

    @Test
    public void firstNonNull_second() {
        Either<Long, Long> either = Either.firstNonNull(null, 2L);
        assertFalse(either.isLeft(), "Did not expect first choice");
        assertTrue(either.isRight(), "Expected second choice");
    }

    @Test
    public void firstNonNull_firstOfTwo() {
        Either<Long, Long> either = Either.firstNonNull(1L, 2L);
        assertTrue(either.isLeft(), "Expected first choice");
        assertFalse(either.isRight(), "Did not expect second choice");
    }

    @Test
    public void with() {
        Either<Long, Long> either = Either.left(1L);
        assertTrue(either.isLeft(), "Expected first choice");
        assertFalse(either.isRight(), "Did not expect second choice");
    }

    @Test
    public void not() {
        Either<Long, Long> either = Either.right(2L);
        assertFalse(either.isLeft(), "Did not expect first choice");
        assertTrue(either.isRight(), "Expected second choice");
    }

    @Test
    public void compose() {
        Either<String, Integer> e = Either.left("Error");
        e = e.second(i -> i + 10);
        assertTrue(e.isLeft());
        e = e.second(i -> i + 100);
        assertTrue(e.isLeft());
        assertEquals("Error", e.left());
        assertTrue(!e.maybeRight().isPresent());
    }

    @Test
    public void bind1() {
        Either<String, Integer> e = Either.left("Error");
        e = e.bind(i -> Either.left("NewError"));
        assertTrue(e.isLeft());
        assertEquals("Error", e.left());
    }

    @Test
    public void bind2() {
        Either<String, Integer> e = Either.right(5);
        e = e.bind(i -> Either.left("NewError"));
        assertTrue(e.isLeft());
        assertEquals("NewError", e.left());
    }

    @Test
    public void bind3() {
        Either<String, Integer> e = Either.right(5);
        e = e.bind(i -> Either.right(i+5));
        assertTrue(e.isRight());
        assertEquals(10, e.right().intValue());
    }

    @Test
    public void bindCompositionExample() {
        Optional<Integer> e = Either.<String, Integer>right(5)
                .bind(i -> Either.right(i + 5))
                .<Integer>bind(i -> Either.left("Error"))
                .bind(i -> Either.right(i * 2))
                .maybeRight();

        assertTrue(!e.isPresent());
    }

    @Test
    public void left_right_fails() {
        Either<String, Integer> e = Either.left("Hello");
        assertTrue(!e.isRight());
        assertThrows(IllegalStateException.class, e::right);
    }

    @Test
    public void right_left_fails() {
        Either<String, Integer> e = Either.right(1);
        assertTrue(!e.isLeft());
        assertThrows(IllegalStateException.class, e::left);
    }

    @Test
    public void flip_both() {
        Either<String, Integer> e = Either.left("Hello");
        Either<Integer, String> flipped = e.flip();
        Either<String, Integer> flippedTwice = flipped.flip();

        assertSame(e.left(), flipped.right());  // object identity
        assertSame(e.left(), flippedTwice.left());  // object identity
    }

    @Test
    public void leftOnly_replaceRightClassButDontChangeValue() {
        Either<String, Integer> e = Either.left("Hello");
        Either<String, Class> any = e.leftOnly();

        assertSame(e.left(), any.left()); // object identity
    }

    @Test
    public void leftOnly_withRight_fails() {
        Either<String, Integer> e = Either.right(1);
        assertThrows(IllegalStateException.class, e::leftOnly);
    }

    @Test
    public void rightOnly_replaceLeftClassButDontChangeValue() {
        Either<String, Integer> e = Either.right(1);
        Either<Class, Integer> any = e.rightOnly();

        assertSame(e.right(), any.right()); // object identity
    }

    @Test
    public void rightOnly_withLeft_fails() {
        Either<String, Integer> e = Either.left("Hello");
        assertThrows(IllegalStateException.class, e::rightOnly);
    }

    @Test
    public void reduce_left_doesntCallRightFunction() {
        Either<String, Integer> e = Either.left("Hello");
        String s = e.reduce(l -> l, r -> { fail(); return ""; });
        assertSame(s, e.left());
    }

    @Test
    public void reduce_right_doesntCallLeftFunction() {
        Either<String, Integer> e = Either.right(1);
        Integer i = e.reduce(l -> { fail(); return 0; }, r -> r);
        assertSame(i, e.right());
    }

    @Test
    public void reduceLeft_withLeft() {
        Either<String, Integer> e = Either.left("Hello");
        Integer i = e.reduceLeft(String::length);
        assertEquals((int) i, e.left().length());
    }

    @Test
    public void reduceRight_withLeft() {
        Either<String, Integer> e = Either.left("Hello");
        String s = e.reduceRight(i -> { fail(); return ""; });
        assertSame(s, e.left());
    }

    @Test
    public void reduceLeft_withRight() {
        Either<String, Integer> e = Either.right(1);
        Integer i = e.reduceLeft(s -> { fail(); return 0; });
        assertSame(i, e.right());
    }

    @Test
    public void reduceRight_withRight() {
        Either<String, Integer> e = Either.right(16);
        String s = e.reduceRight(Integer::toHexString);
        assertEquals("10", s);
    }

    @Test
    public void consumeLeft_withLeft() {
        AtomicReference<String> result = new AtomicReference<>();
        Either<String, Integer> e = Either.left("Hello");
        assertNull(result.get());
        e.accept(result::set, x -> {});
        assertEquals(e.left(), result.get());
    }

    @Test
    public void consumeRight_withLeft() {
        AtomicReference<Integer> result = new AtomicReference<>();
        Either<String, Integer> e = Either.left("Hello");
        assertNull(result.get());
        e.accept(x -> {}, result::set);
        assertNull(result.get());
    }

    @Test
    public void consumeLeft_withRight() {
        AtomicReference<String> result = new AtomicReference<>();
        Either<String, Integer> e = Either.right(1);
        assertNull(result.get());
        e.accept(result::set, x -> {});
        assertNull(result.get());
    }

    @Test
    public void consumeRight_withRight() {
        AtomicReference<Integer> result = new AtomicReference<>();
        Either<String, Integer> e = Either.right(1);
        assertNull(result.get());
        e.accept(x -> {}, result::set);
        assertEquals(e.right(), result.get());
    }

    @Test
    public void filter_withRightAndSuccess() {
        Either<String, Integer> e = Either.right(1);
        Either<String, Integer> result = e.filter(x -> x == 1, "Not One");
        assertTrue(result.isRight());
        assertEquals(1, (int) result.right());
    }

    @Test
    public void filter_withRightAndFailure() {
        Either<String, Integer> e = Either.right(1);
        Either<String, Integer> result = e.filter(x -> x == 2, "Not One");
        assertTrue(result.isLeft());
        assertEquals("Not One", result.left());
    }

    @Test
    public void filter_withLeftAndAny() {
        Either<String, Integer> e = Either.left("Hello");
        Either<String, Integer> result = e.filter(x -> { throw new Error(); }, "Not One");
        assertTrue(result.isLeft());
        assertEquals("Hello", result.left());
    }

    @Test
    public void filterSupplier_withRightAndSuccess() {
        Either<String, Integer> e = Either.right(1);
        Either<String, Integer> result = e.filter(x -> x == 1, () -> { throw new Error(); });
        assertTrue(result.isRight());
        assertEquals(1, (int) result.right());
    }

    @Test
    public void filterSupplier_withRightAndFailure() {
        Either<String, Integer> e = Either.right(1);
        Either<String, Integer> result = e.filter(x -> x == 2, () -> "Not One");
        assertTrue(result.isLeft());
        assertEquals("Not One", result.left());
    }

    @Test
    public void constructFromOptional() {
        Optional<String> s = Optional.of("Hello");
        Either<Integer, String> result = Either.right(s, 0);
        assertTrue(result.isRight());
        assertEquals(s.get(), result.right());
    }

    @Test
    public void constructFromOptionalEmpty() {
        Optional<String> s = Optional.empty();
        Either<Integer, String> result = Either.right(s, 0);
        assertTrue(result.isLeft());
        assertEquals(0, (long)result.left());
    }

    @Test
    public void constructFromOptionalWithSupplier() {
        Optional<String> s = Optional.of("Hello");
        Either<Integer, String> result = Either.right(s, () -> { throw new Error(); });
        assertTrue(result.isRight());
        assertEquals(s.get(), result.right());
    }

    @Test
    public void constructFromOptionalEmptyWithSupplier() {
        Optional<String> s = Optional.empty();
        Either<Integer, String> result = Either.right(s, () -> 0);
        assertTrue(result.isLeft());
        assertEquals(0, (long)result.left());
    }
}