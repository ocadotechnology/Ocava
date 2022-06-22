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
package com.ocadotechnology.indexedcache;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.ocadotechnology.id.Identified;
import com.ocadotechnology.id.Identity;

/**
 * Defines a change to be applied to the {@link IndexedImmutableObjectCache}.  A change can be adding, updating or
 * deleting an object in the cache.
 * @param <C> the type of object in the cache.
 */
public class Change<C extends Identified<?>> implements Serializable {
    @CheckForNull public final C originalObject;
    @CheckForNull public final C newObject;

    private Change(@Nullable C originalObject, @Nullable C newObject) {
        Preconditions.checkState(originalObject != null || newObject != null, "Change should have at least one non null object");
        this.originalObject = originalObject;
        this.newObject = newObject;
    }

    /**
     * Create a change object without checking.  Package-private for internal use only.
     *
     * @throws IllegalArgumentException if both objects are null or if their ids don't match.
     */
    static <C extends Identified<?>> Change<C> change(C oldObject, C newObject) {
        if (newObject == null) {
            return delete(oldObject);
        }
        if (oldObject == null) {
            return add(newObject);
        }
        return update(oldObject, newObject);
    }

    /**
     * Create a Change to add a new object into the cache.
     * @param newObject the object to add into the cache.
     *
     * @throws IllegalArgumentException if the object is null
     */
    public static <C extends Identified<?>> Change<C> add(C newObject) {
        Preconditions.checkArgument(newObject != null, "Attempted to create an addition of a null newObject");

        return new Change<>(null, newObject);
    }

    /**
     * Create a Change to update an existing object in the cache.
     * @param oldObject the value expected to be in the cache
     * @param newObject the object to add into the cache.
     *
     * @throws IllegalArgumentException if either object is null or if their ids do not match.
     */
    public static <C extends Identified<?>> Change<C> update(C oldObject, C newObject) {
        Preconditions.checkArgument(oldObject != null, "Attempted to create an update from a null oldObject");
        Preconditions.checkArgument(newObject != null, "Attempted to create an update to a null newObject");
        Preconditions.checkArgument(newObject.getId().equals(oldObject.getId()), "Attempted to create an update with non-matching ids.");

        return new Change<>(oldObject, newObject);
    }

    /**
     * Create a Change to remove an object from the cache.
     * @param oldObject the value expected to be in the cache.
     *
     * @throws IllegalArgumentException if the object is null
     */
    public static <C extends Identified<?>> Change<C> delete(C oldObject) {
        Preconditions.checkArgument(oldObject != null, "Attempted to create a deletion of a null oldObject");

        return new Change<>(oldObject, null);
    }

    /**
     * Create a Change which is the inverse of the original object
     */
    public Change<C> inverse() {
        return new Change<>(newObject, originalObject);
    }

    /**
     * Create a no-action Change from and to the supplied object.
     *
     * This method is useful when using the map API
     *
     * @param object the value expected to be in the cache.
     *
     * @throws IllegalArgumentException if the object is null
     */
    public static <C extends Identified<?>> Change<C> identity(C object) {
        Preconditions.checkArgument(object != null, "Attempted to create an identity change with a null object");

        return new Change<>(object, object);
    }

    /**
     * Update the exiting Change with a new end-state.
     * @param mapper function to apply a change to the newObject of this Change.
     *
     * @throws IllegalArgumentException if the mapper or returned object is null or if the returned object's id does not
     *                                  match the existing Change.
     */
    public Change<C> map(UnaryOperator<C> mapper) {
        Preconditions.checkArgument(mapper != null, "Attempted to update a Change with a null mapper.");
        Preconditions.checkNotNull(newObject, "Attempted to update a Change with a null newObject.");
        C newObject = Preconditions.checkNotNull(mapper.apply(this.newObject), "mapper returned a null new object.");
        Preconditions.checkArgument(newObject.getId().equals(getId()), "Attempted to update a Change with an incompatible new object");
        return new Change<>(originalObject, newObject);
    }

    /**
     * @return {@link Optional} of the newObject or empty if  it is null
     */
    public Optional<C> getNewObject() {
        return Optional.ofNullable(newObject);
    }

    /**
     * @return {@link Optional} of the originalObject or empty if  it is null
     */
    public Optional<C> getOriginalObject() {
        return Optional.ofNullable(originalObject);
    }

    public Identity<?> getId() {
        if (originalObject != null) {
            return originalObject.getId();
        }
        Preconditions.checkNotNull(newObject, "Unreachable code. Constructor ensures that at least one field is non-null");
        return newObject.getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Change)) return false;
        Change<?> change = (Change<?>) o;
        return Objects.equals(originalObject, change.originalObject)
                && Objects.equals(newObject, change.newObject);
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("originalObject", originalObject)
                .add("newObject", newObject)
                .toString();
    }
}
