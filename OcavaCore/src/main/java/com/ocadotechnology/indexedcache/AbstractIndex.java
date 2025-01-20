/*
 * Copyright © 2017-2025 Ocado (Ocava)
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

import java.util.Optional;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import com.ocadotechnology.id.Identified;

public abstract class AbstractIndex<C extends Identified<?>> extends Index<C> {
    @CheckForNull
    protected final String name;
    protected final String formattedName;

    protected AbstractIndex() {
        this(null);
    }

    protected AbstractIndex(@CheckForNull String name) {
        this.name = name;
        this.formattedName = name != null
                ? String.format("%s[%s]", getClass().getSimpleName(), name)
                : getClass().getSimpleName();
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    @Override
    protected void update(C newObject, C oldObject) throws IndexUpdateException {
        boolean removed = false;
        boolean added = false;
        try {
            removeIfNotNull(oldObject);
            removed = true;
            addIfNotNull(newObject);
            added = true;
            afterUpdate();
        } catch (IndexUpdateException e) {
            rollbackUpdateAndThrow(oldObject, newObject, added, removed, e);
        }
    }

    private void rollbackUpdateAndThrow(C oldObject, C newObject, boolean added, boolean removed, IndexUpdateException cause) throws IndexUpdateException {
        try {
            if (added) {
                removeIfNotNull(newObject);
            }
            if (removed) {
                addIfNotNull(oldObject);
            }
        } catch (IndexUpdateException rollbackFailure) {
            throw new IllegalStateException("Failed to rollback after error: " + cause.getMessage(), rollbackFailure);
        }
        throw cause;
    }

    @Override
    protected final void updateAll(Iterable<Change<C>> changes) throws IndexUpdateException {
        int removed = 0;
        int added = 0;
        try {
            for (Change<C> change : changes) {
                removeIfNotNull(change.originalObject);
                removed++;
            }
            for (Change<C> change : changes) {
                addIfNotNull(change.newObject);
                added++;
            }
            afterUpdate();
        } catch (IndexUpdateException e) {
            rollbackUpdateAllAndThrow(changes, added, removed, e);
        }
    }

    private void rollbackUpdateAllAndThrow(Iterable<Change<C>> changes, int added, int removed, IndexUpdateException cause) throws IndexUpdateException {
        try {
            for (Change<C> change : changes) {
                if (added <= 0) {
                    break;
                }
                removeIfNotNull(change.newObject);
                added--;
            }
            for (Change<C> change : changes) {
                if (removed <= 0) {
                    break;
                }
                addIfNotNull(change.originalObject);
                removed--;
            }
        } catch (IndexUpdateException e) {
            throw new IllegalStateException("Failed to rollback after error: " + cause.getMessage(), e);
        }
        throw cause;
    }

    private void removeIfNotNull(@Nullable C object) throws IndexUpdateException {
        if (object == null) {
            return;
        }
        remove(object);
    }

    private void addIfNotNull(@Nullable C object) throws IndexUpdateException {
        if (object == null) {
            return;
        }
        add(object);
    }

    protected abstract void remove(C object) throws IndexUpdateException;

    protected abstract void add(C object) throws IndexUpdateException;

    protected void afterUpdate() throws IndexUpdateException {
    }
}
