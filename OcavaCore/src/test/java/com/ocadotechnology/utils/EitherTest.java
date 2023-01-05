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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

public class EitherTest {
    @Test
    public void with() {
        Either<Long, Long> either = Either.createLeft(1L);
        assertTrue(either.isLeft(), "Expected first choice");
        assertFalse(either.isRight(), "Did not expect second choice");
    }

    @Test
    public void not() {
        Either<Long, Long> either = Either.createRight(2L);
        assertFalse(either.isLeft(), "Did not expect first choice");
        assertTrue(either.isRight(), "Expected second choice");
    }

    @Test
    public void compose() {
        Either<String, Integer> e = Either.createLeft("Error");
        e = e.map(i -> i + 10);
        assertTrue(e.isLeft());
        e = e.map(i -> i + 100);
        assertTrue(e.isLeft());
        assertEquals("Error", e.leftValue());
        assertTrue(!e.toOptional().isPresent());
    }

    @Test
    public void flatMap1() {
        Either<String, Integer> e = Either.createLeft("Error");
        e = e.flatMap(i -> Either.createLeft("NewError"));
        assertTrue(e.isLeft());
        assertEquals("Error", e.leftValue());
    }

    @Test
    public void flatMap2() {
        Either<String, Integer> e = Either.createRight(5);
        e = e.flatMap(i -> Either.createLeft("NewError"));
        assertTrue(e.isLeft());
        assertEquals("NewError", e.leftValue());
    }

    @Test
    public void flatMap3() {
        Either<String, Integer> e = Either.createRight(5);
        e = e.flatMap(i -> Either.createRight(i+5));
        assertTrue(e.isRight());
        assertEquals(10, e.rightValue().intValue());
    }

    @Test
    public void flatMapCompositionExample() {
        Optional<Integer> e = Either.<String, Integer>createRight(5)
                .flatMap(i -> Either.createRight(i + 5))
                .<Integer>flatMap(i -> Either.createLeft("Error"))
                .flatMap(i -> Either.createRight(i * 2))
                .toOptional();

        assertTrue(!e.isPresent());
    }

    @Test
    public void left_right_fails() {
        Either<String, Integer> e = Either.createLeft("Hello");
        assertTrue(!e.isRight());
        assertThrows(IllegalStateException.class, e::rightValue);
    }

    @Test
    public void right_left_fails() {
        Either<String, Integer> e = Either.createRight(1);
        assertTrue(!e.isLeft());
        assertThrows(IllegalStateException.class, e::leftValue);
    }

    @Test
    public void swap_both() {
        Either<String, Integer> e = Either.createLeft("Hello");
        Either<Integer, String> flipped = e.swap();
        Either<String, Integer> flippedTwice = flipped.swap();

        assertSame(e.leftValue(), flipped.rightValue());  // object identity
        assertSame(e.leftValue(), flippedTwice.leftValue());  // object identity
    }

    @Test
    public void leftOnly_replaceRightClassButDontChangeValue() {
        Either<String, Integer> e = Either.createLeft("Hello");
        Either<String, Class> any = e.leftOnly();

        assertSame(e.leftValue(), any.leftValue()); // object identity
    }

    @Test
    public void leftOnly_withRight_fails() {
        Either<String, Integer> e = Either.createRight(1);
        assertThrows(IllegalStateException.class, e::leftOnly);
    }

    @Test
    public void rightOnly_replaceLeftClassButDontChangeValue() {
        Either<String, Integer> e = Either.createRight(1);
        Either<Class, Integer> any = e.rightOnly();

        assertSame(e.rightValue(), any.rightValue()); // object identity
    }

    @Test
    public void rightOnly_withLeft_fails() {
        Either<String, Integer> e = Either.createLeft("Hello");
        assertThrows(IllegalStateException.class, e::rightOnly);
    }

