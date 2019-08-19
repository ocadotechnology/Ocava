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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

/** Updated based on Haskell data type: https://hackage.haskell.org/package/base-4.8.2.0/docs/Data-Either.html <br>
 *  Note: Either has the same memory overhead as Optional.
 *
 *  The canonical use case is tracking errors: <pre>Either&lt;Error, Result&gt;</pre>
 *  which can then be composed (using {@link #second} or {@link #bimap} or {@link #bind})
 *  to process the result and/or error separately.
 */
public abstract class Either<A, B> {

    public static <C, D> Either<C, D> left(C value) {
        return new Left<>(value);
    }

    public static <C, D> Either<C, D> right(D value) {
        return new Right<>(value);
    }

    /** Constructor from Optional: The Optional value goes rightward. */
    public static <C, D> Either<C, D> right(Optional<D> value, C ifNotPresent) {
        return value.isPresent() ? Either.right(value.get()) : Either.left(ifNotPresent);
    }

    /** Constructor from Optional: The Optional value goes rightward. */
    public static <C, D> Either<C, D> right(Optional<D> value, Supplier<C> ifNotPresent) {
        return value.isPresent() ? Either.right(value.get()) : Either.left(ifNotPresent.get());
    }

    public static <C, D> Either<C, D> exactly(Optional<C> firstChoice, Optional<D> alternativeChoice) {
        Preconditions.checkState(firstChoice.isPresent() ^ alternativeChoice.isPresent());
        return firstChoice.isPresent() ? new Left<>(firstChoice.get()) : new Right<>(alternativeChoice.get());
    }

    public static <C, D> Either<C, D> firstOf(Optional<C> firstChoice, Optional<D> alternativeChoice) {
        if (firstChoice.isPresent()) {
            return new Left<>(firstChoice.get());
        }

        Preconditions.checkState(alternativeChoice.isPresent());
        return new Right<>(alternativeChoice.get());
    }

    public static <C, D> Either<C, D> firstNonNull(C firstChoice, D alternativeChoice) {
        return (firstChoice != null) ? new Left<>(firstChoice) : new Right<>(alternativeChoice);
    }

    public abstract boolean isLeft();
    public abstract boolean isRight();

    public abstract A left();
    public abstract B right();
    public abstract <C> C either(Function<A, C> first, Function<B, C> second);

    public abstract Either<B, A> flip();

    /** Type coercion: cast a left to have a different (irrelevant) right type.<br>
     *  It is an error to cast a right value, so this is safe.
     */
    public abstract <C> Either<A, C> leftOnly();

    /** Type coercion: cast a right to have a different (irrelevant) left type.<br>
     *  It is an error to cast a left value, so this is safe.
     */
    public abstract <C> Either<C, B> rightOnly();

    // Functor
    public <C> Either<A, C> fmap(Function<B, C> functor) {
        return second(functor);
    }

    // Bifunctor
    public abstract <C> Either<C, B> first(Function<A, C> functor);
    public abstract <C> Either<A, C> second(Function<B, C> functor);
    public abstract <C, D> Either<C, D> bimap(Function<A, C> first, Function<B, D> second);

    // Would be nice if these were all called 'reduce', but the (Java 8) compiler can't disambiguate well enough.
    public abstract <C> C reduce(Function<A, C> first, Function<B, C> second);
    public abstract double reduceToDouble(ToDoubleFunction<A> first, ToDoubleFunction<B> second);
    public abstract int reduceToInt(ToIntFunction<A> first, ToIntFunction<B> second);
    public abstract boolean reduceToBoolean(Predicate<A> first, Predicate<B> second);

    public abstract B reduceLeft(Function<A, B> first);
    public abstract A reduceRight(Function<B, A> second);

    /** If 'this' is left-leaning, call consumer with the left value.  Otherwise do nothing. */
    public abstract void first(Consumer<A> consumer);
    /** If 'this' is right-leaning, call consumer with the right value.  Otherwise do nothing. */
    public abstract void second(Consumer<B> consumer);

    /** Identical to {@link #first(Consumer)}<br>
     *  <small>Java 8 can't resolve types between Function&lt;A&gt; and Consumer&lt;A&gt;, so firstc and secondc added as convenience.</small>
     */
    public void firstc(Consumer<A> consumer) {
        first(consumer);
    }

