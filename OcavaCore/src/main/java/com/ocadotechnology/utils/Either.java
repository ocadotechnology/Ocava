/*
 * Copyright © 2017-2021 Ocado (Ocava)
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

/** This class loosely follows the implementation of Either used within the Scala Standard Library:
 *  https://www.scala-lang.org/api/2.12.0/scala/util/Either.html <br>
 *  Note: Either has the same memory overhead as Optional.
 *
 *  The canonical use case is tracking errors: <pre>Either&lt;Error, Result&gt;</pre>
 *  which can then be composed (using {@link #map} or {@link #bimap} or {@link #flatMap})
 *  to process the result and/or error separately.
 */
public abstract class Either<A, B> {

    public static <C, D> Either<C, D> createLeft(C value) {
        return new Left<>(value);
    }

    public static <C, D> Either<C, D> createRight(D value) {
        return new Right<>(value);
    }

    /** Constructor from Optional: The Optional value goes rightward. */
    public static <C, D> Either<C, D> createRight(Optional<D> value, C ifNotPresent) {
        return value.isPresent() ? Either.createRight(value.get()) : Either.createLeft(ifNotPresent);
    }

    /** Constructor from Optional: The Optional value goes rightward. */
    public static <C, D> Either<C, D> createRight(Optional<D> value, Supplier<C> ifNotPresent) {
        return value.isPresent() ? Either.createRight(value.get()) : Either.createLeft(ifNotPresent.get());
    }

    /**
     * Returns true if this is a Left, false otherwise.
     */
    public abstract boolean isLeft();

    /**
     * Returns true if this is a Right, false otherwise.
     */
    public abstract boolean isRight();

    /**
     * Projects this either as a left.
     * When using a left projection the manipulation functions will apply to the left side value.
     */
    public LeftProjection<A, B> left() {
        return new LeftProjection<>(this);
    }

    /**
     * Projects this either as a right.
     * When using a right projection, the manipulation functions will apply to the right side value.
     * The functions on a RightProjection act in the exact same way as functions on the Either class itself due to
     * the right-side bias of the either data-structure.
     */
    public RightProjection<A, B> right() {
        return new RightProjection<>(this);
    }

    /**
     * @return The left value of the either when present.
     * @throws IllegalStateException when the either is not left.
     */
    public abstract A leftValue();

    /**
     * @return The right value of the either when present.
     * @throws IllegalStateException when the either is not right.
     */
    public abstract B rightValue();

    /**
     * Switches the types making the left type the right and vice-versa.
     */
    public abstract Either<B, A> swap();

    /** Type coercion: cast a left to have a different (irrelevant) right type.<br>
     *  It is an error to cast a right value, so this is safe.
     */
    public abstract <C> Either<A, C> leftOnly();

    /** Type coercion: cast a right to have a different (irrelevant) left type.<br>
     *  It is an error to cast a left value, so this is safe.
     */
    public abstract <C> Either<C, B> rightOnly();

    /** Functor
     * Map the right side value of type B to the new value with type C using the functor.
     * The right side is mapped on an Either as the data-structure has a right side bias.
     * If the either has a left value, this will do nothing other than change the type of the right side.
     * This is equivalent to right().map(). To get the same function for the left value, use left().map()
     * @param functor The function for converting value of type B to C
     * @param <C> The new right side value type
     * @return A new either with the new type signature {@code Either<A, C>}
     */
    public <C> Either<A, C> map(Function<B, C> functor) {
        return right().map(functor);
    }

    /** Bifunctor
     * Map both the left and right values to a new either with new types.
     * This function will use whichever map function is appropriate for this either to map to a new either with
     * new value types.
     * @param leftMap The function used on a left valued either
     * @param rightMap The function used on a right valued either
     * @param <C> The left type of the new either
     * @param <D> The right type of the new either
     * @return An either of type {@code Either<C, D>} constructed from the functions given
     */
    public abstract <C, D> Either<C, D> bimap(Function<A, C> leftMap, Function<B, D> rightMap);