    @Test
    public void reduce_left_doesntCallRightFunction() {
        Either<String, Integer> e = Either.createLeft("Hello");
        String s = e.reduce(l -> l, r -> { fail(); return ""; });
        assertSame(s, e.leftValue());
    }

    @Test
    public void reduce_right_doesntCallLeftFunction() {
        Either<String, Integer> e = Either.createRight(1);
        Integer i = e.reduce(l -> { fail(); return 0; }, r -> r);
        assertSame(i, e.rightValue());
    }

    @Test
    public void reduceLeft_withLeft() {
        Either<String, Integer> e = Either.createLeft("Hello");
        Integer i = e.reduceLeft(String::length);
        assertEquals((int) i, e.leftValue().length());
    }

    @Test
    public void reduceRight_withLeft() {
        Either<String, Integer> e = Either.createLeft("Hello");
        String s = e.reduceRight(i -> { fail(); return ""; });
        assertSame(s, e.leftValue());
    }

    @Test
    public void reduceLeft_withRight() {
        Either<String, Integer> e = Either.createRight(1);
        Integer i = e.reduceLeft(s -> { fail(); return 0; });
        assertSame(i, e.rightValue());
    }

    @Test
    public void reduceRight_withRight() {
        Either<String, Integer> e = Either.createRight(16);
        String s = e.reduceRight(Integer::toHexString);
        assertEquals("10", s);
    }

    @Test
    public void consumeLeft_withLeft() {
        AtomicReference<String> result = new AtomicReference<>();
        Either<String, Integer> e = Either.createLeft("Hello");
        assertNull(result.get());
        e.accept(result::set, x -> {});
        assertEquals(e.leftValue(), result.get());
    }

    @Test
    public void consumeRight_withLeft() {
        AtomicReference<Integer> result = new AtomicReference<>();
        Either<String, Integer> e = Either.createLeft("Hello");
        assertNull(result.get());
        e.accept(x -> {}, result::set);
        assertNull(result.get());
    }

    @Test
    public void consumeLeft_withRight() {
        AtomicReference<String> result = new AtomicReference<>();
        Either<String, Integer> e = Either.createRight(1);
        assertNull(result.get());
        e.accept(result::set, x -> {});
        assertNull(result.get());
    }

    @Test
    public void consumeRight_withRight() {
        AtomicReference<Integer> result = new AtomicReference<>();
        Either<String, Integer> e = Either.createRight(1);
        assertNull(result.get());
        e.accept(x -> {}, result::set);
        assertEquals(e.rightValue(), result.get());
    }

    @Test
    public void filter_withRightAndSuccess() {
        Either<String, Integer> e = Either.createRight(1);
        Either<String, Integer> result = e.filter(x -> x == 1, "Not One");
        assertTrue(result.isRight());
        assertEquals(1, (int) result.rightValue());
    }

    @Test
    public void filter_withRightAndFailure() {
        Either<String, Integer> e = Either.createRight(1);
        Either<String, Integer> result = e.filter(x -> x == 2, "Not One");
        assertTrue(result.isLeft());
        assertEquals("Not One", result.leftValue());
    }

    @Test
    public void filter_withLeftAndAny() {
        Either<String, Integer> e = Either.createLeft("Hello");
        Either<String, Integer> result = e.filter(x -> { throw new Error(); }, "Not One");
        assertTrue(result.isLeft());
        assertEquals("Hello", result.leftValue());
    }

    @Test
    public void filterSupplier_withRightAndSuccess() {
        Either<String, Integer> e = Either.createRight(1);
        Either<String, Integer> result = e.filter(x -> x == 1, () -> { throw new Error(); });
        assertTrue(result.isRight());
        assertEquals(1, (int) result.rightValue());
    }

    @Test
    public void filterSupplier_withRightAndFailure() {
        Either<String, Integer> e = Either.createRight(1);
        Either<String, Integer> result = e.filter(x -> x == 2, () -> "Not One");
        assertTrue(result.isLeft());
        assertEquals("Not One", result.leftValue());
    }

