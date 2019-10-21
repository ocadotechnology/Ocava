/*
 * Copyright © 2017-2019 Ocado (Ocava)
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

import javax.annotation.Nullable;

import com.ocadotechnology.id.Identified;

public abstract class AbstractIndex<C extends Identified<?>> extends Index<C> {

    @Override
    protected final void update(C newObject, C oldObject) {
        removeIfNotNull(oldObject);
        addIfNotNull(newObject);
        afterUpdate();
    }

    @Override
    protected final void updateAll(Iterable<Change<C>> changes) {
        changes.forEach(c -> removeIfNotNull(c.originalObject));
        changes.forEach(c -> addIfNotNull(c.newObject));
        afterUpdate();
    }

    private void removeIfNotNull(@Nullable C object) {
        if (object == null) {
            return;
        }
        remove(object);
    }

    private void addIfNotNull(@Nullable C object) {
        if (object == null) {
            return;
        }
        add(object);
    }

    protected abstract void remove(C object);

    protected abstract void add(C object);

    protected void afterUpdate() {
    }

}