    // Would be nice if these were all called 'reduce', but the (Java 8) compiler can't disambiguate well enough.
    /**
     * Reduce an either into a single value of type C
     * @param leftReduce The function used to reduce a left either to C
     * @param rightReduce The function used to reduce a right either to C
     * @param <C> The type of the new value
     * @return The result of the reduction
     */
    public abstract <C> C reduce(Function<A, C> leftReduce, Function<B, C> rightReduce);
    /**
     * Reduce an either into a double
     * @param leftReduce The function used to reduce a left either to double
     * @param rightReduce The function used to reduce a right either to double
     * @return The result of the reduction
     */
    public abstract double reduceToDouble(ToDoubleFunction<A> leftReduce, ToDoubleFunction<B> rightReduce);
    /**
     * Reduce an either into a int
     * @param leftReduce The function used to reduce a left either to int
     * @param rightReduce The function used to reduce a right either to int
     * @return The result of the reduction
     */
    public abstract int reduceToInt(ToIntFunction<A> leftReduce, ToIntFunction<B> rightReduce);
    /**
     * Reduce an either into a boolean
     * @param leftReduce The function used to reduce a left either to boolean
     * @param rightReduce The function used to reduce a right either to boolean
     * @return The result of the reduction
     */
    public abstract boolean reduceToBoolean(Predicate<A> leftReduce, Predicate<B> rightReduce);

    /**
     * If a left, reduce to the right typed value using the reducer function. If right, get the right value.
     * @param reduceLeft left value reducer
     * @return right typed value
     */
    public abstract B reduceLeft(Function<A, B> reduceLeft);

    /**
     * If a right, reduce to a left typed value using the reducer function. If left, get the left value.
     * @param reduceRight right value reducer
     * @return right typed value
     */
    public abstract A reduceRight(Function<B, A> reduceRight);

    /** Consumer: Calls either leftConsumer or rightConsumer (but not both). */
    public abstract void accept(Consumer<A> leftConsumer, Consumer<B> rightConsumer);

    /** Monad
     * Returns an either which is returned as the result of applying the mapper function
     * to the right hand value on this either.
     * If the either has a left value, it will not do anything.
     * This function operates identically to the way the flatMap methods on the
     * Java standard library's Optional and Stream datatypes.
     * @param mapper The function run on the right hand value to create a new Either
     * @param <C> The new right hand type
     */
    public abstract <C> Either<A, C> flatMap(Function<B, Either<A, C>> mapper);

    /**
     * Returns an Optional of the right value if the either is right.
     * Optional.empty() if the either is left.
     * This is by convention on the Either datatype which has a right-side bias.
     * A major use-case of the Either type is keeping successful value on the righthand side and
     * failure cases on the left.
     * Because of this, the righthand, successful case, gets used within the optional conversion.
     */
    public abstract Optional<B> toOptional();

    /**
     * Tests the given predicates against the value of the either.
     * @param leftCheck The predicate that must be satisfied if the either has a left value
     * @param rightCheck The predicate that must be satisfied if the either has a right value
     * @return The result of the applied predicate.
     */
    public abstract boolean testEither(Predicate<A> leftCheck, Predicate<B> rightCheck);

    /** If 'this' is right-leaning, test the predicate against the right value.
     *  If true, return the same value as 'this', otherwise return a left(onNotAccepted).<br>
     *  If 'this' is left-leaning, return the original value.
     */
    public abstract Either<A, B> filter(Predicate<B> acceptor, A onNotAccepted);

    /** Same as {@link #filter(Predicate, Object)} where the Supplier is only called if necessary. */
    public abstract Either<A, B> filter(Predicate<B> acceptor, Supplier<A> onNotAccepted);

    private static final class Left<A, B> extends Either<A, B> {
        private final A leftValue;