    @Test
    public void constructFromOptional() {
        Optional<String> s = Optional.of("Hello");
        Either<Integer, String> result = Either.createRight(s, 0);
        assertTrue(result.isRight());
        assertEquals(s.get(), result.rightValue());
    }

    @Test
    public void constructFromOptionalEmpty() {
        Optional<String> s = Optional.empty();
        Either<Integer, String> result = Either.createRight(s, 0);
        assertTrue(result.isLeft());
        assertEquals(0, (long)result.leftValue());
    }

    @Test
    public void constructFromOptionalWithSupplier() {
        Optional<String> s = Optional.of("Hello");
        Either<Integer, String> result = Either.createRight(s, () -> { throw new Error(); });
        assertTrue(result.isRight());
        assertEquals(s.get(), result.rightValue());
    }

    @Test
    public void constructFromOptionalEmptyWithSupplier() {
        Optional<String> s = Optional.empty();
        Either<Integer, String> result = Either.createRight(s, () -> 0);
        assertTrue(result.isLeft());
        assertEquals(0, (long)result.leftValue());
    }

    // Functor Laws https://wiki.haskell.org/Functor
    // fmap is equivalent to map, Dot operator . is compose

    //Functor must preserve identity (fmap id = id)
    @Test
    public void functorPreserveIdentity() {
        Either<Integer, String> expected = Either.createRight("Hello");
        Either<Integer, String> result = expected.map(Function.identity());
        assertEquals(expected, result);
    }

    @Test
    public void functorPreserveIdentityRightProjection() {
        Either<Integer, String> expected = Either.createRight("Hello");
        Either<Integer, String> result = expected.right().map(Function.identity());
        assertEquals(expected, result);
    }

    @Test
    public void functorPreserveIdentityLeftProjection() {
        Either<Integer, String> expected = Either.createLeft(20);
        Either<Integer, String> result = expected.left().map(Function.identity());
        assertEquals(expected, result);
    }

    //Functor must preserve composition (fmap (f . g) == fmap f . fmap g)
    @Test
    public void functorPreserveComposition() {
        Either<Integer, String> e = Either.createRight("Hello");
        Function<String, Character> f = s -> s.charAt(0);
        Function<Character, Integer> g = c -> (int) c;

        Either<Integer, Integer> result1 = e.map(g.compose(f));
        Either<Integer, Integer> result2 = e.map(f).map(g);

        assertEquals(result1, result2);
    }

    @Test
    public void functorPreserveCompositionRightProjection() {
        Either<Integer, String> e = Either.createRight("Hello");
        Function<String, Character> f = s -> s.charAt(0);
        Function<Character, Integer> g = c -> (int) c;

        Either<Integer, Integer> result1 = e.right().map(g.compose(f));
        Either<Integer, Integer> result2 = e.right().map(f).right().map(g);

        assertEquals(result1, result2);
    }

    @Test
    public void functorPreserveCompositionLeftProjection() {
        Either<String, Integer> e = Either.createLeft("Hello");
        Function<String, Character> f = s -> s.charAt(0);
        Function<Character, Integer> g = c -> (int) c;

        Either<Integer, Integer> result1 = e.left().map(g.compose(f));
        Either<Integer, Integer> result2 = e.left().map(f).left().map(g);

        assertEquals(result1, result2);
    }

    // Monad laws https://wiki.haskell.org/Monad_laws
    // >>= is equivalent to flatMap

    //Left identity (return a >>= f === f a)

    @Test
    public void monadLeftIdentity() {
        String a = "Hello";

        //Either.createRight is equivalent to return
        Either<Integer, String> e = Either.createRight(a);
        Function<String, Either<Integer, Character>> f = s -> Either.createRight(s.charAt(0));

        Either<Integer, Character> result1 = e.flatMap(f);
        Either<Integer, Character> result2 = f.apply(a);

        assertEquals(result1, result2);
    }

