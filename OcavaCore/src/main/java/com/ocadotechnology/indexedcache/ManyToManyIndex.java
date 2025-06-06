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

import java.util.stream.Stream;

import javax.annotation.CheckForNull;

import com.google.common.collect.ImmutableCollection;
import com.ocadotechnology.id.Identified;

/**
 * Abstract many-to-many index.
 */
public abstract class ManyToManyIndex<R, C extends Identified<?>> extends AbstractIndex<C> {
    ManyToManyIndex(@CheckForNull String name) {
        super(name);
    }

    public abstract Stream<C> stream(R r);

    public abstract Stream<R> streamKeySet();

    public abstract boolean containsKey(R r);

    public abstract int count(R r);

    public abstract int countKeys();

    public Stream<C> streamIncludingDuplicates(ImmutableCollection<R> keys) {
        return keys.stream().flatMap(this::stream);
    }
}