        private Left(A a) {
            Preconditions.checkArgument(a != null);
            this.leftValue = a;
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
        public A leftValue() {
            return leftValue;
        }

        @Override
        public B rightValue() {
            Preconditions.checkState(false);
            return null;
        }

        @Override
        public Either<B, A> swap() {
            return new Right<>(leftValue);
        }

        public <C> Either<A, C> leftOnly() {
            return (Either<A, C>)this;
        }

        public <C> Either<C, B> rightOnly() {
            Preconditions.checkState(false);
            return null;
        }

        @Override
        public Optional<B> toOptional() {
            return Optional.empty();
        }

        @Override
        public boolean testEither(Predicate<A> leftCheck, Predicate<B> rightCheck) {
            return leftCheck.test(leftValue);
        }

        @Override
        public void accept(Consumer<A> first, Consumer<B> second) {
            first.accept(leftValue);
        }

        @Override
        public <C, D> Either<C, D> bimap(Function<A, C> leftMap, Function<B, D> rightMap) {
            return Either.createLeft(leftMap.apply(leftValue));
        }

        @Override
        public <C> Either<A, C> flatMap(Function<B, Either<A, C>> g) {
            return (Either<A, C>)this;
        }

        @Override
        public <C> C reduce(Function<A, C> leftReduce, Function<B, C> rightReduce) {
            return leftReduce.apply(leftValue);
        }

        @Override
        public double reduceToDouble(ToDoubleFunction<A> leftReduce, ToDoubleFunction<B> rightReduce) {
            return leftReduce.applyAsDouble(leftValue);
        }

        @Override
        public int reduceToInt(ToIntFunction<A> leftReduce, ToIntFunction<B> rightReduce) {
            return leftReduce.applyAsInt(leftValue);
        }

        @Override
        public boolean reduceToBoolean(Predicate<A> leftReduce, Predicate<B> rightReduce) {
            return leftReduce.test(leftValue);
        }

        @Override
        public B reduceLeft(Function<A, B> reduceLeft) {
            return reduceLeft.apply(leftValue);
        }

        @Override
        public A reduceRight(Function<B, A> reduceRight) {
            return leftValue;
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
            return MoreObjects.toStringHelper(this).add("left", leftValue).toString();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Left && leftValue.equals(((Left<?, ?>)other).leftValue);
        }

        @Override
        public int hashCode() {
            return leftValue.hashCode();
        }
    }

    private static final class Right<A, B> extends Either<A, B> {
        private final B rightValue;

        private Right(B b) {
            Preconditions.checkArgument(b != null);
            this.rightValue = b;
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
        public A leftValue() {
            Preconditions.checkState(false);
            return null;
        }

        @Override
        public B rightValue() {
            return rightValue;
        }

        @Override
        public Either<B, A> swap() {
            return new Left<>(rightValue);
        }

        public <C> Either<A, C> leftOnly() {
            Preconditions.checkState(false);
            return null;
        }

        public <C> Either<C, B> rightOnly() {
            return (Either<C, B>)this;
        }

        @Override
        public Optional<B> toOptional() {
            return Optional.of(rightValue);
        }

        @Override
        public boolean testEither(Predicate<A> leftCheck, Predicate<B> rightCheck) {
            return rightCheck.test(rightValue);
        }

        @Override
        public void accept(Consumer<A> first, Consumer<B> second) {
            second.accept(rightValue);
        }

        @Override
        public <C, D> Either<C, D> bimap(Function<A, C> leftMap, Function<B, D> rightMap) {
            return Either.createRight(rightMap.apply(rightValue));
        }

        @Override
        public <C> Either<A, C> flatMap(Function<B, Either<A, C>> g) {
            return g.apply(rightValue);
        }

        @Override
        public <C> C reduce(Function<A, C> leftReduce, Function<B, C> rightReduce) {
            return rightReduce.apply(rightValue);
        }

        @Override
        public double reduceToDouble(ToDoubleFunction<A> leftReduce, ToDoubleFunction<B> rightReduce) {
            return rightReduce.applyAsDouble(rightValue);
        }

        @Override
        public int reduceToInt(ToIntFunction<A> leftReduce, ToIntFunction<B> rightReduce) {
            return rightReduce.applyAsInt(rightValue);
        }

        @Override
        public boolean reduceToBoolean(Predicate<A> leftReduce, Predicate<B> rightReduce) {
            return rightReduce.test(rightValue);
        }

        @Override
        public B reduceLeft(Function<A, B> reduceLeft) {
            return rightValue;
        }

        @Override
        public A reduceRight(Function<B, A> reduceRight) {
            return reduceRight.apply(rightValue);
        }

        @Override
        public Either<A, B> filter(Predicate<B> acceptor, A onNotAccepted) {
            return acceptor.test(rightValue) ? this : Either.createLeft(onNotAccepted);
        }

        @Override
        public Either<A, B> filter(Predicate<B> acceptor, Supplier<A> onNotAccepted) {
            return acceptor.test(rightValue) ? this : Either.createLeft(onNotAccepted.get());
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("right", rightValue).toString();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Right && rightValue.equals(((Right<?, ?>)other).rightValue);
        }

        @Override
        public int hashCode() {
            return rightValue.hashCode();
        }
    }

    public static final class LeftProjection<A, B> {
        private final Either<A, B> either;

        private LeftProjection(Either<A, B> either) {
            this.either = either;
        }

        /**
         * If the parent either is left it will return an Optional of the left value. Empty otherwise
         */
        public Optional<A> toOptional() {
            if (either.isLeft()) {
                return Optional.of(either.leftValue());
            } else {
                return Optional.empty();
            }
        }