    /** Identical to {@link #second(Consumer)}<br>
     *  <small>Java 8 can't resolve types between Function&lt;A&gt; and Consumer&lt;A&gt;, so firstc and secondc added as convenience.</small>
     */
    public void secondc(Consumer<B> consumer) {
        second(consumer);
    }

    /** Consumer: Calls either {@link #first(Consumer)} or {@link #second(Consumer)} (but not both). */
    public abstract void accept(Consumer<A> first, Consumer<B> second);

    // Monad
    /** If <tt>this</tt> is <em>right</em>, then apply <tt>g</tt> and return new result,
     *  otherwise return original <em>left</em> value (permits composed computation with error side-channel).
     */
    public abstract <C> Either<A, C> bind(Function<B, Either<A, C>> g);

    public abstract Optional<A> maybeLeft();
    public abstract Optional<B> maybeRight();

    public abstract boolean testLeft(Predicate<A> leftCheck);
    public abstract boolean testRight(Predicate<B> RightCheck);
    public abstract boolean testEither(Predicate<A> leftCheck, Predicate<B> rightCheck);

    /** If 'this' is right-leaning, test the predicate against the right value.
     *  If true, return the same value as 'this', otherwise return a left(onNotAccepted).<br>
     *  If 'this' is left-leaning, return the original value.
     */
    public abstract Either<A, B> filter(Predicate<B> acceptor, A onNotAccepted);

    /** Same as {@link #filter(Predicate, Object)} where the Supplier is only called if necessary. */
    public abstract Either<A, B> filter(Predicate<B> acceptor, Supplier<A> onNotAccepted);

    private static final class Left<A, B> extends Either<A, B> {
        private final A left;

        private Left(A a) {
            Preconditions.checkArgument(a != null);
            this.left = a;
        }

        @Override
        public boolean isLeft() {
            return true;
        }

        @Override
        public boolean isRight() {
            return false;
        }

        @Override
        public A left() {
            return left;
        }

        @Override
        public B right() {
            Preconditions.checkState(false);
            return null;
        }

        @Override
        public Either<B, A> flip() {
            return new Right<>(left);
        }

        public <C> Either<A, C> leftOnly() {
            return (Either<A, C>)this;
        }

        public <C> Either<C, B> rightOnly() {
            Preconditions.checkState(false);
            return null;
        }

        @Override
        public Optional<A> maybeLeft() {
            return Optional.of(left);
        }

        @Override
        public Optional<B> maybeRight() {
            return Optional.empty();
        }

        @Override
        public boolean testLeft(Predicate<A> leftCheck) {
            return leftCheck.test(left);
        }

        @Override
        public boolean testRight(Predicate<B> rightCheck) {
            return false;
        }

        @Override
        public boolean testEither(Predicate<A> leftCheck, Predicate<B> rightCheck) {
            return testLeft(leftCheck);
        }

        @Override
        public <C> Either<C, B> first(Function<A, C> functor) {
            return new Left<>(functor.apply(left));
        }

        @Override
        public <C> Either<A, C> second(Function<B, C> functor) {
            return (Either<A, C>)this;
        }

        @Override
        public void first(Consumer<A> consumer) {
            consumer.accept(left);
        }

        @Override
        public void second(Consumer<B> consumer) {
        }

        @Override
        public void accept(Consumer<A> first, Consumer<B> second) {
            first.accept(left);
        }

        @Override
        public <C, D> Either<C, D> bimap(Function<A, C> first, Function<B, D> second) {
            return Either.left(first.apply(left));
        }

        @Override
        public <C> C either(Function<A, C> first, Function<B, C> second) {
            return first.apply(left);
        }

        @Override
        public <C> Either<A, C> bind(Function<B, Either<A, C>> g) {
            return (Either<A, C>)this;
        }

        @Override
        public <C> C reduce(Function<A, C> first, Function<B, C> second) {
            return first.apply(left);
        }

        @Override
        public double reduceToDouble(ToDoubleFunction<A> first, ToDoubleFunction<B> second) {
            return first.applyAsDouble(left);
        }

        @Override
        public int reduceToInt(ToIntFunction<A> first, ToIntFunction<B> second) {
            return first.applyAsInt(left);
        }

