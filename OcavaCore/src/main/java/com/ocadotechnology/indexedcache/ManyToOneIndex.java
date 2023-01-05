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
package com.ocadotechnology.indexedcache;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;

import com.ocadotechnology.id.Identified;

public class ManyToOneIndex<R, C extends Identified<?>> extends AbstractIndex<C> {
    private final Map<R, C> indexValues = new LinkedHashMap<>();
    private Function<? super C, Collection<R>> indexingFunction;

    public ManyToOneIndex(Function<? super C, Collection<R>> indexingFunction) {
        this(null, indexingFunction);
    }

    public ManyToOneIndex(@CheckForNull String name, Function<? super C, Collection<R>> indexingFunction) {
        super(name);
        this.indexingFunction = indexingFunction;
    }

    public boolean isEmpty() {
        return indexValues.isEmpty();
    }

    @Override
    protected void remove(C object) throws IndexUpdateException {
        Collection<R> objectIndexValues = indexingFunction.apply(object);
        if (objectIndexValues == null) {
            throw new IndexUpdateException(
                    getNameOrDefault(),
                    "Error updating %s: Removed object %s returned null index value collection",
                    formattedName,
                    object
            );
        }
        objectIndexValues.forEach(this.indexValues::remove);
    }

    @Override
    protected void add(C newObject) throws IndexUpdateException {
        Collection<R> objectIndexValues = indexingFunction.apply(newObject);
        if (objectIndexValues == null) {
            throw new IndexUpdateException(
                    getNameOrDefault(),
                    "Error updating %s: New object %s returned null index value collection",
                    formattedName,
                    newObject
            );
        }
        int added = 0;
        for (R r : objectIndexValues) {
            C oldObject = this.indexValues.put(r, newObject);
            if (oldObject == null) {
                ++added;
                continue;
            }
            this.indexValues.put(r, oldObject);
            rollback(objectIndexValues, added);
            throw new IndexUpdateException(
                    getNameOrDefault(),
                    "Error updating %s: New object %s blocked by old object %s for index value %s",
                    formattedName,
                    newObject,
                    oldObject,
                    r
            );
        }
    }

    private void rollback(Collection<R> indexValues, int added) {
        for (R r : indexValues) {
            if (added <= 0) {
                return;
            }
            indexValues.remove(r);
            --added;
        }
    }

    private String getNameOrDefault() {
        return name != null ? name : indexingFunction.getClass().getSimpleName();
    }

    public C getOrNull(R r) {
        return indexValues.get(r);
    }

    public boolean contains(R r) {
        return indexValues.containsKey(r);
    }

    public Optional<C> getOptionally(R r) {
        return Optional.ofNullable(indexValues.get(r));
    }

    public Stream<R> streamKeySet() {
        return indexValues.keySet().stream();
    }

    public Set<R> keySet() {
        return indexValues.keySet();
    }
}
