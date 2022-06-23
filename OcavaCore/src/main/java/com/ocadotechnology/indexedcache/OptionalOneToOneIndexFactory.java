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

import java.util.Optional;
import java.util.function.Function;

import javax.annotation.CheckForNull;

import com.ocadotechnology.id.Identified;
import com.ocadotechnology.indexedcache.IndexedImmutableObjectCache.Hints;

public class OptionalOneToOneIndexFactory {
    static <R, C extends Identified<?>> AbstractOptionalOneToOneIndex<R, C> newOptionalOneToOneIndex(@CheckForNull String name, Function<? super C, Optional<R>> function, Hints hint) {
        switch (hint) {
            case optimiseForUpdate:
                return new FastOptionalOneToOneIndex<>(name, function);
            case optimiseForQuery:
                return new DefaultOptionalOneToOneIndex<>(name, function);
            default:
                throw new UnsupportedOperationException("Missing case:" + hint);
        }
    }
}
