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

import java.util.function.Function;

import javax.annotation.CheckForNull;

import com.ocadotechnology.id.Identified;
import com.ocadotechnology.indexedcache.OneToOneIndex.WrappedIndexingFunction;

public final class OneToManyCountValue<R, C extends Identified<?>> extends AbstractIndex<C> {
    private final OptionalOneToManyCountValue<R, C> optionalOneToManyCountValue;

    OneToManyCountValue(@CheckForNull String name, Function<? super C, R> function) {
        super(name);
        this.optionalOneToManyCountValue = new OptionalOneToManyCountValue<>(name, new WrappedIndexingFunction<>(function, formattedName));
    }

    public long getValue(R r) {
        return optionalOneToManyCountValue.getValue(r);
    }

    @Override
    protected void add(C object) throws IndexUpdateException {
        optionalOneToManyCountValue.add(object);
    }

    @Override
    protected void remove(C object) throws IndexUpdateException {
        optionalOneToManyCountValue.remove(object);
    }
}
