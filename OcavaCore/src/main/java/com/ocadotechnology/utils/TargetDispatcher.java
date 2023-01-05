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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Provides dynamic method invocation across registered instances of a particular class. Target instances are registered
 * with some corresponding identifier that is subsequently used to select the instance upon which to invoke a particular
 * method.
 *
 * @param <I> Type of the identifiers which uniquely specify the target instances.
 * @param <T> Type of the target instances upon which method calls will be dynamically invoked.
 */
@ParametersAreNonnullByDefault
public class TargetDispatcher<I, T> {
    private final Map<I, T> targetsById;

    /**
     * Create a TargetDispatcher by providing your own map to start with.
     * The constructor will accept immutable maps but many of the methods in this class will not support them.
     * @param targetsById the initial map to use.
     */
    public TargetDispatcher(Map<I, T> targetsById) {
        this.targetsById = targetsById;
    }

    /**
     * Creates a TargetDispatcher with an initial empty HashMap.
     */
    public TargetDispatcher() {
        this(new HashMap<>());
    }

    /**
     * Adds a new target to the map if there does not already exist one with the provided ID.
     * @param id the ID of the target
     * @param member the value of the target
     * @return true if the target was added successfully.
     */
    public final boolean registerTarget(I id, T member) {
        T existingTarget = targetsById.putIfAbsent(id, member);
        return existingTarget == null;
    }

    /**
     * Updates the target in the map with the provided ID, or adds a new target if the ID is not in the map.
     * @param id the ID of the target
     * @param member the new value to put in the map
     * @return true if there was no existing value to update
     */
    public final boolean updateTarget(I id, T member) {
        T existingTarget = targetsById.put(id, member);
        return existingTarget == null;
    }

    /**
     * Removes a target from the map.
     * @param id the ID of the target to remove
     * @return the removed target
     */
    public final T deregisterTarget(I id) {
        return targetsById.remove(id);
    }

    /**
     * Removes all targets from the map.
     */
    public final void deregisterAllTargets() {
        targetsById.clear();
    }

    /**
     * Checks if a target exists in the map.
     * @param id the ID to check
     * @return true if the target exists in the map.
     */
    public final boolean hasTarget(I id) {
        return targetsById.containsKey(id);
    }

    /**
     * Gets a target from the map.
     * @param id the ID of the target to get.
     * @return the target registered with that ID.
     * @throws UnknownTargetException if no target is currently registered with the specified ID.
     */
    public T getTarget(@CheckForNull I id) {
        if (id == null) {
            throw new IllegalArgumentException("Target ID cannot be NULL");
        }
        T target = targetsById.get(id);
        if (target == null) {
            throw new UnknownTargetException(id);
        }
        return target;
    }

    /**
     * Optionally returns the target from the map.
     * @param id the ID of the target to get.
     * @return an optional containing the target, or empty if no target in the map has that ID.
     */
    public Optional<T> maybeGetTarget(I id) {
        return Optional.ofNullable(targetsById.get(id));
    }

    /**
     * Performs a provided action on the target registered with the provided ID.
     * @param id the ID of the target to get.
     * @param action an action to perform on the target.
     * @throws IllegalArgumentException if the specified ID is NULL.
     * @throws UnknownTargetException if no target is currently registered with the specified ID.
     * @throws NullPointerException if the specified action is NULL.
     */
    public final void doTargetAction(I id, Consumer<T> action) {
        action.accept(getTarget(id));
    }

    /**
     * Performs a provided action on the target registered with the provided ID if one exists.
     * @param id the ID of the target to get.
     * @param action an action to perform on the target.
     * @throws NullPointerException if the specified action is NULL.
     */
    public final void doTargetActionIfRegistered(I id, Consumer<T> action) {
        T target = targetsById.get(id);
        if (target != null) {
            action.accept(target);
        }
    }

    /**
     * Performs a function on the target registered with the provided ID.
     * @param id the ID of the target to get.
     * @param function the function to apply to the target.
     * @param <R> the return type of the function.
     * @return the result of applying the function to the target.
     * @throws IllegalArgumentException if the specified ID is NULL.
     * @throws UnknownTargetException if no target is currently registered with the specified ID.
     * @throws NullPointerException if the specified function is NULL.
     */
    public final <R> R doTargetFunction(I id, Function<T, R> function) {
        return function.apply(getTarget(id));
    }

    /**
     * Makes a copy (in case action causes deregistration), and calls the action for all targets.
     * Use deregisterAllTargets to deregister without performing an action.
     * @param action the action to apply.
     */
    public final void doForAllTargets(Consumer<T> action) {
        Collection<T> targets = ImmutableList.copyOf(targetsById.values());
        targets.forEach(action);
    }

    /**
     * Makes a copy (in case action causes deregistration), and calls the action for all targets matching the filter.
     * Use deregisterAllTargets to deregister without performing an action.
     * @param filter a predicate to filter targets to apply the action to.
     * @param action the action to apply.
     */
    public final void doForAllTargetsWhere(Predicate<T> filter, Consumer<T> action) {
        Collection<T> targets = ImmutableList.copyOf(targetsById.values());
        targets.stream().filter(filter).forEach(action);
    }

    /**
     * Makes a copy (in case action causes deregistration), and performs a function on all targets.
     * @param mapper the function to apply to all targets.
     * @param <R> the return type of the function.
     * @return a stream containing the results of applying the function.
     */
    public final <R> Stream<R> mapAllTargets(Function<T, R> mapper) {
        Collection<T> targets = ImmutableList.copyOf(targetsById.values());
        return targets.stream().map(mapper);
    }

    /**
     * Gets an immutable copy of all targets in the map.
     * @return all targets in the map.
     */
    public final Map<I, T> getAllTargets() {
        return ImmutableMap.copyOf(targetsById);
    }

    /**
     * Makes a copy of all targets and streams them.
     * @return a stream containing the targets.
     */
    public final Stream<T> streamAllTargets() {
        return ImmutableList.copyOf(targetsById.values()).stream();
    }

    /**
     * Makes a copy (in case filter causes deregistration), and filters all targets using the provided predicate.
     * @param filter the filter to apply.
     * @return a stream containing the filtered targets.
     */
    public final Stream<T> filterAllTargets(Predicate<T> filter) {
        Collection<T> targets = ImmutableList.copyOf(targetsById.values());
        return targets.stream().filter(filter);
    }

    /**
     * Makes a copy (in case predicate causes deregistration), and checks if any target matches the provided predicate.
     * @param predicate the predicate to check.
     * @return true if any target matches the predicate.
     */
    public final boolean anyTargetMatches(Predicate<T> predicate) {
        Collection<T> targets = ImmutableList.copyOf(targetsById.values());
        return targets.stream().anyMatch(predicate);
    }

    /**
     * Exception thrown when an attempt to get a target from the map is made but that ID does not exist in the map.
     */
    public static final class UnknownTargetException extends RuntimeException {
        private UnknownTargetException(Object targetId) {
            super("No target registered with ID [" + targetId + "]");
        }
    }
}