    @Test
    public void monadLeftIdentityLeftProjection() {
        String a = "Hello";

        //Either.createLeft is equivalent to return for createLeft projection
        Either<String, Integer> e = Either.createLeft(a);
        Function<String, Either<Character, Integer>> f = s -> Either.createLeft(s.charAt(0));

        Either<Character, Integer> result1 = e.left().flatMap(f);
        Either<Character, Integer> result2 = f.apply(a);

        assertEquals(result1, result2);
    }

    @Test
    public void monadLeftIdentityRightProjection() {
        String a = "Hello";

        //Either.createRight is equivalent to return for createRight projection
        Either<Integer, String> e = Either.createRight(a);
        Function<String, Either<Integer, Character>> f = s -> Either.createRight(s.charAt(0));

        Either<Integer, Character> result1 = e.right().flatMap(f);
        Either<Integer, Character> result2 = f.apply(a);

        assertEquals(result1, result2);
    }

    //Right identity (m >>= return === m)

    @Test
    public void monadRightIdentity() {
        Either<Integer, String> m = Either.createRight("Hello");

        //Either::createRight is equivalent to return
        //Bad type inference on Java 8 means explicit type reference required. Not required for Java 11
        Either<Integer, String> result = m.<String>flatMap(Either::createRight);
        assertEquals(m, result);
    }

    @Test
    public void monadRightIdentityLeftProjection() {
        Either<String, Integer> m = Either.createLeft("Hello");

        //Either::createLeft is equivalent to return for createLeft projection
        //Bad type inference on Java 8 means explicit type reference required. Not required for Java 11
        Either<String, Integer> result = m.left().<String>flatMap(Either::createLeft);
        assertEquals(m, result);
    }

    @Test
    public void monadRightIdentityRightProjection() {
        Either<Integer, String> m = Either.createRight("Hello");

        //Either::createRight is equivalent to return
        //Bad type inference on Java 8 means explicit type reference required. Not required for Java 11
        Either<Integer, String> result = m.right().<String>flatMap(Either::createRight);
        assertEquals(m, result);
    }

    //Associativity ((m >>= f) >>= g === m >>= (\x -> f x >>= g))

    @Test
    public void monadAssociativity() {
        Either<Integer, String> m = Either.createRight("Hello");

        Function<String, Either<Integer, Character>> f = s -> Either.createRight(s.charAt(0));
        Function<Character, Either<Integer, Integer>> g = c -> Either.createRight((int) c);

        Either<Integer, Integer> result1 = m.flatMap(f).flatMap(g);
        Either<Integer, Integer> result2 = m.flatMap(x -> f.apply(x).flatMap(g));

        assertEquals(result1, result2);
    }

    @Test
    public void monadAssociativityLeftProjection() {
        Either<String, Integer> m = Either.createLeft("Hello");

        Function<String, Either<Character, Integer>> f = s -> Either.createLeft(s.charAt(0));
        Function<Character, Either<Integer, Integer>> g = c -> Either.createLeft((int) c);

        Either<Integer, Integer> result1 = m.left().flatMap(f).left().flatMap(g);
        Either<Integer, Integer> result2 = m.left().flatMap(x -> f.apply(x).left().flatMap(g));

        assertEquals(result1, result2);
    }

    @Test
    public void monadAssociativityRightProjection() {
        Either<Integer, String> m = Either.createRight("Hello");

        Function<String, Either<Integer, Character>> f = s -> Either.createRight(s.charAt(0));
        Function<Character, Either<Integer, Integer>> g = c -> Either.createRight((int) c);

        Either<Integer, Integer> result1 = m.right().flatMap(f).right().flatMap(g);
        Either<Integer, Integer> result2 = m.right().flatMap(x -> f.apply(x).right().flatMap(g));

        assertEquals(result1, result2);
    }
}