        /** Functor
         * Map the left side value of type A to the new value with type C using the functor.
         * @param functor The function for converting left value of type A to C
         * @param <C> The new left side type
         * @return A new either with the new type signature {@code Either<C, B>}
         */
        public <C> Either<C, B> map(Function<A, C> functor) {
            if (either.isLeft()) {
                return new Left<C, B>(functor.apply(either.leftValue()));
            } else {
                return (Either<C, B>) either;
            }
        }

        /** Monad
         * Returns the either which is returned as the result of applying the mapper function to
         * the left hand value on this either.
         * If the either has a right value, it will not do anything.
         * This function operates identically to the way the flatMap methods on
         * the Java standard library's Optional and Stream datatypes.
         * @param mapper The function run on the left hand value to create a new Either
         * @param <C> The new left hand type
         */
        public <C> Either<C, B> flatMap(Function<A, Either<C, B>> mapper) {
            if (either.isLeft()) {
                return mapper.apply(either.leftValue());
            } else {
                return (Either<C, B>) either;
            }
        }

        /**
         * Tests the given predicate against the left value
         */
        public boolean test(Predicate<A> acceptor) {
            return either.testEither(acceptor, unused -> false);
        }

        /**
         * Runs the consumer on the left value if it is present.
         */
        public void ifPresent(Consumer<A> f) {
            either.accept(f, unused -> {});
        }

        /**
         * If this is left-leaning, test the predicate against the left value, Otherwise create a right with
         * the provided value.
         */
        public Either<A, B> filter(Predicate<A> acceptor, B onNotAccepted) {
            if (either.isLeft()) {
                return acceptor.test(either.leftValue())
                    ? either
                    : new Right<A, B>(onNotAccepted);
            } else {
                return either;
            }
        }

        /**
         * If this is left-leaning, test the predicate against the left value, Otherwise create a right with
         * the value returned from the supplier.
         */
        public Either<A, B> filter(Predicate<A> acceptor, Supplier<B> onNotAccepted) {
            if (either.isLeft()) {
                return acceptor.test(either.leftValue())
                        ? either
                        : new Right<A, B>(onNotAccepted.get());
            } else {
                return either;
            }
        }
    }

    public static final class RightProjection<A, B> {
        private final Either<A, B> either;

        private RightProjection(Either<A, B> either) {
            this.either = either;
        }

        /**
         * If the parent either is right it will return an Optional of the right value. Empty otherwise
         */
        public Optional<B> toOptional() {
            return either.toOptional();
        }

        /** Functor
         * Map the right side value of type A to the new value with type C using the functor.
         * @param functor The function for converting right value of type B to C
         * @param <C> The new right side type
         * @return A new either with the new type signature {@code Either<A, C>}
         */
        public <C> Either<A, C> map(Function<B, C> functor) {
            if (either.isRight()) {
                return new Right<A, C>(functor.apply(either.rightValue()));
            } else {
                return (Either<A, C>) either;
            }
        }

        /** Monad
         * Returns the either which is returned as the result of applying the mapper function to
         * the right hand value on this either.
         * If the either has a left value, it will not do anything.
         * This function operates identically to the way the flatMap methods on
         * the Java standard library's Optional and Stream datatypes.
         * @param mapper The function run on the right hand value to create a new Either
         * @param <C> The new right hand type
         */
        public <C> Either<A, C> flatMap(Function<B, Either<A, C>> mapper) {
            if (either.isRight()) {
                return mapper.apply(either.rightValue());
            } else {
                return (Either<A, C>) either;
            }
        }

        /**
         * Tests the given predicate against the right value
         */
        public boolean test(Predicate<B> acceptor) {
            return either.testEither(unused -> false, acceptor);
        }

        /**
         * Runs the consumer on the right value if it is present.
         */
        public void ifPresent(Consumer<B> f) {
            either.accept(unused -> {}, f);
        }

        /**
         * If this is right-leaning, test the predicate against the right value, Otherwise create a left with
         * the provided value.
         */
        public Either<A, B> filter(Predicate<B> acceptor, A onNotAccepted) {
            return either.filter(acceptor, onNotAccepted);
        }

        /**
         * If this is right-leaning, test the predicate against the right value, Otherwise create a left with
         * the value returned from the supplier.
         */
        public Either<A, B> filter(Predicate<B> acceptor, Supplier<A> onNotAccepted) {
            return either.filter(acceptor, onNotAccepted);
        }
    }
}
