/*
 * Copyright © 2017-2026 Ocado (Ocava)
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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.ocadotechnology.id.Identified;

/**
 * A wrapper around an index to allow it to only be invoked on a subtype of the main cache type.
 */
@ParametersAreNonnullByDefault
public class SubTypeIndexWrapper<C extends Identified<?>, S extends C> extends Index<C> {
    private final String name;
    private final Class<S> subType;
    private final Index<S> subTypeIndex;

    public SubTypeIndexWrapper(@CheckForNull String name, Class<S> subType, Index<S> subTypeIndex) {
        this.name = name != null ? name : "Subtype index of class: " + subType;
        this.subType = subType;
        this.subTypeIndex = subTypeIndex;
    }

    @Override
    protected void update(@CheckForNull C newObject, @CheckForNull C oldObject) throws IndexUpdateException {
        boolean doChangesMatch = doesChangeMatchSubType(newObject, oldObject);

        if (doChangesMatch) {
            subTypeIndex.update(subType.cast(newObject), subType.cast(oldObject));
        }
    }

    // We do some manual checking of types in the doChangesMatchSubType() function.
    // The static checks aren't clever enough to pick this up, but the cast to Change<S> is safe to do.
    @SuppressWarnings("unchecked")
    @Override
    protected void updateAll(Iterable<Change<C>> changes) throws IndexUpdateException {
        List<Change<S>> filteredChanges = new ArrayList<>();

        for (Change<C> change : changes) {
            boolean doChangesMatch = doesChangeMatchSubType(change.newObject, change.originalObject);

            if (doChangesMatch) {
                filteredChanges.add((Change<S>) change);
            }
        }

        if (!filteredChanges.isEmpty()) {
            subTypeIndex.updateAll(filteredChanges);
        }
    }

    private boolean doesChangeMatchSubType(@CheckForNull C newObject, @CheckForNull C oldObject) throws IndexUpdateException {
        // isInstance() does its own null checking and returns false for null objects.
        // This simplifies things a little as we don't have to do any explicit null checks here.
        boolean newMatches = subType.isInstance(newObject);
        boolean oldMatches = subType.isInstance(oldObject);

        // Only care about the old and new objects matching when they are both non-null
        if (newObject != null && oldObject != null && newMatches != oldMatches) {
            throw new IndexUpdateException(name, "Inconsistent types for old and new objects: old: %s new: %s", oldObject, newObject);
        }

        return newMatches || oldMatches;
    }
}