        @Override
        public boolean reduceToBoolean(Predicate<A> first, Predicate<B> second) {
            return first.test(left);
        }

        @Override
        public B reduceLeft(Function<A, B> first) {
            return first.apply(left);
        }

        @Override
        public A reduceRight(Function<B, A> second) {
            return left;
        }

        @Override
        public Either<A, B> filter(Predicate<B> acceptor, A onNotAccepted) {
            return this;
        }

        @Override
        public Either<A, B> filter(Predicate<B> acceptor, Supplier<A> onNotAccepted) {
            return this;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("left", left).toString();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Left && left.equals(((Left<?, ?>)other).left);
        }

        @Override
        public int hashCode() {
            return left.hashCode();
        }
    }

    private static final class Right<A, B> extends Either<A, B> {
        private final B right;

        private Right(B b) {
            Preconditions.checkArgument(b != null);
            this.right = b;
        }

        @Override
        public boolean isLeft() {
            return false;
        }

        @Override
        public boolean isRight() {
            return true;
        }

        @Override
        public A left() {
            Preconditions.checkState(false);
            return null;
        }

        @Override
        public B right() {
            return right;
        }

        @Override
        public Either<B, A> flip() {
            return new Left<>(right);
        }

        public <C> Either<A, C> leftOnly() {
            Preconditions.checkState(false);
            return null;
        }

        public <C> Either<C, B> rightOnly() {
            return (Either<C, B>)this;
        }

        @Override
        public Optional<A> maybeLeft() {
            return Optional.empty();
        }

        @Override
        public Optional<B> maybeRight() {
            return Optional.of(right);
        }

        @Override
        public boolean testLeft(Predicate<A> leftCheck) {
            return false;
        }

        @Override
        public boolean testRight(Predicate<B> rightCheck) {
            return rightCheck.test(right);
        }

        @Override
        public boolean testEither(Predicate<A> leftCheck, Predicate<B> rightCheck) {
            return testRight(rightCheck);
        }

        @Override
        public <C> Either<C, B> first(Function<A, C> functor) {
            return (Either<C, B>)this;
        }

        @Override
        public <C> Either<A, C> second(Function<B, C> functor) {
            return Either.right(functor.apply(right));
        }

        @Override
        public void first(Consumer<A> consumer) {
        }

        @Override
        public void second(Consumer<B> consumer) {
            consumer.accept(right);
        }

        @Override
        public void accept(Consumer<A> first, Consumer<B> second) {
            second.accept(right);
        }

        @Override
        public <C, D> Either<C, D> bimap(Function<A, C> first, Function<B, D> second) {
            return Either.right(second.apply(right));
        }

        @Override
        public <C> C either(Function<A, C> first, Function<B, C> second) {
            return second.apply(right);
        }

        @Override
        public <C> Either<A, C> bind(Function<B, Either<A, C>> g) {
            return g.apply(right);
        }

        @Override
        public <C> C reduce(Function<A, C> first, Function<B, C> second) {
            return second.apply(right);
        }

        @Override
        public double reduceToDouble(ToDoubleFunction<A> first, ToDoubleFunction<B> second) {
            return second.applyAsDouble(right);
        }

        @Override
        public int reduceToInt(ToIntFunction<A> first, ToIntFunction<B> second) {
            return second.applyAsInt(right);
        }

        @Override
        public boolean reduceToBoolean(Predicate<A> first, Predicate<B> second) {
            return second.test(right);
        }

        @Override
        public B reduceLeft(Function<A, B> first) {
            return right;
        }

        @Override
        public A reduceRight(Function<B, A> second) {
            return second.apply(right);
        }

        @Override
        public Either<A, B> filter(Predicate<B> acceptor, A onNotAccepted) {
            return acceptor.test(right) ? this : Either.left(onNotAccepted);
        }

        @Override
        public Either<A, B> filter(Predicate<B> acceptor, Supplier<A> onNotAccepted) {
            return acceptor.test(right) ? this : Either.left(onNotAccepted.get());
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("right", right).toString();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Right && right.equals(((Right<?, ?>)other).right);
        }

        @Override
        public int hashCode() {
            return right.hashCode();
        }
    }
}
